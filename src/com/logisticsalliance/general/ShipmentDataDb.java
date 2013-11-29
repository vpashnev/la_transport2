package com.logisticsalliance.general;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.logisticsalliance.io.SupportFile;
import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.sqla.ConnectFactory1;
import com.logisticsalliance.sqla.SqlSupport;
import com.logisticsalliance.util.SupportTime;

/**
 * This class reads UP-files to populate database table of shipments.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class ShipmentDataDb {

	private static Logger log = Logger.getLogger(ShipmentDataDb.class);

	public static final DecimalFormat sizeFormat = new DecimalFormat("#.####");

	private static final String SQL = "{call la.update_shipment_data(?,?,?,?,?,?," +
		"?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}",
		SQL_CLEAN_EVT = "{call la.clean_evt(?,?)}",
		SQL_READ_DAILY_RN_FILES = "SELECT daily_rn_files FROM la.henvr",
		SQL_UPDATE_DAILY_RN_FILES = "UPDATE la.henvr SET daily_rn_files=?",
		SQL_LOCAL_STORES = "SELECT status,n,local_dc from la.hstore_profile";

	static String[] sqlCommands;

	static HashSet<String> dailyRnFiles = new HashSet<String>();
	static HashSet<Integer> localDcMissing = new HashSet<Integer>();
	private static int delFound;

	private static void checkLocalDCs(PreparedStatement st,
		HashMap<Integer,String> localDcMap) throws Exception {
		if (localDcMap == null) { return;}
		String open = "OPEN";
		ResultSet rs = st.executeQuery();
		int i = 0, count = 0;
		while (rs.next()) {
			String s = rs.getString(1);
			int n = rs.getInt(2);
			if (!open.equalsIgnoreCase(s) || n > 9000 && n < 10000) {
				continue;
			}
			String dc = rs.getString(3);
			String dc1 = localDcMap.get(n);
			if (dc1 == null) {
				if (!localDcMissing.contains(n)) {
					log.warn("Missing local DC for the store "+n+", status "+s);
					i++;
				}
			}
			else if (!dc1.equals(dc) && !localDcMissing.contains(n)) {
				log.warn("Incorrect local DC for the store "+n+", status "+s);
				i++;
			}
			count++;
		}
		rs.close();
		st.close();
		if (i != 0) {
			log.debug("Check for local DC : Properties "+localDcMap.size()+", Stores "+count);
		}
	}
	static void updateDailyRnFiles(Connection con,
		ArrayList<String> rnFiles) throws Exception {
		if (rnFiles == null) {
			dailyRnFiles.clear();
			try {
				con = ConnectFactory1.one().getConnection();
				PreparedStatement st = con.prepareStatement(SQL_UPDATE_DAILY_RN_FILES);
				st.setNull(1, Types.VARCHAR);
				st.executeUpdate();
				con.commit();
				st.close();
			}
			catch (Exception ex) {
				ex.printStackTrace();
				log.error(ex);
			}
			finally { ConnectFactory1.close(con);}
		}
		else {
			PreparedStatement st = con.prepareStatement(SQL_UPDATE_DAILY_RN_FILES),
				st1 = con.prepareStatement(SQL_READ_DAILY_RN_FILES);
			ResultSet rs = st1.executeQuery();
			if (rs.next()) {
				String v = rs.getString(1);
				if (v != null) {
					dailyRnFiles.clear();
					String[] arr = v.split("\\,");
					dailyRnFiles.addAll(Arrays.asList(arr));
				}
			}
			rs.close(); st1.close();
			dailyRnFiles.addAll(rnFiles);
			StringBuilder sb = new StringBuilder(256);
			for (Iterator<String> it = dailyRnFiles.iterator(); it.hasNext();) {
				if (sb.length() != 0) { sb.append(',');}
				sb.append(it.next());
			}
			st.setString(1, sb.toString());
			st.executeUpdate();
			st.close();
		}
	}
	static void update(File rnFolder, File rnaFolder, RnColumns rnCols,
		HashMap<Integer,String> localDcMap) throws Exception {
		Row r = new Row();
		Connection con = null;
		try {
			File[] fs = rnFolder.listFiles(new UpExtFilter());
			if (fs.length != 0) {
				ArrayList<String> rnFiles = new ArrayList<String>(8);
				HashMap<Long,String> evtMap = new HashMap<Long,String>(8);
				con = ConnectFactory1.one().getConnection();
				checkLocalDCs(con.prepareStatement(SQL_LOCAL_STORES), localDcMap);
				CallableStatement st = con.prepareCall(SQL),
					st1 = con.prepareCall(SQL_CLEAN_EVT);
				for (int i = 0; i != fs.length; i++) {
					File f = fs[i];
					boolean err = update(f, st, r, rnCols, evtMap);
					log.debug("Rows updated for the file "+f);
					rnFiles.add(r.dc+(err ? " ? " : " - ")+f.getName()+" - "+
						SupportTime.MMM_dd_Format.format(r.shipDate));
				}
				cleanEvt(st1, evtMap);
				con.commit();
				st.close(); st1.close();
				ScheduledWorker.move(fs, rnaFolder);
				if (sqlCommands != null) {
					SqlSupport.update(con, sqlCommands);
					con.commit();
				}
				updateDailyRnFiles(con, rnFiles);
				con.commit();
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
	private static void cleanEvt(CallableStatement st, HashMap<Long,String> evtMap) throws Exception {
		for (Iterator<Map.Entry<Long,String>> it = evtMap.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Long,String> e = it.next();
			st.setLong(1, e.getKey());
			st.setString(2, e.getValue());
			st.addBatch();
		}
		st.executeBatch();
	}
	private static boolean update(File f, CallableStatement st,
		Row r, RnColumns rnCols, HashMap<Long,String> evtMap) throws Exception {
		int[] i = {1};
		boolean semicolons = false, err = false;
		HashSet<DsKey> delNotFound = new HashSet<DsKey>();
		String userFile = f.getName();
		Key k = new Key();
		BufferedReader br = new BufferedReader(new FileReader(f));
		try {
			log.debug("\r\n\r\nReading the file '"+f.getPath()+"'...");
			while (true) {
				String ln = br.readLine();
				if (ln == null) { break;}
				ln = ln.trim();
				if (ln.isEmpty()) { continue;}
				int j = ln.indexOf(';');
				if (j != -1) {
					int n = ln.indexOf('|');
					if (n != -1 && n < j) {
						log.error("Semicolon is illegal character:\r\n"+ln.substring(0, j+1));
					}
					else if (!semicolons) { semicolons = true;}
					continue;
				}
				ln = ln.replaceAll("\"", "");
				try {
					setRow(r, k, i, ln, rnCols);
					if (r.cmdty.equals("EVT")) {
						delFound = -1;
						update(st, r, userFile, delNotFound);
						if (delFound == 1) {
							evtMap.put(r.n, r.cmdty);
						}
						else { delFound = 0;}
						r.n = 0;
						r.cmdty = "EVT2";
						update(st, r, userFile, delNotFound);
						if (r.n != 0) {
							evtMap.put(r.n, r.cmdty);
						}
					}
					else {
						delFound = 0;
						update(st, r, userFile, delNotFound);
					}
					if (!err && delFound != 1) { err = true;}
				}
				catch (Exception ex) {
					ex.printStackTrace();
					log.error(k.storeN+", "+k.cmdty+", "+k.shipDate+", "+k.dc+"-DC\r\n"+ex);
					err = true;
				}
			}
			if (semicolons) {
				log.warn("Semicolons appear in the file");
				err = true;
			}
		}
		finally { br.close();}
		return err;
	}
	private static void checkDcOrLw(String v) {
		if (!Character.isDigit(v.charAt(0)) || !Character.isDigit(v.charAt(1))) {
			throw new IllegalArgumentException("Incorrect format of order #");
		}
	}
	private static void setRow(Row r, Key k, int[] i,
		String line, RnColumns rnCols) throws Exception {
		String v = getCellValue(line, RnColumns.STORE_N, rnCols);
		r.storeN = Integer.parseInt(v)/100;
		v = getCellValue(line, RnColumns.SHIP_DATE, rnCols);
		java.util.Date d = SupportTime.yyyy_MM_dd_Format.parse(v);
		r.shipDate = new Date(d.getTime());
		r.dc = getCellValue(line, RnColumns.DC, rnCols);
		checkDcOrLw(r.dc);
		r.lw = getCellValue(line, RnColumns.COMMODITY, rnCols);
		checkDcOrLw(r.lw);
		r.routeN = getCellValue(line, RnColumns.ROUTE_N, rnCols);
		int routeN = Integer.parseInt(r.routeN);
		if (routeN >= 10000 && routeN < 10001) {
			r.addKey = "EVT";
		}
		else { r.addKey = " ";}
		v = getCellValue(line, RnColumns.ORDER_N, rnCols);
		if (v != null && v.length() > 4) {
			String v1 = v.substring(4);
			try {
				Long.parseLong(v1);
			}
			catch (Exception e) {
				v = v+(i[0]++);
			}
		}
		r.orderN = v;
		r.orderType = getCellValue(line, RnColumns.ORDER_TYPE, rnCols);
		r.cmdty = getCommodity(r.dc, r.lw, r.orderType, routeN);
		r.pallets = getQty(line, RnColumns.PALLETS, rnCols);
		r.units = getQty(line, RnColumns.UNITS, rnCols);
		r.weight = getQty(line, RnColumns.WEIGHT, rnCols);
		r.cube = getQty(line, RnColumns.CUBE, rnCols);
		if (r.storeN != k.storeN || !r.cmdty.equals(k.cmdty) || !r.shipDate.equals(k.shipDate) ||
			!r.dc.equals(k.dc) || !r.addKey.equals(k.addKey)) {
			i[0] = 1;
			k.storeN = r.storeN;
			k.cmdty = r.cmdty;
			k.shipDate = r.shipDate;
			k.dc = r.dc;
			k.addKey = r.addKey;
			r.n = 0;
			Calendar c = Calendar.getInstance(); c.setTime(d);
			r.shipDay = c.get(Calendar.DAY_OF_WEEK)-1;
			r.stopN = getCellValue(line, RnColumns.STOP_N, rnCols);
			r.dcDepartTime = getTime(line, RnColumns.DC_DEPART_TIME, rnCols);
			v = getCellValue(line, RnColumns.PREV_DISTANCE, rnCols);
			r.prevDistance = Integer.parseInt(v);
			r.prevTravelTime = getTime(line, RnColumns.PREV_TRAVEL_TIME, rnCols);
			r.arrivalTime = getTime(line, RnColumns.ARRIVAL_TIME, rnCols);
			r.serviceTime = getTime(line, RnColumns.SERVICE_TIME, rnCols);
			r.totalServiceTime = getTime(line, RnColumns.TOTAL_SERVICE_TIME, rnCols);
			r.totalTravelTime = getTime(line, RnColumns.TOTAL_TRAVEL_TIME, rnCols);
			r.equipSize = getCellValue(line, RnColumns.EQUIP_SIZE, rnCols);
		}
	}
	private static String getCellValue(String line, String colName, RnColumns rnCols) {
		RnColumns.ColPosition cp = rnCols.getPosition(colName);
		int i1 = cp.getStartIndex(), i2 = cp.getEndIndex();
		String v = line.substring(i1, i2).trim();
		if (v.charAt(0) == '|') {
			v = line.substring(i1+1, i2+1).trim();
		}
		else if (v.charAt(v.length()-1) == '|') {
			v = line.substring(i1-1, i2-1).trim();
		}
		return v;
	}
	private static double getQty(String line, String colName, RnColumns rnCols) throws ParseException {
		String v = getCellValue(line, colName, rnCols);
		return v.isEmpty() ? 0 : sizeFormat.parse(v).doubleValue();
	}
	private static Time getTime(String line, String colName, RnColumns rnCols) throws ParseException {
		String v = getCellValue(line, colName, rnCols);
		return SupportTime.parseTimeHH_mm(v);
	}
	private static void update(CallableStatement st, Row r, String userFile,
		HashSet<DsKey> delNotFound) throws Exception {
		st.setLong(1, r.n);
		st.setInt(2, r.storeN);
		st.setString(3, r.cmdty);
		st.setDate(4, r.shipDate);
		st.setInt(5, r.shipDay);
		st.setString(6, r.routeN);
		st.setString(7, r.stopN);
		st.setString(8, r.dc);
		st.setTime(9, r.dcDepartTime);
		st.setInt(10, r.prevDistance);
		st.setTime(11, r.prevTravelTime);
		st.setTime(12, r.arrivalTime);
		st.setTime(13, r.serviceTime);
		st.setTime(14, r.totalServiceTime);
		st.setTime(15, r.totalTravelTime);
		st.setString(16, r.equipSize);
		st.setString(17, r.addKey);
		st.setString(18, r.orderN);
		st.setString(19, r.orderType);
		st.setString(20, r.lw);
		st.setDouble(21, r.pallets);
		st.setDouble(22, r.units);
		st.setDouble(23, r.weight);
		st.setDouble(24, r.cube);
		st.setString(25, userFile);
		st.registerOutParameter(1, Types.BIGINT);
		try {
			st.execute();
			delFound = 1;
		}
		catch (SQLException e) {
			String m = e.getMessage();
			if (!m.contains("70064")) {
				throw e;
			}
			else if (delFound == 0) {
				String dcx = "DCX";
				if (!r.cmdty.equals(dcx) && m.contains(",DCX,")) {
					r.cmdty = dcx;
				}
				DsKey k = new DsKey(r.storeN, r.cmdty, r.shipDay);
				if (delNotFound.add(k)) {
					log.error("Delivery not found: "+k+", DC"+r.dc+", Route "+r.routeN);
				}
			}
			return;
		}
		r.n = st.getLong(1);
	}
	private static String getCommodity(String dc, String lw, String ordType, int routeN) {
		if (ordType.equals("S") || routeN >= 8000 && routeN < 9000) {
			return "EVT";
		}
		switch (lw) {
		case "60":
		case "65":
		case "70":
		case "75":
		case "80":
		case "85":
		case "90":
		case "95": return "EVT";
		case "10":
		case "15":
		case "30":
			switch (dc) {
			case "10":
			case "30":
			case "50": return "DCV";
			case "20":
			case "70": return "DCB";
			}
		case "20":
		case "25": return "DCF";
		case "55":
		case "56":
			switch (dc) {
			case "20": return "DCX";
			case "10":
			case "30":
			case "50": return "DCV";
			case "70": return "DCB";
			}
		default: return "RX";
		}
	}
	private static class Key {
		private int storeN;
		private Date shipDate;
		private String cmdty, dc, addKey;
	}
	private static class Row {
		private long n;
		private int storeN, shipDay, prevDistance;
		private Date shipDate;
		private Time dcDepartTime, prevTravelTime, arrivalTime, serviceTime,
			totalServiceTime, totalTravelTime;
		private String cmdty, routeN, stopN, dc, equipSize, orderN, orderType, lw, addKey;
		private double pallets, units, weight, cube;
	}
	private static class UpExtFilter implements FileFilter {

		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) { return false;}
			String ext = SupportFile.getExtension(f);
			return ext != null && ext.toLowerCase().startsWith("up");
		}
	}
}
