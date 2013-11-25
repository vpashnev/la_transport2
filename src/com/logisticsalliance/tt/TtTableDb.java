package com.logisticsalliance.tt;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.DsKey;
import com.logisticsalliance.general.RnColumns;
import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.sqla.ConnectFactory1;
import com.logisticsalliance.text.TBuilder;
import com.logisticsalliance.util.SupportTime;

/**
 * This class populates the deliveries in the table of track and trace project.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class TtTableDb {

	private static Logger log = Logger.getLogger(TtTableDb.class);

	private static final String
		SQL_INS =
		"INSERT INTO OS61LXDTA.OSPDLVS (dvsts,dvstsd,dvlsts,dvlstsd,dvstore#," +
		"dvcom,dvdc,dvshpd,dvdlvd,dvdlvt,dvroute,dvstop#,dvetato,dvetatc,dvcar) " +
		"VALUES ('10','PLAN','10','PLAN',?,?,?,?,?,?,?,?,?,?,?)",

		SQL_UPD =
		"UPDATE OS61LXDTA.OSPDLVS SET dvdlvd=?,dvdlvt=?,dvroute=?,dvstop#=?,dvetato=?," +
		"dvetatc=?,dvcar=? WHERE dvstore#=? AND dvcom=? AND dvdc=? AND dvshpd=?",

		SQL_SEL_DELIVERIES =
		"SELECT DISTINCT " +
		"sd.store_n, sd.cmdty, dc, sd.del_date, arrival_time, route_n, stop_n," +
		"del_time_from, del_time_to, del_carrier_id, sd.first_user_file," +
		"sd.next_user_file, sts.ship_date " +

		"FROM " +
		"la.hship_data sd LEFT JOIN la.hcarrier_schedule1 cs ON " +
		"cs.store_n=sd.store_n AND ((sd.cmdty='DCX' OR sd.cmdty='EVT' OR sd.cmdty='EVT2') AND " +
		"(cs.cmdty='DCB' OR cs.cmdty='DCV') OR sd.cmdty<>'DCX' AND sd.cmdty<>'EVT' AND " +
		"sd.cmdty<>'EVT2' AND cs.cmdty=sd.cmdty) AND cs.del_day=DAYOFWEEK(sd.del_date)-1," +
		"la.hstore_schedule sts " +

		"WHERE " +
		"sts.store_n=sd.store_n AND sts.cmdty=sd.cmdty AND " +
		"(sts.ship_date IS NOT NULL AND sts.ship_date=sd.ship_date OR " +
		"sts.ship_date IS NULL AND sts.ship_day=DAYOFWEEK(sd.ship_date)-1) AND " +
		"sd.cmdty<>'RX' AND sd.ship_date=? "+

		"ORDER BY " +
		"sd.store_n,sd.cmdty,dc";

	private static ConnectFactory connectFactoryI5;

	private static HashSet<DsKey> carriersNotFound = new HashSet<DsKey>();

	private static boolean done;

	public static void setConnectFactoryI5(ConnectFactory cf) {
		connectFactoryI5 = cf;
	}
	public static void clearCarriersNotFound() {
		carriersNotFound.clear();
	}
	public static void process(final Date shipDate) throws InterruptedException {
		done = false;
		Thread t = new Thread() {
			@Override
			public void run() {
				process1(shipDate);
				done = true;
			}
		};
		t.setDaemon(true);
		t.start();
		int i = 0;
		while (!done && i++ != 360) {
			Thread.sleep(5000);
		}
		if (!done) {
			log.error("Report incomplete truck and trace table");
		}
	}
	private static void process1(Date shipDate) {
		Connection con = null, con1 = null;
		try {
			con = ConnectFactory1.one().getConnection();
			con1 = connectFactoryI5.getConnection();
			con1.setAutoCommit(true);
			PreparedStatement st = con.prepareStatement(SQL_SEL_DELIVERIES);
			st.setDate(1, shipDate);
			ResultSet rs = st.executeQuery();
			ArrayList<Row> al = new ArrayList<Row>(1024);
			int count = select(rs, al);
			st.close();
			//st = con1.prepareStatement("DELETE FROM OS61LXDTA.OSPDLVS");
			//int n = st.executeUpdate();
			//st.close();
			update(con1.prepareStatement(SQL_INS), con1.prepareStatement(SQL_UPD), al, shipDate);
			if (al.size() != 0) {
				log.debug("\r\n\r\nSHIPMENTS: "+SupportTime.dd_MM_yyyy_Format.format(shipDate)+
					"\r\n\r\n"+al+
					"\r\n\r\nTotal:   "+al.size()+
					"\r\n\r\nMissing: "+(al.size()-count));
			}
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
	private static int select(ResultSet rs, ArrayList<Row> al) throws Exception {
		int[] count = {0};
		while (rs.next()) {
			Row r = new Row();
			r.storeN = rs.getInt(1);
			r.cmdty = rs.getString(2);
			r.dc = rs.getString(3);
			r.dsShipDate = rs.getDate(13);
			if (addRow(al, r)) {
				r.delDate = rs.getDate(4);
				r.arrivalTime = rs.getTime(5);
				r.delTimeFrom = rs.getTime(8);
				r.delTimeTo = rs.getTime(9);
				r.delCarrier = rs.getString(10);
				r.routeN = rs.getString(6);
				r.stopN = rs.getString(7);
				r.firstUserFile = rs.getString(11);
				String nuf = rs.getString(12);
				if (!r.firstUserFile.equals(nuf)) { r.nextUserFile = nuf;}
				addRow(al, r, count);
			}
		}
		return count[0];
	}
	private static boolean addRow(ArrayList<Row> al, Row r) {
		int sz = al.size();
		if (sz != 0) {
			Row r2 = al.get(sz-1);
			if (r.storeN == r2.storeN && r.cmdty.equals(r2.cmdty) && r.dc.equals(r2.dc)) {
				if (r.dsShipDate == r2.dsShipDate || r.dsShipDate != null &&
					r2.dsShipDate != null || r2.dsShipDate != null) {
					return false;
				}
				else if (r.dsShipDate != null) {
					al.remove(sz-1);
				}
			}
		}
		return true;
	}
	private static void addRow(ArrayList<Row> al, Row r, int[] count) {
		int dow = SupportTime.getDayOfWeek(r.delDate);
		if (r.delCarrier == null) {
			DsKey k = new DsKey(r.storeN, r.cmdty, dow);
			if (carriersNotFound.add(k)) {
				String type = r.dsShipDate == null ? "regular" : "holidays";
				log.error("Carrier not found (" + type + "): "+k);
			}
			r.missing = true;
		}
		else { count[0]++;}
		if (!CommonConstants.CCS.equalsIgnoreCase(r.delCarrier)) {
			r.arrivalTime = new Time(r.delTimeFrom.getTime()+SupportTime.HOUR);
		}
		al.add(r);
	}
	private static void update(PreparedStatement ins, PreparedStatement upd,
		ArrayList<Row> al, Date shipDate) throws Exception {
		for (Iterator<Row> it = al.iterator(); it.hasNext();) {
			Row r = it.next();
			if (r.missing) { continue;}
			upd.setDate(1, r.delDate);
			upd.setInt(2, getTime(r.arrivalTime));
			upd.setString(3, r.routeN);
			upd.setString(4, r.stopN);
			upd.setInt(5, getTime(r.delTimeFrom));
			upd.setInt(6, getTime(r.delTimeTo));
			if (r.delCarrier != null && r.delCarrier.length() > 8) {
				r.delCarrier = r.delCarrier.substring(0, 8);
			}
			upd.setString(7, r.delCarrier);
			upd.setInt(8, r.storeN);
			upd.setString(9, r.cmdty);
			upd.setString(10, r.dc);
			upd.setDate(11, shipDate);
			if (upd.executeUpdate() != 0) {
				continue;
			}
			ins.setInt(1, r.storeN);
			ins.setString(2, r.cmdty);
			ins.setString(3, r.dc);
			ins.setDate(4, shipDate);
			ins.setDate(5, r.delDate);
			ins.setInt(6, getTime(r.arrivalTime));
			ins.setString(7, r.routeN);
			ins.setString(8, r.stopN);
			ins.setInt(9, getTime(r.delTimeFrom));
			ins.setInt(10, getTime(r.delTimeTo));
			ins.setString(11, r.delCarrier);
			ins.addBatch();
		}
		ins.executeBatch();
	}
	private static int getTime(Time t) {
		String v = SupportTime.Hmm_Format.format(t);
		return Integer.parseInt(v);
	}
	private static class Row {
		boolean missing;
		private int storeN;
		private String cmdty, dc, routeN, stopN, delCarrier, firstUserFile, nextUserFile;
		private Time arrivalTime, delTimeFrom, delTimeTo;
		private Date delDate, dsShipDate;
		@Override
		public String toString() {
			TBuilder tb = new TBuilder();
			tb.newLine();
			tb.addProperty20(RnColumns.STORE_N, storeN, 6);
			if (missing) {
				tb.addProperty20("Missing", "true", 4);
			}
			tb.addProperty20(RnColumns.COMMODITY, cmdty, 8);
			tb.addProperty20("Delivery date", SupportTime.dd_MM_yyyy_Format.format(delDate), 10);
			tb.addProperty20(RnColumns.DC, dc, 2);
			tb.addProperty20(RnColumns.ROUTE_N, routeN, 4);
			tb.addProperty20(RnColumns.STOP_N, stopN, 4);
			tb.addProperty20(RnColumns.ARRIVAL_TIME, arrivalTime == null ? CommonConstants.N_A :
				SupportTime.HH_mm_Format.format(arrivalTime), 5);
			tb.addProperty20("Delivery window", SupportTime.HH_mm_Format.format(delTimeFrom)+" - "+
				SupportTime.HH_mm_Format.format(delTimeTo), 20);
			tb.addProperty20("Delivery carrier", delCarrier, 32);
			tb.addProperty20("User files", firstUserFile+(nextUserFile == null ? "":
				", modified "+nextUserFile), 80);
			tb.newLine();
			return tb.toString();
		}
	}
}
