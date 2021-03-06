package com.logisticsalliance.general;

import java.io.File;
import java.util.Arrays;

import javax.mail.Session;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.glossium.io.SupportFile;

/**
 * This class sends e-mails with the application reports.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class EmailReports {

	private static Logger log = Logger.getLogger(EmailReports.class);

	private static final String[] logFileNames = { "general.log",
		"snRpt.log", "saRpt.log", "shpRpt.log", "ttRpt.log"};
	private File[] files;
	private File zip;
	
	public EmailReports(File dir) {
		File logDir = new File(dir, "log");
		zip = new File(logDir, "reports.zip");
		files = new File[logFileNames.length];
		for (int i = 0; i != files.length; i++) {
			files[i] = new File(logDir, logFileNames[i]);
		}
	}

	private String getRnFileListMsg() {
		StringBuilder b = new StringBuilder(256);
		int sz = ShipmentDataDb.dailyRnFiles.size();
		String[] arr = ShipmentDataDb.dailyRnFiles.toArray(new String[sz]);
		Arrays.sort(arr);
		for (int i = 0; i != sz; i++) {
			b.append(arr[i]); b.append("<br>"); b.append('\r'); b.append('\n');
		}
		b.append("<br>Total : "+sz);
		return b.toString();
	}
	String sendRnFileList(EmailSent1 es) throws Exception {
		String m = getRnFileListMsg();
		if (es.emailUnsent == null || es.emailSentOnlyToBbc != null) {
			int[] trials = {0};
			Session s = EmailSender.send(null, es, es.rnFileListTo,
				"List of processed files", m, null, trials);
			while (s == null && trials[0] < 20) {
				Thread.sleep(20000);
				s = EmailSender.send(s, es, es.rnFileListTo,
					"List of processed files", m, null, trials);
			}
			if (s == null) {
				log.error("Unable to send List of processed files");
			}
		}
		return m;
	}
	void send(EmailSent1 es) throws Exception {
		SupportFile.zip(zip, files);
		if (es.emailUnsent == null || es.emailSentOnlyToBbc != null) {
			int[] trials = {0};
			Session s = EmailSender.send(null, es, es.reportsTo, "Application reports", "", zip, trials);
			while (s == null && trials[0] < 20) {
				Thread.sleep(20000);
				s = EmailSender.send(s, es, es.reportsTo, "Application reports", "", zip, trials);
			}
			if (s == null) {
				log.error("Unable to send Application reports");
			}
		}
		LogManager.shutdown();
		File p = files[0].getParentFile();
		ScheduledWorker.move(new File[]{zip}, new File(p, "archive"));
		for (int i = 0; i != files.length; i++) {
			files[i].delete();
		}
		App.configureLog4j(p.getParentFile());
	}
}
