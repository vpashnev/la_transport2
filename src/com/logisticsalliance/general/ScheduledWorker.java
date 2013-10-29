package com.logisticsalliance.general;

import java.io.File;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.logisticsalliance.shp.ShipmentDb;
import com.logisticsalliance.sn.NotificationDb;
import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.sqla.ConnectFactory1;
import com.logisticsalliance.sqla.SqlSupport;
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

	private File srcDir;
	private String dbPassword, dbPasswordI5;
	private int daysOutCleaning = -1;
	private Date shipDate;
	private EmailRead emailRead;
	private EmailSent emailSent;
	private EMailReports emailReports;
	private EMailEmergency emailEmergency;
	private HashMap<Integer,String> localDcMap;
	private Properties appProperties;
	private boolean stopped, sleep;
	private RnColumns rnCols;
	private Thread worker;

	private static final DecimalFormat oooo = new DecimalFormat("0000");

	public ScheduledWorker(File srcDirectory, String databasePassword, String databasePasswordI5,
		String emailReadPassword, String emailSentPassword, Properties appProps,
		HashMap<Integer,String> localDCs, EMailReports mr, RnColumns rcs) {
		if (!srcDirectory.exists() && !srcDirectory.mkdir()) {
			throw new IllegalArgumentException("The directory '"+srcDirectory+"' does not exist");
		}
		srcDir = srcDirectory;
		dbPassword = databasePassword;
		dbPasswordI5 = databasePasswordI5;
		emailRead = new EmailRead(appProps, emailReadPassword);
		emailSent = new EmailSent(appProps, emailSentPassword);
		emailEmergency = new EMailEmergency(emailSent);
		appProperties = appProps;
		localDcMap = localDCs;
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
		int periodInMins = p == null ? 5 : Integer.parseInt(p);
		Calendar curDate = Calendar.getInstance();
		int curHour = 0;

		String quickReport = getValue(appProperties, "quickReport"),
			storeNotifications = getValue(appProperties, "storeNotifications"),
			shipments = getValue(appProperties, "shipments"),
			ttTable = getValue(appProperties, "ttTable"),
			onlyTestStoresToRpt = getValue(appProperties, "onlyTestStoresToRpt"),
			notifyHoursAhead = getValue(appProperties, "notifyHoursAhead"),
			notifyStartingTime = getValue(appProperties, "notifyStartingTime"),
			notifyEndingTime = getValue(appProperties, "notifyEndingTime"),
			shipmentDate = getValue(appProperties, "shipmentDate"),
			daysOutCleaningDB = getValue(appProperties, "daysOutCleaningDB");
		int nTime = notifyHoursAhead == null ? 30 : Integer.parseInt(notifyHoursAhead);
		nTime *= SupportTime.HOUR;

		HashSet<Integer> storeSubset = getStoreSubset(getValue(appProperties, "testStores"));
		//database
		ConnectFactory1 cf = ConnectFactory1.one();
		cf.setDriver(getValue(appProperties, "driver"));
		cf.setUrl(getValue(appProperties, "url"));
		cf.setUser(getValue(appProperties, "user"));
		cf.setPassword(dbPassword);
		ConnectFactory cf1 = new ConnectFactory(
			getValue(appProperties, "driverI5"),
			getValue(appProperties, "urlI5"),
			getValue(appProperties, "userI5"),
			dbPasswordI5
		);
		ShipmentDb.setConnectFactoryI5(cf1);
		TtTableDb.setConnectFactoryI5(cf1);
		while (!stopped) {
			System.out.println("Data in process..., starting at "+
				SupportTime.dd_MM_yyyy_HH_mm_Format.format(new java.util.Date()));
			Thread emgcy = new Thread(emailEmergency);
			emgcy.setDaemon(true);
			emgcy.start();
			try {
				//Store Schedule
				if (emailRead.emailUnread == null && isTimeToReadStoreSchedule()) {
					//email
					EMailReader.read(sysProps, emailRead, emailRead.dsFolder,
						emailRead.dsArchiveFolder, dsFolder);
				}
				//database
				StoreScheduleDb.update(dsFolder, dsaFolder);

				//Roadnet
				if (emailRead.emailUnread == null && isTimeToReadRoadnet()) {
					//email
					EMailReader.read(sysProps, emailRead, emailRead.rnFolder,
						emailRead.rnArchiveFolder, rnFolder);
				}
				//database
				ShipmentDataDb.update(rnFolder, rnaFolder, rnCols, localDcMap);

				boolean isOnlyTestStoresToRpt = onlyTestStoresToRpt != null && storeSubset != null;
				if (storeNotifications != null) {
					//Notify Stores
					NotificationDb.process(notifyStartingTime, notifyEndingTime,
						nTime, emailSent, storeSubset, isOnlyTestStoresToRpt);
					notifyStartingTime = null; notifyEndingTime = null;
				}

				if (ttTable != null) {
					//ttTable
					TtTableDb.process();
				}

				//Make daily reports
				Calendar c = SqlSupport.getDb2CurrentTime();
				int h = c.get(Calendar.HOUR_OF_DAY);
				if (emailSent.rnFileListTo != null && h == 21 && h != curHour) {
					emailReports.sendRnFileList(emailSent);
					curHour = h;
				}
				else if (h != 21) { curHour = h;}
				if (quickReport != null ||
					c.get(Calendar.DAY_OF_MONTH) != curDate.get(Calendar.DAY_OF_MONTH)) {

					if (shipDate == null) {
						shipDate = ShipmentDb.getDate(shipmentDate, c);
					}
					if (shipments != null) {
						//Shipments
						ShipmentDb.process(shipDate, emailSent);
					}
					if (shipmentDate != null) {
						shipDate = ShipmentDb.getDate(null, c);
						shipmentDate = null;
					}

					emailReports.send(emailSent);
					curDate = c;
					ShipmentDataDb.localDcMissing.clear();
					NotificationDb.clearCarriersNotFound();
					ShipmentDb.clearCarriersNotFound();
					TtTableDb.clearCarriersNotFound();
					quickReport = null;

					ShipmentDataDb.updateDailyRnFiles(cf.getConnection(), null);
					if (daysOutCleaningDB != null) {
						if (daysOutCleaning < 0) {
							daysOutCleaning = Integer.parseInt(daysOutCleaningDB);
						}
						CleanDb.clean(c, daysOutCleaning);
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
	private static boolean isTimeToReadStoreSchedule() {
		Calendar c = Calendar.getInstance();
		//int t = c.get(Calendar.DAY_OF_WEEK);
		//if (t == 1 || t == 7) { return false;}
		int t = c.get(Calendar.HOUR_OF_DAY);
		if (t == 9 || t == 10) { return true;}
		return false;
	}
	private static boolean isTimeToReadRoadnet() {
		Calendar c = Calendar.getInstance();
		int t = c.get(Calendar.HOUR_OF_DAY);
		if (t > 10) { return true;}
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
			String fn = f.getName();
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
			f = new File(dir, fn);
		}
		return f;
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

	private static String getValue(Properties props, String propName) {
		String v = props.getProperty(propName);
		if (v != null) {
			v = v.trim();
			return v.isEmpty() ? null : v;
		}
		return null;
	}
	static class EmailRead {
		String protocol, host, email, password, dsFolder, dsArchiveFolder,
			rnFolder, rnArchiveFolder, emailUnread;

		private EmailRead(Properties props, String pwd) {
			protocol = getValue(props, "emailReadProtocol");
			host = getValue(props, "emailReadHost");
			email = getValue(props, "emailRead");
			password = pwd;
			dsFolder = getValue(props, "emailDsFolder");
			dsArchiveFolder = getValue(props, "emailDsArchiveFolder");
			rnFolder = getValue(props, "emailRnFolder");
			rnArchiveFolder = getValue(props, "emailRnArchiveFolder");
			emailUnread = getValue(props, "emailUnread");
		}
	}

	public static class EmailSent {
		public String host, port, email, user, password, sentToBbc, storePrefix1, storePrefix2,
			rcptHost, qcStorePrefix1, qcStorePrefix2, qcRcptHost, reportsTo, rnFileListTo,
			emergencyTo, emailSentOnlyToBbc, emailUnsent;

		private EmailSent(Properties props, String pwd) {
			host = getValue(props, "emailSentHost");
			port = getValue(props, "emailSentPort");
			email = getValue(props, "emailSentFrom");
			user = getValue(props, "emailUser");
			password = pwd;
			sentToBbc = getValue(props, "emailSentToBbc");
			storePrefix1 = getValue(props, "emailStorePrefix1");
			storePrefix2 = getValue(props, "emailStorePrefix2");
			rcptHost = getValue(props, "emailRcptHost");
			qcStorePrefix1 = getValue(props, "emailQcStorePrefix1");
			qcStorePrefix2 = getValue(props, "emailQcStorePrefix2");
			qcRcptHost = getValue(props, "emailQcRcptHost");
			reportsTo = getValue(props, "emailReportsTo");
			rnFileListTo = getValue(props, "emailRnFileListTo");
			emergencyTo = getValue(props, "emailEmergencyTo");
			emailUnsent = getValue(props, "emailUnsent");
			emailSentOnlyToBbc = getValue(props, "emailSentOnlyToBbc");
		}
	}
}
