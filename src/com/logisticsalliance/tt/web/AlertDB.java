package com.logisticsalliance.tt.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import com.glossium.sqla.ConnectFactory;
import com.glossium.sqla.ConnectFactory1;
import com.logisticsalliance.general.SupportGeneral;

public class AlertDB {

	private static final String
	SQL_SEL =
	"SELECT comm_n,email,email2,phone,phone2,dcb,dcv,dcx,dcf,evt,evt2,rx " +
	"FROM la.hstore_alert " +
	"WHERE store_n=? " +
	"ORDER BY comm_n",

	SQL_SEL_PROV =
	"SELECT province " +
	"FROM la.hstore_profile " +
	"WHERE n=? ",

	SQL_UPD = "{call la.update_alert(?,?,?,?,?,?,?,?,?,?,?,?,?)}";

	public static void select(int store, Alert[] alerts,
		boolean setInitEmail) throws Exception {
		reset(alerts);
		boolean empty = true;
		Connection con = null;
		try {
			con = ConnectFactory1.one().getConnection();
			PreparedStatement st = con.prepareStatement(SQL_SEL);
			st.setInt(1, store);
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				int commN = rs.getInt(1);
				Alert a = alerts[commN];
				boolean has = false;
				int n = 2;
				for (int i = 0; i != a.comm.length; i++) {
					a.comm[i] = rs.getString(n++).trim();
					if (!has && !a.comm[i].isEmpty()) {
						has = true;
					}
				}
				if (!has) {
					continue;
				}
				for (int i = 0; i != a.cmdty.length; i++) {
					a.cmdty[i] = rs.getString(n++) != null;
				}
				if (empty) {
					empty = false;
				}
			}
			rs.close();
			st.close();
			if (setInitEmail && empty) {
				setInitEmail(con, store, alerts);
			}
		}
		finally {
			ConnectFactory.close(con);
		}
	}
	private static void setInitEmail(Connection con, int store, Alert[] alerts) throws Exception {
		PreparedStatement st = con.prepareStatement(SQL_SEL_PROV);
		st.setInt(1, store);
		ResultSet rs = st.executeQuery();
		if (rs.next()) {
			String province = rs.getString(1);
			StringBuilder b = new StringBuilder(64);
			SupportGeneral.addEmailAddress(b, LoginServlet.emailSent1, store, province);
			String s = b.toString();
			int i = s.indexOf(',');
			alerts[0].comm[0] = s.substring(0, i);
			alerts[0].comm[1] = s.substring(i+1);
		}
	}
	private static void reset(Alert[] alerts) {
		for (int i = 0; i != alerts.length; i++) {
			reset(alerts[i]);
		}
	}
	static void reset(Alert a) {
		for (int j = 0; j != a.cmdty.length; j++) {
			a.cmdty[j] = false;
		}
	}
	static void update(int store, Alert[] alerts) throws Exception {
		Connection con = null;
		try {
			con = ConnectFactory1.one().getConnection();
			PreparedStatement st = con.prepareStatement(SQL_UPD);
			for (int i = 0; i != alerts.length; i++) {
				Alert a = alerts[i];
				st.setInt(1, store);
				st.setInt(2, i); // comm_n
				int n = 3;
				for (int j = 0; j != a.comm.length; j++) {
					st.setString(n++, a.comm[j]);
				}
				for (int j = 0; j != a.cmdty.length; j++) {
					if (a.cmdty[j]) {
						st.setString(n, "1");
					}
					else {
						st.setNull(n, Types.VARCHAR);
					}
					n++;
				}
				st.addBatch();
			}
			st.executeBatch();
			con.commit();
		}
		finally {
			ConnectFactory.close(con);
		}
	}
}
