package com.logisticsalliance.tt.web;

import java.io.Serializable;
import java.sql.Date;

class Delivery implements Serializable {
	private static final long serialVersionUID = 10L;

	String arrivalTime, cmdty, carrier, status, exp;
	Date delDate;
}
