package com.logisticsalliance.general;

import javax.mail.Session;

import org.apache.log4j.Logger;

import com.logisticsalliance.util.SupportTime;

/**
 * This class sends emergency e-mails when the application has no progress.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class EmailEmergency implements Runnable {

	private static Logger log = Logger.getLogger(EmailEmergency.class);

	private EmailSent1 emailSent1;

	EmailEmergency(EmailSent1 es) {
		emailSent1 = es;
	}

	@Override
	public void run() {
		try {
			long t = SupportTime.HOUR<<1;
			Thread.sleep(t);
			send(emailSent1, "Application has no progress");
		}
		catch (InterruptedException e) { }
	}

	public static void send(EmailSent1 es, String msg) throws InterruptedException {
		if (es.emailUnsent == null || es.emailSentOnlyToBbc != null) {
			int[] trials = {0};
			Session s = EmailSender.send(null, es, es.emergencyTo,
				"Emergency : "+msg, "", null, trials);
			while (s == null && trials[0] < 20) {
				Thread.sleep(20000);
				s = EmailSender.send(s, es, es.emergencyTo,
					"Emergency : "+msg, "", null, trials);
			}
			if (s == null) {
				log.error("Unable to send Emergency");
			}
		}
	}
}
