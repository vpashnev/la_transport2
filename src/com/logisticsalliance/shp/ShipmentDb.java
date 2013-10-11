package com.logisticsalliance.shp;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;

import javax.mail.Session;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.DsKey;
import com.logisticsalliance.general.ScheduledWorker.EmailSent;
import com.logisticsalliance.sql.ConnectFactory;
import com.logisticsalliance.sql.ConnectFactory1;
import com.logisticsalliance.util.SupportTime;

/**
 * This class selects shipments for daily reports.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class ShipmentDb {

	private static Logger log = Logger.getLogger(ShipmentDb.class);

	private static final String SQL_SEL_DEL =
		"SELECT " +
		"sd.store_n, 'FS', sd.ship_date, sd.del_date, route_n, stop_n, dc," +
		"dc_depart_time, prev_distance, prev_travel_time, arrival_time, service_time," +
		"total_service_time, total_travel_time, equip_size," +
		"order_n, pallets, units, weight, cube," +
		"spec_instructs, lh_carrier, lh_service, del_carrier, del_service," +
		"sd.first_user_file, sd.next_user_file, rno.first_user_file, rno.next_user_file " +

		"FROM " +
		"la.hship_data sd LEFT JOIN la.hcarrier_schedule cs ON " +
		"cs.store_n=sd.store_n AND cs.cmdty=sd.cmdty AND cs.del_day=DAYOFWEEK(sd.del_date)-1," +
		"la.hrn_order rno,la.hstore_profile sp " +

		"WHERE " +
		"ship_n=sd.n AND sp.n=sd.store_n AND " +
		"rno.lw NOT IN ('40','45','50') AND " +
		"sd.cmdty IN ('DCB','DCV') " +

		"UNION ALL " +
			
		"SELECT " +
		"sd.store_n, sd.cmdty, sd.ship_date, sd.del_date, route_n, stop_n, dc," +
		"dc_depart_time, prev_distance, prev_travel_time, arrival_time, service_time," +
		"total_service_time, total_travel_time, equip_size," +
		"order_n, pallets, units, weight, cube," +
		"spec_instructs, lh_carrier, lh_service, del_carrier, del_service," +
		"sd.first_user_file, sd.next_user_file, rno.first_user_file, rno.next_user_file " +

		"FROM " +
		"la.hship_data sd LEFT JOIN la.hcarrier_schedule cs ON " +
		"cs.store_n=sd.store_n AND cs.cmdty=sd.cmdty AND cs.del_day=DAYOFWEEK(sd.del_date)-1," +
		"la.hrn_order rno,la.hstore_profile sp " +

		"WHERE " +
		"ship_n=sd.n AND sp.n=sd.store_n AND " +
		"rno.lw NOT IN ('40','45','50') AND " +
		"sd.cmdty NOT IN ('DCB','DCV') " +

		"ORDER BY " +
		"1,2,3,7";

	private static HashSet<DsKey> carriersNotFound = new HashSet<DsKey>();

	public static void clearCarriersNotFound() {
		carriersNotFound.clear();
	}
	public static void process(EmailSent es, HashSet<Integer> storeSubset,
		boolean onlyTestStoresToRpt) throws Exception {
		Session s = null;
		Connection con = ConnectFactory1.one().getConnection();
		try {
			PreparedStatement delSt = con.prepareStatement(SQL_SEL_DEL);
			s = select(delSt, s, es, storeSubset, onlyTestStoresToRpt);
			delSt.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			log.error(ex);
		}
		finally {
			ConnectFactory.close(con);
		}
	}
	private static Session select(PreparedStatement st, Session s, EmailSent es,
		HashSet<Integer> storeSubset, boolean onlyTestStoresToRpt) throws Exception {
		ResultSet rs = st.executeQuery();
		if (!rs.next()) {
			rs.close(); return s;
		}
		int storeN;
		String cmdty, dc;
		Date shipDate;
		ShipmentData sd = null;
		ShipmentItem si = null;
		ArrayList<ShipmentData> al = new ArrayList<ShipmentData>(1024);
		while (true) {
			storeN = rs.getInt(1);
			if (onlyTestStoresToRpt && !storeSubset.contains(storeN)) {
				if (!rs.next()) {
					if (sd != null) { addData(al, sd);}
					rs.close();
					break;
				}
				continue;
			}
			cmdty = rs.getString(2);
			shipDate = rs.getDate(3);
			dc = rs.getString(7);
			if (sd == null) {
				sd = newData(storeN, cmdty, dc, shipDate);
			}
			else if (storeN != sd.storeN || !cmdty.equals(sd.cmdty) ||
				!shipDate.equals(sd.shipDate) || !dc.equals(sd.dc)) {
				addData(al, sd);
				sd = newData(storeN, cmdty, dc, shipDate);
			}
			if (sd.delDate == null) {
				sd.delDate = rs.getDate(4);
				sd.routeN = rs.getString(5);
				sd.stopN = rs.getString(6);
				sd.dcDepartTime = rs.getTime(8);
				sd.prevDistance = rs.getInt(9);
				sd.prevTravelTime = rs.getTime(10);
				sd.arrivalTime = rs.getTime(11);
				sd.serviceTime = rs.getTime(12);
				sd.totalServiceTime = rs.getTime(13);
				sd.totalTravelTime = rs.getTime(14);
				sd.equipSize = rs.getString(15);
				sd.specInstructs = rs.getString(21);
				sd.firstUserFile = rs.getString(26);
				String nuf = rs.getString(27);
				if (!sd.firstUserFile.equals(nuf)) { sd.nextUserFile = nuf;}
			}
			if (sd.lhCarrier == null) {
				sd.lhCarrier = rs.getString(22);
				sd.lhService = rs.getString(23);
			}
			if (sd.delCarrier == null) {
				sd.delCarrier = rs.getString(24);
				sd.delService = rs.getString(25);
			}
			si = new ShipmentItem();
			si.orderN = rs.getString(16);
			si.lw = si.orderN.substring(2, 4);
			si.pallets = rs.getDouble(17);
			si.units = rs.getDouble(18);
			si.weight = rs.getDouble(19);
			si.cube = rs.getDouble(20);
			si.firstUserFile = rs.getString(28);
			String nuf = rs.getString(29);
			if (!si.firstUserFile.equals(nuf)) { si.nextUserFile = nuf;}
			sd.items.add(si);
			if (!rs.next()) {
				addData(al, sd);
				rs.close();
				break;
			}
		}
		if (al.size() != 0) {
			log.debug("\r\n\r\nSHIPMENTS:\r\n\r\n"+al);
		}
		return s;
	}
	private static ShipmentData newData(int storeN, String cmdty, String dc, Date shipDate) {
		ShipmentData sd = new ShipmentData();
		sd.storeN = storeN;
		sd.cmdty = cmdty;
		sd.shipDate = shipDate;
		sd.dc = dc;
		return sd;
	}
	private static void addData(ArrayList<ShipmentData> al, ShipmentData sd) {
		if (!sd.cmdty.equals("DCX")) {
			if (sd.delCarrier == null && sd.lhCarrier == null) {
				int dow = SupportTime.getDayOfWeek(sd.delDate);
				if (carriersNotFound.add(new DsKey(sd.storeN, sd.cmdty, dow))) {
					log.error("Carrier not found: "+sd.storeN+", "+
						sd.cmdty+", "+SupportTime.getDayOfWeek(dow));
				}
			}
		}
		al.add(sd);
	}

}
