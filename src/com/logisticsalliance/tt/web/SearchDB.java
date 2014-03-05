package com.logisticsalliance.tt.web;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import com.glossium.sqla.ConnectFactory;
import com.logisticsalliance.sa.SendAlertDb;
import com.logisticsalliance.sa.TrackingNote;
import com.logisticsalliance.util.SupportTime;

class SearchDB {

	private static final String
	SQL_EXP1 =
	"SELECT 0,dvshpd,dvdlvd,dvdlvt,dvsrvtime,dvdc,dvroute,dvstop#,dvcom," +
	"dvpallets,dvetato,dvetatc,dvcar,mvnarvd,mvnarvt,mvstsd,mvreexc,tmdta,mvtext,mvcrtz " +
	"FROM " +
	"OS61LXDTA.OSPDLVS d " +
	"LEFT OUTER JOIN OS61LXDTA.SMPMOVM m ON " +
	"d.dvstore# = m.mvstore# AND d.dvshpd=m.mvshpd AND d.dvcom=m.mvcom AND d.dvdc=m.mvdc " +
	"LEFT OUTER JOIN OS61LXDTA.##PTABM p ON p.tmnam='*REASEXC' AND m.mvreexc=p.tment " +
	"WHERE dvstore# = ? AND dvdlvd>? AND ",
	SQL_EXP2 = "ORDER BY 3 DESC, 11 DESC, 4 DESC",

	SQL_EXPD = SQL_EXP1+"dvdlvd<=?"+SQL_EXP2,
	SQL_EXPC = SQL_EXP1+"dvcom=?"+SQL_EXP2,
	SQL_EXPDC = SQL_EXP1+"dvdlvd<=? AND dvcom=?"+SQL_EXP2;

	static ArrayList<TrackingNote> select(int store, Date toDate, String cmdty) throws Exception {
		if (toDate == null && cmdty == null) {
			return new ArrayList<TrackingNote>(0);
		}
		Connection con = null;
		try {
			con = LoginServlet.connectFactoryI5.getConnection();
			ResultSet rs = getRowSet(con, store, toDate, cmdty);
			if (!rs.next()) {
				rs.close();
				return new ArrayList<TrackingNote>(0);
			}
			ArrayList<TrackingNote> al = SendAlertDb.select(rs, false);
			return al;
		}
		finally {
			ConnectFactory.close(con);
		}
	}
	private static ResultSet getRowSet(Connection con, int store,
		Date delDate, String cmdty) throws Exception {
		PreparedStatement st;
		if (delDate != null && cmdty == null) {
			st = con.prepareStatement(SQL_EXPD);
			st.setDate(3, delDate);
		}
		else if (delDate == null && cmdty != null) {
			st = con.prepareStatement(SQL_EXPC);
			st.setString(3, cmdty);
		}
		else {
			st = con.prepareStatement(SQL_EXPDC);
			st.setDate(3, delDate);
			st.setString(4, cmdty);
		}
		st.setInt(1, store);
		st.setDate(2, new Date(delDate.getTime()-SupportTime.DAY*90));
		return st.executeQuery();
	}
}
