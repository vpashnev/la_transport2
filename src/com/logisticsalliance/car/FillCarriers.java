package com.logisticsalliance.car;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

public class FillCarriers {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Properties appProps = new Properties();
		appProps.load(new FileReader(new File(".", "app.properties")));
		Connection con = DriverManager.getConnection(appProps.getProperty("url"),
			appProps.getProperty("user"), args[0]);
		PreparedStatement st1 = con.prepareStatement("SELECT " +
			"c.n,store_n,cmdty,del_day,lh_carrier,lh_service,del_carrier_id,del_carrier,del_service " +
			"FROM la.hcarrier_schedule c,la.hstore_profile s " +
			"WHERE c.store_n=s.n " +
			"ORDER BY store_n,cmdty,del_day"),
			st2 = con.prepareStatement("INSERT INTO la.hcarrier (id,name)"+
				"SELECT del_carrier_id,del_carrier "+
				"FROM la.hcarrier_schedule c WHERE n = ? "+
				"AND NOT EXISTS (SELECT * FROM la.hcarrier c1 WHERE c.del_carrier_id = c1.id)"),
			st21 = con.prepareStatement("INSERT INTO la.hcarrier (id,name)"+
				"SELECT lh_carrier,'' "+
				"FROM la.hcarrier_schedule c WHERE n = ? "+
				"AND NOT EXISTS (SELECT * FROM la.hcarrier c1 WHERE c.lh_carrier = c1.id)"),
			st3 = con.prepareStatement("INSERT INTO la.hcarrier_schedule1"+
				"(store_n,cmdty,del_day,lh_carrier_id,lh_service,del_carrier_id,del_service)"+
				"SELECT store_n,cmdty,?,?,?,del_carrier_id,? "+
				"FROM la.hcarrier_schedule c WHERE n = ? "+
				"AND NOT EXISTS (SELECT * FROM la.hcarrier_schedule1 c1 WHERE "+
				"c1.store_n = c.store_n AND c1.cmdty = c.cmdty AND " +
				"c1.del_day = ? AND c1.del_carrier_id = c.del_carrier_id)");
		ResultSet rs = st1.executeQuery();
		while (rs.next()) {
			long n = rs.getLong(1);
			int delDay = rs.getInt(4);
			String lhc = rs.getString(5),
				lhsrv = rs.getString(6),
				dcid = rs.getString(7),
				dsrv = rs.getString(9);
			if (dcid != null && !dcid.trim().isEmpty() &&
				(lhc != null && !lhc.trim().isEmpty() ||
				dsrv != null && !dsrv.trim().isEmpty())) {
				st2.setLong(1, n);
				int c = st2.executeUpdate();
				if (lhc != null && !lhc.trim().isEmpty() && lhc.length()<9) {
					st21.setLong(1, n);
					c = st21.executeUpdate();
				}
				st3.setInt(1, delDay);
				st3.setString(2, lhc == null ? null : lhc.substring(0,lhc.length()<8?lhc.length():8).trim());
				st3.setString(3, lhsrv == null ? null : lhsrv.substring(0,lhsrv.length()<4?lhsrv.length():4).trim());
				st3.setString(4, dsrv == null ? null : dsrv.substring(0,dsrv.length()<4?dsrv.length():4).trim());
				st3.setLong(5, n);
				st3.setInt(6, delDay);
				c = st3.executeUpdate();
				c = c+0;
			}
		}
		rs.close();
		rs = st1.executeQuery();
		while (rs.next()) {
			long n = rs.getLong(1);
			String dcid = rs.getString(7);
			if (dcid != null && !dcid.trim().isEmpty()) {
				for (int i = 0; i != 7; i++) {
					st2.setLong(1, n);
					int c = st2.executeUpdate();
					st3.setInt(1, i);
					st3.setString(2, null);
					st3.setString(3, null);
					st3.setString(4, null);
					st3.setLong(5, n);
					st3.setInt(6, i);
					c = st3.executeUpdate();
					c = c+0;
				}
			}
		}
		rs.close();
		con.commit();
		System.out.println("Done");
	}

}
