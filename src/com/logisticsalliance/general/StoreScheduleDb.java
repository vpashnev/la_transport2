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
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;

import org.apache.log4j.Logger;

import com.glossium.io.SupportFile;
import com.glossium.sqla.ConnectFactory;
import com.glossium.sqla.ConnectFactory1;
import com.glossium.sqla.SqlSupport;
import com.logisticsalliance.util.SupportTime;

/**
 * This class reads XLS-files to populate database table of store schedule.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class StoreScheduleDb {

	private static Logger log = Logger.getLogger(StoreScheduleDb.class);

	private static final String SQL =
		"{call la.update_scheduled_delivery(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}",
		SQL_RESET_IN_USE = "UPDATE la.hstore_schedule SET in_use=NULL WHERE ship_date IS NULL",
		SQL_DELETE_NOT_IN_USE = "DELETE FROM la.hstore_schedule WHERE in_use IS NULL",
		SQL_STORES = "SELECT DISTINCT sp.n, sts.dc, local_dc from la.hstore_profile sp," +
			"la.hstore_schedule sts WHERE sp.n=sts.store_n AND (UPPER(status)='OPEN' OR " +
			"UPPER(status)='ACTIVE') AND sp.n < 9000",
		SQL_DCB = "SELECT dc FROM la.hstore_schedule WHERE store_n=? AND cmdty='DCB'",
		SQL_UPD_LDC = "UPDATE la.hstore_profile SET local_dc=? WHERE n=?";

	static String[] sqlCommands;

	private static void updateLocalDc(Connection con) throws Exception {
		PreparedStatement st = con.prepareStatement(SQL_STORES),
			dcbSt = con.prepareStatement(SQL_DCB),
			updSt = con.prepareStatement(SQL_UPD_LDC);
		ResultSet rs = st.executeQuery();
		while (rs.next()) {
			int n = rs.getInt(1);
			String dc = rs.getString(2), ldc = rs.getString(3);
			dcbSt.setInt(1, n);
			ResultSet rs1 = dcbSt.executeQuery();
			boolean dcb = rs1.next();
			switch (dc) {
			case CommonConstants.DC30:
				if (dcb) {
					dc = CommonConstants.DC20;
				}
				break;
			case CommonConstants.DC50:
				if (dcb) {
					dc = CommonConstants.DC70;
				}
			}
			if (!dc.equals(ldc)) {
				updSt.setString(1, dc);
				updSt.setInt(2, n);
				updSt.executeUpdate();
			}
		}
		con.commit();
	}
	static void update(File dsFolder, File dsaFolder, int weeksBeforeHolidays) throws Exception {
		int[] rowCount = {0};
		Row r = new Row();
		Date curDate = new Date(System.currentTimeMillis());
		Connection con = null;
		try {
			File[] fs = dsFolder.listFiles(new XslExtFilter());
			if (fs.length != 0) {
				con = ConnectFactory1.one().getConnection();
				CallableStatement st = con.prepareCall(SQL);
				Statement st1 = con.createStatement();
				int n = st1.executeUpdate(SQL_RESET_IN_USE);
				st1.close();
				for (int i = 0; i != fs.length; i++) {
					update(fs[i], st, r, rowCount, curDate, weeksBeforeHolidays);
					log.debug("Rows updated for the file "+fs[i]);
				}
				st.executeBatch();
				st1 = con.createStatement();
				n = st1.executeUpdate(SQL_DELETE_NOT_IN_USE);
				if (n != 0) {
					log.debug("Deleted store scheduled deliveries: "+n);
				}
				st1.close();
				con.commit();
				st.close();
				ScheduledWorker.move(fs, dsaFolder);
				if (sqlCommands != null) {
					SqlSupport.update(con, sqlCommands);
					con.commit();
				}
				updateLocalDc(con);
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
	private static void update(File f, CallableStatement st, Row r, int[] rowCount,
		Date curDate, int weeksBeforeHolidays) throws Exception {
		String userFile = f.getName();
		BufferedReader br = new BufferedReader(new FileReader(f));
		try {
			log.debug("\r\n\r\nReading the file '"+f.getPath()+"'...");
			int i = 0;
			while (true) {
				String ln = br.readLine();
				if (ln == null) { break;}
				if (i < 3) {
					i++; continue;
				}
				ln = ln.trim();
				String[] arr = ln.split("\\\t");
				if (arr.length < 28 || arr.length > 30 && arr.length < 35) {
					if (!ln.isEmpty()) { log.error("Cells out of range: "+ln);}
					continue;
				}
				for (int j = 0; j != arr.length; j++) {
					arr[j] = arr[j].trim();
				}
				boolean hday = arr.length >= 35;
				try {
					if (setRow(r, arr, hday, curDate, weeksBeforeHolidays)) {
						update(st, r, arr, userFile, hday, rowCount);
					}
				}
				catch (Exception ex) {
					ex.printStackTrace();
					Object d = hday ? r.shipDate : r.shipDay;
					log.error(r.storeN+", "+r.cmdty+", "+d+"\r\n"+ex);
				}
			}
		}
		finally { br.close();}
	}
	private static boolean setRow(Row r, String[] arr, boolean hday,
		Date curDate, int weeksBeforeHolidays) throws Exception {
		if (hday) {
			r.storeN = Integer.parseInt(arr[1]);
			r.address = arr[2];
			r.city = arr[3];
			r.province = arr[4];
			r.postCode = arr[5];
			r.description = arr[6];
			r.polDay = SupportTime.getDayNumber(arr[9]);
			java.util.Date d = SupportTime.dd_MM_yyyy_Format.parse(arr[10]);
			r.polDate = new Date(d.getTime());
			r.polTime = SupportTime.parseTimeHHmm(arr[12]);
			r.dallasDay = SupportTime.getDayNumber(arr[13]);
			d = SupportTime.dd_MM_yyyy_Format.parse(arr[14]);
			r.dallasDate = new Date(d.getTime());
			r.dallasTime = SupportTime.parseTimeHHmm(arr[15]);
			r.shipDay = SupportTime.getDayNumber(arr[16]);
			d = SupportTime.dd_MM_yyyy_Format.parse(arr[17]);
			r.shipDate = new Date(d.getTime());
			if (ignore(r.shipDate, curDate, weeksBeforeHolidays)) {
				return false;
			}
			r.shipTime = SupportTime.parseTimeHHmm(arr[18]);
			r.delDay = SupportTime.getDayNumber(arr[19]);
			d = SupportTime.dd_MM_yyyy_Format.parse(arr[20]);
			r.delDate = new Date(d.getTime());
			if (r.delDate.compareTo(r.shipDate) < 0) {
				throw new IllegalArgumentException("Illegal delivery date " +
					"in holidays store schedule (less than truck date)");
			}
			r.delTimeFrom = SupportTime.parseTimeHHmm(arr[21]);
			r.delTimeTo = SupportTime.parseTimeHHmm(arr[22]);
		}
		else {
			r.storeN = Integer.parseInt(arr[0]);
			r.address = arr[1];
			r.city = arr[2];
			r.province = arr[3];
			r.postCode = arr[4];
			r.polDay = SupportTime.getDayNumber(arr[5]);
			r.polTime = SupportTime.parseTimeHHmm(arr[7]);
			r.dallasDay = SupportTime.getDayNumber(arr[8]);
			r.dallasTime = SupportTime.parseTimeHHmm(arr[9]);
			r.shipDay = SupportTime.getDayNumber(arr[10]);
			r.shipTime = SupportTime.parseTimeHHmm(arr[11]);
			r.delDay = SupportTime.getDayNumber(arr[12]);
			r.delTimeFrom = SupportTime.parseTimeHHmm(arr[13]);
			r.delTimeTo = SupportTime.parseTimeHHmm(arr[14]);
			r.delWeek = SupportTime.getWeeks(arr[15]);
		}
		return true;
	}
	private static boolean ignore(Date d, Date curDate, int weeksBeforeHolidays) {
		if (weeksBeforeHolidays > 0) {
			d = SupportTime.getFirstDayOfWeek(d, -weeksBeforeHolidays);
			if (curDate.compareTo(d) < 0) {
				return true;
			}
		}
		return false;
	}
	private static void update(CallableStatement st, Row r, String[] arr,
		String userFile, boolean hday, int[] rowCount) throws Exception {
		int ln1 = arr.length-1; // decrement for index of Store Status
		r.storeStatus = arr[ln1];
		String cmdty = null;
		if (hday) {
			for (int i = 23; i != ln1; i++) {
				String v = arr[i];
				if (!v.isEmpty()) {
					switch (i) {
					case 23:
						set(st, r, CommonConstants.EVT, userFile, hday, rowCount);
						cmdty = CommonConstants.DCB; break;
					case 32:
						cmdty = CommonConstants.DCV; break;
					case 24:
						cmdty = CommonConstants.DCF; break;
					case 28:
						cmdty = CommonConstants.DCX; break;
					case 29:
						cmdty = CommonConstants.EVT; break;
					case 30:
						cmdty = CommonConstants.EVT2; break;
					case 25:
					case 31:
					case 33:
						cmdty = CommonConstants.RX; break;
					}
					if (cmdty != null) { set(st, r, cmdty, userFile, hday, rowCount);}
				}
			}
		}
		else {
			for (int i = 16; i != ln1; i++) {
				String v = arr[i];
				if (!v.isEmpty()) {
					switch (i) {
					case 16:
						set(st, r, CommonConstants.EVT, userFile, hday, rowCount);
						cmdty = CommonConstants.DCB; break;
					case 25:
						cmdty = CommonConstants.DCV; break;
					case 17:
						cmdty = CommonConstants.DCF; break;
					case 21:
						cmdty = CommonConstants.DCX; break;
					case 22:
						cmdty = CommonConstants.EVT; break;
					case 23:
						cmdty = CommonConstants.EVT2; break;
					case 18:
					case 24:
					case 26:
						cmdty = CommonConstants.RX; break;
					}
					if (cmdty != null) { set(st, r, cmdty, userFile, hday, rowCount);}
				}
			}
		}
	}
	private static String getDc(String userFile) {
		int i = userFile.indexOf("_DC");
		if (i == -1) {
			return "";
		}
		else {
			i += 5;
			if (i < userFile.length()) {
				String dc = userFile.substring(i-2, i);
				return dc;
			}
			else { return "";}
		}
	}
	private static void set(CallableStatement st, Row r, String cmdty,
		String userFile, boolean hday, int[] rowCount) throws Exception {
		//log.info(r.storeN +", "+r.cmdty+", "+r.shipDay);
		r.cmdty = cmdty;
		st.setInt(1, r.storeN);
		st.setString(2, r.address);
		st.setString(3, r.city);
		st.setString(4, r.province);
		st.setString(5, r.postCode);
		st.setString(6, getDc(userFile));
		st.setString(7, r.cmdty);
		st.setInt(8, r.shipDay);
		st.setInt(9, r.delDay);
		st.setTime(10, r.delTimeFrom);
		st.setTime(11, r.delTimeTo);
		st.setInt(14, r.polDay);
		st.setTime(16, r.polTime);
		st.setInt(17, r.dallasDay);
		st.setTime(19, r.dallasTime);
		st.setTime(20, r.shipTime);
		if (hday) {
			st.setInt(12, 0);
			st.setString(13, r.description);
			st.setDate(15, r.polDate);
			st.setDate(18, r.dallasDate);
			st.setDate(21, r.shipDate);
			st.setDate(22, r.delDate);
		}
		else {
			st.setInt(12, r.delWeek);
			st.setNull(13, Types.VARCHAR);
			st.setNull(15, Types.DATE);
			st.setNull(18, Types.DATE);
			st.setNull(21, Types.DATE);
			st.setNull(22, Types.DATE);
		}
		st.setString(23, r.storeStatus);
		st.setString(24, userFile);
		st.addBatch();
		rowCount[0]++;
		if (rowCount[0] == 1000) {
			rowCount[0] = 0;
			st.executeBatch();
		}
	}

	private static class Row {
		private int storeN, polDay, dallasDay, shipDay, delDay, delWeek;
		private String cmdty, address, city, province, postCode, description, storeStatus;
		private Time delTimeFrom, delTimeTo, polTime, dallasTime, shipTime;
		private Date polDate, dallasDate, shipDate, delDate;
	}
	private static class XslExtFilter implements FileFilter {

		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) { return false;}
			String ext = SupportFile.getExtension(f);
			return ext != null && ext.toLowerCase().startsWith("xls");
		}
	}
}
