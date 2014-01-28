package com.logisticsalliance.tt;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;

import com.logisticsalliance.general.RnColumns;
import com.logisticsalliance.general.ShipmentDataDb;
import com.logisticsalliance.shp.OrderItem;
import com.logisticsalliance.text.TBuilder;
import com.logisticsalliance.util.SupportTime;

class DeliveryRow implements Serializable {
	private static final long serialVersionUID = 10L;

	boolean missing;
	int storeN, pallets;
	String cmdty, dc, routeN, stopN, delCarrier, firstUserFile, nextUserFile;
	Time arrivalTime, serviceTime, delTimeFrom, delTimeTo;
	Date delDate;
	ArrayList<OrderItem> items = new ArrayList<OrderItem>(8);

	int getTotalPallets() {
		double v = 0;
		for (Iterator<OrderItem> it = items.iterator(); it.hasNext();) {
			OrderItem e = it.next();
			v += e.pallets;
		}
		return (int)Math.ceil(v);
	}
	
	@Override
	public String toString() {
		TBuilder tb = new TBuilder();
		tb.newLine();
		tb.addProperty20(RnColumns.STORE_N, storeN, 6);
		if (missing) {
			tb.addProperty20("Missing", "true", 4);
		}
		tb.addProperty20(RnColumns.COMMODITY, cmdty, 8);
		tb.addProperty20("Delivery date", SupportTime.dd_MM_yyyy_Format.format(delDate), 10);
		tb.addProperty20(RnColumns.DC, dc, 2);
		tb.addProperty20(RnColumns.ROUTE_N, routeN, 4);
		tb.addProperty20(RnColumns.STOP_N, stopN, 4);
		tb.addProperty20(RnColumns.ARRIVAL_TIME, SupportTime.HH_mm_Format.format(arrivalTime), 5);
		tb.addProperty20(RnColumns.SERVICE_TIME, SupportTime.HH_mm_Format.format(serviceTime), 5);
		tb.addProperty20("Delivery window", SupportTime.HH_mm_Format.format(delTimeFrom)+" - "+
			SupportTime.HH_mm_Format.format(delTimeTo), 20);
		tb.addProperty20("Delivery carrier", delCarrier, 32);
		tb.addProperty20("User files", firstUserFile+(nextUserFile == null ? "":
			", modified "+nextUserFile), 80);
		tb.add('_', 120);
		tb.newLine();
		tb.addCell(RnColumns.ORDER_N, 26, false, false);
		tb.addCell(RnColumns.LW, 4, false, false);
		tb.addCell(RnColumns.PALLETS, 8, true, false);
		tb.addCell("User files", 14, false, true);
		tb.newLine();
		tb.add('_', 120);
		tb.newLine();
		for (Iterator<OrderItem> it = items.iterator(); it.hasNext();) {
			OrderItem e = it.next();
			tb.addCell(e.orderN, 20, false, false);
			tb.addCell(e.lw, 4, false, false);
			tb.addCell(e.pallets, 8, ShipmentDataDb.sizeFormat, false);
			tb.addCell(e.firstUserFile+(e.nextUserFile == null ? "":
				", modified "+e.nextUserFile), 80, false, true);
			tb.add(' ', 4);
			tb.addCell(e.dsFirstUserFile+(e.dsNextUserFile == null ? "":
				",modified "+e.dsNextUserFile), 112, false, true);
			tb.newLine();
		}
		tb.add('_', 120);
		tb.newLine();
		tb.addCell("Total:", 26, false, false);
		tb.addCell(pallets, 8, true, true);
		tb.newLine(); tb.newLine();
		return tb.toString();
	}
}
