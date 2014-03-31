package com.logisticsalliance.tt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.glossium.sqla.ConnectFactory1;
import com.logisticsalliance.general.DsKey;
import com.logisticsalliance.general.SupportGeneral;

class AddCars {

	private static final String SQL_INS =
		"INSERT INTO LA.HCARRIER_SCHEDULE1 (store_n,cmdty,del_day,del_carrier_id) "+
		"VALUES (?,?,?,?)",
		SQL_INS1 =
		"INSERT INTO LA.HCARRIER (id) VALUES (?)",
		SQL_SEL = "SELECT n FROM LA.HCARRIER_SCHEDULE1 " +
			"WHERE store_n=? AND cmdty=? AND del_day=? AND del_carrier_id=?",
		SQL_SEL1 = "SELECT id FROM LA.HCARRIER WHERE id=?",
		SQL_SEL2 = "SELECT store_n,cmdty,del_day,del_carrier_id FROM LA.HCARRIER_SCHEDULE1";

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		File appDir = new File(args[0]);
		Properties appProps = new Properties();
		appProps.load(new FileReader(new File(appDir, "app.properties")));

		String dbPwd = args[1];
		SupportGeneral.makeDataSource1I5(appProps, dbPwd, null);
		
		Connection con = ConnectFactory1.one().getConnection();
		PreparedStatement stIns = con.prepareStatement(SQL_INS),
			stIns1 = con.prepareStatement(SQL_INS1),
			stSel = con.prepareStatement(SQL_SEL),
			stSel1 = con.prepareStatement(SQL_SEL1);
		File f = new File(args[2]);
		BufferedReader br = new BufferedReader(new FileReader(f));
		int i = 0, j = 0;
		while (true) {
			String ln = br.readLine();
			if (ln == null) { break;}
			ln = ln.trim();
			String[] arr = ln.split(",");
			stSel.setInt(1, Integer.parseInt(arr[0].trim()));
			stSel.setString(2, arr[1].trim());
			stSel.setInt(3, Integer.parseInt(arr[2].trim()));
			stSel.setString(4, arr[3].trim());
			ResultSet rs = stSel.executeQuery();
			if (!rs.next()) {
				stSel1.setString(1, arr[3].trim());
				ResultSet rs1 = stSel1.executeQuery();
				if (!rs1.next()) {
					stIns1.setString(1, arr[3].trim());
					j += stIns1.executeUpdate();
				}

				stIns.setInt(1, Integer.parseInt(arr[0].trim()));
				stIns.setString(2, arr[1].trim());
				stIns.setInt(3, Integer.parseInt(arr[2].trim()));
				stIns.setString(4, arr[3].trim());
				i += stIns.executeUpdate();
			}
		}
		br.close();
		con.commit();
		con.close();
		ArrayList<DuplCars> al = getDuplicates();
		System.out.println("Inserted rows: "+i+", "+j);
		System.out.println(); System.out.println();
		System.out.println(al);
	}
	static ArrayList<DuplCars> getDuplicates() throws Exception {
		HashMap<DsKey,HashSet<String>> m = new HashMap<DsKey,HashSet<String>>();
		Connection con = ConnectFactory1.one().getConnection();
		PreparedStatement st = con.prepareStatement(SQL_SEL2);
		ResultSet rs = st.executeQuery();
		while (rs.next()) {
			DsKey k = new DsKey();
			k.setStoreN(rs.getInt(1));
			k.setCommodity(rs.getString(2));
			k.setDay(rs.getInt(3));
			HashSet<String> hs = m.get(k);
			if (hs == null) {
				hs = new HashSet<String>();
				m.put(k, hs);
			}
			hs.add(rs.getString(4));
		}
		rs.close();
		st.close();
		con.commit();
		con.close();
		ArrayList<DuplCars> al = new ArrayList<DuplCars>();
		for (Iterator<Map.Entry<DsKey,HashSet<String>>> it = m.entrySet().iterator();
			it.hasNext();) {
			Map.Entry<DsKey,HashSet<String>> e = it.next();
			HashSet<String> v = e.getValue();
			if (v.size() > 1) {
				DuplCars dc = new DuplCars();
				dc.k = e.getKey();
				dc.cars = v;
				al.add(dc);
			}
		}
		return al;
	}
	static class DuplCars {
		DsKey k;
		HashSet<String> cars;
		@Override
		public String toString() {
			return k + " : "+cars+"\r\n";
		}
	}
}
