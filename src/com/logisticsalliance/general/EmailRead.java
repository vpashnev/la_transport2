package com.logisticsalliance.general;

import java.util.Properties;

class EmailRead {
	String protocol, host, email, password, dsFolder, dsArchiveFolder,
		rnFolder, rnArchiveFolder, emailUnread;

	EmailRead(Properties props, String pwd) {
		protocol = ScheduledWorker.getValue(props, "emailReadProtocol");
		host = ScheduledWorker.getValue(props, "emailReadHost");
		email = ScheduledWorker.getValue(props, "emailRead");
		password = pwd;
		dsFolder = ScheduledWorker.getValue(props, "emailDsFolder");
		dsArchiveFolder = ScheduledWorker.getValue(props, "emailDsArchiveFolder");
		rnFolder = ScheduledWorker.getValue(props, "emailRnFolder");
		rnArchiveFolder = ScheduledWorker.getValue(props, "emailRnArchiveFolder");
		emailUnread = ScheduledWorker.getValue(props, "emailUnread");
	}
}