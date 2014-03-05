package com.logisticsalliance.general;

import java.util.Properties;

public class EmailSent {
	public String host, port, email, user, password, sentToBbc, sentToCc,
	emailSentOnlyToBbc, emailUnsent;
	
	public EmailSent(Properties props, String pwd, String prefix) {
		host = ScheduledWorker.getValue(props, prefix+"mailSentHost");
		port = ScheduledWorker.getValue(props, prefix+"mailSentPort");
		email = ScheduledWorker.getValue(props, prefix+"mailSentFrom");
		user = ScheduledWorker.getValue(props, prefix+"mailUser");
		password = pwd;
		sentToBbc = ScheduledWorker.getValue(props, prefix+"mailSentToBbc");
		sentToCc = ScheduledWorker.getValue(props, prefix+"mailSentToCc");
		emailSentOnlyToBbc = ScheduledWorker.getValue(props, prefix+"mailSentOnlyToBbc");
		emailUnsent = ScheduledWorker.getValue(props, prefix+"mailUnsent");
	}
}