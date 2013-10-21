package com.logisticsalliance.shp;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;

import javax.mail.Session;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.DsKey;
import com.logisticsalliance.general.ScheduledWorker;
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

	private static String SQL_SEL_DEL =
		"SELECT " +
		"sd.store_n, sd.cmdty, sd.ship_date, sd.del_date, route_n, stop_n, dc, add_key," +
		"dc_depart_time, prev_distance, prev_travel_time, arrival_time, service_time," +
		"total_service_time, total_travel_time, equip_size," +
		"order_n, pallets, units, weight, cube," +
		"spec_instructs, lh_carrier_id, lh_service, del_carrier_id, del_service," +
		"sd.first_user_file, sd.next_user_file, rno.first_user_file, rno.next_user_file " +

		"FROM " +
		"la.hship_data sd LEFT JOIN la.hcarrier_schedule cs ON " +
		"cs.store_n=sd.store_n AND cs.cmdty=sd.cmdty AND cs.del_day=DAYOFWEEK(sd.del_date)-1," +
		"la.hrn_order rno,la.hstore_profile sp " +

		"WHERE " +
		"ship_n=sd.n AND sp.n=sd.store_n AND " +
		"rno.lw NOT IN (" +CommonConstants.RX_LW+") " +
		(ScheduledWorker.shipQryCarriers == null ? "" : "AND cs.del_carrier_id NOT IN (" +
		ScheduledWorker.shipQryCarriers+") ") +

		"ORDER BY " +
		"1,2,3,7,8";

	private static HashSet<DsKey> carriersNotFound = new HashSet<DsKey>();

	public static void clearCarriersNotFound() {
		carriersNotFound.clear();
	}
	public static void process(EmailSent es, HashSet<Integer> storeSubset,
		boolean onlyTestStoresToRpt) throws Exception {
		Session s = null;
		Connection con = ConnectFactory1.one().getConnection();
		try {
			PreparedStatement st = con.prepareStatement(SQL_SEL_DEL);
			s = select(st, s, es, storeSubset, onlyTestStoresToRpt);
			st.close();
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
		String cmdty, dc, addKey;
		Date shipDate;
		ShipmentData sd = null;
		ShipmentItem si = null;
		ArrayList<ShipmentData> al = new ArrayList<ShipmentData>(1024);
		while (true) {
			storeN = rs.getInt(1);
			if (onlyTestStoresToRpt && !storeSubset.contains(storeN)) {
				if (!rs.next()) {
					rs.close();
					if (sd != null) { addData(al, sd);}
					break;
				}
				continue;
			}
			cmdty = rs.getString(2);
			shipDate = rs.getDate(3);
			dc = rs.getString(7);
			addKey = rs.getString(8);
			if (sd == null) {
				sd = newData(storeN, cmdty, dc, addKey, shipDate);
			}
			else if (storeN != sd.storeN || !cmdty.equals(sd.cmdty) ||
				!shipDate.equals(sd.shipDate) || !dc.equals(sd.dc) || !addKey.equals(sd.addKey)) {
				addData(al, sd);
				sd = newData(storeN, cmdty, dc, addKey, shipDate);
			}
			if (sd.delDate == null) {
				sd.delDate = rs.getDate(4);
				sd.routeN = rs.getString(5);
				sd.stopN = rs.getString(6);
				sd.dcDepartTime = rs.getTime(9);
				sd.prevDistance = rs.getInt(10);
				sd.prevTravelTime = rs.getTime(11);
				sd.arrivalTime = rs.getTime(12);
				sd.serviceTime = rs.getTime(13);
				sd.totalServiceTime = rs.getTime(14);
				sd.totalTravelTime = rs.getTime(15);
				sd.equipSize = rs.getString(16);
				sd.specInstructs = rs.getString(22);
				sd.firstUserFile = rs.getString(27);
				String nuf = rs.getString(28);
				if (!sd.firstUserFile.equals(nuf)) { sd.nextUserFile = nuf;}
			}
			if (sd.lhCarrier == null) {
				sd.lhCarrier = rs.getString(23);
				sd.lhService = rs.getString(24);
			}
			if (sd.delCarrier == null) {
				sd.delCarrier = rs.getString(25);
				sd.delService = rs.getString(26);
			}
			si = new ShipmentItem();
			si.orderN = rs.getString(17);
			si.lw = si.orderN.substring(2, 4);
			si.pallets = rs.getDouble(18);
			si.units = rs.getDouble(19);
			si.weight = rs.getDouble(20);
			si.cube = rs.getDouble(21);
			si.firstUserFile = rs.getString(29);
			String nuf = rs.getString(30);
			if (!si.firstUserFile.equals(nuf)) { si.nextUserFile = nuf;}
			sd.items.add(si);
			if (!rs.next()) {
				rs.close();
				addData(al, sd);
				break;
			}
		}
		if (al.size() != 0) {
			log.debug("\r\n\r\nSHIPMENTS:\r\n\r\n"+al);
		}
		return s;
	}
	private static ShipmentData newData(int storeN, String cmdty,
		String dc, String addKey, Date shipDate) {
		ShipmentData sd = new ShipmentData();
		sd.storeN = storeN;
		sd.cmdty = cmdty;
		sd.shipDate = shipDate;
		sd.dc = dc;
		sd.addKey = dc;
		return sd;
	}
	private static void addData(ArrayList<ShipmentData> al, ShipmentData sd) {
		if (!sd.cmdty.equals("DCX")) {
			if (sd.delCarrier == null && sd.lhCarrier == null) {
				int dow = SupportTime.getDayOfWeek(sd.delDate);
				DsKey k = new DsKey(sd.storeN, sd.cmdty, dow);
				if (carriersNotFound.add(k)) {
					log.error("Carrier not found: "+k);
				}
			}
		}
		al.add(sd);
	}

}
