package org.osm2world.core.target.custom_binary;

import static java.lang.Math.round;
import static org.osm2world.core.target.custom_binary.EntryType.TRIANGLES;
import static org.osm2world.core.target.custom_binary.EntryType.VECTOR3;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.PrimitiveTarget;
import org.osm2world.core.target.common.material.Material;

import com.google.common.io.LittleEndianDataOutputStream;

public class CustomBinaryTarget extends PrimitiveTarget<RenderableToAllTargets> {

	private final LittleEndianDataOutputStream outputStream;

	private final TObjectIntMap<VectorXYZ> vector3IndexMap = new TObjectIntHashMap<VectorXYZ>();
	private final List<VectorXYZ> unwrittenVector3s = new ArrayList<VectorXYZ>();

	public CustomBinaryTarget(LittleEndianDataOutputStream outputStream) {

		this.outputStream = outputStream;

	}

	@Override
	public Class<RenderableToAllTargets> getRenderableType() {
		return RenderableToAllTargets.class;
	}

	@Override
	public void render(RenderableToAllTargets renderable) {
		renderable.renderTo(this);
	}

	@Override
	protected void drawPrimitive(Type type, Material material,
			List<VectorXYZ> vs, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists) {

		try {

			//TODO check for maximum size of 256 indices per call

			if (type == Type.TRIANGLES) {

				int[] indices = vector3sToIndices(vs);

				writeVectors();

				writeTriangles(material, indices);

			}

		} catch (IOException e) {
			//TODO exception handling
		}

	}
	
	// The Guava Little Endian output stream does not work as intended when writing a byte array
	// So we need to output the byte array in Little Endian ourselves
	private static byte[] intTo24ByteArrayLE(int v) {
		return new byte[] {
				(byte)v,
				(byte)(v>>8),
				(byte)(v>>16)
		};
	}
	
	private static byte[] intTo16ByteArrayLE(int v) {
		return new byte[] {
				(byte)v,
				(byte)(v>>8)
		};
	}

	private int[] vector3sToIndices(List<? extends VectorXYZ> vs) {
		return vectorsToIndices(vector3IndexMap, unwrittenVector3s, vs);
	}

	private static <V> int[] vectorsToIndices(TObjectIntMap<V> indexMap,
			List<V> unwrittenList, List<? extends V> vectors) {

		int[] indices = new int[vectors.size()];

		for (int i=0; i<vectors.size(); i++) {

			final V v = vectors.get(i);

			int index;

			if (indexMap.containsKey(v)) {
				index = indexMap.get(v);
			} else {
				index = indexMap.size();
				unwrittenList.add(v);
				indexMap.put(v, index);
			}

			indices[i] = index;

		}

		return indices;

	}

	private void writeBlockHeader(EntryType e, int size) throws IOException {

		outputStream.writeByte(e.id);
		outputStream.writeByte(size);
		
//		offset += 2;

	}

	private void writeVectors() throws IOException {

		List<VectorXYZ> vectors = unwrittenVector3s;

		writeBlockHeader(VECTOR3, vectors.size());

		int x, y, z;
		
		for (VectorXYZ v : vectors) {
			x = (int)round(v.x*1000);
			y = (int)round(v.y*1000);
			z = (int)round(v.z*1000);
			
			// The Guava documentation is misleading here
			// Specifying an offset and length throws an IndexOutOfBoundsException
			outputStream.write(intTo24ByteArrayLE(x));
			outputStream.write(intTo24ByteArrayLE(y));
			outputStream.write(intTo24ByteArrayLE(z));
		}

		unwrittenVector3s.clear();

	}

	private void writeTriangles(Material material, int[] vertexIndices) throws IOException {

		writeBlockHeader(TRIANGLES, 1);

		outputStream.writeByte(material.ambientColor().getRed());
		outputStream.writeByte(material.ambientColor().getGreen());
		outputStream.writeByte(material.ambientColor().getBlue());
		outputStream.writeByte(material.diffuseColor().getRed());
		outputStream.writeByte(material.diffuseColor().getGreen());
		outputStream.writeByte(material.diffuseColor().getBlue());
		outputStream.writeByte(vertexIndices.length);

		for (int i=0; i < vertexIndices.length; i++) {
			outputStream.write(intTo16ByteArrayLE(vertexIndices[i]));
		}

	}

}
