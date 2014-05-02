package com.logisticsalliance.tp;

import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.DsKey;
import com.logisticsalliance.util.SupportTime;

public class ShipmentRow implements Serializable, Comparable<ShipmentRow> {
	private static final long serialVersionUID = 10L;

	public DsKey delKey = new DsKey();
	public double group = 0;
	public int polDay, shipDay, shipDay1, relCmdtyShipDay = -1, route,
		distance, stop, stop1, carrierN;
	public Date pollDate, shipDate, delDate;
	public String relCmdty, relCmdty1, stopN, carrier, carrier1,
		city, prov, postCode, polTime, shipTime, shipTime1, delTimeFrom, delTimeTo,
		localDc, carrierType, lhCarrier, lhService, delCarrier, delService, stagingLane,
		specInstructs, truckSize, maxTruckSize, trailerN, driverFName, arrivalTime,
		route1, evtFlag, nextUserFile, relNextUserFile;
	boolean relDcx, aRoutePerGroup, holidays, sameGroup, sameCar, active, missing;
	ShipmentRow rxRow;
	ArrayList<String> replacedRows = new ArrayList<String>(2);

	private static int toValue(int v, int v1) {
		return v1 == -1 ? v : v1;
	}
	private static String toValue(String v, String v1) {
		return v1 == null ? v : v1;
	}
	private static int getTime(String vs, int dif) {
		int v = Integer.parseInt(vs);
		v -= dif;
		if (v < 0) { v += 2400;}
		return v;
	}
	String getCsvRow1(int dc20, boolean dc50, boolean dc70) {
		StringBuilder b = new StringBuilder(512);
		String cmdty = delKey.getCommodity();
		String vs = toValue(String.valueOf(route), route1);
		if (missing && active) {
			vs = CommonConstants.ACTIVE;
		}
		b.append(vs); b.append(',');
		int stop2 = dc50 && CommonConstants.FS.equals(cmdty) ? 1 : stop;
		b.append(stop2); b.append(',');
		b.append(delKey.getStoreN()); b.append(',');
		b.append(",,,,");
		b.append(city); b.append(',');
		b.append(SupportTime.getDayOfWeek(shipDay)); b.append(",");
		boolean rx = cmdty.equals(CommonConstants.RX);
		if (rxRow != null || rx) {
			b.append(stop2); b.append(',');
			if (rxRow == null) {
				b.append(route1 == null ? route+100 : vs);
			}
			else {
				b.append(vs);
			}
		}
		else { b.append(',');}
		b.append(',');
		b.append(",");
		vs = toValue(carrier, carrier1);
		b.append(vs); b.append(',');
		b.append(prov); b.append(',');
		b.append(postCode); b.append(',');
		String polDay2 = SupportTime.getDayOfWeek(polDay);
		b.append(polDay2); b.append(',');
		b.append(polTime); b.append(',');
		b.append(",,,,,,");
		if (dc20 == 1) {
			b.append(SupportTime.getDayOfWeek(relCmdtyShipDay)); b.append(',');
		}
		else if (rx) {
			b.append(polDay2); b.append(',');
			b.append(polTime); 
		}
		else { b.append(',');}
		b.append(',');
		b.append(",,,,,,");
		int vi = toValue(shipDay, shipDay1);
		b.append(SupportTime.getDayOfWeek(vi)); b.append(',');
		vs = toValue(shipTime, shipTime1);
		vi = Integer.parseInt(vs);
		if (dc50 || dc70) {
			b.append(getTime(vs, 200));
		}
		else {
			b.append(getTime(vs, 100));
		}
		b.append(",");
		b.append(vs); b.append(',');
		b.append(",,,,,,,,,,,,,,,,,,,,,,,,,,");
		b.append(SupportTime.getDayOfWeek(delKey.getDay())); b.append(',');
		b.append(delTimeFrom); b.append(',');
		b.append(delTimeTo); b.append(',');
		b.append(",");
		add(b, truckSize);
		add(b, maxTruckSize);
		if (dc20 == 1) {
			b.append(localDc);
			if (specInstructs != null) { b.append("; ");}
		}
		add(b, specInstructs);
		b.append(",,");
		add(b, stagingLane);
		b.append(",,,,,,");
		add(b, trailerN);
		b.append(",,,,");
		add(b, driverFName);
		b.append(",,");
		add(b, evtFlag);
		b.append(",,,,");
		add(b, arrivalTime);
		b.append(",,,");
		if (distance != 0) { b.append(distance);}
		b.append(',');
		b.append(",,,,,,,, ,,,,,,,,,,,,,,,,,,,,,,,,,,,,");
		add(b, lhCarrier);
		b.append(",");
		add(b, delCarrier);
		b.append(",,");
		add(b, delService);
		add(b, lhService);
		b.append(",,,");
		b.append(' ');
		b.append(SupportTime.yyyy_MM_dd_Format.format(delDate)); b.append(',');
		if (!sameGroup && group != 0) {
			b.append(group);
		}
		b.append(',');
		if (holidays) { b.append('H');}
		b.append(',');
		add(b, carrierType);
		if (aRoutePerGroup) { b.append('1');}
		b.append(',');
		b.append('\r'); b.append('\n');

		return b.toString();
	}
	String getCsvRow(int dc20) {
		StringBuilder b = new StringBuilder(256);
		String cmdty = delKey.getCommodity();
		if (holidays) { b.append('H');}
		int sz = replacedRows.size();
		if (sz > 0) {
			b.append('R');
			if (sz > 1) { b.append(sz);}
		}
		if (relCmdtyShipDay == -1 && cmdty.equals(CommonConstants.DCX)) {
			b.append('N');
		}
		if (relNextUserFile != null && !relNextUserFile.equals(nextUserFile)) {
			b.append('D');
		}
		b.append(',');
		b.append(route); b.append(',');
		b.append(stop); b.append(',');
		b.append(delKey.getStoreN()); b.append(',');
		if (rxRow != null || cmdty.equals(CommonConstants.RX)) {
			if (rxRow == null) {
				b.append(route+100);
			}
			else { b.append(route);}
			b.append(',');
			b.append(stop);
		}
		else { b.append(',');}
		b.append(',');
		if (!sameGroup && group != 0) { b.append(group);} b.append(',');
		b.append(city); b.append(',');
		b.append(carrier); b.append(',');
		add(b, carrier1); b.append(',');
		b.append(prov); b.append(',');
		b.append(postCode); b.append(',');
		b.append(SupportTime.getDayOfWeek(polDay)); b.append(',');
		b.append(polTime); b.append(',');
		b.append(SupportTime.getDayOfWeek(shipDay)); b.append(',');
		b.append(shipTime); b.append(',');
		if (shipDay1 != -1) {
			b.append(SupportTime.getDayOfWeek(shipDay1));
		}
		b.append(',');
		if (shipTime1 != null) {
			b.append(shipTime1);
		}
		b.append(',');
		b.append(','); b.append(','); b.append(','); b.append(',');
		b.append(SupportTime.yyyy_MM_dd_Format.format(delDate)); b.append(',');
		b.append(SupportTime.getDayOfWeek(delKey.getDay())); b.append(',');
		b.append(delTimeFrom); b.append(',');
		b.append(delTimeTo); b.append(',');
		int ln = b.length();
		if (relDcx) { b.append('Y');} b.append(',');
		b.append(SupportTime.getDayOfWeek(relCmdtyShipDay)); b.append(',');
		add(b, lhCarrier);
		add(b, lhService);
		add(b, delCarrier);
		add(b, delService);
		add(b, stagingLane);
		if (dc20 == 1) {
			b.append(localDc);
			if (specInstructs != null) { b.append("; ");}
		}
		add(b, specInstructs);
		if (distance != 0) { b.append(distance);}
		b.append(',');
		add(b, truckSize);
		add(b, maxTruckSize);
		b.append(nextUserFile); b.append(',');
		if (b.length() != ln) {
			b.append(relNextUserFile == null ? "" : relNextUserFile);
		}
		b.append(',');
		if (sz > 0) { b.append(replacedRows);}
		b.append(','); b.append(carrierType);
		b.append('\r'); b.append('\n');
		return b.toString();
	}
	private static void add(StringBuilder b, String v) {
		if (v != null) {
			b.append(v);
		}
		b.append(',');
	}

	@Override
	public String toString() {
		return delKey.toString()+"\r\n";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ShipmentRow) {
			ShipmentRow r = (ShipmentRow)obj;
			return delKey.equals(r.delKey);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return delKey.hashCode();
	}

	@Override
	public int compareTo(ShipmentRow r) {
		if (equals(r)) {
			return 0;
		}
		int v = 0;
		String cmdty = delKey.getCommodity();
		v = cmdty.compareTo(r.delKey.getCommodity());
		if (missing) {
			return v;
		}
		v = carrierN - r.carrierN;
		if (v != 0) {
			return v;
		}
		boolean fs = cmdty.equals(CommonConstants.FS);
		if (fs) {
			v = compare(carrierType, r.carrierType);
			if (v == 0 && carrierType != null && r.carrierType != null) {
				v = carrierType.compareTo(r.carrierType);
			}
		}
		if (v == 0) {
			double v1 = group - r.group;
			if (v1 == 0) {
				v = compare(carrier, r.carrier);
				if (v == 0 && carrier != null && r.carrier != null) {
					v = carrier.compareTo(r.carrier);
				}
				if (v == 0) {
					v = route - r.route;
					if (v == 0) {
						if (stop1 == -1) {
							v = stop - r.stop;
						}
						else {
							v = stop1 - r.stop1;
						}
					}
				}
			}
			else {
				return v1 > 0 ? 1 : -1;
			}
		}
		return v;
	}
	private static int compare(String v1, String v2) {
		if (v1 == null && v2 != null) {
			return -1;
		}
		if (v1 != null && v2 == null) {
			return 1;
		}
		return 0;
	}
}
