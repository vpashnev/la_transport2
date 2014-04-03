package com.logisticsalliance.general;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.glossium.sqla.ConnectFactory;
import com.glossium.sqla.ConnectFactory1;
import com.glossium.ui.html.HNode;
import com.glossium.ui.html.HOption;
import com.ibm.as400.access.AS400JDBCConnectionPoolDataSource;
import com.ibm.db2.jcc.DB2SimpleDataSource;

public class SupportGeneral {

	public static void addEmailAddress(StringBuilder b,
		EmailSent1 es, int storeN, String province) {
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
		//cf1.setDataSource(ds);
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
	public static String getValue(Properties props, String propName) {
		String v = props.getProperty(propName);
		if (v != null) {
			v = v.trim();
			return v.isEmpty() ? null : v;
		}
		return null;
	}
	/**
	 * 
	 * Configures Log4j output for the property file in the given parent {@code folder}.
	 * @param folder the folder, where property file is placed.
	 * @param propFile the property file name
	 */
	public static void configureLog4j(File folder, String propFile) {
		String fp = new File(folder, propFile).getPath();
		PropertyConfigurator.configure(fp);
	}
	public static void setMonth(HNode month) {
		new HOption(month, "01", "Jan");
		new HOption(month, "02", "Feb");
		new HOption(month, "03", "Mar");
		new HOption(month, "04", "Apr");
		new HOption(month, "05", "May");
		new HOption(month, "06", "Jun");
		new HOption(month, "07", "Jul");
		new HOption(month, "08", "Aug");
		new HOption(month, "09", "Sep");
		new HOption(month, "10", "Oct");
		new HOption(month, "11", "Nov");
		new HOption(month, "12", "Dec");
	}
	public static void setCmdty(HNode cmdty) {
		new HOption(cmdty, "0", "");
		new HOption(cmdty, "DCB", "DCB");
		new HOption(cmdty, "DCV", "DCV");
		new HOption(cmdty, "DCX", "DCX");
		new HOption(cmdty, "DCF", "DCF");
		new HOption(cmdty, "EVT", "EVT");
		new HOption(cmdty, "EVT2", "EVT2");
		new HOption(cmdty, "RX", "RX");
	}
}
