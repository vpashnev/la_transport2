package com.logisticsalliance.tt.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.sqla.ConnectFactory1;

class UserDB {

	private static String
	SQL_USER_LOGIN =
	"SELECT n " +
	"FROM la.hstore_profile " +
	"WHERE upper(user_id)=? AND user_pwd=?";

	static int login(String user, String pwd) throws Exception{
		Connection con = null;
		try {
			con = ConnectFactory1.one().getConnection();
			PreparedStatement st = con.prepareStatement(SQL_USER_LOGIN);
			st.setString(1, user.toUpperCase());
			st.setString(2, pwd);
			ResultSet rs = st.executeQuery();
			if (rs.next()) {
				int n = rs.getInt(1);
				return n;
			}
			return 0;
		}
		finally {
			ConnectFactory.close(con);
		}
	}
}
