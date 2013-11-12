package com.logisticsalliance.general;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.ScheduledWorker.EmailRead;
import com.logisticsalliance.util.SupportTime;
import com.sun.mail.imap.IMAPFolder;

/**
 * This class reads file attachments from email.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class EMailReader {

	private static Logger log = Logger.getLogger(EMailReader.class);

	public static void read(Properties sysProps, EmailRead er,
		String folder, String archiveFolder, File dir) throws InterruptedException {
		int trials = read1(sysProps, er, folder, archiveFolder, dir, 0);
		while (trials > 0 && trials < 20) {
			Thread.sleep(20000);
			trials = read1(sysProps, er, folder, archiveFolder, dir, trials);
		}
	}
	private static int read1(Properties sysProps, EmailRead er,
		String folder, String archiveFolder, File dir, int trials) {
		Session s = Session.getDefaultInstance(sysProps, null);
		IMAPFolder dsf = null, dsfa = null;
		Store store = null;
		try {
			store = s.getStore(er.protocol);
			store.connect(er.host, er.email, er.password);
			dsf = (IMAPFolder)store.getFolder(folder);
			if(!dsf.isOpen()) { dsf.open(Folder.READ_WRITE);}
			Message arr[] = dsf.getMessages();
			for (int i = 0; i != arr.length; i++) {
				Message m = arr[i];
				String user = getUser(m);
				Object c = m.getContent();
				if (c instanceof Multipart) {
					Multipart mp = (Multipart)c;
					int count = mp.getCount();
					for (int j = 0; j != count; j++) {
						MimeBodyPart p = (MimeBodyPart)mp.getBodyPart(j);
						if (Part.ATTACHMENT.equalsIgnoreCase(p.getDisposition())) {
							String fn = p.getFileName(), fnuc = fn.toUpperCase();
							if (fnuc.endsWith(".GZ")) {
								fn = toName(fn, user);
								fn = fn.substring(0, fn.length()-3);
								log.debug("Email message: "+fn);
								write(p.getInputStream(), dir, fn);
							}
							else {
								if (fnuc.endsWith(".UP")) {
									Date d = m.getReceivedDate();
									fn = SupportTime.yyyyMMdd_HHmmss__Format.format(d)+".up";
								}
								fn = toName(fn, user);
								File f = new File(dir, fn);
								log.debug("Email message: "+fn);
								f = ScheduledWorker.toUnique(dir, f);
								p.saveFile(f);
							}
						}
					}
				}
				else {
					String ct = m.getContentType();
					if (ct.equalsIgnoreCase("APPLICATION/X-GZIP-COMPRESSED")) {
						String fn = m.getFileName();
						if (fn == null || fn.trim().isEmpty()) {
							Date d = m.getReceivedDate();
							fn = user+SupportTime.yyyyMMdd_Format.format(d)+".xls";
						}
						else {
							fn = toName(fn, user);
							if (fn.toUpperCase().endsWith(".GZ")) {
								fn = fn.substring(0, fn.length()-3);
							}
						}
						log.debug("Email message: "+fn);
						write((InputStream)c, dir, fn);
					}
					else {
						log.error("Unknown message content: "+m.getContent());
					}
				}
			}
			dsfa = (IMAPFolder)store.getFolder(archiveFolder);
			dsf.copyMessages(arr, dsfa);
			for (int i = 0; i != arr.length; i++) {
				arr[i].setFlag(Flags.Flag.DELETED, true);
			}
		}
		catch (Throwable ex) {
			if (++trials >= 20) {
				ex.printStackTrace();
				log.error(ex);
			}
			String trl = "Trial "+trials+" to read email";
			System.out.println(trl);
			log.debug(trl);
			return trials;
		}
		finally {
			try {
				if (dsf != null && dsf.isOpen()) { dsf.close(true);}
				if (dsfa != null && dsfa.isOpen()) { dsfa.close(false);}
				if (store != null) { store.close();}
			}
			catch (Exception ex) { }
		}
		return 0;
	}
	private static void write(InputStream in, File dir, String fileName) throws Exception {
		File f = new File(dir, fileName);
		f = ScheduledWorker.toUnique(dir, f);
		GZIPInputStream gzis = new GZIPInputStream(in);
		FileOutputStream fos = null;
		try {
			byte[] buffer = new byte[4096];
			fos = new FileOutputStream(f);
			int n;
			while ((n = gzis.read(buffer)) != -1) {
				fos.write(buffer, 0, n);
			}
		}
		finally {
			gzis.close();
			if (fos != null) { fos.close();}
		}
	}
	private static String toName(String fileName, String user) {
		fileName = fileName.replace('\\', '_');
		fileName = fileName.replace('/', '_');
		fileName = fileName.replace(' ', '_');
		return user+fileName;
	}
	private static String getUser(Message m) throws Exception {
		Address[] arr = m.getFrom();
		if (arr != null && arr.length != 0) {
			String a = arr[0].toString();
			int i = a.lastIndexOf('@');
			if (i == -1) {
				return a+'@';
			}
			else {
				int i0 = a.lastIndexOf('<', i)+1;
				return a.substring(i0, i+1);
			}
		}
		return "unknown@";
	}
}
