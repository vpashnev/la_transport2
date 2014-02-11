package com.logisticsalliance.tp;

import java.io.Serializable;
import java.sql.Date;

public class ShipmentRow implements Serializable {
	private static final long serialVersionUID = 10L;

	public int storeN;
	public Date pollDate, shipDate, fsDcxShipDate, delDate;
	public String cmdty, route, stopN, crossDock, carrier, city, prov, postCode,
		pollTime, shipTime, delTimeFrom, delTimeTo;
	boolean fsDcx;

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(256);
		return b.toString();
	}
}
