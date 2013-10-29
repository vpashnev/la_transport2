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
import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.sqla.ConnectFactory1;
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
		"sd.store_n, sd.cmdty, dc, sd.ship_date, sd.del_date, arrival_time, route_n, stop_n," +
		"del_time_from, del_time_to, del_carrier_id, target_open, sts.ship_date " +

		"FROM " +
		"la.hship_data sd LEFT JOIN la.hcarrier_schedule cs ON " +
		"cs.store_n=sd.store_n AND (sd.cmdty='DCX' AND (cs.cmdty='DCB' OR cs.cmdty='DCV') OR " +
		"sd.cmdty<>'DCX' AND cs.cmdty=sd.cmdty) AND cs.del_day=DAYOFWEEK(sd.del_date)-1," +
		"la.hstore_schedule sts " +

		"WHERE " +
		"sts.store_n=sd.store_n AND sts.cmdty=sd.cmdty AND " +
		"(sts.ship_date IS NOT NULL AND sts.ship_date=sd.ship_date OR " +
		"sts.ship_date IS NULL AND sts.ship_day=DAYOFWEEK(sd.ship_date)-1) AND " +
		"tt_table IS NOT NULL "+
		"AND sd.ship_date>'2013-10-10' "+

		"ORDER BY " +
		"sd.store_n,sd.cmdty,dc,sd.ship_date",

		SQL_RESET_TT_TABLE = "UPDATE la.hship_data SET tt_table=NULL WHERE tt_table IS NOT NULL";

	private static ConnectFactory connectFactoryI5;

	private static HashSet<DsKey> carriersNotFound = new HashSet<DsKey>();

	public static void setConnectFactoryI5(ConnectFactory cf) {
		connectFactoryI5 = cf;
	}
	public static void clearCarriersNotFound() {
		carriersNotFound.clear();
	}
	public static void process() throws Exception {
		Connection con = ConnectFactory1.one().getConnection(),
			con1 = connectFactoryI5.getConnection();
		con1.setAutoCommit(true);
		try {
			PreparedStatement st = con.prepareStatement(SQL_SEL_DELIVERIES);
			ResultSet rs = st.executeQuery();
			ArrayList<Row> al = select(rs);
			st.close();
			st = con1.prepareStatement("DELETE FROM OS61LXDTA.OSPDLVS");
			/*int n = */st.executeUpdate();
			st.close();
			update(con1.prepareStatement(SQL_INS), con1.prepareStatement(SQL_UPD), al);
			st = con.prepareStatement(SQL_RESET_TT_TABLE);
			st.executeUpdate();
			con.commit();
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
	private static ArrayList<Row> select(ResultSet rs) throws Exception {
		ArrayList<Row> al = new ArrayList<Row>(128);
		while (rs.next()) {
			Row r = new Row();
			r.storeN = rs.getInt(1);
			r.cmdty = rs.getString(2);
			r.dc = rs.getString(3);
			r.shipDate = rs.getDate(4);
			r.dsShipDate = rs.getDate(13);
			if (addRow(al, r)) {
				r.delDate = rs.getDate(5);
				r.arrivalTime = rs.getTime(6);
				r.delCarrier = rs.getString(11);
				r.targetOpen = rs.getTime(12);
				if (addRow(r)) {
					r.routeN = rs.getString(7);
					r.stopN = rs.getString(8);
					r.delTimeFrom = rs.getTime(9);
					r.delTimeTo = rs.getTime(10);
					al.add(r);
				}
			}
		}
		return al;
	}
	private static boolean addRow(ArrayList<Row> al, Row r) {
		int sz = al.size();
		if (sz != 0) {
			Row r2 = al.get(sz-1);
			if (r.storeN == r2.storeN && r.cmdty.equals(r2.cmdty) &&
				r.dc.equals(r2.dc) && r.shipDate.equals(r2.shipDate)) {
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
	private static boolean addRow(Row r) {
		int dow = SupportTime.getDayOfWeek(r.delDate);
		if (r.delCarrier == null) {
			DsKey k = new DsKey(r.storeN, r.cmdty, dow);
			if (carriersNotFound.add(k)) {
				String type = r.dsShipDate == null ? "regular" : "holidays";
				log.error("Carrier not found (" + type + "): "+k);
			}
			//return false;
			r.delCarrier = "";
		}
		else { r.delCarrier = r.delCarrier.trim();}

		if (!CommonConstants.CCS.equalsIgnoreCase(r.delCarrier) ||
			CommonConstants.DCF.equalsIgnoreCase(r.cmdty)) {
			if (r.targetOpen == null) {
				log.error("Torget open not defined : "+r.delCarrier+
					", "+r.storeN+", "+r.cmdty+", "+dow);
				//return false;
			}
			else { r.arrivalTime = r.targetOpen;}
		}
		return true;
	}
	private static void update(PreparedStatement ins, PreparedStatement upd,
		ArrayList<Row> al) throws Exception {
		for (Iterator<Row> it = al.iterator(); it.hasNext();) {
			Row r = it.next();
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
			upd.setDate(11, r.shipDate);
			if (upd.executeUpdate() != 0) {
				continue;
			}
			ins.setInt(1, r.storeN);
			ins.setString(2, r.cmdty);
			ins.setString(3, r.dc);
			ins.setDate(4, r.shipDate);
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
		private int storeN;
		private String cmdty, dc, routeN, stopN, delCarrier;
		private Time arrivalTime, delTimeFrom, delTimeTo, targetOpen;
		private Date shipDate, delDate, dsShipDate;
	}
}
