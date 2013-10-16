package com.logisticsalliance.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlSupport {

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
