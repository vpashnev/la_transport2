package com.logisticsalliance.sa;

import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.logisticsalliance.general.RnColumns;
import com.logisticsalliance.text.TBuilder;
import com.logisticsalliance.util.SupportTime;

public class TrackingNote implements Serializable {
	private static final long serialVersionUID = 10L;

	public int storeN, pallets;
	public Date shipDate, delDate, newDelDate, timestamp;
	public String arrivalTime, newArrivalTime, serviceTime, dc, route, stopN,
		carrier, cmdtyList, delTimeFrom, delTimeTo;
	boolean exception;
	HashMap<String,Integer> cmdtyPallets = new HashMap<String,Integer>(4, .5f);
	public ArrayList<AlertItem> items = new ArrayList<AlertItem>(2);

	int getTotalPallets() {
		double v = 0;
		for (Iterator<Integer> it = cmdtyPallets.values().iterator(); it.hasNext();) {
			Integer p = it.next();
			v += p;
		}
		return (int)Math.ceil(v);
	}
	static String getCmdtyList(Set<String> set) {
		StringBuilder sb = new StringBuilder(32);
		ArrayList<String> al = new ArrayList<String>(set);
		Collections.sort(al);
		for (Iterator<String> it = al.iterator(); it.hasNext();) {
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
		tb.addProperty20(RnColumns.SERVICE_TIME, serviceTime, 5);
		tb.addProperty20(RnColumns.DC, dc, 8);
		tb.addProperty20(RnColumns.ROUTE_N, route, 4);
		tb.addProperty20(RnColumns.STOP_N, stopN, 4);
		tb.addProperty20(RnColumns.COMMODITY, cmdtyList, 64);
		tb.addProperty20(RnColumns.PALLETS, pallets, 6);
		tb.addProperty20("Delivery window", delTimeFrom+" - "+delTimeTo, 20);
		tb.addProperty20("Carrier", carrier, 32);
		tb.addProperty20("Timestamp", SupportTime.dd_MM_yyyy_Format.format(timestamp), 10);
		if (!delDate.equals(timestamp)) {
			tb.add("outdated");
			tb.newLine();
		}
		tb.add(getItems());
		return tb.toString();
	}
}
