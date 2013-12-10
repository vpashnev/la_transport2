package com.logisticsalliance.general;

import java.util.Properties;

import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.sqla.ConnectFactory1;

public class SupportGeneral {

	public static ConnectFactory makeDataSource1I5(Properties appProps,
		String dbPwd1, String dbPwdI5) {
		ConnectFactory1 cf = ConnectFactory1.one();
		cf.setDriver(getValue(appProps, "driver"));
		cf.setUrl(getValue(appProps, "url"));
		cf.setUser(getValue(appProps, "user"));
		cf.setPassword(dbPwd1);
		return dbPwdI5 == null ? null : new ConnectFactory(getValue(appProps, "driverI5"),
			getValue(appProps, "urlI5"), getValue(appProps, "userI5"), dbPwdI5);
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
