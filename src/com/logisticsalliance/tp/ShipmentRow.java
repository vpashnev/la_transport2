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
	public int group, polDay, shipDay, shipDay1, relCmdtyShipDay = -1, route,
		distance, stop, stop1 = -1;
	public Date pollDate, shipDate, delDate;
	public String relCmdty, stopN, carrier, carrier1,
		city, prov, postCode, polTime, shipTime, shipTime1, delTimeFrom, delTimeTo,
		localDc, carrierType, lhCarrier, lhService, delCarrier, delService, stagingLane,
		specInstructs, truckSize, maxTruckSize, nextUserFile, relNextUserFile;
	boolean relDcx, aRoutePerGroup, holidays, sameGroup, sameCar, samePC;
	ShipmentRow rxRow;
	ArrayList<String> replacedRows = new ArrayList<String>(2);

	String getCsvRow1() {
		StringBuilder b = new StringBuilder(200);
		String cmdty = delKey.getCommodity();
		b.append(route); b.append(',');
		b.append(stop); b.append(',');
		b.append(delKey.getStoreN()); b.append(',');
		b.append(",,,,");
		b.append(city); b.append(',');
		b.append(",");
		if (rxRow != null || cmdty.equals(CommonConstants.RX)) {
			b.append(stop);
			b.append(',');
			if (rxRow == null) {
				b.append(route+100);
			}
			else { b.append(route);}
		}
		else { b.append(',');}
		b.append(',');
		b.append(",");
		b.append(carrier); b.append(',');
		if (samePC) {
			b.append(',');
		}
		else {
			b.append(prov); b.append(','); b.append(postCode);
		}
		b.append(',');
		b.append(SupportTime.getDayOfWeek(polDay)); b.append(',');
		b.append(polTime); b.append(',');
		b.append(",,,,,,,,,,,,,,");
		b.append(SupportTime.getDayOfWeek(shipDay)); b.append(',');
		b.append(",");
		b.append(shipTime); b.append(',');
		b.append(",,,,,,,,,,,,,,,,,,,,,,,,,,");
		b.append(SupportTime.getDayOfWeek(delKey.getDay())); b.append(',');
		b.append(delTimeFrom); b.append(',');
		b.append(delTimeTo); b.append(',');
		b.append(",");
		add(b, truckSize);
		add(b, maxTruckSize);
		add(b, specInstructs);
		b.append(",,,,,,,,,,");
		add(b, stagingLane);
		b.append(",,,,,,,,,,,,,,,");
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
		b.append(SupportTime.yyyy_MM_dd_Format.format(delDate)); b.append(',');
		if (!sameGroup && group != 0) {
			b.append(group);
		}
		b.append(',');
		b.append('\r'); b.append('\n');

		return b.toString();
	}
	String getCsvRow(boolean dc50) {
		StringBuilder b = new StringBuilder(200);
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
		if (!sameGroup) { b.append(group);} b.append(',');
		b.append(city); b.append(',');
		b.append(carrier); b.append(',');
		add(b, carrier1);
		if (samePC) {
			b.append(',');
		}
		else {
			b.append(prov); b.append(','); b.append(postCode);
		}
		b.append(',');
		b.append(SupportTime.getDayOfWeek(polDay)); b.append(',');
		b.append(polTime); b.append(',');
		b.append(SupportTime.getDayOfWeek(shipDay)); b.append(',');
		b.append(shipTime); b.append(',');
		if (dc50) {
			if (shipDay1 != -1) {
				b.append(SupportTime.getDayOfWeek(shipDay1));
			}
			b.append(',');
			if (shipTime1 != null) {
				b.append(shipTime1);
			}
			b.append(',');
		}
		b.append(','); b.append(','); b.append(','); b.append(',');
		b.append(SupportTime.MMM_dd_yy_Format.format(delDate)); b.append(',');
		b.append(SupportTime.getDayOfWeek(delKey.getDay())); b.append(',');
		b.append(delTimeFrom); b.append(',');
		b.append(delTimeTo); b.append(',');
		int ln = b.length();
		if (relDcx) { b.append('Y');} b.append(',');
		if (relCmdtyShipDay != -1) {
			b.append(SupportTime.getDayOfWeek(relCmdtyShipDay));
		}
		b.append(',');
		if (dc50) {
			add(b, lhCarrier);
			add(b, lhService);
			add(b, delCarrier);
			add(b, delService);
			add(b, stagingLane);
			add(b, specInstructs);
		}
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
		v = group - r.group;
		if (v == 0) {
			boolean fs = cmdty.equals(CommonConstants.FS);
			if (fs) {
				if (v == 0) {
					v = compare(carrierType, r.carrierType);
					if (carrierType != null && r.carrierType != null) {
						v = carrierType.compareTo(r.carrierType);
					}
				}
			}
			if (v == 0) {
				v = compare(carrier, r.carrier);
				if (v == 0) {
					if (carrier != null && r.carrier != null) {
						v = carrier.compareTo(r.carrier);
					}
					if (v == 0) {
						if (cmdty.equals(CommonConstants.DCX)) {
							v = relCmdtyShipDay - r.relCmdtyShipDay;
							if (v == 0) {
								v = localDc.compareTo(r.localDc);
								if (v == 0) {
									v = postCode.compareTo(r.postCode);
								}
							}
						}
						else if (fs && stop1 == -1) {
							if (v == 0) {
								v = rxRow != null && r.rxRow == null ? -1 :
									(rxRow == null && r.rxRow != null ? 1 : 0);
								if (v == 0) {
									v = postCode.compareTo(r.postCode);
								}
							}
						}
						else {
							v = route - r.route;
						}
						if (v == 0) {
							v = stop - r.stop;
						}
					}
				}
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
