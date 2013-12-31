package com.logisticsalliance.tt.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.sqla.ConnectFactory1;

class AlertDB {

	private static String
	SQL_SEL =
	"SELECT comm_n,comm_id,note_type,cmdty " +
	"FROM la.hstore_alert " +
	"WHERE store_n=?" +
	"ORDER BY comm_n",

	SQL_UPD = "{call la.update_alert(?,?,?,?,?)}";

	static void select(int store, Alert[] alerts) throws Exception {
		reset(alerts);
		int commN1 = -1;
		Connection con = null;
		try {
			con = ConnectFactory1.one().getConnection();
			PreparedStatement st = con.prepareStatement(SQL_SEL);
			st.setInt(1, store);
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				int commN = rs.getInt(1), noteType = rs.getInt(3);
				Alert a = alerts[commN];
				if (commN != commN1) {
					a.commId = rs.getString(2).trim();
					if (commN > 1 && a.commId.length() < 12) { // phone e-mails
						a.commId = "";
					}
					commN1 = commN;
					if (a.commId.isEmpty()) {
						continue;
					}
				}
				if (noteType > a.noteType) { a.noteType = noteType;}
				if (noteType != 0) {
					String cmdty = rs.getString(4);
					switch (cmdty) {
					case (CommonConstants.DCB): a.cmdty[0] = true; break;
					case (CommonConstants.DCV): a.cmdty[1] = true; break;
					case (CommonConstants.DCX): a.cmdty[2] = true; break;
					case (CommonConstants.DCF): a.cmdty[3] = true; break;
					case (CommonConstants.EVT): a.cmdty[4] = true; break;
					case (CommonConstants.EVT2): a.cmdty[5] = true; break;
					}
				}
			}
		}
		finally {
			ConnectFactory.close(con);
		}
	}
	private static void reset(Alert[] alerts) {
		for (int i = 0; i != alerts.length; i++) {
			reset(alerts[i]);
		}
	}
	static void reset(Alert a) {
		a.noteType = 0;
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
				for (int j = 0; j != a.cmdty.length; j++) {
					st.setInt(1, store);
					st.setInt(2, i); // comm_n
					st.setString(3, getCmdty(j));
					st.setString(4, a.commId);
					st.setInt(5, a.cmdty[j] ? a.noteType : 0);
					st.addBatch();
				}
			}
			st.executeBatch();
			con.commit();
		}
		finally {
			ConnectFactory.close(con);
		}
	}
	private static String getCmdty(int index) throws Exception {
		switch (index) {
		case (0): return CommonConstants.DCB;
		case (1): return CommonConstants.DCV;
		case (2): return CommonConstants.DCX;
		case (3): return CommonConstants.DCF;
		case (4): return CommonConstants.EVT;
		default: return CommonConstants.EVT2;
		}
	}
}
