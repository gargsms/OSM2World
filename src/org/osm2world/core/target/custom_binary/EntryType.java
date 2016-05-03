package org.osm2world.core.target.custom_binary;

public enum EntryType {

	VECTOR3((short)3),
	TRIANGLES((short)11);

	public final short id;

	private EntryType(short id) {
		this.id = id;
	}

}
