package com.logisticsalliance.tp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.SupportGeneral;
import com.logisticsalliance.io.SupportFile;
import com.logisticsalliance.util.SupportTime;

/**
 * This class starts the application model.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class AppModel {

	static final String dc1 = "dc", carrier1 = "carrier",
		fromDate1 = "fromDate", toDate1 = "toDate", delDates1 = "delDates";
	static final String[] cmdty1 = {
		CommonConstants.DCB, CommonConstants.DCV, CommonConstants.DCX,
		CommonConstants.DCF, CommonConstants.EVT, CommonConstants.EVT2,
		CommonConstants.RX
	};

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws Exception {
		File appDir; // configuration directory
		if (args.length == 0) {
			appDir = new File(".");
		}
		else {
			appDir = new File(args[0]);
			if (!appDir.exists() && !appDir.mkdir()) {
				throw new IllegalArgumentException("The directory '"+appDir+"' does not exist");
			}
		}
		new File(appDir, "logweb").mkdir();
		configureLog4j(appDir);

		Properties appProps = new Properties();
		appProps.load(new FileReader(new File(appDir, "app.properties")));

		Properties inputProps = new Properties();
		inputProps.load(new FileReader(new File(appDir, "tp_input.properties")));
		SearchInput si = getInput(inputProps);

		String dbPwd = null;
		if (args.length > 1) {
			dbPwd = args[1];
		}
		SupportGeneral.makeDataSource1I5(appProps, dbPwd, null);
		for (int i = 0; i <= si.toDay-si.fromDay; i++) {
			ArrayList<ShipmentRow> al = FillGridDB.process(si, i);
			SpreadSheet.fill(al);
		}
	}

	private static SearchInput getInput(Properties inputProps) throws ParseException {
		SearchInput si = new SearchInput();
		String v = inputProps.getProperty(fromDate1);
		si.fromDate = new Date(SupportTime.dd_MM_yyyy_Format.parse(v).getTime());
		si.fromDay = SupportTime.getDayOfWeek(si.fromDate);
		v = inputProps.getProperty(toDate1);
		si.toDate = new Date(SupportTime.dd_MM_yyyy_Format.parse(v).getTime());
		si.toDay = SupportTime.getDayOfWeek(si.toDate);
		if (si.fromDay > si.toDay) {
			throw new IllegalArgumentException("Illegal date range");
		}
		si.dc = inputProps.getProperty(dc1);
		si.carrier = inputProps.getProperty(carrier1);
		si.delDates = inputProps.getProperty(delDates1) != null;
		si.cmdty = getCmdty(inputProps);
		return si;
	}
	private static boolean[] getCmdty(Properties inputProps) {
		boolean[] arr = new boolean[cmdty1.length];
		boolean has = false;
		for (int i = 0; i != arr.length; i++) {
			String v = inputProps.getProperty(cmdty1[i]);
			arr[i] = v != null && !v.isEmpty();
			if (!has && arr[i]) {
				has = true;
			}
		}
		if (!has) {
			for (int i = 0; i != arr.length; i++) {
				arr[i] = true;
			}
		}
		return arr;
	}
	static void configureLog4j(File appDir) {
		SupportFile.configureLog4j(appDir, "log4jTPWeb.properties");
	}

}
