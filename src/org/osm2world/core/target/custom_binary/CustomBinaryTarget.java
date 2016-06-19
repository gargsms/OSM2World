package org.osm2world.core.target.custom_binary;

import static java.lang.Math.round;
import static org.osm2world.core.target.custom_binary.EntryType.TRIANGLES;
import static org.osm2world.core.target.custom_binary.EntryType.VECTOR3;
import static org.osm2world.core.target.custom_binary.EntryType.TRIANGLE_STRIP;
import static org.osm2world.core.target.custom_binary.EntryType.TRIANGLE_FAN;
import static org.osm2world.core.target.custom_binary.EntryType.CONVEX_POLYGON;
import static org.osm2world.core.target.custom_binary.EntryType.TREE;
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

			// The maximum count limit of 255 is handled by the write functions

			List<Integer> indices = vector3sToIndices(vs);

			writeVectors(unwrittenVector3s);

			writePrimitive(type, material, indices);

			unwrittenVector3s.clear();

		} catch (IOException e) {
			//TODO exception handling
		}

	}

	public void drawPrimitiveTree(VectorXYZ pos, double height, boolean coniferous) {

		try {
			writePrimitiveTree(pos, height, coniferous);
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

	private List<Integer> vector3sToIndices(List<? extends VectorXYZ> vs) {
		return vectorsToIndices(vector3IndexMap, unwrittenVector3s, vs);
	}

	private static <V> List<Integer> vectorsToIndices(TObjectIntMap<V> indexMap,
			List<V> unwrittenList, List<? extends V> vectors) {

		List<Integer> indices = new ArrayList<Integer>();

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

			indices.add(i, index);

		}

		return indices;

	}

	private void writeBlockHeader(EntryType e, int size) throws IOException {

		outputStream.writeByte(e.id);
		outputStream.writeByte(size);

	}

	private void writeVectors(List<VectorXYZ> vectors) throws IOException {

		List<VectorXYZ> current, remaining = null;

		// We can only have a block of 255 vertices at once
		if( vectors.size() > 255 ) {
			current = vectors.subList(0, 255);
			remaining = vectors.subList(255, vectors.size());
		} else {
			current = vectors;
		}

		writeBlockHeader(VECTOR3, current.size());

		int x, y, z;
		
		for (VectorXYZ v : current) {
			x = (int)round(v.x*1000);
			y = (int)round(v.y*1000);
			z = (int)round(v.z*1000);
			
			// The Guava documentation is misleading here
			// Specifying an offset and length throws an IndexOutOfBoundsException
			outputStream.write(intTo24ByteArrayLE(x));
			outputStream.write(intTo24ByteArrayLE(y));
			outputStream.write(intTo24ByteArrayLE(z));
		}

		if(remaining != null) {
			writeVectors(remaining);
		}

	}

	private void writePrimitive(Type type, Material material, List<Integer> vertexIndices) throws IOException {

		List<Integer> current, remaining = null;

		if(vertexIndices.size() > 255) {
			current = vertexIndices.subList(0, 255);
			remaining = vertexIndices.subList(255, vertexIndices.size());
		} else {
			current = vertexIndices;
		}

		//TODO optimize this later. We are wasting 5 bytes per recursive call
		//1 for type, 1 for count, and 3 for the Colour info
		switch (type) {
		case TRIANGLES:
			writeBlockHeader(TRIANGLES, 1);
			break;
		case TRIANGLE_STRIP:
			writeBlockHeader(TRIANGLE_STRIP, 1);
			break;
		case TRIANGLE_FAN:
			writeBlockHeader(TRIANGLE_FAN, 1);
			break;
		case CONVEX_POLYGON:
			writeBlockHeader(CONVEX_POLYGON, 1);
			break;
		default:
			System.out.println(type);
		}

		// The client does not support ambient material colors, so we don't write them in the tiles
		// outputStream.writeByte(material.ambientColor().getRed());
		// outputStream.writeByte(material.ambientColor().getGreen());
		// outputStream.writeByte(material.ambientColor().getBlue());
		outputStream.writeByte(material.diffuseColor().getRed());
		outputStream.writeByte(material.diffuseColor().getGreen());
		outputStream.writeByte(material.diffuseColor().getBlue());

		// We need to remove degenerate primitives
		// This is absolutely useless information
		if(type == Type.TRIANGLES) {
			outputStream.writeByte(current.size());

			for (int i=0; i < current.size(); i++) {
				outputStream.write(intTo16ByteArrayLE(current.get(i)));
			}
		} else if(type == Type.TRIANGLE_STRIP){
			int size = current.size();
			int a, b, c;
			List<Integer> goodIndices = new ArrayList<Integer>();
			boolean skip = false;

			for(int i = 0; i < size - 2; i++) {
				a = current.get(i);
				b = current.get(i+1);
				c = current.get(i+2);
				if(a==b || b==c || c==a) {
					skip = false;
					continue;
				} else {
					if(skip) {
						goodIndices.add(c);
					} else {
						goodIndices.add(a);
						goodIndices.add(b);
						goodIndices.add(c);
						skip = true;
					}
				}
			}

			size = goodIndices.size();
			outputStream.writeByte(size);

			for(int i = 0; i < size; i++) {
				outputStream.write(intTo16ByteArrayLE(goodIndices.get(i)));
			}

		} else if(type == Type.TRIANGLE_FAN || type == Type.CONVEX_POLYGON) {
			int size = current.size();
			int a = current.get(0), b, c;
			List<Integer> goodIndices = new ArrayList<Integer>();
			goodIndices.add(a);
			boolean skip = false;

			for(int i = 1; i < size - 1; i++) {
				b = current.get(i);
				c = current.get(i+1);
				if(a==b || b==c || c==a) {
					skip = false;
					continue;
				} else {
					if(skip) {
						goodIndices.add(c);
					} else {
						goodIndices.add(b);
						goodIndices.add(c);
						skip = true;
					}
				}
			}

			size = goodIndices.size();
			outputStream.writeByte(size);

			for(int i = 0; i < size; i++) {
				outputStream.write(intTo16ByteArrayLE(goodIndices.get(i)));
			}
		}

		if(remaining != null) {
			writePrimitive(type, material, remaining);
		}

	}

	private void writePrimitiveTree(VectorXYZ pos, double height, boolean coniferous) throws IOException {

		outputStream.writeByte(TREE.id);
		outputStream.writeByte(coniferous ? 1 : 0);
		outputStream.write(intTo24ByteArrayLE((int)round(pos.x*1000)));
		outputStream.write(intTo24ByteArrayLE((int)round(pos.y*1000)));
		outputStream.write(intTo24ByteArrayLE((int)round(pos.z*1000)));
		outputStream.write(intTo16ByteArrayLE((int)round(height*1000)));

	}

}
