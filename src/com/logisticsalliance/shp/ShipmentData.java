package com.logisticsalliance.shp;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;

import com.logisticsalliance.general.RnColumns;
import com.logisticsalliance.general.ShipmentDataDb;
import com.logisticsalliance.text.TBuilder;
import com.logisticsalliance.util.SupportTime;

/**
 * This class represents source data for shipment. It is formed from lines in roadnet.up file with the
 * same store #, commodity, and shipment date. It contains the list of ShipmentItems taken from the lines
 * of roadnet.up file for the same store #, commodity, and shipment date. It has the delivery date and
 * the properties related to carrier.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class ShipmentData implements Serializable {
	private static final long serialVersionUID = 10L;

	int storeN, prevDistance;
	Date shipDate, delDate;
	Time dcDepartTime, prevTravelTime, arrivalTime, serviceTime, totalServiceTime, totalTravelTime;
	String cmdty, routeN, stopN, dc, ordN, addKey, equipSize, lhCarrier, lhService,
		delCarrier, delService, hub, specInstructs, firstUserFile, nextUserFile;
	ArrayList<ShipmentItem> items = new ArrayList<ShipmentItem>(8);

	double getTotalPallets() {
		double v = 0;
		for (Iterator<ShipmentItem> it = items.iterator(); it.hasNext();) {
			v += it.next().pallets;
		}
		return Math.ceil(v);
	}
	double getTotalUnits(String lw) {
		double v = 0;
		for (Iterator<ShipmentItem> it = items.iterator(); it.hasNext();) {
			ShipmentItem ro = it.next();
			if (lw != null && !lw.equals(lw)) { continue;}
			v += ro.units;
		}
		return Math.ceil(v);
	}
	double getTotalCube(boolean feets) {
		double v = 0;
		for (Iterator<ShipmentItem> it = items.iterator(); it.hasNext();) {
			v += it.next().cube;
		}
		return feets ? v*35.315/1000 : v/1000;
	}
	double getTotalWeight(boolean pounds) {
		double v = 0;
		for (Iterator<ShipmentItem> it = items.iterator(); it.hasNext();) {
			v += it.next().weight;
		}
		return pounds ? v*2.2046 : v;
	}
	/**
	 * Returns the string representation of this object properties
	 */
	@Override
	public String toString() {
		TBuilder tb = new TBuilder();
		tb.addProperty20(RnColumns.STORE_N, storeN, 6);
		tb.addProperty20(RnColumns.COMMODITY, cmdty, 4);
		tb.addProperty20(RnColumns.SHIP_DATE, SupportTime.dd_MM_yyyy_Format.format(shipDate), 10);
		tb.addProperty20(RnColumns.DC, dc, 2);
		tb.addProperty20(RnColumns.ROUTE_N, routeN, 6);
		tb.addProperty20(RnColumns.STOP_N, stopN, 3);

		tb.addProperty20(RnColumns.DC_DEPART_TIME, SupportTime.HH_mm_Format.format(dcDepartTime), 5);
		tb.addProperty20(RnColumns.PREV_DISTANCE, prevDistance, 10);
		tb.addProperty20(RnColumns.PREV_TRAVEL_TIME, SupportTime.HH_mm_Format.format(prevTravelTime), 5);
		tb.addProperty20(RnColumns.ARRIVAL_TIME, SupportTime.HH_mm_Format.format(arrivalTime), 5);
		tb.addProperty20(RnColumns.SERVICE_TIME, SupportTime.HH_mm_Format.format(serviceTime), 5);
		tb.addProperty20(RnColumns.TOTAL_SERVICE_TIME, SupportTime.HH_mm_Format.format(totalServiceTime), 5);
		tb.addProperty20(RnColumns.TOTAL_TRAVEL_TIME, SupportTime.HH_mm_Format.format(totalTravelTime), 5);
		tb.addProperty20(RnColumns.EQUIP_SIZE, equipSize, 15);

		tb.addProperty20("Delivery Date", SupportTime.dd_MM_yyyy_Format.format(delDate), 10);
		tb.addProperty20("specInstructs", specInstructs, 4);
		tb.addProperty20("lhCarrier", lhCarrier, 12);
		tb.addProperty20("lhService", lhService, 12);
		tb.addProperty20("delCarrier", delCarrier, 12);
		tb.addProperty20("delService", delService, 12);
		tb.addProperty20("hub", hub, 12);
		tb.addProperty20("User files", firstUserFile+(nextUserFile == null ? "":
			", modified "+nextUserFile), 80);
		tb.add('_', 112);
		tb.newLine();
		tb.addCell(RnColumns.ORDER_N, 20, false, false);
		tb.addCell(RnColumns.LW, 4, false, false);
		tb.addCell(RnColumns.PALLETS, 8, true, false);
		tb.addCell(RnColumns.UNITS, 8, true, false);
		tb.addCell(RnColumns.WEIGHT, 12, true, false);
		tb.addCell(RnColumns.CUBE, 14, true, true);
		tb.newLine();
		tb.add('_', 112);
		tb.newLine();
		DecimalFormat sizeFormat = ShipmentDataDb.sizeFormat;
		for (Iterator<ShipmentItem> it = items.iterator(); it.hasNext();) {
			ShipmentItem e = it.next();
			tb.addCell(e.orderN, 20, false, false);
			tb.addCell(e.lw, 4, false, false);
			tb.addCell(e.pallets, 8, sizeFormat, false);
			tb.addCell(e.units, 8, sizeFormat, false);
			tb.addCell(e.weight, 12, sizeFormat, false);
			tb.addCell(e.cube, 14, sizeFormat, false);
			tb.addCell(e.firstUserFile+(e.nextUserFile == null ? "":
				", modified "+e.nextUserFile), 80, false, true);
			tb.newLine();
		}
		tb.add('_', 112);
		tb.newLine();
		tb.addCell("Total:", 26, false, false);
		tb.addCell(getTotalPallets(), 8, sizeFormat, false);
		tb.addCell(getTotalUnits(null), 8, sizeFormat, false);
		tb.addCell(getTotalWeight(false), 12, sizeFormat, false);
		tb.addCell(getTotalCube(false), 14, sizeFormat, true);
		tb.newLine();
		tb.addCell("lb(kg*2.2046),ft3(m3*35.315)", 46, false, false);
		tb.addCell(getTotalWeight(true), 12, sizeFormat, false);
		tb.addCell(getTotalCube(true), 14, sizeFormat, true);

		tb.newLine(); tb.newLine();
		return tb.toString();
	}

}
