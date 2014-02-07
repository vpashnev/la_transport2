package com.logisticsalliance.sa;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.mail.Session;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.ScheduledWorker.EmailSent;
import com.logisticsalliance.sn.Notify1;
import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.sqla.ConnectFactory1;
import com.logisticsalliance.sqla.SqlSupport;
import com.logisticsalliance.tt.web.Alert;
import com.logisticsalliance.tt.web.AlertDB;
import com.logisticsalliance.util.SupportTime;

/**
 * This class selects the delivery alerts that should be sent to stores by email.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class SendAlertDb extends Notify1 {

	private static Logger log = Logger.getLogger(SendAlertDb.class);

	private static String
	SQL_SEL_ALERT =
	"SELECT dvstore#,dvshpd,dvdlvd,dvdlvt,dvsrvtime,dvdc,dvroute,dvstop#,dvcom," +
	"dvpallets,dvetato,dvetatc,dvcar,mvnarvd,mvnarvt,mvstsd,mvreexc,tmdsc,mvtext,mvcrtz " +
	"FROM OS61LYDTA.OSPDLVS, " +
	"OS61LYDTA.SMPMOVM LEFT JOIN OS61LYDTA.##PTABM ON mvreexc = tment " +
	"WHERE dvstore# = mvstore# AND dvshpd=mvshpd AND dvcom=mvcom AND dvdc=mvdc AND " +
	"mvcrtz>=? AND mvcrtz<? " +
//	"DVCHGZ NOT IN (SELECT alts FROM OS61LYDTA.OSPALERTS) " +
	"ORDER BY 1, 3 DESC, 11 DESC, 4 DESC";

	private final static String
	SQL_SEL_ENVR = "SELECT time_store_alerted FROM la.henvr",
	SQL_INS_ENVR = "INSERT INTO la.henvr (time_store_alerted) VALUES (?)",
	SQL_UPD_ENVR = "UPDATE la.henvr SET time_store_alerted=?";

	static HashMap<Integer,Alert[]> alertPrefs = new HashMap<Integer,Alert[]>(4, .5f);

	private static boolean done;
	private static Timestamp nextTime = new Timestamp(0);
	private static ConnectFactory connectFactoryI5;

	public static void setConnectFactoryI5(ConnectFactory cf) {
		ConnectFactory cf1 = new ConnectFactory(cf.getDriver(),
			"jdbc:as400:tmsodev.nulogx.com;prompt=false", cf.getUser(), cf.getPassword());
		connectFactoryI5 = cf1;
	}

	public static void process(final String alertStartingTime, final String alertEndingTime,
		final EmailSent es, final boolean alertStoresByPhone) throws InterruptedException {
		done = false;
		Thread t = new Thread() {
			@Override
			public void run() {
				process1(alertStartingTime, alertEndingTime, es, alertStoresByPhone);
				done = true;
			}
		};
		t.setDaemon(true);
		t.start();
		int i = 0;
		while (!done && i++ != 180) {
			Thread.sleep(5000);
		}
		if (!done) {
			log.error("Report incomplete Store Alerts");
		}
	}
	private static void process1(String alertStartingTime, String alertEndingTime,
		EmailSent es, boolean alertStoresByPhone) {
		Timestamp t0 = null, t1 = null;
		try {
			if (alertStartingTime != null) {
				t0 = SupportTime.parseDdMMyyyyHHmm(alertStartingTime);
			}
			if (alertEndingTime != null) {
				t1 = SupportTime.parseDdMMyyyyHHmm(alertEndingTime);
			}
		}
		catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
		alertPrefs.clear();
		Session s = null;
		Connection con = null, con1 = null, con5 = null;
		try {
			con = ConnectFactory1.one().getConnection();
			con1 = ConnectFactory1.one().getConnection();
			con5 = connectFactoryI5.getConnection();
			PreparedStatement selSt = con5.prepareStatement(SQL_SEL_ALERT);
			if (t1 == null) {
				t1 = SqlSupport.getDb2CurrentTime(con1);
			}
			while (true) {
				t0 = getNextTime(con1, SQL_SEL_ENVR, SQL_INS_ENVR,
					t0, t1, nextTime, 150000, 0, log);
				if (t0 == null) {
					break;
				}
				// Select alerts
				Timestamp t2 = new Timestamp(t0.getTime()-150000);// less 15 minutes
				s = select(selSt, t2, t0, s, es, alertStoresByPhone);
				if (alertEndingTime == null) {
					updateNotifyEndingTime(con1, SQL_UPD_ENVR, t0);
				}
			}
			selSt.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			log.error(ex);
		}
		finally {
			ConnectFactory.close(con);
			ConnectFactory.close(con1);
			ConnectFactory.close(con5);
		}
	}
	private static Session select(PreparedStatement st, Timestamp t0, Timestamp t,
		Session s, EmailSent es, boolean alertStoresByPhone) throws Exception {
		st.setTimestamp(1, t0);
		st.setTimestamp(2, t);
		ResultSet rs = st.executeQuery();
		if (!rs.next()) {
			rs.close();
			return s;
		}
		ArrayList<TrackingNote> al = select(rs, true);
		if (al.size() != 0) {
			String interval = SupportTime.dd_MM_yyyy_HH_mm_Format.format(t0)+" - "+
				SupportTime.dd_MM_yyyy_HH_mm_Format.format(t);
			log.debug("\r\n\r\nALERTS for "+interval+"\r\n\r\n"+al);
			if (es.emailUnsent == null) {
				Thread.sleep(2000);
				s = AlertMail.send(s, es, al, interval, alertStoresByPhone);
			}
		}
		return s;
	}
	public static ArrayList<TrackingNote> select(ResultSet rs, boolean alertPref) throws Exception {
		int storeN = 0;
		Date delDate = null;
		String delTimeFrom = null;
		TrackingNote tn = null;
		ArrayList<TrackingNote> al = new ArrayList<TrackingNote>(8);
		while (true) {
			storeN = rs.getInt(1);
			delDate = rs.getDate(3);
			delTimeFrom = SupportTime.toHH_mm(rs.getString(11));
			if (tn == null) {
				tn = newData(storeN, delDate, delTimeFrom, rs, alertPref);
			}
			else if (storeN != tn.storeN || !delDate.equals(tn.delDate) ||
				!delTimeFrom.equals(tn.delTimeFrom)) {
				addData(al, tn);
				tn = newData(storeN, delDate, delTimeFrom, rs, alertPref);
			}
			putCmdtyAndAlerts(tn, rs);
			if (!rs.next()) {
				rs.close();
				addData(al, tn);
				break;
			}
		}
		return al;
	}
	private static TrackingNote newData(int storeN, Date delDate,
		String delTimeFrom, ResultSet rs, boolean alertPref) throws Exception {
		TrackingNote tn = new TrackingNote();
		tn.storeN = storeN;
		tn.shipDate = rs.getDate(2);
		tn.delDate = delDate;
		tn.delDate1 = SupportTime.dd_MM_yyyy_Format.format(delDate);
		tn.arrivalTime = SupportTime.toHH_mm(rs.getString(4));
		tn.serviceTime = SupportTime.toHH_mm(rs.getString(5));
		tn.newDelDate = rs.getDate(14);
		String nt = rs.getString(15);
		tn.newArrivalTime = nt == null ? "" : SupportTime.toHH_mm(nt);
		tn.dc = rs.getString(6).trim();
		tn.route = rs.getString(7);
		tn.stopN = rs.getString(8);
		tn.delTimeFrom = delTimeFrom;
		tn.delTimeTo = SupportTime.toHH_mm(rs.getString(12));
		tn.carrier = rs.getString(13);
		if (alertPref && !alertPrefs.containsKey(storeN)) {
			Alert[] alerts = { new Alert(), new Alert(), new Alert()};
			AlertDB.select(storeN, alerts, false);
			alertPrefs.put(storeN, alerts);
		}
		return tn;
	}
	private static void addData(ArrayList<TrackingNote> al, TrackingNote tn) {
		if (tn.cmdtyAlerts.containsKey(CommonConstants.DCF) ||
			!CommonConstants.CCS.equals(tn.carrier)) {
			tn.serviceTime = CommonConstants.N_A;
		}
		tn.updateAlerts();
		al.add(tn);
	}
	private static void putCmdtyAndAlerts(TrackingNote tn, ResultSet rs) throws Exception {
		String cmdty = rs.getString(9).trim();
		Alerts as = tn.cmdtyAlerts.get(cmdty);
		if (as == null) {
			as = new Alerts();
			tn.cmdtyAlerts.put(cmdty, as);
		}
		as.pallets = rs.getInt(10);
		AlertItem ai = new AlertItem();
		ai.status = trim(rs, 16);
		if (ai.status.length() == 0) {
			ai.status = "Plan";
		}
		ai.reasonID = trim(rs, 17);
		ai.comment = trim(rs, 19).trim();
		if (ai.status.equalsIgnoreCase(CommonConstants.EXCE)) {
			ai.status = CommonConstants.EXCEPTION;
			ai.exception = true;
			if (!tn.exception) {
				tn.exception = true;
			}
		}
		if (!as.hset.contains(ai)) {
			as.hset.add(ai);
			ai.reason = trim(rs, 18);
			ai.ts = rs.getTimestamp(20);
			Date tsd = rs.getDate(20);
			if (tsd == null) {
				ai.ts = new Timestamp(0);
				tn.timestamp = new Date(0);
			}
			else if (!tsd.equals(tn.timestamp)) {
				tn.timestamp = tsd;
			}
		}
	}
	private static String trim(ResultSet rs, int idx) throws Exception {
		String v = rs.getString(idx);
		if (v == null) {
			return "";
		}
		else { return v.trim();}
	}
}
