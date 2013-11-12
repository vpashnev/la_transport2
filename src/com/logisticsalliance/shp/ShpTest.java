package com.logisticsalliance.shp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.logisticsalliance.general.RnColumns;
import com.logisticsalliance.text.TBuilder;

class ShpTest {

	private static String SQL_SEL_SHP2 = "SELECT " +
	"I1ORD#,I1SKD,I1VOL,I1WGT,I1PCS,I1SHP$,I1ORD$,I1SHPI,t21.I2CAR,t21.I2SRV,t21.I2HUB2,t21.I2DSTP," +
	"t22.I2CAR,t22.I2SRV,t22.I2DSTP " +
	"FROM OS61LYDTA.OSPIFC1 " +
	"LEFT JOIN OS61LYDTA.OSPIFC2 t21 ON t21.I2PIKD=? AND I1ORD#=t21.I2ORD# AND t21.I2SEQN=1 " +
	"LEFT JOIN OS61LYDTA.OSPIFC2 t22 ON t22.I2PIKD=? AND I1ORD#=t22.I2ORD# AND t22.I2SEQN=2 " +
	"WHERE I1FPKD=?";

	static String test(Connection con, int date,
		ArrayList<ShipmentData> al) throws Exception {
		StringBuilder b = new StringBuilder();
		TBuilder tb = new TBuilder();
		HashMap<String,ShipmentData> m = getMap(al);
		PreparedStatement st = con.prepareStatement(SQL_SEL_SHP2);
		st.setInt(1, date);
		st.setInt(2, date);
		st.setInt(3, date);
		ResultSet rs = st.executeQuery();
		while (rs.next()) {
			boolean dif = false;
			String ordN = rs.getString(1).trim();
			tb.newLine();
			tb.addProperty20(RnColumns.ORDER_N, ordN, 20);
			ShipmentData sd = m.get(ordN);
			if (sd == null) {
				tb.add("Missing shipment\r\n");
				dif = true;
			}
			else {
				double v = rs.getDouble(2), v1 = sd.getTotalPallets();
				if (Math.abs(v - v1) > 1) {
					tb.addProperty20(RnColumns.PALLETS, v+" - "+v1, 20);
					dif = true;
				}
				v = rs.getDouble(3); v1 = sd.getTotalCube(true);
				if (Math.abs(v - v1) > 2) {
					tb.addProperty20(RnColumns.CUBE, v+" - "+v1, 20);
					dif = true;
				}
				v = rs.getDouble(4); v1 = sd.getTotalWeight(true);
				if (Math.abs(v - v1) > 2) {
					tb.addProperty20(RnColumns.WEIGHT, v+" - "+v1, 20);
					dif = true;
				}
				v = rs.getDouble(5); v1 = sd.getTotalUnits(null);
				if (v != 0 && v != v1) {
					tb.addProperty20(RnColumns.UNITS, v+" - "+v1, 20);
					dif = true;
				}
				v = rs.getDouble(6); v1 = sd.prevDistance;
				if (v != v1) {
					tb.addProperty20(RnColumns.PREV_DISTANCE, v+" - "+v1, 20);
					dif = true;
				}
				int t1 = Functions.toMins(sd.serviceTime);
				Time t = new Time(sd.prevTravelTime.getTime()+t1*60000);
				v = rs.getDouble(7); v1 = Functions.toDouble(t);
				if (v != v1) {
					tb.addProperty20(RnColumns.PREV_TRAVEL_TIME, v+" - "+v1, 20);
					dif = true;
				}
				String spi = rs.getString(8).trim(),
					spi1 = sd.specInstructs == null ? "" : sd.specInstructs;
				if (!spi.equals(spi1)) {
					tb.addProperty20("specInstructs", spi+" - "+spi1, 20);
					dif = true;
				}
				String car1 = rs.getString(9), srv1 = rs.getString(10), hub = rs.getString(11),
					car2 = rs.getString(13), srv2 = rs.getString(14);
				int stop1 = rs.getInt(12), stop2 = rs.getInt(15), stop = Integer.parseInt(sd.stopN)+1;
				if (car1 != null) {
					car1 = car1.trim(); srv1 = srv1.trim(); hub = hub.trim();
					if (car2 == null) {
						car2 = car1.trim(); srv2 = srv1.trim();
						if (!car1.equals(sd.delCarrier)) {
							tb.addProperty20("delCarrier", car1+" - "+sd.delCarrier, 20);
							dif = true;
						}
						if (!srv1.equals(sd.delService)) {
							tb.addProperty20("delService", srv1+" - "+sd.delService, 20);
							dif = true;
						}
						if (stop1 != stop) {
							tb.addProperty20(RnColumns.STOP_N, stop1+" - "+stop, 20);
							dif = true;
						}
					}
					else {
						if (!car1.equals(sd.lhCarrier)) {
							tb.addProperty20("lhCarrier", car1+" - "+sd.lhCarrier, 20);
							dif = true;
						}
						if (!srv1.equals(sd.lhService)) {
							tb.addProperty20("lhService", srv1+" - "+sd.lhService, 20);
							dif = true;
						}
						if (!hub.equals(sd.hub)) {
							tb.addProperty20("hub", hub+" - "+sd.hub, 20);
							dif = true;
						}
						if (!car2.equals(sd.delCarrier)) {
							tb.addProperty20("delCarrier", car2+" - "+sd.delCarrier, 20);
							dif = true;
						}
						if (!srv2.equals(sd.delService)) {
							tb.addProperty20("delService", srv2+" - "+sd.delService, 20);
							dif = true;
						}
						if (stop2 != stop) {
							tb.addProperty20(RnColumns.STOP_N, stop2+" - "+stop, 20);
							dif = true;
						}
					}
				}
				//Functions.putRefs(new ArrayList<Ref>(), sd, 1);
			}
			if (dif) {
				b.append(tb.clear());
				dif = false;
			}
			else { tb.clear();}
		}
		return b.toString();
	}
	private static HashMap<String,ShipmentData> getMap(ArrayList<ShipmentData> al) {
		HashMap<String,ShipmentData> m = new HashMap<String,ShipmentData>(al.size(), .5f);
		for (Iterator<ShipmentData> it = al.iterator(); it.hasNext();) {
			ShipmentData sd = it.next();
			m.put(sd.ordN, sd);
		}
		return m;
	}
}
