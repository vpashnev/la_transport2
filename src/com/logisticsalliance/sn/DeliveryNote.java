package com.logisticsalliance.sn;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;

import com.logisticsalliance.general.RnColumns;
import com.logisticsalliance.general.ShipmentDataDb;
import com.logisticsalliance.text.TBuilder;
import com.logisticsalliance.util.SupportTime;

/**
 * This class represents a delivery that should be sent to store by email.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class DeliveryNote implements Serializable {
	private static final long serialVersionUID = 10L;

	static final String N_A = "n/a";

	int storeN, totalPallets;
	Date shipDate, delDate;
	Time arrivalTime, serviceTime, delTimeFrom, delTimeTo, targetOpen;
	String id, routeN, addKey, province, delCarrier, firstUserFile, nextUserFile;
	ArrayList<Cmdty> cmdtyList = new ArrayList<Cmdty>(4);
	ArrayList<DeliveryItem> items = new ArrayList<DeliveryItem>(32);

	String getCmdtyList(boolean totalPallets) {
		StringBuilder sb = new StringBuilder(32);
		for (Iterator<Cmdty> it = cmdtyList.iterator(); it.hasNext();) {
			Cmdty c = it.next();
			if (totalPallets) {
				sb.append(c.cmdty);
				sb.append('=');
				sb.append(getTotalPallets(c.cmdty));
			}
			else {
				String v = addKey.isEmpty() ? c.cmdty : addKey;
				sb.append(v);
				if (v.equals("EVT")) {
					sb.append('2');
				}
			}
			sb.append(','); sb.append(' ');
		}
		return sb.toString();
	}
	int getTotalPallets(String cmdty) {
		double v = 0;
		for (Iterator<DeliveryItem> it = items.iterator(); it.hasNext();) {
			DeliveryItem e = it.next();
			if (cmdty != null && !cmdty.equals(e.cmdty)) {
				continue;
			}
			v += e.pallets;
		}
		return (int)Math.ceil(v);
	}

	@Override
	public String toString() {
		TBuilder tb = new TBuilder();
		tb.newLine();
		tb.add(id);
		tb.newLine();
		tb.addProperty20(RnColumns.STORE_N, storeN, 6);
		tb.addProperty20("Province", province, 4);
		tb.addProperty20(RnColumns.COMMODITY, getCmdtyList(true), 64);
		tb.addProperty20("Shipment date", SupportTime.dd_MM_yyyy_Format.format(shipDate), 10);
		tb.addProperty20("Delivery date", SupportTime.dd_MM_yyyy_Format.format(delDate), 10);
		tb.addProperty20(RnColumns.ROUTE_N, routeN, 4);
		tb.addProperty20(RnColumns.ARRIVAL_TIME, arrivalTime == null ? N_A :
			SupportTime.HH_mm_Format.format(arrivalTime), 5);
		tb.addProperty20(RnColumns.SERVICE_TIME, serviceTime == null ? N_A :
			SupportTime.HH_mm_Format.format(serviceTime), 5);
		tb.addProperty20("Delivery window", SupportTime.HH_mm_Format.format(delTimeFrom)+" - "+
			SupportTime.HH_mm_Format.format(delTimeTo), 20);
		tb.addProperty20("Delivery carrier", delCarrier, 32);
		tb.addProperty20("Target open", targetOpen == null  ?
			N_A : SupportTime.HH_mm_Format.format(targetOpen), 8);
		tb.addProperty20("User files", firstUserFile+(nextUserFile == null ? "":
			", modified "+nextUserFile), 80);
		tb.add('_', 120);
		tb.newLine();
		tb.addCell(RnColumns.ORDER_N, 26, false, false);
		tb.addCell(RnColumns.PALLETS, 8, true, false);
		tb.addCell("User files", 14, false, true);
		tb.newLine();
		tb.add('_', 120);
		tb.newLine();
		for (Iterator<DeliveryItem> it = items.iterator(); it.hasNext();) {
			DeliveryItem e = it.next();
			tb.addCell(e.orderN, 20, false, false);
			tb.addCell(e.cmdty, 4, false, false);
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
		tb.addCell(totalPallets == 0 ? getTotalPallets(null) : totalPallets, 8, true, true);
		tb.newLine();

		tb.newLine(); tb.newLine();
		return tb.toString();
	}

	static class Cmdty {
		String cmdty;
		Date dsShipDate;
		Time arrivalTime, serviceTime;
		Cmdty(String cmdty, Date dsShipDate, Time arrivalTime, Time serviceTime) {
			this.cmdty = cmdty;
			this.dsShipDate = dsShipDate;
			this.arrivalTime = arrivalTime;
			this.serviceTime = serviceTime;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Cmdty) {
				Cmdty c = (Cmdty)obj;
				return c.cmdty.equals(cmdty);
			}
			return false;
		}
		@Override
		public int hashCode() {
			return cmdty.hashCode();
		}
	}
}

