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
	public int polDay, shipDay, relCmdtyShipDay = -1, route, stop;
	public Date pollDate, shipDate, delDate;
	public String relCmdty, stopN, group, carrier,
		city, prov, postCode, polTime, shipTime, delTimeFrom, delTimeTo,
		localDc, carrierType, nextUserFile, relNextUserFile;
	boolean relDcx, aRoutePerGroup, holidays, sameGroup, sameCar, samePC;
	ArrayList<String> replacedRows = new ArrayList<String>(2);

	String getRow() {
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
		if (!sameGroup) { b.append(group);} b.append(',');
		b.append(city); b.append(',');
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
		b.append(SupportTime.getDayOfWeek(shipDay)); b.append(',');
		b.append(shipTime); b.append(',');
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
		b.append(nextUserFile); b.append(',');
		if (b.length() != ln) {
			b.append(relNextUserFile == null ? "" : relNextUserFile);
		}
		b.append(',');
		if (sz > 0) { b.append(replacedRows);}
		b.append('\r'); b.append('\n');
		return b.toString();
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
		v = compare(group, r.group);
		if (v == 0) {
			if (group != null && r.group != null) {
				v = group.compareTo(r.group);
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
						else if (cmdty.equals(CommonConstants.FS)) {
							v = shipDay - r.shipDay;
							if (v == 0) {
								v = compare(carrierType, r.carrierType);
								if (carrierType != null && r.carrierType != null) {
									v = carrierType.compareTo(r.carrierType);
								}
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
