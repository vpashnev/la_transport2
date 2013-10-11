package com.logisticsalliance.general;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.util.Properties;

import com.logisticsalliance.util.SupportTime;


public class ImportCarriers {

	public static void main(String[] args) throws Exception {
		Properties appProps = new Properties();
		appProps.load(new FileReader(new File(".", "app.properties")));
		// DB2
		Driver dr = (Driver)Class.forName("com.ibm.as400.access.AS400JDBCDriver").newInstance();
		DriverManager.registerDriver(dr);
		dr = (Driver)Class.forName(appProps.getProperty("driver")).newInstance();
		DriverManager.registerDriver(dr);
		Connection con1 = DriverManager.getConnection("jdbc:as400://tmsoprod.nulogx.com/;naming=system;" +
			"libraries=OS61LXDTA:OS61LXCUST:LXLIB;transaction\nisolation=none;",
			"VPASHNEVX","VPASHNEVX".toLowerCase()+1);
		Connection con2 = DriverManager.getConnection(appProps.getProperty("url"),
			appProps.getProperty("user"), args[0]);
		PreparedStatement st1 = con1.prepareStatement("SELECT STORENO,COMMODITY,DELIVERY_DOW," +
			"LH_CARRIER,LH_SERVICE,DELIVERY_CARRIER,DELIVERY_SERVICE,TARGET_OPEN,TARGET_CLOSE FROM " +
			"LXLIB.FSTORE_DELIVERYSCHEDULE ORDER BY STORENO,COMMODITY,DELIVERY_DOW");
		PreparedStatement st2 = con2.prepareStatement("INSERT INTO la.hcarrier_schedule " +
			"(store_n,cmdty,del_day,lh_carrier,lh_service,del_carrier,del_service,target_open," +
			"target_close) VALUES (?,?,?,?,?,?,?,?,?)");
		PreparedStatement st3 = con2.prepareStatement("UPDATE la.hcarrier_schedule SET " +
			"lh_carrier=?,lh_service=?,del_carrier=?,del_service=?,target_open=?," +
			"target_close=? WHERE n=?");
		PreparedStatement st4 = con2.prepareStatement("SELECT n,lh_carrier,lh_service," +
			"del_carrier,del_service,target_open,target_close FROM la.hcarrier_schedule " +
			"WHERE store_n=? AND cmdty=? AND del_day=?");
		StringBuilder sb = new StringBuilder();
		sb.append("store_n,cmdty,del_day,lh_carrier,lh_service,del_carrier,del_service,target_open,target_close\r\n");
		ResultSet rs = st1.executeQuery();
		while (rs.next()) {
			String v = rs.getString(1);
			if (v == null || v.trim().isEmpty() || !Character.isDigit(v.charAt(0))) { continue;}
			int storeN = rs.getInt(1);
			if (storeN == 0) { continue;}
			String cmdty = rs.getString(2).trim();
			v = rs.getString(3);
			int delDay = SupportTime.getDayNumber(v);
			st4.setInt(1, storeN);
			st4.setString(2, cmdty.toString());
			st4.setInt(3, delDay);
			String lhc = rs.getString(4), dc = rs.getString(6);
			if (lhc != null) { lhc = lhc.trim();}
			if (dc != null) { dc = dc.trim();}
			ResultSet rs1 = st4.executeQuery();
			if (rs1.next()) {
				Time t1 = rs.getTime(8), t2 = rs.getTime(9);
				if (t1 != null && t2 != null) {
					long n = rs1.getLong(1);
					st3.setString(1, lhc);
					st3.setString(2, rs.getString(5));
					st3.setString(3, dc);
					st3.setString(4, rs.getString(7));
					st3.setTime(5, t1);
					st3.setTime(6, t2);
					st3.setLong(7, n);
					st3.executeUpdate();
				}
				else {
					t2 = t1;
				}
				sb.append(storeN+",");
				sb.append(cmdty+',');
				sb.append(rs.getString(3)+',');
				sb.append(lhc+',');
				sb.append(rs.getString(5)+',');
				sb.append(dc+',');
				sb.append(rs.getString(7)+',');
				sb.append(rs.getString(8)+',');
				sb.append(rs.getString(9));
				sb.append("\r\n,,");
				sb.append(delDay+",");
				sb.append(rs1.getString(2)+',');
				sb.append(rs1.getString(3)+',');
				sb.append(rs1.getString(4)+',');
				sb.append(rs1.getString(5)+',');
				sb.append(rs1.getString(6)+',');
				sb.append(rs1.getString(7));
				sb.append("\r\n");
			}
			else {
				st2.setInt(1, storeN);
				st2.setString(2, cmdty);
				st2.setInt(3, delDay);
				st2.setString(4, lhc);
				st2.setString(5, rs.getString(5));
				st2.setString(6, dc);
				st2.setString(7, rs.getString(7));
				st2.setTime(8, rs.getTime(8));
				st2.setTime(9, rs.getTime(9));
				st2.executeUpdate();
			}
		}
		System.out.println(sb);
		con2.commit();
		con2.close();
		con1.close();
		System.out.println("Done");
	}
}
