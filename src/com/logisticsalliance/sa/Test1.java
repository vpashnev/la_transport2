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
			//"WHERE province IN ('NB','NS','PE','NL')",
			"WHERE province IN ('BC')",
			ins = "INSERT INTO la.hstore_alert " +
			"(store_n,comm_n,email,email2,dcb,dcv,dcx,dcf,evt,evt2,rx) VALUES " +
			"(?,0,?,?,'1','1','1','1','1','1','1')";
			//"(store_n,comm_n,email,dcb,dcv,dcx,dcf,evt,evt2,rx) VALUES " +
			//"(?,1,?,'1','1','1','1','1','1','1')";
		PreparedStatement selSt = con.prepareStatement(sel),
			insSt = con.prepareStatement(ins);
		ResultSet rs = selSt.executeQuery();
		int r = 0;
		while (rs.next()) {
			int n = rs.getInt(1);
			insSt.setInt(1, n);
			String p = rs.getString(2);
			StringBuilder b = new StringBuilder();
			SupportGeneral.addEmailAddress(b, es, n, p);
			String s = b.toString();
			int i = s.indexOf(',');
			insSt.setString(2, s.substring(0, i));
			insSt.setString(3, s.substring(i+1));
			//insSt.setString(2, "monctondc@shoppersdrugmart.ca");
			//insSt.executeUpdate();
			insSt.addBatch();
			r++;
		}
		insSt.executeBatch();
		con.commit();
		con.close();
		System.out.println("Updated rows: "+r);
	}
}
