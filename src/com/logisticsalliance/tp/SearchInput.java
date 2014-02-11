package com.logisticsalliance.tp;

import java.io.Serializable;
import java.sql.Date;

/**
 * This class represents a search criteria.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class SearchInput implements Serializable {
	private static final long serialVersionUID = 10L;

	String dc, carrier;
	Date fromDate, toDate;
	boolean delDates;
	boolean[] cmdty;
}
