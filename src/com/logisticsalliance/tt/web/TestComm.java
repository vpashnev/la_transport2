package com.logisticsalliance.tt.web;

import javax.mail.Session;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.EMailSender;
import com.logisticsalliance.general.ScheduledWorker.EmailSent;

public class TestComm {

	private static Logger log = Logger.getLogger(TestComm.class);
	private static final String subject = "Shoppers Drug Mart - Store # ", msg = "Congratulations!\r\n" +
		"You have successfully registered to receive alerts via Track and Trace portal.";
	
	static void send(final EmailSent es, Alert[] alerts, final int store) {
		StringBuilder b = new StringBuilder(200);
		for (int n = 0; n != alerts.length; n++) {
			Alert a = alerts[n];
			for (int i = 0; i != a.comm.length; i++) {
				if (!a.comm[i].isEmpty()) {
					b.append(a.comm[i]);
					b.append(',');
				}
			}
		}
		final String to = b.toString();
		if (to.isEmpty()) {
			return;
		}
		if (es.emailUnsent == null || es.emailSentOnlyToBbc != null) {
			Thread t = new Thread() {
				@Override
				public void run() {
					int[] trials = {0};
					Session s = EMailSender.send(null, es, to, subject+store, msg, trials);
					while (s == null && trials[0] < 20) {
						try {
							Thread.sleep(500);
						}
						catch (InterruptedException e) { }
						s = EMailSender.send(null, es, to, subject+store, msg, trials);
					}
					if (s == null) {
						log.error("Unable to send test message");
					}
				}
				
			};
			t.setDaemon(true);
			t.start();
		}
	}
}
