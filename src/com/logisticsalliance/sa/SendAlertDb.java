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
	"SELECT dvstore#,dvshpd,dvdlvd,dvdlvt,dvroute,dvcar,dvcom," +
	"mvnarvd,mvnarvt,mvstsd,mvreexc,tmdsc,mvtext,mvcrtz " +
	"FROM OS61LYDTA.OSPDLVS, " +
	"OS61LYDTA.SMPMOVM LEFT JOIN OS61LYDTA.##PTABM ON mvreexc = tment " +
	"WHERE dvstore# = mvstore# AND dvshpd=mvshpd AND dvcom=mvcom AND dvdc=mvdc AND " +
	"mvcrtz>=? AND mvcrtz<? " +
//	"DVCHGZ NOT IN (SELECT alts FROM OS61LYDTA.OSPALERTS) " +
	"ORDER BY 1, 3, 4 DESC, 6";

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
					t0, t1, nextTime, 120000, 0, log);
				if (t0 == null) {
					break;
				}
				// Select exceptions
				Timestamp t2 = new Timestamp(t0.getTime()-120000);// less 15 minutes
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
		int storeN = 0;
		Date delDate = null;
		String carrier = null;
		AlertNote an = null;
		ArrayList<AlertNote> al = new ArrayList<AlertNote>(8);
		while (true) {
			storeN = rs.getInt(1);
			delDate = rs.getDate(3);
			carrier = rs.getString(6);
			if (an == null) {
				an = newData(storeN, delDate, carrier, rs);
			}
			else if (storeN != an.storeN || !delDate.equals(an.delDate) ||
				!carrier.equals(an.carrier)) {
				al.add(an);
				an = newData(storeN, delDate, carrier, rs);
			}
			String cmdty = rs.getString(7).trim();
			an.cmdtySet.add(cmdty);
			addItem(an, rs);
			if (!rs.next()) {
				rs.close();
				al.add(an);
				break;
			}
		}
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
	private static AlertNote newData(int storeN, Date delDate,
		String carrier, ResultSet rs) throws Exception {
		AlertNote an = new AlertNote();
		an.storeN = storeN;
		an.shipDate = rs.getDate(2);
		an.delDate = delDate;
		an.arrivalTime = SupportTime.toHH_mm(rs.getString(4));
		an.newDelDate = rs.getDate(8);
		an.newArrivalTime = SupportTime.toHH_mm(rs.getString(9));
		an.route = rs.getString(5);
		an.carrier = carrier;
		if (!alertPrefs.containsKey(storeN)) {
			Alert[] alerts = { new Alert(), new Alert(), new Alert(), new Alert()};
			AlertDB.select(storeN, alerts, false);
			alertPrefs.put(storeN, alerts);
		}
		return an;
	}
	private static void addItem(AlertNote an, ResultSet rs) throws Exception {
		AlertItem ai = new AlertItem();
		ai.status = rs.getString(10).trim();
		ai.reasonID = rs.getString(11).trim();
		ai.reason = rs.getString(12);
		if (ai.reason == null) {
			ai.reason = "noDescription";
		}
		else { ai.reason = ai.reason.trim();}
		ai.comment = rs.getString(13).trim();
		ai.ts = rs.getTimestamp(14);
		Date tsd = rs.getDate(14);
		if (!tsd.equals(an.timestamp)) {
			an.timestamp = tsd;
		}
		if (ai.status.equalsIgnoreCase(CommonConstants.EXCE)) {
			ai.status = CommonConstants.EXCEPTION;
			ai.exception = true;
			if (!an.exception) {
				an.exception = true;
			}
		}
		an.items.add(ai);
	}
}
