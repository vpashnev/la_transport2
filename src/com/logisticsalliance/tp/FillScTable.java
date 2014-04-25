package com.logisticsalliance.tp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Properties;

import com.glossium.sqla.ConnectFactory1;
import com.logisticsalliance.general.SupportGeneral;

class FillScTable {

	private static final String SQL_INS =
		"INSERT INTO la.hstore_carrier (store_n,carrier_id,dc,group1,cmdty,aroute_per_group," +
		"carrier_type,holidays,ship_day,ship_day1,ship_time1,spec_instructs,lh_carrier_id," +
		"lh_service,del_carrier_id,del_service,stop1,staging_lane,carrier1,distance," +
		"truck_size,max_truck_size,trailer_n,driver_fname,arrival_time,route1,carrier_n," +
		"evt_flag,fs_rx_flag) "+
		"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
		SQL_INS1 =
		"INSERT INTO LA.HCARRIER (id) VALUES (?)",
		SQL_DEL = "DELETE FROM la.hstore_carrier",
		SQL_SEL1 = "SELECT id FROM LA.HCARRIER WHERE id=?";

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
			stDel = con.prepareStatement(SQL_DEL),
			stSel1 = con.prepareStatement(SQL_SEL1);
		stDel.executeUpdate();
		con.commit();
		File f = new File(args[2]);
		BufferedReader br = new BufferedReader(new FileReader(f));
		int i = 0, j = 0;
		while (true) {
			String ln = br.readLine();
			if (ln == null) { break;}
			ln = ln.trim();
			String[] arr = ln.split(",");
			stSel1.setString(1, arr[1].trim().toUpperCase());
			ResultSet rs1 = stSel1.executeQuery();
			if (!rs1.next()) {
				System.out.println("Car: "+arr[1]);
				stIns1.setString(1, arr[1].trim().toUpperCase());
				j += stIns1.executeUpdate();
			}

			System.out.println(arr[0]+","+arr[1]+","+arr[2]);
			set(stIns, 1, arr[0], true);
			set(stIns, 2, arr[1].toUpperCase(), false);
			set(stIns, 3, arr[2], false);
			set(stIns, 4, arr[3], true);
			set(stIns, 5, arr[4], false);
			set(stIns, 6, arr[5], false);
			set(stIns, 7, arr[6], false);
			set(stIns, 8, arr[7], true);
			set(stIns, 9, arr[8], true);
			set(stIns, 10, arr[9], true);
			set(stIns, 11, arr[10], false);
			set(stIns, 12, arr[11], false);
			set(stIns, 13, arr[12], false);
			set(stIns, 14, arr[13], false);
			set(stIns, 15, arr[14], false);
			set(stIns, 16, arr[15], false);
			set(stIns, 17, arr[16], true);
			set(stIns, 18, arr[17], false);
			set(stIns, 19, arr[18], false);
			set(stIns, 20, arr[19], true);
			set(stIns, 21, arr[20], false);
			set(stIns, 22, arr[21], false);
			set(stIns, 23, arr[22], false);
			set(stIns, 24, arr[23], false);
			set(stIns, 25, arr[24], false);
			set(stIns, 26, arr[25], false);
			set(stIns, 27, arr[26], false);
			set(stIns, 28, arr[27], false);
			set(stIns, 29, arr[28], false);
			try {
				i += stIns.executeUpdate();
			}
			catch (Exception e) {
				throw e;
			}
		}
		br.close();
		con.commit();
		con.close();
		System.out.println("Inserted rows: "+i+", "+j);
	}
	private static void set(PreparedStatement st, int idx,
		String v, boolean num) throws Exception {
		v = v.trim();
		if (v.isEmpty()) {
			if (num) {
				st.setNull(idx, Types.INTEGER);
			}
			else {
				st.setNull(idx, Types.VARCHAR);
			}
		}
		else {
			if (num) {
				st.setDouble(idx, Double.parseDouble(v));
			}
			else {
				st.setString(idx, v);
			}
		}
	}
}
