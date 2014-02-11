package com.logisticsalliance.tp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
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
		CommonConstants.DCF, CommonConstants.RX,
		CommonConstants.EVT, CommonConstants.EVT2
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
		appProps.load(new FileReader(new File(appDir, "tp_input.properties")));
		SearchInput si = getInput(inputProps);

		String dbPwd = null;
		if (args.length > 1) {
			dbPwd = args[1];
		}
		SupportGeneral.makeDataSource1I5(appProps, dbPwd, null);
		FillGridDB.process(si);
	}

	private static SearchInput getInput(Properties inputProps) throws ParseException {
		SearchInput si = new SearchInput();
		si.dc = inputProps.getProperty(dc1);
		si.carrier = inputProps.getProperty(carrier1);
		si.delDates = inputProps.getProperty(delDates1) != null;
		String v = inputProps.getProperty(fromDate1);
		si.fromDate = new Date(SupportTime.dd_MM_yyyy_Format.parse(v).getTime());
		v = inputProps.getProperty(toDate1);
		si.toDate = new Date(SupportTime.dd_MM_yyyy_Format.parse(v).getTime());
		si.cmdty = getCmdty(inputProps);
		return si;
	}
	private static boolean[] getCmdty(Properties inputProps) {
		boolean[] arr = new boolean[cmdty1.length];
		for (int i = 0; i != arr.length; i++) {
			String v = inputProps.getProperty(cmdty1[i]);
			arr[i] = v != null;
		}
		return arr;
	}
	static void configureLog4j(File appDir) {
		SupportFile.configureLog4j(appDir, "log4jTPWeb.properties");
	}

}
