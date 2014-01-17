package com.logisticsalliance.sa;

import java.io.Serializable;
import java.sql.Date;
import java.util.HashSet;
import java.util.Iterator;

import com.logisticsalliance.general.RnColumns;
import com.logisticsalliance.text.TBuilder;
import com.logisticsalliance.util.SupportTime;

class AlertNote implements Serializable {
	private static final long serialVersionUID = 10L;

	int storeN;
	Date shipDate, delDate, newDelDate, timestamp;
	String arrivalTime, newArrivalTime, route, carrier, cmdtyList;
	boolean exception;
	HashSet<String> cmdtySet = new HashSet<String>(2, .5f);
	HashSet<AlertItem> items = new HashSet<AlertItem>(2, .5f);

	String getCmdtyList() {
		StringBuilder sb = new StringBuilder(32);
		for (Iterator<String> it = cmdtySet.iterator(); it.hasNext();) {
			String v = it.next();
			if (sb.length() != 0) {
				sb.append(','); sb.append(' ');
			}
			sb.append(v);
		}
		return sb.toString();
	}
	String getItems() {
		StringBuilder sb = new StringBuilder(64);
		for (Iterator<AlertItem> it = items.iterator(); it.hasNext();) {
			AlertItem v = it.next();
			sb.append(v.toString());
			sb.append('\r'); sb.append('\n');
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		TBuilder tb = new TBuilder();
		tb.newLine();
		tb.addProperty20(RnColumns.STORE_N, storeN, 6);
		tb.addProperty20("Shipment date", SupportTime.dd_MM_yyyy_Format.format(shipDate), 10);
		tb.addProperty20("Delivery date", SupportTime.dd_MM_yyyy_Format.format(delDate), 10);
		tb.addProperty20(RnColumns.ARRIVAL_TIME, arrivalTime, 5);
		tb.addProperty20("New date", SupportTime.dd_MM_yyyy_Format.format(newDelDate), 10);
		tb.addProperty20("New time", newArrivalTime, 5);
		tb.addProperty20(RnColumns.ROUTE_N, route, 8);
		tb.addProperty20("Carrier", carrier, 32);
		tb.addProperty20(RnColumns.COMMODITY, cmdtyList, 64);
		tb.addProperty20("Timestamp", SupportTime.dd_MM_yyyy_Format.format(timestamp), 10);
		if (!delDate.equals(timestamp)) {
			tb.add("outdated");
			tb.newLine();
		}
		tb.add(getItems());
		return tb.toString();
	}
}
