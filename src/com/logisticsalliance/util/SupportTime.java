package com.logisticsalliance.util;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * This class provides several helpful properties and methods to calculate and format date.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class SupportTime {

	public static final SimpleDateFormat yyyy_MM_dd_Format = new SimpleDateFormat("yyyy-MM-dd"),
		yyyyMMdd_HHmmss__Format = new SimpleDateFormat("yyyyMMdd_HHmmss_"),
		yyyyMMdd_Format = new SimpleDateFormat("yyyyMMdd"),
		yyMMdd_Format = new SimpleDateFormat("yyMMdd"),
		MM_dd_Format = new SimpleDateFormat("MMM.dd"),
		dd_MM_yyyy_Format = new SimpleDateFormat("dd/MM/yyyy"),
		dd_MM_yyyy_HH_mm_Format = new SimpleDateFormat("dd-MM-yyyy HH:mm"),
		MM_dd_yyyy_HH_mm_Format = new SimpleDateFormat("MM/dd/yyyy HH:mm"),
		Hmm_Format = new SimpleDateFormat("Hmm"),
		HHmm_Format = new SimpleDateFormat("HHmm"),
		H_mm_Format = new SimpleDateFormat("H:mm"),
		HH_mm_Format = new SimpleDateFormat("HH:mm");
	public static final long HOUR = 60L*60000, DAY = HOUR*24;

	public static Time parseTimeHHmm(String v) throws ParseException {
		Date d = v.length() == 3 ? Hmm_Format.parse(v) : HHmm_Format.parse(v);
		return new Time(d.getTime());
	}
	public static Time parseTimeHH_mm(String v) throws ParseException {
		Date d = v.length() == 4 ? H_mm_Format.parse(v) : HH_mm_Format.parse(v);
		return new Time(d.getTime());
	}
	public static int getDayOfWeek(Date d) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		return cal.get(Calendar.DAY_OF_WEEK)-1;
	}
	public static int getWeeks(String weekN) {
		weekN = weekN.trim();
		String n = weekN.substring(weekN.length()-1);
		return Integer.parseInt(n)-1;
	}
	/**
	 * Converts the short name of week day to the number to return.
	 * @param dayOfWeek the day of week is the string that belongs to
	 * {SUN =0, MON =1, TUE =2, WED =3, THU =4, FRI =5, SAT =6}
	 * @return Number of day between 0 and 6;
	 */
	public static int getDayNumber(String dayOfWeek) {
		switch (dayOfWeek.trim().toUpperCase()) {
		case "SUN": return 0;
		case "MON": return 1;
		case "TUE": return 2;
		case "WED": return 3;
		case "THU": return 4;
		case "FRI": return 5;
		case "SAT": return 6;
		}
		throw new IllegalArgumentException("Incorrect day of week: "+dayOfWeek);
	}
	public static String getDayOfWeek(int d) {
		switch (d) {
		case 0: return "SUN";
		case 1: return "MON";
		case 2: return "TUE";
		case 3: return "WED";
		case 4: return "THU";
		case 5: return "FRI";
		case 6: return "SAT";
		default: return "N/A";
		}
	}
}
