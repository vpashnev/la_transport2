package com.logisticsalliance.tp;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;

import com.glossium.sqla.ConnectFactory;
import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.DsKey;
import com.logisticsalliance.util.SupportTime;

class FillGridDB {

	static final String
	SQL_SEL =
	"SELECT " +
	"sc.dc, s.cmdty, s.store_n, sc.group1, sp.city, sc.carrier_id, sc.carrier1,\r\n" +
	"sp.province, sp.post_code, s.pol_day, s.pol_time, s.ship_date, s.ship_day,\r\n" +
	"s.ship_time, sc.ship_day1, sc.ship_time1, s1.cmdty, s1.ship_day, s.del_date,\r\n" +
	"s.del_day, s.del_week, s.del_time_from, s.del_time_to, sc.lh_carrier_id,\r\n" +
	"sc.lh_service, sc.del_carrier_id, sc.del_service, sc.staging_lane, sc.spec_instructs,\r\n" +
	"sc.distance, sc.max_truck_size, sc.truck_size, sc.trailer_n, sc.driver_fname,\r\n" +
	"sc.arrival_time, sc.evt_flag, sc.route1, sc.stop1, sp.local_dc, sc.carrier_type,\r\n" +
	"carrier_n, sc.aroute_per_group, sfr.dc, sc.fs_rx_flag, sdmt.store_n, s.dc,\r\n" +
	"sp.status, s.next_user_file, s1.next_user_file\r\n" +

	"FROM\r\n" +
	"la.hstore_schedule s\r\n" +
	"INNER JOIN la.hstore_profile sp ON s.store_n=sp.n AND (upper(sp.status)='OPEN' OR " +
	"upper(sp.status)='ACTIVE')\r\n" +
	"LEFT JOIN la.hholidays h ON s.description=h.name\r\n" +
	"LEFT JOIN la.hstore_carrier sc ON sc.store_n=s.store_n AND (sc.cmdty=s.cmdty OR\r\n" +
	"((s.cmdty='DCB' OR s.cmdty='DCV') AND sc.cmdty='FS')) AND\r\n" +
	"(sc.ship_day IS NULL OR sc.ship_day=s.ship_day) AND\r\n",

	SQL_SEL_REG1 = "sc.holidays IS NULL",
	SQL_SEL_HOL1 = "(sc.holidays=h.week_day)",

	SQL_SEL1 = " AND sc.dc=?\r\n" +
	"LEFT JOIN la.hstore_fs_rx sfr ON s.store_n=sfr.store_n\r\n" +
	"LEFT JOIN la.hstore_dcx_mo_tu sdmt ON s.store_n=sdmt.store_n\r\n" +
	"LEFT JOIN la.hstore_schedule s1 ON s.store_n=s1.store_n AND\r\n" +
	//"s.cmdty<>s1.cmdty AND s.cmdty<>'DCF' AND s1.cmdty<>'DCF' AND\r\n" +
	"((s.cmdty='DCB' OR s.cmdty='DCV') AND s1.cmdty='DCX' OR\r\n" +
	"(s1.cmdty='DCB' OR s1.cmdty='DCV') AND s.cmdty='DCX') AND\r\n",

	SQL_SEL_REG2 = "s1.ship_date IS NULL AND s.del_day=s1.del_day",
	SQL_SEL_HOL2 = "s1.ship_date IS NOT NULL AND s.del_date=s1.del_date",

	SQL_SEL2 = " AND s.del_time_from=s1.del_time_from\r\n" +
	"WHERE\r\n" +
	"s.store_n<9000 AND s.cmdty IN (",

	SQL_SHP_REG = "\r\n) AND s.ship_date IS NULL AND s.ship_day=?",
	SQL_SHP_HOL = "\r\n) AND s.ship_date IS NOT NULL AND s.ship_date=?",

	SQL_SEL3 = "\r\nORDER BY s.cmdty,sc.carrier_id,sc.group1,sc.stop1",
	
	SQL_CHK = "SELECT n FROM la.hstore_carrier WHERE dc='20' AND " +
		"store_n=? AND cmdty=? AND ship_day=?",
	SQL_CHK1 = "SELECT n FROM la.hstore_carrier WHERE dc<>'20' AND " +
		"store_n=? AND cmdty=? AND ship_day=?",

	missing = "Missing records", BC = "BC", PQ = "PQ";

	static ConnectFactory connectFactory;
	private static PreparedStatement chk, chk1;
	private static DsKey searchKey = new DsKey();

	private static String getSql(int dc20, boolean dc2030, boolean holidaySQL, String cmdty) {
		StringBuilder b = new StringBuilder(512);
		b.append(SQL_SEL);
		if (holidaySQL) {
			b.append(SQL_SEL_HOL1);
			b.append(SQL_SEL1);
			b.append(SQL_SEL_HOL2); b.append(SQL_SEL2);
			b.append(cmdty);
			b.append(SQL_SHP_HOL);
		}
		else {
			b.append(SQL_SEL_REG1);
			b.append(SQL_SEL1);
			b.append(SQL_SEL_REG2); b.append(SQL_SEL2);
			b.append(cmdty);
			b.append(SQL_SHP_REG);
		}
		if (dc20 == 0) {
			b.append(" AND s.dc=?\r\n");
		}
		else if (dc20 == 1) {
			if (dc2030) {
				b.append(" AND s.dc='30'\r\n");
			}
			else {b.append(" AND s.dc<>'30'\r\n");}
		}
		b.append(SQL_SEL3);
		return b.toString();
	}
	private static ResultSet getRowSet(Connection con, SearchInput si,
		int idx, int dc20, boolean dc2030, boolean holidaySQL) throws Exception {
		PreparedStatement st;
		String cmdty = si.getCmdty(si.dc, dc20 == 1);
		st = con.prepareStatement(getSql(dc20, dc2030, holidaySQL, cmdty));
		st.setString(1, si.dc);
		if (holidaySQL) {
			Date d = new Date(si.fromDate.getTime() + SupportTime.DAY*idx);
			st.setDate(2, d);
		}
		else { st.setInt(2, si.fromDay+idx);}
		if (dc20 == 0) { st.setString(3, SearchInput.toDc(si.dc));}
		return st.executeQuery();
	}
	static void process(HashMap<Integer,HashMap<String,HashMap<DsKey,ShipmentRow>>> all,
		HashMap<String,HashMap<DsKey,ShipmentRow>> m, SearchInput si, int idx,
		int dc20, boolean dc2030, boolean hasHolidayWeeks) throws Exception {
		Connection con = connectFactory.getConnection();
		if (chk == null) {
			chk = con.prepareStatement(SQL_CHK);
			chk1 = con.prepareStatement(SQL_CHK1);
		}
		ResultSet rs = getRowSet(con, si, idx, dc20, dc2030, false); // regular
		process(all, m, si, idx, dc20, hasHolidayWeeks, false, rs);
		rs.close();
		rs = getRowSet(con, si, idx, dc20, dc2030, true); // holidays
		process(all, m, si, idx, dc20, hasHolidayWeeks, true, rs);
		rs.close();
	}
	private static boolean ignore(PreparedStatement st,
		int storeN, String cmdty, int day) throws Exception {
		st.setInt(1, storeN);
		st.setString(2, cmdty);
		st.setInt(3, day);
		ResultSet rs = st.executeQuery();
		return rs.next();
	}
	private static boolean ignore(ShipmentRow r, boolean dcxMoTu) {
		if (dcxMoTu) {
			int delDay = r.delKey.getDay();
			if (delDay != 1 && delDay != 2) {
				return true;
			}
		}
		return false;
	}
	private static boolean ignore(HashMap<DsKey,ShipmentRow> m1, ShipmentRow r) {
		if (r.relCmdty1 != null) {
			ShipmentRow r1 = m1.get(r.delKey);
			if (r1 != null && r1.relCmdty1.equals(CommonConstants.DCB) &&
				r.relCmdty1.equals(CommonConstants.DCV)) {
				return true;
			}
		}
		return false;
	}
	private static void process(HashMap<Integer,HashMap<String,HashMap<DsKey,ShipmentRow>>> all,
		HashMap<String,HashMap<DsKey,ShipmentRow>> m, SearchInput si, int idx,
		int dc20, boolean hasHolidayWeeks, boolean holidaySQL,
		ResultSet rs) throws Exception {
		Date holidayEnd = hasHolidayWeeks ?
			new Date(si.fromDate.getTime()+7*si.holidayWeeks*SupportTime.DAY) : null; 
		while (rs.next()) {
			String dc = rs.getString(1),
				cmdty = rs.getString(2),
				prov = rs.getString(8);
			if (ignore(si.dc, cmdty, prov)) {
				continue;
			}
			String cmdty1 = cmdty;
			cmdty = DsKey.toCmdty(cmdty);
			ShipmentRow r = new ShipmentRow();
			r.delKey.setStoreN(rs.getInt(3));
			if (idx==0 && rs.getInt(3)==6072 && cmdty.equals("RX")) {
				//System.out.println(idx+", "+rs.getInt(3)+", "+cmdty);
			}
			if (dc == null) {
				// missing record
				if (cmdty1.equals(CommonConstants.DCB) || cmdty1.equals(CommonConstants.DCF)) {
					if (dc20 == 0) {
						if (ignore(chk, r.delKey.getStoreN(), cmdty, idx)) {
							//System.out.println(idx+", "+rs.getInt(3)+", "+cmdty);
							continue;
						}
					}
					else if (dc20 == 2) {
						if (ignore(chk1, r.delKey.getStoreN(), cmdty, idx)) {
							continue;
						}
					}
				}
				String dc1 = rs.getString(46);
				dc1 = SearchInput.toDc(si.dc, dc1);
				if (dc20 == 0 && !si.dc.equals(dc1) || holidaySQL && !hasHolidayWeeks) {
					continue;
				}
				r.missing = true;
			}
			r.shipDay = rs.getInt(13);
			r.shipDay1 = getInt(rs.getObject(15));
			r.delKey.setDay(rs.getInt(20));
			if (holidaySQL) {
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
			r.delKey.setCommodity(cmdty);
			if (dc20 == 1 && ignore(r, rs.getString(45) != null)) {
				continue;
			}
			r.nextUserFile = rs.getString(48);
			r.relNextUserFile = rs.getString(49);
			if (ignore(all, r, idx)) {
				continue;
			}
			String c1 = r.missing ? missing : cmdty;
			HashMap<DsKey,ShipmentRow> m1 = m.get(c1);
			if (m1 == null) {
				m1 = new HashMap<DsKey,ShipmentRow>(512, .5f);
				m.put(c1, m1);
			}
			ShipmentRow r1 = m1.get(r.delKey);
			if (r1 != null) {
				r.replacedRows.add(r1.nextUserFile);
			}
			r.relCmdty1 = rs.getString(17);
			if (dc20 == 1 && ignore(m1, r)) {
				continue;
			}
			m1.put(r.delKey, r);
			if (r.holidays && si.dc.equals(CommonConstants.DC30)) {
				if (rs.getString(43) != null) { //rxToFs
					setRx(m, r, cmdty);
				}
			}
			else {
				if (rs.getString(44) != null) { //rxToFs
					setRx(m, r, cmdty);
				}
			}
			r.group = rs.getDouble(4);
			r.city = rs.getString(5);
			r.carrier = r.missing ? cmdty : rs.getString(6);
			r.carrier1 = rs.getString(7);
			r.prov = prov;
			r.postCode = rs.getString(9);
			r.polDay = rs.getInt(10);
			r.polTime = SupportTime.HHmm_Format.format(rs.getTime(11));
			r.shipTime = SupportTime.HHmm_Format.format(rs.getTime(14));
			r.shipTime1 = rs.getString(16);
			r.relCmdty = DsKey.toCmdty(r.relCmdty1);
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
			r.delTimeFrom = SupportTime.HHmm_Format.format(rs.getTime(22));
			r.delTimeTo = SupportTime.HHmm_Format.format(rs.getTime(23));
			r.lhCarrier = rs.getString(24);
			r.lhService = rs.getString(25);
			r.delCarrier = rs.getString(26);
			r.delService = rs.getString(27);
			r.stagingLane = rs.getString(28);
			r.specInstructs = rs.getString(29);
			r.distance = rs.getInt(30);
			r.maxTruckSize = rs.getString(31);
			r.truckSize = rs.getString(32);
			r.trailerN = rs.getString(33);
			r.driverFName = rs.getString(34);
			r.arrivalTime = rs.getString(35);
			r.evtFlag = rs.getString(36);
			r.route1 = rs.getString(37);
			r.stop1 = getInt(rs.getObject(38));
			r.localDc = rs.getString(39);
			r.carrierType = rs.getString(40);
			r.carrierN = getInt(rs.getObject(41));
			if (r.carrierN == -1) { r.carrierN = Integer.MAX_VALUE;}
			r.aRoutePerGroup = rs.getString(42) != null;
			r.active = CommonConstants.ACTIVE.equalsIgnoreCase(rs.getString(47));
		}
	}
	private static boolean ignore(HashMap<Integer,HashMap<String,HashMap<DsKey,ShipmentRow>>> all,
		ShipmentRow r, int idx) {
		String cmdty = r.delKey.getCommodity();
		for (Iterator<HashMap<String,HashMap<DsKey,ShipmentRow>>> it = all.values().iterator();
			it.hasNext();) {
			HashMap<String,HashMap<DsKey,ShipmentRow>> m = it.next();
			boolean v = ignore(m, cmdty, r, idx);
			if (!v) {
				v = ignore(m, missing, r, idx);
				if (v) {
					return true;
				}
			}
		}
		return false;
	}
	private static boolean ignore(HashMap<String,HashMap<DsKey,ShipmentRow>> m,
		String cmdty, ShipmentRow r, int idx) {
		HashMap<DsKey,ShipmentRow> m1 = m.get(cmdty);
		if (m1 != null) {
			ShipmentRow r1 = m1.get(r.delKey);
			if (r1 != null) {
				if (r.holidays) {
					if (!r1.holidays) {
						m1.remove(r.delKey);
						r.replacedRows.add(r1.nextUserFile);
					}
				}
				else if (r1.holidays) {
					//System.out.println(idx+", "+r.delKey);
					return true;
				}
			}
		}
		return false;
	}
	private static boolean ignore(String dc, String cmdty, String prov) {
		switch (dc) {
		case CommonConstants.DC30:
			if (prov.equals(PQ) && cmdty.equals(CommonConstants.DCF)) {
				return true;
			}
			break;
		case CommonConstants.DC50:
			if (prov.equals(BC) && cmdty.equals(CommonConstants.DCF)) {
				return true;
			}
			break;
		case CommonConstants.DC70:
			if (!prov.equals(BC)) {
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
	private static void setRx(HashMap<String,HashMap<DsKey,ShipmentRow>> m,
		ShipmentRow r, String cmdty) {
		int storeN = r.delKey.getStoreN();
		searchKey.setStoreN(storeN);
		searchKey.setDay(r.delKey.getDay());
		if (CommonConstants.FS.equals(cmdty)) {
			searchKey.setCommodity(CommonConstants.RX);
			HashMap<DsKey,ShipmentRow> m1 = m.get(CommonConstants.RX);
			if (m1 != null) {
				ShipmentRow r1 = m1.get(searchKey);
				if (r1 != null) {
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
				if (r1 != null) {
					m1 = m.get(CommonConstants.RX);
					m1.remove(r.delKey);
					r1.rxRow = r;
				}
			}
		}
	}
	
}
