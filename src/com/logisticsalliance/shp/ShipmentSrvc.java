package com.logisticsalliance.shp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.CommonConstants;

public class ShipmentSrvc {

	private static Logger log = Logger.getLogger(ShipmentSrvc.class);

	private static final String
	SQL_SEL_HUB =
	"SELECT CZVERS,CZPSTC,CZCTY,CZSTA,LNSRV " +
	"FROM " +
	"OS61LXDTA.SMPCARZ2, OS61LXDTA.OSPTARF, OS61LXDTA.SMPCARP " +
	",OS61LXDTA.SMPLAN1 " +
	"WHERE " +
	"CZZON = LNZON2 AND CZTAR# = LNTAR# AND " +
	"CZTAR# = TFTAR# AND TFCAR# = CPCAR# AND "+
	"CPCAR = ? AND " +
	"(CZPSTC = ? OR CZPSTC = '*ALL' AND (CZCTY = ? OR CZCTY = '*ANY' AND CZSTA = ?)) " +
	"AND TFEFFD <= ? AND TFEXPD >= ? " +
	"ORDER BY CZZON",

	SQL_SEL_HUB1 =
	"SELECT CZVERS " +
	"FROM " +
	"OS61LXDTA.SMPCARZ2, OS61LXDTA.OSPTARF, OS61LXDTA.SMPCARP " +
	",OS61LXDTA.SMPLAN1 " +
	"WHERE " +
	"CZZON = LNZON2 AND CZTAR# = LNTAR# AND " +
	"CZTAR# = TFTAR# AND TFCAR# = CPCAR# AND "+
	"CPCAR = ? AND TFEFFD <= ? AND TFEXPD >= ?",

	SQL_SEL_STORE_PLACE =
	"SELECT CNPSTC,CNCTY,CNSTA FROM OS61LXDTA.OSPCONS WHERE CNCON=?";

	static String getDelService(ShipmentData sd, String service) {
		boolean isCCS = CommonConstants.CCS.equals(sd.delCarrier);
		if (sd.equipSize.startsWith("60H")) {
			return "HWYT";
		}
		if (sd.equipSize.startsWith("60")) {
			return "SGCT";
		}
		if (sd.cmdty.equals(CommonConstants.DCF)) {
			if (sd.equipSize.startsWith("24")) {
				return "STFC";
			}
			return isCCS ? "FFSC" : "FFS";
		}
		else {
			if (sd.equipSize.startsWith("24")) {
				return isCCS ? "STGC" : "STG";
			}
		}
		if (isCCS) {
			return "SGL";
		}
		String v = Functions.cut(service, 4);
		if (v.isEmpty()) {
			if (CommonConstants.DT.equals(sd.delCarrier)) {
				return "SGL";
			}
			if (CommonConstants.M_O.equals(sd.delCarrier)) {
				return "DDCT";
			}
			if (sd.cmdty.equals(CommonConstants.DCF)) {
				v = "FFS";
			}
			else { v = "LTL";}
		}
		return v;
	}
	static void setHub(ShipmentData sd) throws Exception {
		if (sd.delCarrier == null || sd.delCarrier.equals(CommonConstants.CCS)) {
			sd.hub = "";
		}
		if (sd.delCarrier.equals("SONAR")) {
			if ("DDCT".equals(sd.delService)) {
				sd.hub = "DC20";
			}
			else if ("LTL".equals(sd.delService)) {
				sd.hub = "SONA";
			}
		}
	}

	static void setService(HubStatements hst, ShipmentData sd,
		int shipDate) throws Exception {
		hst.selStorePlace.setString(1, String.valueOf(sd.storeN));
		ResultSet rs = hst.selStorePlace.executeQuery();
		if (!rs.next()) {
			log.error("No place record exists for the store "+sd.storeN);
			return;
		}
		String postCode = rs.getString(1), city = rs.getString(2), prov = rs.getString(3);
		rs.close();
		ArrayList<Srvc> al = new ArrayList<Srvc>(32);
		PreparedStatement st = hst.selHub;
		st.setString(1, sd.delCarrier);
		st.setString(2, postCode);
		st.setString(3, city);
		st.setString(4, prov);
		st.setInt(5, shipDate);
		st.setInt(6, shipDate);
		rs = st.executeQuery();
		Srvc s1 = null;
		while (rs.next()) {
			Srvc s = new Srvc();
			s.hub = rs.getString(1);
			s.postCode = rs.getString(2);
			s.city = rs.getString(3);
			s.prov = rs.getString(4);
			s.service = rs.getString(5).trim();
			if (s.postCode.charAt(0) != '*') {
				s1 = s;
				break;
			}
			al.add(s);
		}
		rs.close();
		if (s1 == null) {
			if (al.size() == 0) {
				st = hst.selHub1;
				st.setString(1, sd.delCarrier);
				st.setInt(2, shipDate);
				st.setInt(3, shipDate);
				rs = st.executeQuery();
				if (rs.next()) {
					s1 = new Srvc();
					s1.hub = rs.getString(1);
				}
				else {
					log.error("No hub exists for the store "+sd.storeN+", and carrier "+sd.delCarrier);
					return;
				}
			}
			else {
				for (Iterator<Srvc> it = al.iterator(); it.hasNext();) {
					s1 = it.next();
					if (s1.city.charAt(0) != '*') {
						break;
					}
				}
			}
		}
		sd.hub = s1.hub.trim();
		if (s1.service != null && s1.service.charAt(0) != '*') {
			sd.delService = s1.service.trim();
		}
	}
	private static class Srvc {
		String hub, postCode, city, prov, service;

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder(64);
			b.append(hub); b.append(','); b.append(' ');
			b.append(postCode); b.append(','); b.append(' ');
			b.append(city); b.append(','); b.append(' ');
			b.append(prov); b.append(','); b.append(' ');
			b.append(service);
			return b.toString();
		}
	}
	static class HubStatements {
		PreparedStatement selHub, selHub1, selStorePlace;

		public HubStatements(Connection con) throws Exception {
			selHub = con.prepareStatement(SQL_SEL_HUB);
			selHub1 = con.prepareStatement(SQL_SEL_HUB1);
			selStorePlace = con.prepareStatement(SQL_SEL_STORE_PLACE);
		}
		
	}
}
