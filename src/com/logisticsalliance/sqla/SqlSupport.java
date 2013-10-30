package com.logisticsalliance.sqla;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;

public class SqlSupport {

	public static final String SQL_DB2_NOW = "SELECT CURRENT_TIMESTAMP FROM SYSIBM.DUAL";

	public static Calendar getDb2CurrentTime() throws SQLException {
		Calendar c = Calendar.getInstance();
		Connection con = ConnectFactory1.one().getConnection();
		try {
			Timestamp t = getDb2CurrentTime(con);
			c.setTime(t);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			ConnectFactory1.close(con);
		}
		return c;
	}
	public static Timestamp getDb2CurrentTime(Connection con) throws SQLException {
		PreparedStatement st = con.prepareStatement(SQL_DB2_NOW);
		ResultSet rs = st.executeQuery();
		Timestamp t;
		if (rs.next()) {
			t = rs.getTimestamp(1);
		}
		else { t = new Timestamp(System.currentTimeMillis());}
		rs.close(); st.close();
		return t;
	}
	public static void update(Connection con, String[] sql) throws SQLException {
		for (int i = 0; i != sql.length; i++) {
			String v = sql[i].trim();
			if (v.isEmpty()) { continue;}
			Statement st = con.createStatement();
			st.executeUpdate(v);
			st.close();
		}
	}
}
