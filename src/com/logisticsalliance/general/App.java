package com.logisticsalliance.general;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import com.glossium.io.SupportFile;

/**
 * This class starts the application.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class App {

	/**
	 * @param args
	 * @throws Exception 
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
		new File(appDir, "log").mkdir();
		configureLog4j(appDir);

		setSqlCommands(appDir);

		Properties appProps = new Properties();
		appProps.load(new FileReader(new File(appDir, "app.properties")));

		File srcDir = new File(appProps.getProperty("sourceDocsDirectory")); // directory of source documents
		String dbPwd = null, dbPwdI5 = null, ftpPwd = null, emReadPwd = null,
			emSentPwd = null, ttEmSentPwd = null, ksPwd = null;
		if (args.length > 1) {
			dbPwd = args[1];
			if (args.length > 2) {
				dbPwdI5 = args[2];
				if (args.length > 3) {
					ftpPwd = args[3];
					if (args.length > 4) {
						emReadPwd = args[4];
						if (args.length > 5) {
							emSentPwd = args[5];
							if (args.length > 6) {
								ttEmSentPwd = args[6];
								if (args.length > 7) {
									ksPwd = args[7];
								}
							}
						}
					}
				}
			}
		}
		EmailReports mr = new EmailReports(appDir);
		RnColumns rnCols = new RnColumns(appDir);
		ScheduledWorker sr = new ScheduledWorker(appDir, srcDir, dbPwd, dbPwdI5,
			ftpPwd, emReadPwd, emSentPwd, ttEmSentPwd, ksPwd, appProps, mr, rnCols);
		sr.start();
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
		String s = null;
		while (true) {
			System.out.println("Press Q to quit");
			System.out.print(">");
			try {
				s = r.readLine();
			}
			catch (IOException e) {
				System.err.println("Console reading error: " + e.getMessage());
			}
			if (s != null && s.equalsIgnoreCase("Q") && sr.stop()) {
				return;
			}
		}
	}

	private static void setSqlCommands(File appDir) throws Exception {
		File f = new File(appDir, "DsScript.sql");
		if (f.exists()) {
			String sql = SupportFile.readText(f);
			StoreScheduleDb.sqlCommands = sql.split("\\ ;");
		}
		f = new File(appDir, "RnScript.sql");
		if (f.exists()) {
			String sql = SupportFile.readText(f);
			ShipmentDataDb.sqlCommands = sql.split("\\ ;");
		}
	}

	static void configureLog4j(File appDir) {
		SupportGeneral.configureLog4j(appDir, "log4j.properties");
	}

}
