package com.logisticsalliance.shp;

import java.sql.PreparedStatement;

public class ShipmentHub {

	//private 
	static final String
	SQL_SEL_ZONE1 =
	"SELECT CZZON " +
	"FROM SMPCARZ2, SMPLAN1, OSPTARF, SMPCARP " +
	"WHERE CZZON = LNZON2 AND CZTAR# = LNTAR# AND LNTAR# = TFTAR# AND TFCAR# = CPCAR# AND ",
	SQL_SEL_ZONE2 =" = ? AND CPCAR = ? AND TFEFFD <= ? AND TFEXPD >= ? " +
	"ORDER BY CZZON " +
	"FETCH FIRST 1 ROWS ONLY",

	SQL_SEL_ZONE_BY_POST_CODE = SQL_SEL_ZONE1 + "CZPSTC" + SQL_SEL_ZONE2,
	SQL_SEL_ZONE_BY_CITY = SQL_SEL_ZONE1 + "CZCTY" + SQL_SEL_ZONE2,
	SQL_SEL_ZONE_BY_PROV = SQL_SEL_ZONE1 +
	"CZPSTC = '*ALL' AND CZCTY = '*ALL' AND CZCTY" +
	SQL_SEL_ZONE2,

	SQL_SEL_PLACE =
	"SELECT CNPSTC,CNCTY,CNSTA FROM OS61LXDTA.OSPCONS WHERE CNCON=?";

	static String getHub(HubStatements st, ShipmentData sd) throws Exception {
		if (sd.delCarrier == "SONAR" && sd.delService == "DDCT") {
			return "DC20";
		}
		if (sd.delCarrier == "SONAR" && sd.delService == "LTL") {
			return "SONA";
		}
		return null;
	}

	static String getZone(HubStatements st, ShipmentData sd) throws Exception {
		if (sd.delCarrier == "SONAR" && sd.delService == "DDCT") {
			return "DC20";
		}
		if (sd.delCarrier == "SONAR" && sd.delService == "LTL") {
			return "SONA";
		}
		return null;
	}

	static class HubStatements {
		PreparedStatement selZoneByPostCode, selZoneByCity, selZoneByProv;
		
	}
}
