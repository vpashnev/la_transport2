package com.logisticsalliance.tt.web;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.util.SupportTime;

class SearchDB {

	private static final String
	SQL_EXP1 =
	"SELECT dvdlvd,dvdlvt,dvcar,dvcom,dvstsd,mvtext " +
	"FROM OS61LYDTA.OSPDLVS d LEFT JOIN OS61LYDTA.SMPMOVM m ON " +
	"dvstore# = mvstore# AND dvshpd=mvshpd AND dvcom=mvcom AND dvdc=mvdc " +
	"WHERE dvstore# = ? AND dvdlvd>? AND ",
	SQL_EXP2 = " ORDER BY 1 DESC, 2 DESC, 3",

	SQL_EXPD = SQL_EXP1+"dvdlvd<=?"+SQL_EXP2,
	SQL_EXPC = SQL_EXP1+"dvcom=?"+SQL_EXP2,
	SQL_EXPDC = SQL_EXP1+"dvdlvd<=? AND dvcom=?"+SQL_EXP2;

	static ArrayList<Delivery> select(int store, Date toDate, String cmdty) throws Exception {
		ArrayList<Delivery> al = new ArrayList<Delivery>();
		if (toDate == null && cmdty == null) { return al;}
		Connection con = null;
		try {
			con = LoginServlet.connectFactoryI5.getConnection();
			ResultSet rs = getRowSet(con, store, toDate, cmdty);
			String carrier = null, status = null, exp = null;
			Date delDate = null;
			while (rs.next()) {
				Delivery d = new Delivery();
				d.delDate = rs.getDate(1);
				d.carrier = rs.getString(3);
				d.cmdty = rs.getString(4);
				d.status = rs.getString(5);
				if (d.status.equalsIgnoreCase(CommonConstants.EXCE)) {
					d.status = CommonConstants.EXCEPTION;
				}
				boolean delDateSame = d.delDate.equals(delDate);
				if (delDateSame && d.carrier.equals(carrier) && d.status.equals(status)) {
					d.delDate = null; d.arrivalTime = null;
					d.carrier = null; d.status = null;
				}
				else {
					delDate = d.delDate;
					d.arrivalTime = SupportTime.toHH_mm(rs.getString(2));
					carrier = d.carrier; status = d.status;
				}
				d.exp = rs.getString(6);
				if (d.exp != null) {
					if (delDateSame && d.exp.equals(exp)) {
						d.exp = null;
					}
					else {
						exp = d.exp;
					}
				}
				al.add(d);
			}
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
