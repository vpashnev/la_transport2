package com.logisticsalliance.general;

import java.io.Serializable;

import com.logisticsalliance.util.SupportTime;

/**
 * This class represents a key object for delivery schedule. It has three properties: store #,
 * commodity, and shipment/delivery day.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class DsKey implements Serializable {
	private static final long serialVersionUID = 10L;

	private int storeN, day;
	private String commodity;

	public DsKey() { }

	public DsKey(int storeN, String commodity, int day) {
		this.storeN = storeN;
		this.commodity = commodity;
		this.day = day;
	}
	public int getStoreN() {
		return storeN;
	}
	public void setStoreN(int v) {
		storeN = v;
	}
	public String getCommodity() {
		return commodity;
	}
	public void setCommodity(String v) {
		commodity = v;
	}

	public int getDay() {
		return day;
	}

	public void setDay(int v) {
		day = v;
	}

	public static String toCmdty(String v) {
		if (v == null) { return null;}
		return v.equals(CommonConstants.DCB) || v.equals(CommonConstants.DCV) ?
			CommonConstants.FS : v;
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
	 * properties (store #, commodity, and shipment/delivery day) are equal.
	 * @param obj the object to compare with the current one
	 * @return {@code true} if the DsKey objects equal; otherwise {@code false}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DsKey) {
			DsKey k = (DsKey)obj;
			return storeN == k.storeN && commodity.equals(k.commodity) && day == k.day;
		}
		return false;
	}
	/**
	 * Returns the string representation of this object properties
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(64);
		b.append(storeN);
		b.append(','); b.append(' ');
		b.append(commodity);
		b.append(','); b.append(' ');
		b.append(SupportTime.getDayOfWeek(day));
		return b.toString();
	}

}
