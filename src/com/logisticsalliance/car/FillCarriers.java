package com.logisticsalliance.car;

public class FillCarriers {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		/*Properties appProps = new Properties();
		appProps.load(new FileReader(new File(".", "app.properties")));
		Connection con = DriverManager.getConnection(appProps.getProperty("url"),
			appProps.getProperty("user"), args[0]);
		PreparedStatement st1 = con.prepareStatement("SELECT " +
			"c.n,store_n,cmdty,del_day,lh_carrier,del_carrier_id,del_carrier " +
			"FROM la.hcarrier_schedule c,la.hstore_profile s" +
			"WHERE c.store_n=s.n " +
			"ORDER BY store_n,cmdty,del_day,del_carrier_id"),
			st2 = con.prepareStatement("INSERT INTO la.hcarrier_schedule1"+
				"(store_n,cmdty,del_day,lh_carrier_id,del_carrier_id)"+
				"SELECT store_n,cmdty,del_day,lh_carrier_id,del_carrier,id "+
				"FROM la.hcarrier_schedule c WHERE n = ? "+
				"AND NOT EXISTS (SELECT * FROM la.hcarrier_schedule1 c1 WHERE "+
				"c.store_n = c1.store_n AND c.cmdty = c1.cmdty AND c.del_day = c1.del_day)"),
			st3 = con.prepareStatement("INSERT INTO la.hcarrier_schedule1"+
				"(store_n,cmdty,del_day,lh_carrier_id,del_carrier_id)"+
				"SELECT store_n,cmdty,del_day,lh_carrier_id,del_carrier,id "+
				"FROM la.hcarrier_schedule c WHERE store_n = ? AND cmdty = ? AND del_day = ? "+
				"AND NOT EXISTS (SELECT * FROM la.hcarrier_schedule1 c1 WHERE "+
				"c.store_n = c1.store_n AND c.cmdty = c.cmdty AND c.del_day = c.del_day)");
		ResultSet rs = st1.executeQuery();
		while (rs.next()) {
			long n = rs.getLong(1);
			int storeN = rs.getInt(2), delDay = rs.getInt(4);
			String cmdty = rs.getString(3),
				lhc = rs.getString(5),
				dcid = rs.getString(6),
				dc = rs.getString(7);
		}
		*/
	}

}
