package com.logisticsalliance.general;

import java.util.Properties;

public class EmailSent1 extends EmailSent {
	public String storePrefix1, storePrefix2, rcptHost, qcStorePrefix1, qcStorePrefix2,
		qcRcptHost, reportsTo, rnFileListTo, emergencyTo;

	public EmailSent1(Properties props, String pwd) {
		super(props, pwd, "e");
		storePrefix1 = ScheduledWorker.getValue(props, "emailStorePrefix1");
		storePrefix2 = ScheduledWorker.getValue(props, "emailStorePrefix2");
		rcptHost = ScheduledWorker.getValue(props, "emailRcptHost");
		qcStorePrefix1 = ScheduledWorker.getValue(props, "emailQcStorePrefix1");
		qcStorePrefix2 = ScheduledWorker.getValue(props, "emailQcStorePrefix2");
		qcRcptHost = ScheduledWorker.getValue(props, "emailQcRcptHost");
		reportsTo = ScheduledWorker.getValue(props, "emailReportsTo");
		rnFileListTo = ScheduledWorker.getValue(props, "emailRnFileListTo");
		emergencyTo = ScheduledWorker.getValue(props, "emailEmergencyTo");
	}
}