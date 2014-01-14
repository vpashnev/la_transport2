package com.logisticsalliance.general;

import java.util.Properties;

import com.ibm.as400.access.AS400JDBCConnectionPoolDataSource;
import com.ibm.db2.jcc.DB2SimpleDataSource;
import com.logisticsalliance.general.ScheduledWorker.EmailSent;
import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.sqla.ConnectFactory1;

public class SupportGeneral {

	public static void addEmailAddress(StringBuilder b, EmailSent es, int storeN, String province) {
		if (province != null && (province.trim().equalsIgnoreCase("PQ") ||
			province.trim().equalsIgnoreCase("QC"))) {
			String sn = String.valueOf(storeN);
			while (sn.length() < 3) {
				sn = "0" + sn;
			}
			b.append(es.qcStorePrefix1); b.append(sn);
			b.append('@'); b.append(es.qcRcptHost);
			b.append(',');
			b.append(es.qcStorePrefix2); b.append(sn);
			b.append('@'); b.append(es.qcRcptHost);
		}
		else {
			b.append(es.storePrefix1); b.append(storeN);
			b.append('@'); b.append(es.rcptHost);
			b.append(',');
			b.append(es.storePrefix2); b.append(storeN);
			b.append('@'); b.append(es.rcptHost);
		}
	}
	public static ConnectFactory makeDataSource1I5(Properties appProps,
		String dbPwd1, String dbPwdI5) {
		ConnectFactory1 cf1 = ConnectFactory1.one();
		String u = getValue(appProps, "user");
		DB2SimpleDataSource ds = new DB2SimpleDataSource();
		String dsrv = getValue(appProps, "dbServer");
		if (dsrv != null) {
			ds.setServerName(dsrv);
			ds.setPortNumber(50000);
		}
		ds.setDatabaseName(getValue(appProps, "database"));
		ds.setUser(u);
		ds.setPassword(dbPwd1);
		cf1.setDataSource(ds);
		cf1.setDriver(getValue(appProps, "driver"));
		cf1.setUrl(getValue(appProps, "url"));
		cf1.setUser(u);
		cf1.setPassword(dbPwd1);
		if (dbPwdI5 == null) { return null;}
		u = getValue(appProps, "userI5");
		ConnectFactory cfI5 = new ConnectFactory(getValue(appProps, "driverI5"),
			getValue(appProps, "urlI5"), u, dbPwdI5);
		AS400JDBCConnectionPoolDataSource dsi = new AS400JDBCConnectionPoolDataSource();
		dsi.setServerName(getValue(appProps, "dbServerI5"));
		dsi.setDatabaseName(getValue(appProps, "databaseI5"));
		dsi.setUser(u);
		dsi.setPassword(dbPwdI5);
		cfI5.setDataSource(dsi);
		return cfI5;
	}
	static String getValue(Properties props, String propName) {
		String v = props.getProperty(propName);
		if (v != null) {
			v = v.trim();
			return v.isEmpty() ? null : v;
		}
		return null;
	}
}
