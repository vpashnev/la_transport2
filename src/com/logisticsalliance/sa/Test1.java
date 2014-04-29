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
			"WHERE n IN (205,260,2288,2290,2133,2266,214,246,253,2127,2250,2255,2283,217,2251,265," +
			"231,251,2203,2236,211,255,258,287,2113,2205,2204,2207,2208,279,2243,252,291,2200,2223," +
			"2225,2209,2143,2264,2107,2244,227,228,236,237,2109,2237,2220,267,2212,2222,2224,2231," +
			"2235,2238,2239,2270,201,202,204,222,232,234,238,263,272,280,288,2102,2151,2221,2234," +
			"2246,2249,2252,2273,2275,2276,2277,2279,2289,2292,2294,3005,4023,212,243,2295,2126,273)",
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
