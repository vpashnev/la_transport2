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

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.DsKey;
import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.util.SupportTime;

class FillGridDB {

	static String
	SQL_SEL =
	"SELECT " +
	"sc.dc, s.cmdty, s.store_n, sc.group1, sp.city, sc.carrier_id, sp.province,\r\n" +
	"sp.post_code, s.pol_day, s.pol_time, s.ship_date, s.ship_day, s.ship_time, s1.cmdty,\r\n" +
	"s1.ship_day, s.del_date, s.del_day, s.del_week, s.del_time_from, s.del_time_to,\r\n" +
	"sp.local_dc, sc.carrier_type, sc.aroute_per_group, s.next_user_file,\r\n" +
	"s1.next_user_file\r\n" +

	"FROM\r\n" +
	"la.hstore_schedule s " +
	"INNER JOIN la.hstore_profile sp ON s.store_n=sp.n\r\n" +
	"LEFT JOIN la.hholidays h ON s.description=h.name\r\n" +
	"LEFT JOIN la.hstore_carrier sc ON sc.store_n=s.store_n AND sc.dc=? AND " +
	"(sc.cmdty=s.cmdty OR ((s.cmdty='DCB' OR s.cmdty='DCV') AND sc.cmdty='FS')) AND " +
	"(sc.ship_day IS NULL OR sc.ship_day=s.ship_day) AND\r\n",

	SQL_SEL_REG1 = "sc.holidays IS NULL",
	SQL_SEL_HOL1 = "(sc.holidays IS NULL OR sc.holidays=h.week_day)",

	SQL_SEL1 = "\r\nLEFT JOIN la.hstore_schedule s1 ON s.store_n=s1.store_n AND\r\n" +
	//"s.cmdty<>s1.cmdty AND s.cmdty<>'DCF' AND s1.cmdty<>'DCF' AND\r\n" +
	"((s.cmdty='DCB' OR s.cmdty='DCV') AND s1.cmdty='DCX' OR\r\n" +
	"(s1.cmdty='DCB' OR s1.cmdty='DCV') AND s.cmdty='DCX') AND\r\n",

	SQL_SEL_REG2 = "s.del_day=s1.del_day",
	SQL_SEL_HOL2 = "s.del_date=s1.del_date",

	SQL_SEL2 = " AND s.del_time_from=s1.del_time_from\r\n" +
	"WHERE\r\n s.dc=? AND s.cmdty IN (\r\n",

	SQL_SHP_REG = "\r\n) AND s.ship_date IS NULL AND s1.ship_date IS NULL AND " +
		"s.ship_day=?\r\n",
	SQL_SHP_HOL = "\r\n) AND s.ship_date IS NOT NULL AND s1.ship_date IS NOT NULL AND " +
		"s.ship_date=?\r\n",

	SQL_DEL_REG = "\r\n) AND s.ship_date IS NULL AND s1.ship_date IS NULL AND " +
		"s.del_day=?\r\n",
	SQL_DEL_HOL = "\r\n) AND s.ship_date IS NOT NULL AND s1.ship_date IS NOT NULL AND " +
		"s.del_date=?\r\n",
			
	SQL_SEL3 =" ORDER BY s.cmdty,s.ship_date,sc.group1,sc.carrier_id,sc.holidays";

	static ConnectFactory connectFactory;

	private static String getSql(boolean hol, boolean del, String cmdty) {
		StringBuilder b = new StringBuilder(512);
		b.append(SQL_SEL);
		if (hol) {
			b.append(SQL_SEL_HOL1); b.append(SQL_SEL1);
			b.append(SQL_SEL_HOL2); b.append(SQL_SEL2);
			b.append(cmdty);
			if (del) {
				b.append(SQL_DEL_HOL);
			}
			else {
				b.append(SQL_SHP_HOL);
			}
		}
		else {
			b.append(SQL_SEL_REG1); b.append(SQL_SEL1);
			b.append(SQL_SEL_REG2); b.append(SQL_SEL2);
			b.append(cmdty);
			if (del) {
				b.append(SQL_DEL_REG);
			}
			else {
				b.append(SQL_SHP_REG);
			}
		}
		b.append(SQL_SEL3);
		return b.toString();
	}
	private static ResultSet getRowSet(Connection con, SearchInput si,
		int idx, boolean holidays) throws Exception {
		PreparedStatement st;
		String cmdty = si.getCmdty();
		if (si.delDates) {
			st = con.prepareStatement(getSql(holidays, true, cmdty));
		}
		else {
			st = con.prepareStatement(getSql(holidays, false, cmdty));
		}
		st.setString(1, si.dc);
		st.setString(2, SearchInput.toDc(si.dc));
		if (holidays) {
			Date d = new Date(si.fromDate.getTime() + SupportTime.DAY*idx);
			st.setDate(3, d);
		}
		else { st.setInt(3, si.fromDay+idx);}
		return st.executeQuery();
	}
	static HashMap<String,ArrayList<ShipmentRow>> process(SearchInput si,
		int idx) throws Exception {
		HashMap<String,HashMap<ShipmentRow,ShipmentRow>> m =
			new HashMap<String,HashMap<ShipmentRow,ShipmentRow>>(8, .5f);
		Connection con = connectFactory.getConnection();
		ResultSet rs = getRowSet(con, si, idx, false); // regular
		process(si, idx, false, rs, m);
		rs = getRowSet(con, si, idx, true); // holidays
		process(si, idx, true, rs, m);
		HashMap<String,ArrayList<ShipmentRow>> m1 =
			new HashMap<String,ArrayList<ShipmentRow>>(8, .5f);
		for (Iterator<Map.Entry<String,HashMap<ShipmentRow,ShipmentRow>>> it =
			m.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String,HashMap<ShipmentRow,ShipmentRow>> e = it.next();
			HashMap<ShipmentRow,ShipmentRow> v = e.getValue();
			ArrayList<ShipmentRow> al = new ArrayList<ShipmentRow>(v.values());
			Collections.sort(al);
			m1.put(e.getKey(), al);
		}
		return m1;
	}
	private static void process(SearchInput si, int idx, boolean holidays, ResultSet rs,
		HashMap<String,HashMap<ShipmentRow,ShipmentRow>> m) throws Exception {
		while (rs.next()) {
			String dc = rs.getString(1);
			if (!si.dc.equals(CommonConstants.DC10) && !si.dc.equals(dc)) {
				continue;
			}
			ShipmentRow r = new ShipmentRow();
			r.shipDay = rs.getInt(12);
			r.delKey.setDay(rs.getInt(17));
			if (holidays) {
				r.shipDate = rs.getDate(11);
				r.delDate = rs.getDate(16);
				r.holidays = true;
			}
			else {
				int delWeek = rs.getInt(18);
				setDates(si, idx, r, delWeek);
				if (si.holidays && r.delDate.compareTo(si.toDate) <= 0) {
					continue;
				}
			}

			String cmdty = DsKey.toCmdty(rs.getString(2));
			r.delKey.setCommodity(cmdty);
			r.delKey.setStoreN(rs.getInt(3));
			r.group = rs.getString(4);
			r.city = rs.getString(5);
			r.carrier = rs.getString(6);
			r.prov = rs.getString(7);
			r.postCode = rs.getString(8);
			r.polDay = rs.getInt(9);
			r.polTime = SupportTime.HHmm_Format.format(rs.getTime(10));
			r.shipTime = SupportTime.HHmm_Format.format(rs.getTime(13));
			r.relCmdty = DsKey.toCmdty(rs.getString(14));
			if (r.relCmdty != null) {
				if (CommonConstants.FS.equals(cmdty) &&
					CommonConstants.DCX.equals(r.relCmdty)) {
					r.relDcx = true;
				}
				else if (CommonConstants.FS.equals(r.relCmdty) &&
					CommonConstants.DCX.equals(cmdty)) {
					r.relCmdtyShipDay = rs.getInt(15);
				}
			}
			r.delTimeFrom = SupportTime.HHmm_Format.format(rs.getTime(19));
			r.delTimeTo = SupportTime.HHmm_Format.format(rs.getTime(20));
			r.localDc = rs.getString(21);
			r.carrierType = rs.getString(22);
			r.aRoutePerGroup = rs.getString(23) != null;
			r.nextUserFile = rs.getString(24);
			r.relNextUserFile = rs.getString(25);
			HashMap<ShipmentRow,ShipmentRow> m1 = m.get(cmdty);
			if (m1 == null) {
				m1 = new HashMap<ShipmentRow,ShipmentRow>(512);
				m.put(cmdty, m1);
			}
			ShipmentRow r1 = m1.get(r);
			if (r1 != null) {
				r.replacedRows.add(r1.nextUserFile);
			}
			m1.put(r, r);
		}
	}
	private static void setDates(SearchInput si, int idx,
		ShipmentRow r, int delWeek) {
		Date d = new Date(si.fromDate.getTime() + SupportTime.DAY*idx);
	    int delDays = r.delKey.getDay() - r.shipDay;
	    if (delDays < 0) {
	    	delDays += 7;
	    }
	    delDays += delWeek*7;
		if (si.delDates) {
			r.shipDate = new Date(d.getTime() - SupportTime.DAY*delDays);
			r.delDate = d;
		}
		else {
			r.shipDate = d;
			r.delDate = new Date(d.getTime() + SupportTime.DAY*delDays);
		}
	}
}
