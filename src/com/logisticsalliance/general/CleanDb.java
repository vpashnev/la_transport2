package com.logisticsalliance.general;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.Calendar;

import com.glossium.sqla.ConnectFactory;
import com.glossium.sqla.ConnectFactory1;
import com.logisticsalliance.util.SupportTime;

public class CleanDb {

	private static final String
		//SQL_CLEAN_RN = "DELETE FROM la.hrn_order " +
		//"WHERE ship_n IN (SELECT sd.n FROM la.hship_data sd WHERE del_date<?)",
		//SQL_CLEAN_SHP = "DELETE FROM la.hship_data WHERE del_date<?",
		SQL_CLEAN_DS = "DELETE FROM la.hstore_schedule WHERE del_date<?";

	static void clean(Calendar now, int daysOutCleaning) throws Exception {
		if (daysOutCleaning <= 0) { return;}
		Date before = new Date(now.getTimeInMillis()-SupportTime.DAY*daysOutCleaning);
		Connection con = ConnectFactory1.one().getConnection();
		try {
			//clean(con, SQL_CLEAN_RN, before);
			//clean(con, SQL_CLEAN_SHP, before);
			clean(con, SQL_CLEAN_DS, before);
			con.commit();
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
}
