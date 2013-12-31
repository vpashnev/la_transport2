package com.logisticsalliance.tt.web;

import javax.mail.Session;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.EMailSender;
import com.logisticsalliance.general.ScheduledWorker.EmailSent;

public class TestComm {

	private static Logger log = Logger.getLogger(TestComm.class);
	
	static void send(final EmailSent es, final String to1, final String to2,
		final String to3, final String to4, final int store) {
		if (to1.isEmpty() && to2.isEmpty() && to3.isEmpty() && to4.isEmpty()) { return;}
		if (es.emailUnsent == null || es.emailSentOnlyToBbc != null) {
			Thread t = new Thread() {
				@Override
				public void run() {
					int[] trials = {0};
					String to = to1+','+to2+','+to3+','+to4;
					Session s = EMailSender.send(null, es, to, "Shoppers Drug Mart - Store # "+store,
						"Truck and Trace updates confirmation", trials);
					while (s == null && trials[0] < 20) {
						try {
							Thread.sleep(500);
						}
						catch (InterruptedException e) { }
						s = EMailSender.send(s, es, to, "Shoppers Drug Mart - Store # "+store,
							"Truck and Trace updates confirmation", trials);
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
