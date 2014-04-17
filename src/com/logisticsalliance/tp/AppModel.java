package com.logisticsalliance.tp;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.glossium.io.SupportFile;
import com.glossium.sqla.ConnectFactory1;
import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.DsKey;
import com.logisticsalliance.general.SupportGeneral;
import com.logisticsalliance.util.SupportTime;

/**
 * This class starts the application model.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class AppModel {

	static final String dc1 = "dc", carrier1 = "carrier",
		fromDate1 = "fromDate", days1 = "days",
		holidayWeeks1 = "holidayWeeks", test1 = "test";
	static final String[] cmdty1 = {
		CommonConstants.DCB, CommonConstants.DCV, CommonConstants.DCX,
		CommonConstants.DCF, CommonConstants.EVT, CommonConstants.EVT2,
		CommonConstants.RX
	};

	/*private static String getText() {
		StringBuilder b = new StringBuilder(256);
		b.append("#date format dd/MM/yy\r\n");
		b.append("fromDate\t\t= 23/02/2014\r\n");
		b.append("toDate\t\t= 01/03/2014\r\n");
		b.append("dc\t\t= 30\r\n");
		b.append("holidays\t\t= \r\n\r\n");
		b.append("#Commodities:\r\n");
		b.append("DCB\t\t= 1\r\n");
		b.append("DCV\t\t= 1\r\n");
		b.append("DCX\t\t= 1\r\n");
		b.append("DCF\t\t= 1\r\n");
		b.append("RX\t\t= 1\r\n");
		return b.toString();
	}
	private static String showDialog(JTextArea ta, String txt) {
		if (txt != null) { ta.setText(txt);}
		ta.setFont(ta.getFont().deriveFont(20f));
		JPanel p = new JPanel(new BorderLayout());
		p.add(ta, BorderLayout.CENTER);
		JOptionPane op = new JOptionPane(p);
		op.setOptions(new String[]{"OK", "Cancel"});
		JDialog dlg = op.createDialog(null, "Login");
		dlg.pack();
		int i = SupportUI.show(dlg, op);
		return i == 0 ? ta.getText() : null;
	}*/
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
		new File(appDir, "logweb").mkdir();
		configureLog4j(appDir);

		Properties appProps = new Properties(), inputProps = new Properties();
		appProps.load(new FileReader(new File(appDir, "app.properties")));

		String dbPwd = null;
		if (args.length > 1) {
			dbPwd = args[1];
		}
		SupportGeneral.makeDataSource1I5(appProps, dbPwd, null);
		FillGridDB.connectFactory = ConnectFactory1.one();

		/*FillGridDB.connectFactory =
			SupportNet.getConnectFactory(args[1], args[2], new String[2]);
		JTextArea ta = new JTextArea(12, 40);
		String t = getText();
		t = showDialog(ta, t);
		while (t != null) {
			inputProps.load(new StringReader(t));
			SearchInput si = getInput(inputProps);

			process(appDir, si);
			t = showDialog(ta, null);
		}*/
		inputProps.load(new FileReader(new File(appDir, "tp_input.properties")));
		SearchInput si = getInput(inputProps);

		process(appDir, si);
		System.out.print("Done !");
		System.exit(0);
	}

	private static void process(File appDir, SearchInput si) throws Exception {
		int dc20 = 0;
		if (si.dc.equals(CommonConstants.DC20)) {
			dc20 = 1;
		}
		else if (si.dc.toUpperCase().equals(CommonConstants.DC20F)) { dc20 = 2;}
		boolean hasHolidayWeeks = si.holidayWeeks > 0,
			dc50 = si.dc.equals(CommonConstants.DC50),
			dc70 = si.dc.equals(CommonConstants.DC70);
		String a = dc20 == 2 ? "F" : "";
		si.dc = si.dc.substring(0, 2);
		//Workbook wb = new XSSFWorkbook();
		int count = si.toDay-si.fromDay;
		HashMap<Integer,HashMap<String,HashMap<DsKey,ShipmentRow>>> all =
			new HashMap<Integer,HashMap<String,HashMap<DsKey,ShipmentRow>>>(7, .5f);
		for (int i = 0; i <= count; i++) {
			HashMap<String,HashMap<DsKey,ShipmentRow>> m =
				new HashMap<String,HashMap<DsKey,ShipmentRow>>(8, .5f);
			FillGridDB.process(all, m, si, i, dc20, hasHolidayWeeks);
			all.put(i, m);
		}
		ArrayList<File> al = new ArrayList<File>(8);
		String pfx = hasHolidayWeeks ? "TH_DC" : "TR_DC";
		//File dir = new File(appDir, "log");
		File dir = new File("c:/dev/temp");
		int i = 0;
		for (Iterator<HashMap<String,HashMap<DsKey,ShipmentRow>>> it =
			all.values().iterator(); it.hasNext();) {
			HashMap<String,HashMap<DsKey,ShipmentRow>> m = it.next();
			HashMap<String,ArrayList<ShipmentRow>> m1 =
				new HashMap<String,ArrayList<ShipmentRow>>(8, .5f);
			for (Iterator<Map.Entry<String,HashMap<DsKey,ShipmentRow>>> it1 =
				m.entrySet().iterator(); it1.hasNext();) {
				Map.Entry<String,HashMap<DsKey,ShipmentRow>> e = it1.next();
				HashMap<DsKey,ShipmentRow> v = e.getValue();
				ArrayList<ShipmentRow> al2 = new ArrayList<ShipmentRow>(v.values());
				Collections.sort(al2);
				m1.put(e.getKey(), al2);
			}
			int di = si.fromDay+(i++);
			String day = SupportTime.getDayOfWeek(di);

			File f = new File(dir, pfx+si.dc+a+"_"+day+".csv");
			FileWriter w = new FileWriter(f);
			try {
				SpreadSheet.fill(w, si, day, dc20, dc50, dc70, m1);
			}
			finally {
				w.close();
			}
			al.add(f);

	        //Sheet sh = wb.createSheet(day);
			//SpreadSheet.fill(sh, si, day, m);
		}

		dir = new File(dir, "archive");
		String fn = pfx+si.dc+a+"_"+SupportTime.MMM_dd_yy_Format.format(si.fromDate);
		zip(dir, fn, al);
		//write(dir, fn, wb);
	}
	private static void zip(File dir, String file, ArrayList<File> al) throws Exception {
		File[] fs = new File[al.size()];
		al.toArray(fs);
		File f = new File(dir, file+".zip");
		SupportFile.zip(f, fs);
		for (int i = 0; i != fs.length; i++) {
			fs[i].delete();
		}
	}
	/*private static void write(File dir, String file, Workbook wb) throws Exception {
		File f = new File(dir, file+".xlsx");
		FileOutputStream out = new FileOutputStream(f);
		try {
			wb.write(out);
		}
		finally {
			out.close();
		}
	}*/
	private static SearchInput getInput(Properties inputProps) throws ParseException {
		SearchInput si = new SearchInput();
		String v = SupportGeneral.getValue(inputProps, fromDate1);
		si.fromDate = new Date(SupportTime.dd_MM_yyyy_Format.parse(v).getTime());
		si.fromDay = SupportTime.getDayOfWeek(si.fromDate);
		v = SupportGeneral.getValue(inputProps, days1);
		int days = Integer.parseInt(v);
		si.toDate = new Date(si.fromDate.getTime()+(days-1)*SupportTime.DAY);
		si.toDay = SupportTime.getDayOfWeek(si.toDate);
		if (si.fromDay > si.toDay) {
			throw new IllegalArgumentException("Illegal date range");
		}
		si.dc = SupportGeneral.getValue(inputProps, dc1);
		si.carrier = SupportGeneral.getValue(inputProps, carrier1);
		v = SupportGeneral.getValue(inputProps, holidayWeeks1);
		si.holidayWeeks = v == null || days != 7 || si.fromDay != 0 ? 0 : Integer.parseInt(v);
		v = SupportGeneral.getValue(inputProps, test1);
		si.test = v != null;
		si.cmdty = getCmdty(inputProps);
		return si;
	}
	private static boolean[] getCmdty(Properties inputProps) {
		boolean[] arr = new boolean[cmdty1.length];
		boolean has = false;
		for (int i = 0; i != arr.length; i++) {
			String v = SupportGeneral.getValue(inputProps, cmdty1[i]);
			arr[i] = v != null;
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
		SupportGeneral.configureLog4j(appDir, "log4jTPWeb.properties");
	}

}
