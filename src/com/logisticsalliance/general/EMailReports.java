package com.logisticsalliance.general;

import java.io.File;
import java.util.Arrays;

import javax.mail.Session;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.logisticsalliance.general.ScheduledWorker.EmailSent;
import com.logisticsalliance.io.SupportFile;

/**
 * This class sends e-mails with the application reports.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class EMailReports {

	private static Logger log = Logger.getLogger(EMailReports.class);

	static final String[] logFileNames = { "general.log", "snRpt.log", "shpRpt.log", "ttRpt.log"};
	private File[] files;
	private File zip;
	
	public EMailReports(File dir) {
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
	void sendRnFileList(EmailSent es) throws Exception {
		if (es.emailUnsent == null || es.emailSentOnlyToBbc != null) {
			int[] trials = {0};
			String m = getRnFileListMsg();
			Session s = EMailSender.send(null, es, es.rnFileListTo,
				"List of processed files", m, null, trials);
			while (s == null && trials[0] < 20) {
				Thread.sleep(20000);
				s = EMailSender.send(s, es, es.rnFileListTo,
					"List of processed files", m, null, trials);
			}
			if (s == null) {
				log.error("Unable to send List of processed files");
			}
		}
	}
	void send(EmailSent es) throws Exception {
		SupportFile.zip(zip, files);
		if (es.emailUnsent == null || es.emailSentOnlyToBbc != null) {
			int[] trials = {0};
			Session s = EMailSender.send(null, es, es.reportsTo, "Application reports", "", zip, trials);
			while (s == null && trials[0] < 20) {
				Thread.sleep(20000);
				s = EMailSender.send(s, es, es.reportsTo, "Application reports", "", zip, trials);
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
