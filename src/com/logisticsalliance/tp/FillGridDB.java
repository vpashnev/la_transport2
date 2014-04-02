package com.logisticsalliance.tp;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.glossium.sqla.ConnectFactory;
import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.DsKey;
import com.logisticsalliance.util.SupportTime;

class FillGridDB {

	static String
	SQL_SEL =
	"SELECT " +
	"sc.dc, s.cmdty, s.store_n, sc.group1, sp.city, sc.carrier_id, sc.carrier1,\r\n" +
	"sp.province, sp.post_code, s.pol_day, s.pol_time, s.ship_date, s.ship_day,\r\n" +
	"s.ship_time, sc.ship_day1, sc.ship_time1, s1.cmdty, s1.ship_day, s.del_date,\r\n" +
	"s.del_day, s.del_week, s.del_time_from, s.del_time_to, sc.lh_carrier_id,\r\n" +
	"sc.lh_service, sc.del_carrier_id, sc.del_service, sc.staging_lane, sc.spec_instructs,\r\n" +
	"sc.distance, sc.max_truck_size, sc.truck_size, sc.stop1, sp.local_dc, sc.carrier_type,\r\n" +
	"sc.aroute_per_group, sfr.dc, s.next_user_file, s1.next_user_file\r\n" +

	"FROM\r\n" +
	"la.hstore_schedule s " +
	"INNER JOIN la.hstore_profile sp ON s.store_n=sp.n AND (upper(sp.status)='OPEN' OR " +
	"upper(sp.status)='ACTIVE')\r\n" +
	"LEFT JOIN la.hholidays h ON s.description=h.name\r\n" +
	"LEFT JOIN la.hstore_carrier sc ON sc.store_n=s.store_n AND (sc.cmdty=s.cmdty OR\r\n" +
	"((s.cmdty='DCB' OR s.cmdty='DCV') AND sc.cmdty='FS')) AND\r\n" +
	"(sc.ship_day IS NULL OR sc.ship_day=s.ship_day) AND\r\n",

	SQL_SEL_REG1 = "sc.holidays IS NULL",
	SQL_SEL_HOL1 = "(sc.holidays=h.week_day)",

	SQL_SEL1 = "\r\nLEFT JOIN la.hstore_fs_rx sfr ON " +
	"s.store_n=sfr.store_n AND sfr.dc=sc.dc\r\n" +
	"\r\nLEFT JOIN la.hstore_schedule s1 ON s.store_n=s1.store_n AND\r\n" +
	//"s.cmdty<>s1.cmdty AND s.cmdty<>'DCF' AND s1.cmdty<>'DCF' AND\r\n" +
	"((s.cmdty='DCB' OR s.cmdty='DCV') AND s1.cmdty='DCX' OR\r\n" +
	"(s1.cmdty='DCB' OR s1.cmdty='DCV') AND s.cmdty='DCX') AND\r\n",

	SQL_SEL_REG2 = "s1.ship_date IS NULL AND s.del_day=s1.del_day",
	SQL_SEL_HOL2 = "s1.ship_date IS NOT NULL AND s.del_date=s1.del_date",

	SQL_SEL2 = " AND s.del_time_from=s1.del_time_from\r\n" +
	"WHERE\r\n" +
	"s.store_n<9000 AND s.cmdty IN (",

	SQL_SHP_REG = "\r\n) AND s.ship_date IS NULL AND s.ship_day=?\r\n",
	SQL_SHP_HOL = "\r\n) AND s.ship_date IS NOT NULL AND s.ship_date=?\r\n",

	SQL_SEL3 =" ORDER BY s.cmdty,s.ship_date,sc.group1,sc.carrier_id,sc.holidays";

	static ConnectFactory connectFactory;
	private static DsKey searchKey = new DsKey();

	private static String getSql(boolean hol, boolean dc20, String cmdty) {
		StringBuilder b = new StringBuilder(512);
		b.append(SQL_SEL);
		if (hol) {
			b.append(SQL_SEL_HOL1);
			if (dc20) {
				b.append(" AND sc.dc=s.dc");
			}
			else { b.append(" AND sc.dc=?");};
			b.append(SQL_SEL1);
			b.append(SQL_SEL_HOL2); b.append(SQL_SEL2);
			b.append(cmdty);
			b.append(SQL_SHP_HOL);
		}
		else {
			b.append(SQL_SEL_REG1);
			if (!dc20) {
				b.append(" AND sc.dc=?");
			}
			b.append(SQL_SEL1);
			b.append(SQL_SEL_REG2); b.append(SQL_SEL2);
			b.append(cmdty);
			b.append(SQL_SHP_REG);
		}
		if (!dc20) {
			b.append(" AND s.dc=?\r\n");
		}
		b.append(SQL_SEL3);
		return b.toString();
	}
	private static ResultSet getRowSet(Connection con, SearchInput si,
		int idx, boolean holidays, boolean dc20) throws Exception {
		PreparedStatement st;
		String cmdty = si.getCmdty();
		st = con.prepareStatement(getSql(holidays, dc20, cmdty));
		int i = 1;
		if (!dc20) { st.setString(i++, si.dc);}
		if (holidays) {
			Date d = new Date(si.fromDate.getTime() + SupportTime.DAY*idx);
			st.setDate(i++, d);
		}
		else { st.setInt(i++, si.fromDay+idx);}
		if (!dc20) { st.setString(i, SearchInput.toDc(si.dc));}
		return st.executeQuery();
	}
	static HashMap<String,ArrayList<ShipmentRow>> process(SearchInput si,
		int idx, boolean dc20, boolean dc50) throws Exception {
		HashMap<String,HashMap<DsKey,ShipmentRow>> m =
			new HashMap<String,HashMap<DsKey,ShipmentRow>>(8, .5f);
		Connection con = connectFactory.getConnection();
		ResultSet rs = getRowSet(con, si, idx, false, dc20); // regular
		process(si, idx, false, dc20, dc50, rs, m);
		rs = getRowSet(con, si, idx, true, dc20); // holidays
		process(si, idx, true, dc20, dc50, rs, m);
		HashMap<String,ArrayList<ShipmentRow>> m1 =
			new HashMap<String,ArrayList<ShipmentRow>>(8, .5f);
		for (Iterator<Map.Entry<String,HashMap<DsKey,ShipmentRow>>> it =
			m.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String,HashMap<DsKey,ShipmentRow>> e = it.next();
			HashMap<DsKey,ShipmentRow> v = e.getValue();
			ArrayList<ShipmentRow> al = new ArrayList<ShipmentRow>(v.values());
			Collections.sort(al);
			m1.put(e.getKey(), al);
		}
		return m1;
	}
	private static void process(SearchInput si, int idx, boolean holidays,
		boolean dc20, boolean dc50, ResultSet rs, HashMap<String,HashMap<DsKey,ShipmentRow>> m)
		throws Exception {
		Date holidayEnd = si.holidayWeeks > 0 ?
			new Date(si.fromDate.getTime()+7*si.holidayWeeks*SupportTime.DAY) : null; 
		while (rs.next()) {
			String dc = rs.getString(1);
			String cmdty = rs.getString(2);
			if (ignore(si.dc, cmdty)) {
				continue;
			}
			cmdty = DsKey.toCmdty(cmdty);
			if (dc20) {
				if (!cmdty.equals(CommonConstants.DCX) && !si.dc.equals(dc)) {
					continue;
				}
			}
			else {
				if (cmdty.equals(CommonConstants.DCX)) {
					continue;
				}
				if (!si.dc.equals(CommonConstants.DC10) && !si.dc.equals(dc)) {
					continue;
				}
			}
			ShipmentRow r = new ShipmentRow();
			r.shipDay = rs.getInt(13);
			r.shipDay1 = getInt(rs.getObject(15));
			r.delKey.setDay(rs.getInt(20));
			if (holidays) {
				r.shipDate = rs.getDate(12);
				r.delDate = rs.getDate(19);
				r.holidays = true;
			}
			else {
				int delWeek = rs.getInt(21);
				setDates(si, idx, r, delWeek);
				if (holidayEnd != null && r.delDate.compareTo(holidayEnd) < 0) {
					continue;
				}
			}
			HashMap<DsKey,ShipmentRow> m1 = m.get(cmdty);
			if (m1 == null) {
				m1 = new HashMap<DsKey,ShipmentRow>(512, .5f);
				m.put(cmdty, m1);
			}
			r.delKey.setCommodity(cmdty);
			r.delKey.setStoreN(rs.getInt(3));
			r.nextUserFile = rs.getString(38);
			r.relNextUserFile = rs.getString(39);
			ShipmentRow r1 = m1.get(r.delKey);
			if (r1 != null) {
				r.replacedRows.add(r1.nextUserFile);
			}
			m1.put(r.delKey, r);
			r.delTimeFrom = SupportTime.HHmm_Format.format(rs.getTime(22));
			r.delTimeTo = SupportTime.HHmm_Format.format(rs.getTime(23));
			boolean rxToFs = rs.getString(37) != null;
			if (rxToFs && r.delKey.getStoreN()==6060) {
				//System.out.println(idx+", "+r.delKey.getStoreN()+", "+r.delKey.getDay()+", "+cmdty);
			}
			setRx(r, cmdty, rxToFs, m);
			r.group = rs.getString(4);
			r.city = rs.getString(5);
			r.carrier = rs.getString(6);
			r.carrier1 = rs.getString(7);
			r.prov = rs.getString(8);
			r.postCode = rs.getString(9);
			r.polDay = rs.getInt(10);
			r.polTime = SupportTime.HHmm_Format.format(rs.getTime(11));
			r.shipTime = SupportTime.HHmm_Format.format(rs.getTime(14));
			r.shipTime1 = rs.getString(16);
			r.relCmdty = DsKey.toCmdty(rs.getString(17));
			if (r.relCmdty != null) {
				if (CommonConstants.FS.equals(cmdty) &&
					CommonConstants.DCX.equals(r.relCmdty)) {
					r.relDcx = true;
				}
				else if (CommonConstants.FS.equals(r.relCmdty) &&
					CommonConstants.DCX.equals(cmdty)) {
					r.relCmdtyShipDay = rs.getInt(18);
				}
			}
			r.lhCarrier = rs.getString(24);
			r.lhService = rs.getString(25);
			r.delCarrier = rs.getString(26);
			r.delService = rs.getString(27);
			r.stagingLane = rs.getString(28);
			r.specInstructs = rs.getString(29);
			r.distance = rs.getInt(30);
			r.truckSize = rs.getString(31);
			r.maxTruckSize = rs.getString(32);
			if (dc50) {
				r.stop1 = getInt(rs.getObject(33));
				if (r.stop1 == -1) { r.stop1 = 0;}
				r.stop = r.stop1;
			}
			r.localDc = rs.getString(34);
			r.carrierType = rs.getString(35);
			r.aRoutePerGroup = rs.getString(36) != null;
		}
	}
	private static boolean ignore(String dc, String cmdty) {
		switch (dc) {
		case CommonConstants.DC10:
		case CommonConstants.DC30:
		case CommonConstants.DC50:
			if (cmdty.equals(CommonConstants.DCB)) {
				return true;
			}
			break;
		case CommonConstants.DC20:
		case CommonConstants.DC70:
			if (cmdty.equals(CommonConstants.DCV)) {
				return true;
			}
		}
		return false;
	}
	private static int getInt(Object v) {
		if (v == null) {
			return -1;
		}
		else { return (Integer)v;}
	}
	private static void setDates(SearchInput si, int idx,
		ShipmentRow r, int delWeek) {
		Date d = new Date(si.fromDate.getTime() + SupportTime.DAY*idx);
	    int delDays = r.delKey.getDay() - r.shipDay;
	    if (delDays < 0) {
	    	delDays += 7;
	    }
	    delDays += delWeek*7;
		r.shipDate = d;
		r.delDate = new Date(d.getTime() + SupportTime.DAY*delDays);
	}
	private static void setRx(ShipmentRow r, String cmdty, boolean toFs,
		HashMap<String,HashMap<DsKey,ShipmentRow>> m) {
		int storeN = r.delKey.getStoreN();
		searchKey.setStoreN(storeN);
		searchKey.setDay(r.delKey.getDay());
		if (CommonConstants.FS.equals(cmdty)) {
			searchKey.setCommodity(CommonConstants.RX);
			HashMap<DsKey,ShipmentRow> m1 = m.get(CommonConstants.RX);
			if (m1 != null) {
				ShipmentRow r1 = m1.get(searchKey);
				if (r1 != null && (r1.delTimeFrom.equals(r.delTimeFrom) || toFs)) {
					m1.remove(searchKey);
					r.rxRow = r1;
				}
			}
		}
		else if (CommonConstants.RX.equals(cmdty)) {
			searchKey.setCommodity(CommonConstants.FS);
			HashMap<DsKey,ShipmentRow> m1 = m.get(CommonConstants.FS);
			if (m1 != null) {
				ShipmentRow r1 = m1.get(searchKey);
				if (r1 != null && (r1.delTimeFrom.equals(r.delTimeFrom) || toFs)) {
					m1 = m.get(CommonConstants.RX);
					m1.remove(r.delKey);
					r1.rxRow = r;
				}
			}
		}
	}
	
}
