package com.logisticsalliance.general;

/**
 * This class inherits properties from DsKey adding DC one.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class ShipKey extends DsKey {
	private static final long serialVersionUID = 10L;

	private String dc, addKey = "";

	public ShipKey() { }

	public ShipKey(int storeN, String commodity, int day, String dc, String addKey) {
		super(storeN, commodity, day);
		this.dc = dc;
		this.addKey = addKey;
	}
	public String getDc() {
		return dc;
	}

	public void setDc(String v) {
		dc = v;
	}

	public String getAddKey() {
		return addKey;
	}

	public void setAddKey(String v) {
		addKey = v;
	}

	/**
	 * Returns the hash code of the string representation of this object as
	 * {@code toString().hashCode()}
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	/**
	 * Compares this object with the specified one {@code obj}. The two objects are equal if their
	 * properties are equal.
	 * @param obj the object to compare with the current one
	 * @return {@code true} if the ShipKey objects equal; otherwise {@code false}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ShipKey) {
			ShipKey k = (ShipKey)obj;
			return super.equals(k) && dc.equals(k.dc) && addKey.equals(k.addKey);
		}
		return false;
	}
	/**
	 * Returns the string representation of this object properties
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(super.toString());
		b.append(','); b.append(' ');
		b.append(dc);
		b.append(','); b.append(' ');
		b.append(addKey);
		return b.toString();
	}

}
