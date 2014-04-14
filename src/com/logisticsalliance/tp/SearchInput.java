package com.logisticsalliance.tp;

import java.io.Serializable;
import java.sql.Date;

import com.logisticsalliance.general.CommonConstants;

/**
 * This class represents a search criteria.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
class SearchInput implements Serializable {
	private static final long serialVersionUID = 10L;

	String dc, carrier;
	int fromDay, toDay, holidayWeeks;
	Date fromDate, toDate;
	boolean test;
	boolean[] cmdty;

	private static boolean ignore(String dc, boolean dc20, String cmdty) {
		boolean dcx = cmdty.equals(CommonConstants.DCX);
		if (dc20) {
			return !dcx;
		}
		else if (dcx) {
			return true;
		}
		switch (dc) {
		case CommonConstants.DC30:
		case CommonConstants.DC50:
		case CommonConstants.DC10:
			if (cmdty.equals(CommonConstants.DCB)) {
				return true;
			}
			break;
		case CommonConstants.DC70:
		case CommonConstants.DC20:
			if (cmdty.equals(CommonConstants.DCV) || cmdty.equals(CommonConstants.RX)) {
				return true;
			}
		}
		return false;
	}
	private void addCmdty(String dc, boolean dc20, StringBuilder b, int i, String c) {
		if (ignore(dc, dc20, c)) {
			return;
		}
		if (cmdty[i]) {
			b.append('\'');
			b.append(c);
			b.append('\'');
			b.append(',');
		}
	}
	String getCmdty(String dc, boolean dc20) {
		StringBuilder b = new StringBuilder(32);
		addCmdty(dc, dc20, b, CommonConstants.DCBi, CommonConstants.DCB);
		addCmdty(dc, dc20, b, CommonConstants.DCVi, CommonConstants.DCV);
		addCmdty(dc, dc20, b, CommonConstants.DCXi, CommonConstants.DCX);
		addCmdty(dc, dc20, b, CommonConstants.DCFi, CommonConstants.DCF);
		addCmdty(dc, dc20, b, CommonConstants.EVTi, CommonConstants.EVT);
		addCmdty(dc, dc20, b, CommonConstants.EVT2i, CommonConstants.EVT2);
		addCmdty(dc, dc20, b, CommonConstants.RXi, CommonConstants.RX);
		b.deleteCharAt(b.length()-1);
		return b.toString();
	}
	static String toDc(String dc) {
		if (dc.equals(CommonConstants.DC20)) {
			return CommonConstants.DC30;
		}
		if (dc.equals(CommonConstants.DC70)) {
			return CommonConstants.DC50;
		}
		return dc;
	}
	static String toDc(String dc, String dc1) {
		if (dc.equals(CommonConstants.DC20) && dc1.equals(CommonConstants.DC30)) {
			return CommonConstants.DC20;
		}
		if (dc.equals(CommonConstants.DC70) && dc1.equals(CommonConstants.DC50)) {
			return CommonConstants.DC70;
		}
		return dc1;
	}
}
