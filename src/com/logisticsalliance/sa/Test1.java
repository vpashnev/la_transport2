package com.logisticsalliance.sa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.glossium.sqla.ConnectFactory1;
import com.logisticsalliance.general.EmailSent1;
import com.logisticsalliance.general.SupportGeneral;

public class Test1 {

	public static void updateStoreEmail(EmailSent1 es) throws Exception {
		Connection con = ConnectFactory1.one().getConnection();
		String sel = "SELECT n,province FROM la.hstore_profile " +
			"WHERE province IN ('NB','NS','PE','NL')",
			ins = "INSERT INTO la.hstore_alert " +
			"(store_n,comm_n,email,email2,dcb,dcv,dcx,dcf,evt,evt2,rx) VALUES " +
			"(?,0,?,?,'1','1','1','1','1','1','1')";
		PreparedStatement selSt = con.prepareStatement(sel),
			insSt = con.prepareStatement(ins);
		ResultSet rs = selSt.executeQuery();
		while (rs.next()) {
			int n = rs.getInt(1);
			String p = rs.getString(2);
			StringBuilder b = new StringBuilder();
			SupportGeneral.addEmailAddress(b, es, n, p);
			String s = b.toString();
			int i = s.indexOf(',');
			insSt.setInt(1, n);
			insSt.setString(2, s.substring(0, i));
			insSt.setString(3, s.substring(i+1));
			insSt.addBatch();
		}
		insSt.executeBatch();
		con.commit();
		con.close();
	}
}
