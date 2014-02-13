package com.logisticsalliance.tp;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import com.logisticsalliance.sqla.ConnectFactory1;
import com.logisticsalliance.util.SupportTime;

public class FillGridDB {

	static String
	SQL_SEL =
	"SELECT " +
	"s.store_n, s.cmdty, sp.city, sp.province, sp.post_code, s.pol_day, s.pol_time,\r\n" +
	"s.ship_day, s.ship_time, s1.ship_day, s.del_date, s.del_day, s.del_week,\r\n" +
	"s.del_time_from, s.del_time_to, s.first_user_file, s.next_user_file\r\n" +

	"FROM\r\n" +
	"la.hstore_schedule s LEFT JOIN la.hstore_schedule s1 ON s.store_n=s1.store_n AND\r\n" +
	"(s.cmdty='DCB' OR s.cmdty='DCV') AND s1.cmdty='DCX' AND\r\n",

	SQL_SEL_REG = "s.ship_date IS NULL AND s1.ship_date IS NULL AND " +
	"s.del_day=s1.del_day",
	SQL_SEL_HOL = "s.ship_date IS NOT NULL AND s1.ship_date IS NOT NULL AND " +
	"s.del_date=s1.del_date",

	SQL_SEL1 = ",\r\nla.hstore_profile sp\r\n" +
	"WHERE\r\n" +
	"s.store_n=sp.n AND sp.local_dc=? AND s.cmdty IN (\r\n",

	SQL_SHP_REG = "\r\n) AND s.ship_day=?\r\n",
	SQL_SHP_HOL = "\r\n) AND s.ship_date=?\r\n",

	SQL_DEL_REG = "\r\n) AND s.del_day=?\r\n",
	SQL_DEL_HOL = "\r\n) AND s.del_date=?\r\n",
			
	SQL_SEL2 =" ORDER BY 2";

	private static String getSql(boolean hol, boolean del, String cmdty) {
		StringBuilder b = new StringBuilder(512);
		b.append(SQL_SEL);
		if (hol) {
			b.append(SQL_SEL_HOL);
			b.append(SQL_SEL1);
			b.append(cmdty);
			if (del) {
				b.append(SQL_DEL_HOL);
			}
			else {
				b.append(SQL_SHP_HOL);
			}
		}
		else {
			b.append(SQL_SEL_REG);
			b.append(SQL_SEL1);
			b.append(cmdty);
			if (del) {
				b.append(SQL_DEL_REG);
			}
			else {
				b.append(SQL_SHP_REG);
			}
		}
		b.append(SQL_SEL2);
		return b.toString();
	}
	private static ResultSet getRegRowSet(Connection con, SearchInput si,
		int idx) throws Exception {
		PreparedStatement st;
		String cmdty = si.getCmdty();
		if (si.delDates) {
			st = con.prepareStatement(getSql(false, true, cmdty));
			Date d = (Date)si.fromDate.clone();
			d.setTime(d.getTime() + SupportTime.DAY*idx);
			st.setDate(2, d);
		}
		else {
			st = con.prepareStatement(getSql(false, false, cmdty));
			st.setInt(2, si.fromDay+idx);
		}
		st.setString(1, SearchInput.toDc(si.dc));
		return st.executeQuery();
	}
	static ArrayList<ShipmentRow> process(SearchInput si, int idx) throws Exception {
		ArrayList<ShipmentRow> al = new ArrayList<ShipmentRow>(512);
		Connection con = ConnectFactory1.one().getConnection();
		ResultSet rs = getRegRowSet(con, si, idx);
		while (rs.next()) {
			ShipmentRow r = new ShipmentRow();
			r.storeN = rs.getInt(1);
			r.cmdty = rs.getString(2);
			r.city = rs.getString(3);
			r.prov = rs.getString(4);
		}
		return al;
	}
}
