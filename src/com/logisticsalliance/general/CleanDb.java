package com.logisticsalliance.general;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import com.glossium.sqla.ConnectFactory;
import com.glossium.sqla.ConnectFactory1;
import com.logisticsalliance.util.SupportTime;

public class CleanDb {

	private static final String
		//SQL_CLEAN_RN = "DELETE FROM la.hrn_order " +
		//"WHERE ship_n IN (SELECT sd.n FROM la.hship_data sd WHERE del_date<?)",
		//SQL_CLEAN_SHP = "DELETE FROM la.hship_data WHERE del_date<?",
		SQL_CLEAN_DS = "DELETE FROM la.hstore_schedule WHERE del_date<?",
		SQL_SELECT_ST = "SELECT n FROM la.hstore_profile",
		SQL_CLEAN_ST = "DELETE FROM la.hstore_profile WHERE n=?";

	static void clean(Calendar now, int daysOutCleaning) throws Exception {
		if (daysOutCleaning <= 0) { return;}
		Date /*before = new Date(now.getTimeInMillis()-SupportTime.DAY*daysOutCleaning),*/
			before1 = new Date(now.getTimeInMillis()-SupportTime.DAY*16);
		Connection con = ConnectFactory1.one().getConnection();
		try {
			//clean(con, SQL_CLEAN_RN, before);
			//clean(con, SQL_CLEAN_SHP, before);
			clean(con, SQL_CLEAN_DS, before1);
			con.commit();
			cleanStores(con);
		}
		finally {
			ConnectFactory.close(con);
		}
	}
	private static void clean(Connection con, String sql, Date before) throws Exception {
		PreparedStatement st = con.prepareStatement(sql);
		st.setDate(1, before);
		st.executeUpdate();
		st.close();
	}
	private static void cleanStores(Connection con) throws Exception {
		ArrayList<Integer> al = new ArrayList<Integer>(1800);
		PreparedStatement st = con.prepareStatement(SQL_SELECT_ST);
		ResultSet rs = st.executeQuery();
		while (rs.next()) {
			al.add(rs.getInt(1));
		}
		rs.close(); st.close();
		st = con.prepareStatement(SQL_CLEAN_ST);
		for (Iterator<Integer> it = al.iterator(); it.hasNext();) {
			int n = it.next();
			st.setInt(1, n);
			try {
				st.executeUpdate();
			}
			catch (Exception e) { }
		}
		st.close();
		con.commit();
	}
}
