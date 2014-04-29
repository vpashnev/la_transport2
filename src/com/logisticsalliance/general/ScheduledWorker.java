package com.logisticsalliance.general;

import java.io.File;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.glossium.sqla.ConnectFactory;
import com.glossium.sqla.ConnectFactory1;
import com.glossium.sqla.SqlSupport;
import com.logisticsalliance.sa.SendAlertDb;
import com.logisticsalliance.shp.ShipmentDb;
import com.logisticsalliance.sn.NotificationDb;
import com.logisticsalliance.tt.TtTableDb;
import com.logisticsalliance.util.SupportTime;

/**
 * This class schedules reading files to process.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class ScheduledWorker implements Runnable {

	private static Logger log = Logger.getLogger(ScheduledWorker.class);
	
	public static String shipQryCarriers;

	private File appDir, srcDir;
	private String dbPassword, dbPasswordI5, ksPassword;
	private int daysOutCleaning = -1;
	private FtpManager ftpManager;
	private EmailRead emailRead;
	private EmailSent1 emailSent1;
	private EmailSent ttEmailSent;
	private EmailReports emailReports;
	private EmailEmergency emailEmergency;
	private Properties appProperties;
	private boolean stopped, sleep;
	private RnColumns rnCols;
	private Thread worker;

	private static final DecimalFormat oooo = new DecimalFormat("0000");

	public ScheduledWorker(File appDirectory, File srcDirectory, String databasePassword,
		String databasePasswordI5, String ftpPassword, String emailReadPassword,
		String emailSentPassword, String ttEmailSentPassword, String keyStorePassword,
		Properties appProps, EmailReports mr, RnColumns rcs) {
		if (!srcDirectory.exists() && !srcDirectory.mkdir()) {
			throw new IllegalArgumentException("The directory '"+srcDirectory+"' does not exist");
		}
		appDir = appDirectory;
		srcDir = srcDirectory;
		dbPassword = databasePassword;
		dbPasswordI5 = databasePasswordI5;
		ksPassword = keyStorePassword;
		ftpManager = new FtpManager(appProps, ftpPassword);
		emailRead = new EmailRead(appProps, emailReadPassword);
		emailSent1 = new EmailSent1(appProps, emailSentPassword);
		ttEmailSent = new EmailSent(appProps, ttEmailSentPassword, "ttE");
		emailEmergency = new EmailEmergency(emailSent1);
		appProperties = appProps;
		emailReports = mr;
		rnCols = rcs;
		shipQryCarriers = getValue(appProps, "shipQryCarriers");
	}

	@Override
	public void run() {
		Properties sysProps = System.getProperties();
		sysProps.setProperty("mail.store.protocol", getValue(appProperties, "emailReadProtocol"));
		new File(srcDir, "archive").mkdir();
		File dsFolder = new File(srcDir, "ds"), dsaFolder = new File(srcDir, "archive/ds"),
			rnFolder = new File(srcDir, "rn"), rnaFolder = new File(srcDir, "archive/rn");
		dsFolder.mkdir(); rnFolder.mkdir();
		// period
		String p = getValue(appProperties, "periodToRead");
		Calendar curDate = Calendar.getInstance();
		int curHour = 0, curHour1 = 0, periodInMins = p == null ? 5 : Integer.parseInt(p);

		String quickReport = getValue(appProperties, "quickReport"),
			storeNotifications = getValue(appProperties, "storeNotifications"),
			storeAlerts = getValue(appProperties, "storeAlerts"),
			shipments = getValue(appProperties, "shipments"),
			ttTable = getValue(appProperties, "ttTable"),
			onlyTestStoresToRpt = getValue(appProperties, "onlyTestStoresToRpt"),
			sendDelayedNotesOff = getValue(appProperties, "sendDelayedNotesOff"),
			notifyHoursAhead = getValue(appProperties, "notifyHoursAhead"),
			notifyStartingTime = getValue(appProperties, "notifyStartingTime"),
			notifyEndingTime = getValue(appProperties, "notifyEndingTime"),
			alertStartingTime = getValue(appProperties, "alertStartingTime"),
			alertEndingTime = getValue(appProperties, "alertEndingTime"),
			shipmentDate = getValue(appProperties, "shipmentDate"),
			daysOutCleaningDB = getValue(appProperties, "daysOutCleaningDB"),
			alertStoresByPhone = getValue(appProperties, "alertStoresByPhone");
		int nTime = notifyHoursAhead == null ? 30 : Integer.parseInt(notifyHoursAhead);
		nTime *= SupportTime.HOUR;

		HashSet<Integer> storeSubset = getStoreSubset(getValue(appProperties, "testStores"));
		//database
		ConnectFactory1 cf = ConnectFactory1.one();
		ConnectFactory cfI5 = SupportGeneral.makeDataSource1I5(appProperties,
			dbPassword, dbPasswordI5);
		ShipmentDb.setConnectFactoryI5(cfI5);
		TtTableDb.setConnectFactoryI5(cfI5);
		SendAlertDb.setConnectFactoryI5(cfI5);
		while (!stopped) {
			System.out.println("Data in process..., starting at "+
				SupportTime.dd_MM_yyyy_HH_mm_Format.format(new java.util.Date()));
			Thread emgcy = new Thread(emailEmergency);
			emgcy.setDaemon(true);
			emgcy.start();
			try {
				//Test1.updateStoreEmail(emailSent1);
				if (!UserAuth.ok) { UserAuth.process(appDir, ksPassword, 3000, cf);}
				//Store Schedule
				if (emailRead.emailUnread == null && isTimeToReadStoreSchedule()) {
					//email
					EmailReader.read(sysProps, emailRead, emailRead.dsFolder,
						emailRead.dsArchiveFolder, dsFolder);
				}
				//database
				StoreScheduleDb.update(dsFolder, dsaFolder);

				//Roadnet
				if (ftpManager.unread == null && isTimeToReadRoadnet()) {
					//email
					FtpReader.read(ftpManager, rnFolder, emailSent1);
				}
				if (emailRead.emailUnread == null && isTimeToReadRoadnet()) {
					//email
					EmailReader.read(sysProps, emailRead, emailRead.rnFolder,
						emailRead.rnArchiveFolder, rnFolder);
				}
				//database
				ShipmentDataDb.update(rnFolder, rnaFolder, rnCols);

				if (storeNotifications != null) {
					//Notify Stores
					boolean isOnlyTestStoresToRpt = onlyTestStoresToRpt != null && storeSubset != null,
						isSendDelayedNotesOff = sendDelayedNotesOff != null;
					NotificationDb.process(notifyStartingTime, notifyEndingTime, nTime,
						emailSent1, storeSubset, isOnlyTestStoresToRpt, isSendDelayedNotesOff);
					notifyStartingTime = null; notifyEndingTime = null;
				}

				if (storeAlerts != null) {
					//Alert Stores
					SendAlertDb.process(alertStartingTime, alertEndingTime,
						ttEmailSent, alertStoresByPhone != null);
					alertStartingTime = null; alertEndingTime = null;
				}

				Calendar c = SqlSupport.getDb2CurrentTime();
				int h = c.get(Calendar.HOUR_OF_DAY);

				if (ttTable != null && quickReport == null &&
					(h == 11 || h == 14 || h == 17) && h != curHour) {
					//ttTable
					if (h == 14) {
						TtTableDb.process(new Date(c.getTimeInMillis()), emailSent1, false);
					}
					TtTableDb.process(new Date(c.getTimeInMillis()+SupportTime.DAY),
						emailSent1, false);
					curHour = h;
				}
				else if (h != 11 && h != 14 && h != 17) { curHour = h;}

				//Make daily reports
				if (emailSent1.rnFileListTo != null && h == 21 && h != curHour1) {
					String m = emailReports.sendRnFileList(emailSent1);
					log.debug("\r\n\r\nList of processed files:\r\n\r\n"+m+"\r\n");
					curHour1 = h;
					ShipmentDataDb.updateDailyRnFiles(null, null);
				}
				else if (h != 21) { curHour1 = h;}
				if (quickReport != null ||
					c.get(Calendar.DAY_OF_MONTH) != curDate.get(Calendar.DAY_OF_MONTH)) {
					quickReport = null;

					Date d = getShipDate(shipmentDate, c);
					if (shipments != null) {
						//Shipments
						ShipmentDb.process(d, getValue(appProperties, "ftpNLServer"), emailSent1);
					}
					if (ttTable != null) {
						//ttTable
						TtTableDb.process(d, emailSent1, true);
					}
					if (shipmentDate != null) {
						shipmentDate = null;
					}

					if (ShipmentDb.getTrials() == 0 && TtTableDb.getTrials() == 0) {
						curDate = c;
						emailReports.send(emailSent1);

						ShipmentDataDb.localDcMissing.clear();
						NotificationDb.clearCarriersNotFound();

						if (daysOutCleaningDB != null) {
							if (daysOutCleaning < 0) {
								daysOutCleaning = Integer.parseInt(daysOutCleaningDB);
							}
							CleanDb.clean(c, daysOutCleaning);
						}
					}
				}
			}
			catch (Exception ex) {
				sleep = true;
				ex.printStackTrace();
				log.error(ex);
			}
			emgcy.interrupt();
			sleep = true;
			try {
				System.out.println("Idle "+periodInMins+" minutes, starting at "+
					SupportTime.dd_MM_yyyy_HH_mm_Format.format(new java.util.Date()));
				System.out.print(">");
				Thread.sleep(periodInMins*60000);
			}
			catch (InterruptedException e) { }
			finally { sleep = false;}
		}
	}
	private static Date getShipDate(String date, Calendar curTime) throws Exception {
		if (date == null) {
			return new Date(curTime.getTimeInMillis());
		}
		else {
			java.util.Date d = SupportTime.dd_MM_yyyy_Format.parse(date);
			return new Date(d.getTime());
		}
	}
	private static boolean isTimeToReadStoreSchedule() {
		Calendar c = Calendar.getInstance();
		int t = c.get(Calendar.HOUR_OF_DAY);
		if (t == 9) { return true;}
		return false;
	}
	private static boolean isTimeToReadRoadnet() {
		Calendar c = Calendar.getInstance();
		int t = c.get(Calendar.HOUR_OF_DAY);
		if (t != 9) { return true;}
		return false;
	}
	private static HashSet<Integer> getStoreSubset(String stores) {
		if (stores == null) { return null;}
		HashSet<Integer> hs = new HashSet<Integer>();
		String[] arr = stores.split("\\,");
		for (int i = 0; i != arr.length; i++) {
			String v = arr[i].trim();
			if (!v.isEmpty()) { hs.add(Integer.parseInt(v));}
		}
		return hs.size() == 0 ? null : hs;
	}
	static void move(File[] fs, File toFolder) {
		for (int i = 0; i != fs.length; i++) {
			File f = fs[i], f1 = new File(toFolder, f.getName());
			f1 = toUnique(toFolder, f1);
			f.renameTo(f1);
		}
	}
	static File toUnique(File dir, File f) {
		if (!dir.exists()) {
			if (!dir.mkdir()) { return f;}
		}
		for (int i = 0; f.exists() && i != 10000; i++) {
			String fn = toUniqueName(f.getName(), i);
			f = new File(dir, fn);
		}
		return f;
	}
	static String toUniqueName(String fn, int i) {
		int idx = fn.lastIndexOf('.');
		if (idx == -1) {
			fn = fn+i;
		}
		else {
			int j = idx-1, idx0 = j-4;
			for (; j != idx0 && j >= 0; j--) {
				char c = fn.charAt(j);
				if (!Character.isDigit(c)) { break;}
			}
			String n = oooo.format(i+1);
			StringBuilder b = new StringBuilder(fn);
			if (j == idx0++) {
				b.delete(idx0, idx);
				b.insert(idx0, n);
			}
			else { b.insert(idx, n);}
			fn = b.toString();
		}
		return fn;
	}

	void start() {
		worker = new Thread(this);
		worker.start();
	}
	boolean stop() {
		if (sleep) {
			stopped = true;
			worker.interrupt();
			return true;
		}
		else {
			System.out.println("Cannot terminate process. Try later...");
			System.out.print(">");
			return false;
		}
	}

	static String getValue(Properties props, String propName) {
		return SupportGeneral.getValue(props, propName);
	}
	static class FtpManager {
		String host, user, password, folder, archiveFolder, unread;

		private FtpManager(Properties props, String pwd) {
			host = getValue(props, "ftpHost");
			user = getValue(props, "ftpUser");
			password = pwd;
			folder = getValue(props, "ftpFolder");
			archiveFolder = getValue(props, "ftpArchiveFolder");
			unread = getValue(props, "ftpUnread");
		}
	}
}
