package com.logisticsalliance.shp;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.mail.Session;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.DsKey;
import com.logisticsalliance.general.ScheduledWorker;
import com.logisticsalliance.general.ScheduledWorker.EmailSent;
import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.sqla.ConnectFactory1;
import com.logisticsalliance.text.TBuilder;
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

	private static final String
		SQL_INS1 =
		"INSERT INTO OS61LXDTA.OSPIFC1 (I1RQS,I1STS,I1ISRC,I1DIV,I1WHS,I1OTYP,I1SRV,I1CUS,I1PSTS," +
		"I1TPGM,I1WGT,I1PCS,I1VOL,I1HGT,I1SKD,I1FPKD,I1FDLD,I1CON,I1LOD,I1SHP$,I1ORD$,I1SHPI,I1CAR," +
		"I1SRVP,I1ORD#) " +
		"VALUES ('*CREATE','91','02','LA','STR','NR','*ANY','SHOPPERS','0','OSR1200',?,?,?,?,?,?,?," +
		"?,?,?,?,?,?,?,?)",

		SQL_UPD1 =
		"UPDATE OS61LXDTA.OSPIFC1 SET " +
		"I1RQS='*CREATE'," +
		"I1STS='91'," +
		"I1ISRC='02'," +
		"I1DIV='LA'," +
		"I1WHS='STR'," +
		"I1OTYP='NR'," +
		"I1SRV='*ANY'," +
		"I1CUS='SHOPPERS'," +
		"I1PSTS='0'," +
		"I1TPGM='OSR1200'," +
		"I1WGT=?," +
		"I1PCS=?," +
		"I1VOL=?," +
		"I1HGT=?," +
		"I1SKD=?," +
		"I1FPKD=?," +
		"I1FDLD=?," +
		"I1CON=?," +
		"I1LOD=?," +
		"I1SHP$=?," +
		"I1ORD$=?," +
		"I1SHPI=?," +
		"I1CAR=?," +
		"I1SRVP=? " +
		"WHERE I1ORD#=?",

		SQL_INS2 =
		"INSERT INTO OS61LXDTA.OSPIFC2 (I2CUS,I2CAR,I2SRV,I2HUB2,I2PIKD,I2DRPD,I2CCF,I2SCF," +
		"I2PSTP,I2DSTP,I2SHPGRP,I2ORD#,I2SEQN) " +
		"VALUES ('SHOPPERS',?,?,?,?,?,?,?,?,?,?,?,?)",

		SQL_UPD2 =
		"UPDATE OS61LXDTA.OSPIFC2 SET " +
		"I2CUS='SHOPPERS'," +
		"I2CAR=?," +
		"I2SRV=?," +
		"I2HUB2=?," +
		"I2PIKD=?," +
		"I2DRPD=?," +
		"I2CCF=?," +
		"I2SCF=?," +
		"I2PSTP=?," +
		"I2DSTP=?," +
		"I2SHPGRP=?" +
		"WHERE I2ORD#=? AND I2SEQN=?",

		SQL_INS9 =
		"INSERT INTO OS61LXDTA.OSPIFC9 (I9CUS,I9ENT,I9ENTQ,I9REF#,I9ORD#,I9LEG#,I9QUAL) " +
		"VALUES ('SHOPPERS','SHOPPERS','5',?,?,?,?)",

		SQL_UPD9 =
		"UPDATE OS61LXDTA.OSPIFC9 SET " +
		"I9CUS='SHOPPERS'," +
		"I9ENT='SHOPPERS'," +
		"I9ENTQ='5'," +
		"I9REF#=? " +
		"WHERE I9ORD#=? AND I9LEG#=? AND I9QUAL=?",

		MATRIXDC = "MATRIXDC";

	private static String SQL_SEL_SHP =
		"SELECT " +
		"sd.store_n, sd.cmdty, sd.ship_date, sd.del_date, route_n, stop_n, dc," +
		"dc_depart_time, prev_distance, prev_travel_time, arrival_time, service_time," +
		"total_service_time, total_travel_time, equip_size," +
		"order_n, pallets, units, weight, cube, del_time_from, del_time_to," +
		"spec_instructs, lh_carrier, lh_service, del_carrier_id, del_service," +
		"sd.first_user_file, sd.next_user_file, rno.first_user_file, rno.next_user_file," +
		"sts.first_user_file, sts.next_user_file, sts.ship_date " +

		"FROM " +
		"la.hship_data sd LEFT JOIN la.hcarrier_schedule cs ON " +
		"cs.store_n=sd.store_n AND ((sd.cmdty='DCX' OR sd.cmdty='EVT' OR sd.cmdty='EVT2') AND " +
		"(cs.cmdty='DCB' OR cs.cmdty='DCV') OR sd.cmdty<>'DCX' AND sd.cmdty<>'EVT' AND " +
		"sd.cmdty<>'EVT2' AND cs.cmdty=sd.cmdty) AND cs.del_day=DAYOFWEEK(sd.del_date)-1," +
		"la.hrn_order rno,la.hstore_schedule sts,la.hstore_profile sp " +

		"WHERE " +
		"ship_n=sd.n AND sp.n=sd.store_n AND " +
		"sts.store_n=sd.store_n AND sts.cmdty=sd.cmdty AND " +
		"(sts.ship_date IS NOT NULL AND sts.ship_date=sd.ship_date OR " +
		"sts.ship_date IS NULL AND sts.ship_day=DAYOFWEEK(sd.ship_date)-1) AND " +
		"sd.ship_date=? AND rno.lw NOT IN (" +CommonConstants.RX_LW+") " +
		(ScheduledWorker.shipQryCarriers == null ? "" : "AND cs.del_carrier_id IN (" +
		ScheduledWorker.shipQryCarriers+") ") +

		"ORDER BY " +
		"1,7,5,2";

	private static ConnectFactory connectFactoryI5;

	private static HashSet<DsKey> carriersNotFound = new HashSet<DsKey>();

	public static void setConnectFactoryI5(ConnectFactory cf) {
		connectFactoryI5 = cf;
	}
	public static void clearCarriersNotFound() {
		carriersNotFound.clear();
	}
	public static void process(Date date, EmailSent es) throws Exception {
		Session s = null;
		ArrayList<ShipmentData> al = new ArrayList<ShipmentData>(1024);
		Connection con = null, con1 = null;
		try {
			con = ConnectFactory1.one().getConnection();
			con1 = connectFactoryI5.getConnection();
			con1.setAutoCommit(true);
			PreparedStatement st = con.prepareStatement(SQL_SEL_SHP);
			s = select(st, new ShipmentHub.HubStatements(con1), date, al, s, es);
			st.close();
			PreparedStatement st1 = con1.prepareStatement("DELETE FROM OS61LXDTA.OSPIFC1");
			int n = st1.executeUpdate();
			st1.close();
			st1 = con1.prepareStatement("DELETE FROM OS61LXDTA.OSPIFC2");
			n = st1.executeUpdate();
			st1.close();
			st1 = con1.prepareStatement("DELETE FROM OS61LXDTA.OSPIFC9");
			n = st1.executeUpdate();
			st1.close();
			update(con1.prepareStatement(SQL_INS1), con1.prepareStatement(SQL_UPD1),
				con1.prepareStatement(SQL_INS2), con1.prepareStatement(SQL_UPD2),
				con1.prepareStatement(SQL_INS9), con1.prepareStatement(SQL_UPD9), al);
			if (al.size() != 0) {
				log.debug("\r\n\r\nSHIPMENTS: "+SupportTime.dd_MM_yyyy_Format.format(date)+
					"\r\n\r\n"+al+"\r\n\r\nTotal: "+al.size());
			}
			String v = ShpTest.test(con1, Functions.toInt(date), al);
			if (v.length() != 0) {
				log.error("\r\n\r\nDifferences:\r\n"+v);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			log.error(ex);
		}
		finally {
			ConnectFactory.close(con);
		}
	}
	private static String getOrdN(ShipmentData sd) {
		StringBuilder b = insDigits(sd.routeN);
		String sn = String.valueOf(sd.storeN);
		b.append(insDigits(sn));
		return sd.dc+b.toString();
	}
	private static Session select(PreparedStatement st, ShipmentHub.HubStatements hst,
		Date date, ArrayList<ShipmentData> al, Session s, EmailSent es) throws Exception {
		st.setDate(1, date);
		HashMap<String,ShipmentItem> m = new HashMap<String,ShipmentItem>(64, .5f);
		ResultSet rs = st.executeQuery();
		if (!rs.next()) {
			rs.close(); return s;
		}
		int storeN, shipDate = 0;
		String dc, routeN;
		ShipmentData sd = null;
		ShipmentItem si = null;
		while (true) {
			storeN = rs.getInt(1);
			routeN = rs.getString(5);
			dc = rs.getString(7);
			if (sd == null) {
				sd = newData(storeN, dc, routeN);
			}
			else if (storeN != sd.storeN || !dc.equals(sd.dc) || !routeN.equals(sd.routeN)) {
				addData(al, sd);
				m.clear();
				sd = newData(storeN, dc, routeN);
			}
			si = new ShipmentItem();
			si.orderN = rs.getString(16);
			si.dsShipDate = rs.getDate(34);
			if (addItem(m, sd, si)) {
				if (sd.shipDate == null) {
					sd.cmdty = rs.getString(2);
					sd.shipDate = rs.getDate(3);
					shipDate = Functions.toInt(sd.shipDate);
					sd.delDate = rs.getDate(4);
					sd.ordN = getOrdN(sd);
					sd.stopN = rs.getString(6);
					sd.dcDepartTime = rs.getTime(8);
					sd.prevDistance = rs.getInt(9);
					sd.prevTravelTime = rs.getTime(10);
					sd.arrivalTime = rs.getTime(11);
					sd.serviceTime = rs.getTime(12);
					sd.totalServiceTime = rs.getTime(13);
					sd.totalTravelTime = rs.getTime(14);
					sd.equipSize = rs.getString(15);
					sd.delTimeFrom = rs.getTime(21);
					sd.delTimeTo = rs.getTime(22);
					sd.specInstructs = rs.getString(23);
					if (sd.specInstructs != null) {
						sd.specInstructs = sd.specInstructs.trim();
					}
					sd.firstUserFile = rs.getString(28);
					String nuf = rs.getString(29);
					if (!sd.firstUserFile.equals(nuf)) { sd.nextUserFile = nuf;}
				}
				if (sd.delCarrier == null) {
					sd.delCarrier = rs.getString(26);
					String srv = rs.getString(27);
					sd.delService = Functions.getService(sd, srv, 2);
					if (sd.lhCarrier != null && sd.delCarrier != null) {
						sd.hub = ShipmentHub.getHub(hst, sd, shipDate);
					}
				}
				if (sd.lhCarrier == null) {
					sd.lhCarrier = rs.getString(24);
					String srv = rs.getString(25);
					sd.lhService = Functions.getService(sd, srv, 1);
					if (sd.lhCarrier != null && sd.delCarrier != null) {
						sd.hub = ShipmentHub.getHub(hst, sd, shipDate);
					}
				}
				si.lw = si.orderN.substring(2, 4);
				si.pallets = rs.getDouble(17);
				si.units = rs.getDouble(18);
				si.weight = rs.getDouble(19);
				si.cube = rs.getDouble(20);
				si.firstUserFile = rs.getString(30);
				String nuf = rs.getString(31);
				if (!si.firstUserFile.equals(nuf)) { si.nextUserFile = nuf;}
				si.dsFirstUserFile = rs.getString(32);
				nuf = rs.getString(33);
				if (!si.dsFirstUserFile.equals(nuf)) { si.dsNextUserFile = nuf;}
			}
			if (!rs.next()) {
				rs.close();
				addData(al, sd);
				break;
			}
		}
		return s;
	}
	private static ShipmentData newData(int storeN, String dc, String routeN) {
		ShipmentData sd = new ShipmentData();
		sd.storeN = storeN;
		sd.dc = dc;
		sd.routeN = routeN;
		return sd;
	}
	private static boolean addItem(HashMap<String,ShipmentItem> m, ShipmentData sd, ShipmentItem si) {
		ShipmentItem si2 = m.get(si.orderN);
		if (si2 != null) {
			if (si.dsShipDate == si2.dsShipDate || si.dsShipDate != null &&
				si2.dsShipDate != null || si2.dsShipDate != null) {
				return false;
			}
			else if (si.dsShipDate != null) {
				sd.items.remove(si2);
			}
		}
		m.put(si.orderN, si);
		sd.items.add(si);
		return true;
	}
	private static void addData(ArrayList<ShipmentData> al, ShipmentData sd) {
		if (sd.delCarrier == null && sd.lhCarrier == null) {
			int dow = SupportTime.getDayOfWeek(sd.delDate);
			DsKey k = new DsKey(sd.storeN, sd.cmdty, dow);
			if (carriersNotFound.add(k)) {
				String type = sd.items.get(0).dsShipDate == null ? "regular" : "holidays";
				log.error("Carrier not found (" + type + "): "+k);
			}
		}
		else { al.add(sd);}
	}
	private static void update(PreparedStatement ins1, PreparedStatement upd1,
		PreparedStatement ins2, PreparedStatement upd2, PreparedStatement ins9,
		PreparedStatement upd9, ArrayList<ShipmentData> al) throws Exception {
		ArrayList<Functions.Ref> refs = new ArrayList<Functions.Ref>(8);
		for (Iterator<ShipmentData> it = al.iterator(); it.hasNext();) {
			ShipmentData sd = it.next();
			setTable1(upd1, sd);
			if (upd1.executeUpdate() == 0) {
				setTable1(ins1, sd);
				ins1.addBatch();
			}
			Functions.putRefs(refs, sd, 0);
			updateTable9(upd9, ins9, refs, sd.ordN, 0);
		}
		ins1.executeBatch();
		ins9.executeBatch();

		for (Iterator<ShipmentData> it = al.iterator(); it.hasNext();) {
			ShipmentData sd = it.next();
			int i = 1;
			if (sd.lhCarrier != null) {
				setTable2(upd2, sd, sd.lhCarrier, sd.lhService, i);
				if (upd2.executeUpdate() == 0) {
					setTable2(ins2, sd, sd.lhCarrier, sd.lhService, i);
					ins2.addBatch();
				}
				Functions.putRefs(refs, sd, i);
				updateTable9(upd9, ins9, refs, sd.ordN, i);
				i++;
			}
			if (sd.delCarrier != null) {
				setTable2(upd2, sd, sd.delCarrier, sd.delService, i);
				if (upd2.executeUpdate() == 0) {
					setTable2(ins2, sd, sd.delCarrier, sd.delService, i);
					ins2.addBatch();
				}
				Functions.putRefs(refs, sd, i);
				updateTable9(upd9, ins9, refs, sd.ordN, i);
			}
		}
		ins2.executeBatch();
		ins9.executeBatch();
	}
	private static void setTable1(PreparedStatement st, ShipmentData sd) throws Exception {
		st.setDouble(1, sd.getTotalWeight(true));
		st.setDouble(2, sd.getTotalUnits(null));
		st.setDouble(3, sd.getTotalCube(true));
		st.setDouble(4, 0);//height
		st.setDouble(5, sd.getTotalPallets());
		st.setInt(6, Functions.toInt(sd.shipDate));
		st.setInt(7, Functions.toInt(sd.delDate));
		st.setString(8, String.valueOf(sd.storeN));
		st.setString(9, MATRIXDC+sd.dc);
		st.setDouble(10, sd.prevDistance/10d);
		int t1 = Functions.toMins(sd.serviceTime);
		Time t = new Time(sd.prevTravelTime.getTime()+t1*60000);
		st.setDouble(11, Functions.toDouble(t));
		st.setString(12, sd.specInstructs == null ? "" : sd.specInstructs);
		st.setString(13, Functions.cut(sd.delCarrier, 8));
		st.setString(14, sd.delService);
		st.setString(15, sd.ordN);
	}
	private static void setTable2(PreparedStatement st, ShipmentData sd,
		String carrier, String service, int leg) throws Exception {
		st.setString(1, Functions.cut(carrier, 8));
		st.setString(2, service);
		st.setString(3, sd.hub);
		st.setInt(4, Functions.toInt(sd.shipDate));
		st.setInt(5, Functions.toInt(sd.delDate));
		st.setString(6, "Y");//Carrier-Commit Flag
		st.setString(7, "Y");//Service-Commit Flag
		st.setInt(8, 1);//Pick Stop #
		st.setInt(9, Integer.parseInt(sd.stopN)+1);//Drop Stop #
		st.setString(10, sd.routeN);//Shipment Group Id
		st.setString(11, sd.ordN);
		st.setDouble(12, leg);
	}
	private static void updateTable9(PreparedStatement upd, PreparedStatement ins,
		ArrayList<Functions.Ref> refs, String ordN, int leg) throws Exception {
		for (Iterator<Functions.Ref> it = refs.iterator(); it.hasNext();) {
			Functions.Ref r = it.next();
			setTable9(upd, r, ordN, leg);
			if (upd.executeUpdate() == 0) {
				setTable9(ins, r, ordN, leg);
				ins.addBatch();
			}
		}
	}
	private static void setTable9(PreparedStatement st, Functions.Ref r,
		String ordN, int leg) throws Exception {
		st.setString(1, r.value);//Reference #
		st.setString(2, ordN);
		st.setDouble(3, leg);
		st.setString(4, r.name);//Reference Qualifier
	}
	private static StringBuilder insDigits(String v) {
		StringBuilder b = new StringBuilder(4);
		int n = 4-v.length();
		if (n > 0) {
			TBuilder.add(b, '0', n);
		}
		b.append(v);
		return b;
	}

}
