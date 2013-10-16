package com.logisticsalliance.sn;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.mail.Session;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.DsKey;
import com.logisticsalliance.general.ScheduledWorker.EmailSent;
import com.logisticsalliance.sql.ConnectFactory;
import com.logisticsalliance.sql.ConnectFactory1;
import com.logisticsalliance.util.SupportTime;

/**
 * This class selects the deliveries that should be sent to stores by email.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class NotificationDb {

	private static Logger log = Logger.getLogger(NotificationDb.class);

	private final static String SQL_SEL_DELIVERIES =
		"SELECT " +
		"sd.store_n, sd.cmdty, dc, sd.ship_date, sd.del_date, route_n, arrival_time, service_time," +
		"order_n, pallets, del_time_from, del_time_to, province, del_carrier, target_open," +
		"sd.first_user_file, sd.next_user_file, rno.first_user_file, rno.next_user_file," +
		"sts.first_user_file, sts.next_user_file, sts.ship_date " +
		//",TIMESTAMP(sd.del_date,del_time_from) AS del_dateTime " +

		"FROM " +
		"la.hship_data sd LEFT JOIN la.hcarrier_schedule cs ON " +
		"cs.store_n=sd.store_n AND cs.cmdty=sd.cmdty AND cs.del_day=DAYOFWEEK(sd.del_date)-1," +
		"la.hrn_order rno,la.hstore_schedule sts,la.hstore_profile sp " +

		"WHERE " +
		"ship_n=sd.n AND sp.n=sd.store_n AND " +
		"sts.store_n=sd.store_n AND sts.cmdty=sd.cmdty AND " +
		"(sts.ship_date IS NOT NULL AND sts.ship_date=sd.ship_date OR " +
		"sts.ship_date IS NULL AND sts.ship_day=DAYOFWEEK(sd.ship_date)-1) AND " +
		"TIMESTAMP(sd.del_date,del_time_from)>=? AND TIMESTAMP(sd.del_date,del_time_from)<? AND " +
		"rno.lw NOT IN ('40','45','50') " +

		"ORDER BY " +
		"sd.store_n,sts.del_time_from,sd.cmdty",

		SQL_NOW = "SELECT CURRENT_TIMESTAMP FROM SYSIBM.DUAL",
		SQL_SEL_ENVR = "SELECT time_store_notified FROM la.henvr",
		SQL_INS_ENVR = "INSERT INTO la.henvr (time_store_notified) VALUES (?)",
		SQL_UPD_ENVR = "UPDATE la.henvr SET time_store_notified=?",
				
		DCF = "DCF", DCX = "DCX", CCS = "Canada Cartage Systems", TBD ="TBD";

	private final static DeliveryNote.Cmdty DCV_CMDTY = new DeliveryNote.Cmdty("DCV", null, null, null);

	private static HashSet<DsKey> carriersNotFound = new HashSet<DsKey>();

	private static Timestamp nextTime;

	public static void clearCarriersNotFound() {
		carriersNotFound.clear();
	}
	public static void process(String notifyStartingTime, String notifyEndingTime,
		long timeAhead, EmailSent es, HashSet<Integer> storeSubset,
		boolean onlyTestStoresToRpt) throws Exception {
		long timeAheadInMins = timeAhead/60000;
		Timestamp t0 = null, t1 = null;
		if (notifyStartingTime != null) {
			java.util.Date d = SupportTime.dd_MM_yyyy_HH_mm_Format.parse(notifyStartingTime);
			t0 = new Timestamp(d.getTime());
		}
		if (notifyEndingTime != null) {
			java.util.Date d = SupportTime.dd_MM_yyyy_HH_mm_Format.parse(notifyEndingTime);
			t1 = new Timestamp(d.getTime());
		}
		Session s = null;
		Connection con = ConnectFactory1.one().getConnection(),
			con1 = ConnectFactory1.one().getConnection();
		try {
			PreparedStatement timeSt, selDelSt = con.prepareStatement(SQL_SEL_DELIVERIES);
			if (t1 == null) {
				timeSt = con1.prepareStatement(SQL_NOW);
				ResultSet rs = timeSt.executeQuery();
				if (rs.next()) {
					t1 = rs.getTimestamp(1);
				}
				else { t1 = new Timestamp(System.currentTimeMillis());}
				timeSt.close();
			}
			while (true) {
				t0 = getNextTime(con1, t0, t1, timeAhead, timeAheadInMins);
				if (t0 == null) {
					break;
				}
				// Select deliveries
				s = select(selDelSt, t0, s, es, storeSubset, onlyTestStoresToRpt);
				if (notifyEndingTime == null) {
					// Update time
					timeSt = con1.prepareStatement(SQL_UPD_ENVR);
					timeSt.setTimestamp(1, t0);
					timeSt.executeUpdate();
					con1.commit();
					timeSt.close();
				}
			}
			selDelSt.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			log.error(ex);
		}
		finally {
			ConnectFactory.close(con);
			ConnectFactory.close(con1);
		}
	}
	private static Timestamp getNextTime(Connection con, Timestamp t0,
		Timestamp t1, long timeAhead, long timeAheadInMins) throws Exception {
		PreparedStatement st = con.prepareStatement(SQL_SEL_ENVR);
		ResultSet rs = st.executeQuery();
		if (rs.next()) {
			if (t0 == null) { t0 = rs.getTimestamp(1);}
			st.close();
		}
		else {
			st.close();
			st = con.prepareStatement(SQL_INS_ENVR);
			st.setNull(1, Types.TIMESTAMP);
			st.executeUpdate();
			con.commit();
			st.close();
		}
		if (t0 == null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(t1);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			t0 = new Timestamp(cal.getTime().getTime()+timeAheadInMins);
			return t0;
		}
		else {
			long mins = (t0.getTime() - t1.getTime())/60000;
			if (mins < timeAheadInMins) { // 24-30 hours
				t0.setTime(t0.getTime()+1800000); // plus 30 minutes
				return t0;
			}
			if (!t0.equals(nextTime)) {
				log.debug("NEXT "+SupportTime.MM_dd_yyyy_HH_mm_Format.format(t0)+"\r\n");
				nextTime = t0;
			}
		}
		return null;
	}
	private static Session select(PreparedStatement st, Timestamp t, Session s, EmailSent es,
		HashSet<Integer> storeSubset, boolean onlyTestStoresToRpt) throws Exception {
		Timestamp t0 = new Timestamp(t.getTime()-1800000);// less 30 minutes
		st.setTimestamp(1, t0);
		st.setTimestamp(2, t);
		HashMap<String,DeliveryItem> m = new HashMap<String,DeliveryItem>(64, .5f);
		ResultSet rs = st.executeQuery();
		if (!rs.next()) {
			rs.close(); return s;
		}
		int storeN = 0, i = 0;
		DeliveryNote dn = null;
		DeliveryItem di = null;
		ArrayList<DeliveryNote> al = new ArrayList<DeliveryNote>(128);
		while (true) {
			storeN = rs.getInt(1);
			if (onlyTestStoresToRpt && !storeSubset.contains(storeN)) {
				if (!rs.next()) {
					rs.close();
					if (dn != null) { add(al, dn);}
					break;
				}
				continue;
			}
			Time delTimeFrom = rs.getTime(11);
			if (dn == null) {
				dn = new DeliveryNote();
				dn.storeN = storeN;
				dn.delTimeFrom = delTimeFrom;
			}
			else if (storeN != dn.storeN || !delTimeFrom.equals(dn.delTimeFrom)) {
				add(al, dn);
				m.clear();
				dn = new DeliveryNote();
				dn.storeN = storeN;
				dn.delTimeFrom = delTimeFrom;
				di = null;
			}
			Date dsShipDate = rs.getDate(22);
			Time arrivalTime = rs.getTime(7);
			Time serviceTime = rs.getTime(8);
			String cmdty = rs.getString(2).toUpperCase();
			if (di == null || !cmdty.equals(di.cmdty)) {
				dn.cmdtyList.add(new DeliveryNote.Cmdty(cmdty,
					dsShipDate, arrivalTime, serviceTime));
			}
			di = new DeliveryItem();
			di.cmdty = cmdty;
			di.orderN = rs.getString(9);
			di.dsShipDate = dsShipDate;

			if (addItem(m, dn, di)) {
				if (dn.shipDate == null) {
					dn.id = "m"+(++i);
					dn.shipDate = rs.getDate(4);
					dn.delDate = rs.getDate(5);
					dn.routeN = rs.getString(6);
					dn.arrivalTime = arrivalTime;
					dn.serviceTime = serviceTime;
					dn.delTimeTo = rs.getTime(12);
					dn.province = rs.getString(13);
					dn.firstUserFile = rs.getString(16);
					String nuf = rs.getString(17);
					if (!dn.firstUserFile.equals(nuf)) { dn.nextUserFile = nuf;}
				}
				if (dn.delCarrier == null) {
					dn.delCarrier = rs.getString(14);
					dn.targetOpen = rs.getTime(15);
				}
				di.pallets = rs.getDouble(10);
				di.firstUserFile = rs.getString(18);
				String nuf = rs.getString(19);
				if (!di.firstUserFile.equals(nuf)) { di.nextUserFile = nuf;}
				di.dsFirstUserFile = rs.getString(20);
				nuf = rs.getString(21);
				if (!di.dsFirstUserFile.equals(nuf)) { di.dsNextUserFile = nuf;}
			}
			if (!rs.next()) {
				rs.close();
				add(al, dn);
				break;
			}
		}
		if (al.size() != 0) {
			String interval = SupportTime.MM_dd_yyyy_HH_mm_Format.format(t0)+" - "+
				SupportTime.MM_dd_yyyy_HH_mm_Format.format(t);
			log.debug("\r\n\r\nNOTIFICATIONS for "+interval+"\r\n\r\n"+al);
			if (es.emailUnsent == null) {
				Thread.sleep(2000);
				s = NotificationMail.send(s, es, storeSubset, al, interval);
			}
		}
		return s;
	}

	private static boolean addItem(HashMap<String,DeliveryItem> m, DeliveryNote dn, DeliveryItem di) {
		DeliveryItem di2 = m.get(di.orderN);
		if (di2 != null) {
			if (di.dsShipDate == null && di2.dsShipDate != null) {
				return false;
			}
			else if (di2.dsShipDate == null && di.dsShipDate != null) {
				dn.items.remove(di2);
			}
			else {
				log.error("Duplicate order: "+di.orderN+", store "+dn.storeN);
				return false;
			}
		}
		m.put(di.orderN, di);
		dn.items.add(di);
		return true;
	}
	private static void add(ArrayList<DeliveryNote> al, DeliveryNote dn) {
		if (dn.delCarrier == null) {
			int dow = SupportTime.getDayOfWeek(dn.delDate);
			for (Iterator<DeliveryNote.Cmdty> it = dn.cmdtyList.iterator(); it.hasNext();) {
				DeliveryNote.Cmdty c = it.next();
				if (!c.cmdty.equals(DCX) && carriersNotFound.add(new DsKey(dn.storeN, c.cmdty, dow))) {
					String type = c.dsShipDate == null ? "regular" : "holidays";
					log.error("Carrier not found (" + type + "): "+dn.storeN+", "+
						c.cmdty+", "+SupportTime.getDayOfWeek(dow));
				}
			}
		}
		dn.totalPallets = dn.getTotalPallets(null);
		if (dn.totalPallets > 30) {
			return;
		}
		if (dn.delCarrier == null) {
			dn.delCarrier = TBD;
		}
		else { dn.delCarrier = dn.delCarrier.trim();}

		if (dn.cmdtyList.size() == 1 && DCF.equals(dn.cmdtyList.get(0).cmdty)) {
			dn.arrivalTime = null;
			dn.serviceTime = null;
		}
		else if (CCS.equalsIgnoreCase(dn.delCarrier)) {
			int i = dn.cmdtyList.indexOf(DCV_CMDTY);
			if (i == -1) {
				dn.arrivalTime = dn.targetOpen;
				dn.serviceTime = null;
			}
			else {
				DeliveryNote.Cmdty c = dn.cmdtyList.get(i);
				dn.arrivalTime = c.arrivalTime;
				dn.serviceTime = c.serviceTime;
			}
		}
		else {
			dn.arrivalTime = dn.targetOpen;
			dn.serviceTime = null;
		}
		al.add(dn);
	}

}
