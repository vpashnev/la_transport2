package com.logisticsalliance.general;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.logisticsalliance.io.SupportFile;

/**
 * This class starts the application.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class App {

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
		new File(appDir, "log").mkdir();
		configureLog4j(appDir);

		Properties appProps = new Properties();
		appProps.load(new FileReader(new File(appDir, "app.properties")));

		File srcDir = new File(appProps.getProperty("sourceDocsDirectory")); // directory of source documents
		String dbPwd = null, dbPwdI5 = null, emReadPwd = null, emSentPwd = null;
		if (args.length > 1) {
			dbPwd = args[1];
			if (args.length > 2) {
				dbPwdI5 = args[2];
				if (args.length > 3) {
					emReadPwd = args[3];
					if (args.length > 4) {
						emSentPwd = args[4];
					}
				}
			}
		}
		EMailReports mr = new EMailReports(appDir);
		RnColumns rnCols = new RnColumns(appDir);
		ScheduledWorker sr = new ScheduledWorker(srcDir, dbPwd, dbPwdI5,
			emReadPwd, emSentPwd, appProps, getLocalDC(appDir), mr, rnCols);
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

	private static HashMap<Integer,String> getLocalDC(File appDir) throws Exception {
		File localDc = new File(appDir, "LocalDC.properties");
		Properties localDcProps = null;
		if (localDc.exists()) {
			localDcProps = new Properties();
			localDcProps.load(new FileReader(localDc));
			HashMap<Integer,String> m = new HashMap<Integer,String>(1600, .5f);
			for (Iterator<Map.Entry<Object,Object>> it = localDcProps.entrySet().iterator(); it.hasNext();) {
				Map.Entry<Object,Object> e = it.next();
				String k = e.getKey().toString(), v = e.getValue().toString();
				Integer i = new Integer(k);
				m.put(i, v);
			}
			return m;
		}
		return null;
	}

	static void configureLog4j(File appDir) {
		SupportFile.configureLog4j(appDir, "log4j.properties");
	}

}
