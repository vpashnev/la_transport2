package com.logisticsalliance.tt.web;

import java.util.Arrays;

import javax.mail.Session;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.EmailSender;
import com.logisticsalliance.general.EmailSent1;

public class TestComm {

	private static Logger log = Logger.getLogger(TestComm.class);
	private static final String subject = "Shoppers Drug Mart - Store # ",
		msg = "Congratulations!\r\n" +
		"You have successfully registered to receive alerts via Track and Trace portal.";
	
	static void send(final EmailSent1 es, Alert[] oldAlerts, Alert[] newAlerts, final int store) {
		StringBuilder b = new StringBuilder(200);
		for (int n = 0; n != newAlerts.length; n++) {
			Alert a = newAlerts[n], a0 = oldAlerts[n];
			for (int i = 0; i != a.comm.length; i++) {
				if (!a.comm[i].isEmpty() && (!a.comm[i].equals(a0.comm[i]) ||
					!Arrays.equals(a.cmdty, a0.cmdty))) {
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
					Session s = EmailSender.send(null, es, to, subject+store, msg, trials);
					while (s == null && trials[0] < 20) {
						try {
							Thread.sleep(500);
						}
						catch (InterruptedException e) { }
						s = EmailSender.send(null, es, to, subject+store, msg, trials);
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
