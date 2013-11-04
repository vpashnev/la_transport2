package com.logisticsalliance.shp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.CommonConstants;

public class ShipmentHub {

	private static Logger log = Logger.getLogger(ShipmentHub.class);

	private static final String
	SQL_SEL_ZONE =
	"SELECT CZVERS,CZPSTC,CZCTY,CZSTA " +
	"FROM " +
	"OS61LXDTA.SMPCARZ2, OS61LXDTA.OSPTARF, OS61LXDTA.SMPCARP " +
	//",OS61LXDTA.SMPLAN1 " +
	"WHERE " +
	//"CZZON = LNZON2 AND CZTAR# = LNTAR# AND " +
	"CZTAR# = TFTAR# AND " +
	"TFCAR# = CPCAR# AND "+
	"CPCAR = ? AND " +
	"(CZPSTC = ? OR CZPSTC = '*ALL' AND (CZCTY = ? OR CZCTY = '*ANY' AND CZSTA = ?)) " +
	"AND TFEFFD <= ? AND TFEXPD >= ? " +
	"ORDER BY CZZON",

	SQL_SEL_STORE_PLACE =
	"SELECT CNPSTC,CNCTY,CNSTA FROM OS61LXDTA.OSPCONS WHERE CNCON=?";

	static String getHub(HubStatements hst, ShipmentData sd) throws Exception {
		if (sd.delCarrier == null || sd.delCarrier.equals(CommonConstants.CCS)) {
			return "";
		}
		if (sd.delCarrier.equals("SONAR")) {
			if ("DDCT".equals(sd.delService)) {
				return "DC20";
			}
			else if ("LTL".equals(sd.delService)) {
				return "SONA";
			}
		}
		hst.selStorePlace.setString(1, String.valueOf(sd.storeN));
		ResultSet rs = hst.selStorePlace.executeQuery();
		if (!rs.next()) {
			log.error("No place record exists for the store "+sd.storeN);
			return "";
		}
		String postCode = rs.getString(1), city = rs.getString(2), prov = rs.getString(3);
		rs.close();
		return getHub(hst.selZone, sd, postCode, city, prov);
	}

	private static String getHub(PreparedStatement st, ShipmentData sd,
		String postCode, String city, String prov) throws Exception {
		ArrayList<Place> al = new ArrayList<Place>(32);
		st.setString(1, sd.delCarrier);
		st.setString(2, postCode);
		st.setString(3, city);
		st.setString(4, prov);
		st.setInt(5, ShipmentDb.toInt(sd.shipDate));
		st.setInt(6, ShipmentDb.toInt(sd.shipDate));
		ResultSet rs = st.executeQuery();
		Place p1 = null;
		while (rs.next()) {
			Place p = new Place();
			p.hub = rs.getString(1);
			p.postCode = rs.getString(2);
			p.city = rs.getString(3);
			p.prov = rs.getString(4);
			if (p.postCode.charAt(0) != '*') {
				p1 = p;
				//break;
			}
			al.add(p);
		}
		rs.close();
		if (al.size() > 2) {
			rs = null;
		}
		if (p1 == null) {
			if (al.size() == 0) {
				log.error("No hub exists for the store "+sd.storeN+", and carrier "+sd.delCarrier);
				return "";
			}
			for (Iterator<Place> it = al.iterator(); it.hasNext();) {
				p1 = it.next();
				if (p1.city.charAt(0) != '*') {
					break;
				}
			}
		}
		return p1.hub.trim();
	}
	private static class Place {
		String hub, postCode, city, prov;

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder(64);
			b.append(hub); b.append(','); b.append(' ');
			b.append(postCode); b.append(','); b.append(' ');
			b.append(city); b.append(','); b.append(' ');
			b.append(prov);
			return b.toString();
		}
	}
	static class HubStatements {
		PreparedStatement selZone, selStorePlace;

		public HubStatements(Connection con) throws Exception {
			selZone = con.prepareStatement(SQL_SEL_ZONE);
			selStorePlace = con.prepareStatement(SQL_SEL_STORE_PLACE);
		}
		
	}
}
