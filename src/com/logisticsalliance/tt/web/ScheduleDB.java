package com.logisticsalliance.tt.web;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import com.glossium.sqla.ConnectFactory;
import com.glossium.sqla.ConnectFactory1;

class ScheduleDB {

	private static final String
	SQL_REG =
	"SELECT del_day,del_time_from,del_time_to,ship_day,del_week,cmdty,ship_date " +
	"FROM la.hstore_schedule " +
	"WHERE store_n=?" +
	"ORDER BY del_day",
	SQL_HLD =
	"SELECT del_date,del_time_from,del_time_to,ship_date,del_week,cmdty,description " +
	"FROM la.hstore_schedule " +
	"WHERE store_n=?" +
	"ORDER BY del_date DESC";

	static ArrayList<DelSchedule> select(int store, int type) throws Exception {
		ArrayList<DelSchedule> al = new ArrayList<DelSchedule>();
		if (type == 0) { return al;}
		Connection con = null;
		try {
			con = ConnectFactory1.one().getConnection();
			PreparedStatement st;
			if (type == 1) {
				st = con.prepareStatement(SQL_REG);
			}
			else {
				st = con.prepareStatement(SQL_HLD);
			}
			st.setInt(1, store);
			ResultSet rs = st.executeQuery();
			int delDay = -1;
			Date delDate = null;
			while (rs.next()) {
				DelSchedule d = new DelSchedule();
				if (type == 1) {
					d.shipDate = rs.getDate(7);
					if (d.shipDate != null) { continue;}
					d.delDay = rs.getInt(1);
					d.shipDay = rs.getInt(4);
					if (d.delDay == delDay) {
						d.delDay = -1;
					}
					else { delDay = d.delDay;}
				}
				else {
					d.shipDate = rs.getDate(4);
					if (d.shipDate == null) { continue;}
					d.delDate = rs.getDate(1);
					d.descr = rs.getString(7).trim();
					if (d.delDate.equals(delDate)) {
						d.delDate = null;
					}
					else { delDate = d.delDate;}
				}
				d.delTimeFrom = rs.getTime(2);
				d.delTimeTo = rs.getTime(3);
				d.week = rs.getInt(5)+1;
				d.cmdty = rs.getString(6);
				al.add(d);
			}
			return al;
		}
		finally {
			ConnectFactory.close(con);
		}
	}
	static String toTime(String t) throws Exception {
		if (t.length() == 3) {
			t = "0"+t;
		}
		return t.substring(0, 2)+":"+t.substring(2);
	}
}
