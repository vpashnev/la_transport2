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
	int fromDay, toDay;
	Date fromDate, toDate;
	boolean delDates, holidays;
	boolean[] cmdty;

	private void addCmdty(StringBuilder b, int i, String c) {
		if (cmdty[i]) {
			b.append('\'');
			b.append(c);
			b.append('\'');
			b.append(',');
		}
	}
	String getCmdty() {
		StringBuilder b = new StringBuilder(32);
		addCmdty(b, CommonConstants.DCBi, CommonConstants.DCB);
		addCmdty(b, CommonConstants.DCVi, CommonConstants.DCV);
		addCmdty(b, CommonConstants.DCXi, CommonConstants.DCX);
		addCmdty(b, CommonConstants.DCFi, CommonConstants.DCF);
		addCmdty(b, CommonConstants.EVTi, CommonConstants.EVT);
		addCmdty(b, CommonConstants.EVT2i, CommonConstants.EVT2);
		addCmdty(b, CommonConstants.RXi, CommonConstants.RX);
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
}
