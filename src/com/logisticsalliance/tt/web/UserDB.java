package com.logisticsalliance.tt.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;

import com.glossium.sqla.ConnectFactory;
import com.glossium.sqla.ConnectFactory1;

class UserDB {

	private static final String
	SQL_USER_LOGIN =
	"SELECT n " +
	"FROM la.hstore_profile " +
	"WHERE upper(user_id)=? AND user_pwd=?",

	SQL_SEL_PASS = "SELECT n,user_id,user_pwd FROM la.hstore_profile";

	private static Hashtable<String,Pass> map = new Hashtable<String,Pass>(1000, .5f);

	static void fill() throws Exception {
		Connection con = null;
		try {
			con = ConnectFactory1.one().getConnection();
			PreparedStatement st = con.prepareStatement(SQL_SEL_PASS);
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				Pass p = new Pass();
				p.storeN = rs.getInt(1);
				p.pwd = rs.getString(3);
				map.put(rs.getString(2).toUpperCase(), p);
			}
		}
		finally {
			ConnectFactory.close(con);
		}
	}
	static int login(String user, String pwd) throws Exception {
		Pass p = map.get(user);
		if (p == null) { return 0;}
		if (p.pwd.equals(pwd)) {
			return p.storeN;
		}
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
	private static class Pass {
		private int storeN;
		private String pwd;
	}

}
