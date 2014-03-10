package com.logisticsalliance.sa;

import java.io.Serializable;
import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.logisticsalliance.general.RnColumns;
import com.logisticsalliance.text.TBuilder;
import com.logisticsalliance.util.SupportTime;

public class TrackingNote implements Serializable {
	private static final long serialVersionUID = 10L;

	public int storeN, statusN;
	public Date shipDate, delDate, newDelDate, timestamp;
	public String delDate1, newDelDate1, arrivalTime, newArrivalTime, serviceTime, dc,
		route, stopN, carrier, delTimeFrom, delTimeTo;
	public HashMap<String,Alerts> cmdtyAlerts = new HashMap<String,Alerts>(4, .5f);

	void updateAlerts() {
		for (Iterator<Alerts> it = cmdtyAlerts.values().iterator(); it.hasNext();) {
			Alerts a = it.next();
			a.updateItems();
		}
	}
	private String getCmdtyAlerts() {
		StringBuilder sb = new StringBuilder(64);
		for (Iterator<Map.Entry<String,Alerts>> it = cmdtyAlerts.entrySet().iterator();
			it.hasNext();) {
			Map.Entry<String,Alerts> e = it.next();
			String k = e.getKey();
			Alerts v = e.getValue();
			sb.append(k);
			sb.append(' '); sb.append('-'); sb.append(' ');
			sb.append(v.pallets);
			sb.append('\r'); sb.append('\n');
			sb.append(v.toString());
		}
		sb.append('\r'); sb.append('\n');
		return sb.toString();
	}

	@Override
	public String toString() {
		TBuilder tb = new TBuilder();
		tb.newLine();
		tb.addProperty20(RnColumns.STORE_N, storeN, 6);
		tb.addProperty20("Shipment date", SupportTime.dd_MM_yyyy_Format.format(shipDate), 10);
		tb.addProperty20("Delivery date", delDate1, 10);
		tb.addProperty20(RnColumns.ARRIVAL_TIME, arrivalTime, 5);
		tb.addProperty20("New date", newDelDate1, 10);
		tb.addProperty20("New time", newArrivalTime, 5);
		tb.addProperty20(RnColumns.SERVICE_TIME, serviceTime, 5);
		tb.addProperty20(RnColumns.DC, dc, 8);
		tb.addProperty20(RnColumns.ROUTE_N, route, 4);
		tb.addProperty20(RnColumns.STOP_N, stopN, 4);
		tb.addProperty20("Delivery window", delTimeFrom+" - "+delTimeTo, 20);
		tb.addProperty20("Carrier", carrier, 32);
		tb.addProperty20("Timestamp", SupportTime.dd_MM_yyyy_Format.format(timestamp), 10);
		if (!arrivalTime.equals(newArrivalTime)) {
			tb.add("ETA update");
			tb.newLine();
		}
		if (!delDate.equals(timestamp)) {
			tb.add("outdated");
			tb.newLine();
		}
		tb.add(getCmdtyAlerts());
		return tb.toString();
	}
}
