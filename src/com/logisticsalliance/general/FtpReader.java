package com.logisticsalliance.general;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import com.logisticsalliance.general.ScheduledWorker.EmailSent1;
import com.logisticsalliance.general.ScheduledWorker.FtpManager;
import com.logisticsalliance.util.SupportTime;

/**
 * This class reads file from ftp server.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class FtpReader {

	private static Logger log = Logger.getLogger(FtpReader.class);

	public static void read(FtpManager fm, File dir, EmailSent1 es) throws InterruptedException {
		int trials = read1(fm, dir, es, 0);
		while (trials > 0 && trials < 20) {
			Thread.sleep(20000);
			trials = read1(fm, dir, es, trials);
		}
		if (trials >= 20) {
			EMailEmergency.send(es, "Failed to read FTP road-net files");
		}
	}
	private static int read1(FtpManager fm, File dir, EmailSent1 es, int trials) {
		FTPClient fc = new FTPClient();
		//fc.setTrustManager(TrustManagerUtils.getValidateServerCertificateTrustManager());
		//fc.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
		try {
			fc.connect(fm.host); checkReply(fc);
			fc.login(fm.user, fm.password); checkReply(fc);
			fc.enterLocalPassiveMode(); checkReply(fc);
			if (!fc.changeWorkingDirectory(fm.archiveFolder)) {
				fc.makeDirectory(fm.archiveFolder);
				checkReply(fc);
			}
			fc.changeWorkingDirectory(fm.folder); checkReply(fc);
			FTPFile[] ffs = fc.listFiles();
			if (ffs != null) {
				File1[] fs = toFiles(ffs);
				for (int i = 0; i != fs.length; i++) {
					FTPFile ff = fs[i].f;
					if (!ff.isFile()) {
						continue;
					}
					String fn = ff.getName();
					if (!fn.toUpperCase().endsWith(".UP")) {
						continue;
					}
					String user = getUser(fn);
					Date d = ff.getTimestamp().getTime();
					fn = SupportTime.yyyyMMdd_HHmmss__Format.format(d)+".up";
					fn = toName(fn, user);
					File f = new File(dir, "ftp_"+fn);
					f = ScheduledWorker.toUnique(dir, f);
					fn = f.getName();
					log.debug("FTP file: "+fn);
					FileOutputStream out = new FileOutputStream(f);
					String fn0 = ff.getName();
					fc.setControlKeepAliveTimeout(300000);
					checkReply(fc);
					fc.retrieveFile(fn0, out);
					out.close();
					checkReply(fc);
					rename(fm, fc, fn0, fn);
				}
			}
		}
		catch (Throwable ex) {
			if (++trials >= 20) {
				ex.printStackTrace();
				log.error(ex);
			}
			String trl = "Trial "+trials+" to read FTP host";
			System.out.println(trl);
			log.debug(trl);
			return trials;
		}
		finally {
			try {
				fc.disconnect();
			}
			catch (Exception e) { }
		}
		return 0;
	}
	private static void checkReply(FTPClient fc) {
		int r = fc.getReplyCode();
		if (!FTPReply.isPositiveCompletion(r)) {
			throw new IllegalStateException(fc.getReplyString());
		}
	}
	private static String toName(String fileName, String user) {
		fileName = fileName.replace('\\', '_');
		fileName = fileName.replace('/', '_');
		fileName = fileName.replace(' ', '_');
		return user+fileName;
	}
	private static void rename(FtpManager fm, FTPClient fc,
		String from, String to) throws Exception {
		for (int i = 0; !fc.rename(fm.folder+'/'+from, fm.archiveFolder+'/'+to) &&
			i != 10000; i++) {
			to = ScheduledWorker.toUniqueName(to, i);
		}
		checkReply(fc);
	}
	private static String getUser(String fileName) {
		if (fileName.length() > 8) {
			int i = fileName.indexOf('-', 8);
			if (i > 0) {
				return fileName.substring(8, i)+'@';
			}
		}
		return "unknown@";
	}
	private static File1[] toFiles(FTPFile[] fs) {
		File1[] arr = new File1[fs.length];
		for (int i = 0; i != fs.length; i++) {
			arr[i] = new File1();
			arr[i].f = fs[i];
		}
		Arrays.sort(arr);
		return arr;
	}
	private static class File1 implements Comparable<File1> {
		FTPFile f;
		@Override
		public int compareTo(File1 f1) {
			return f.getTimestamp().compareTo(f1.f.getTimestamp());
		}
	}
}
