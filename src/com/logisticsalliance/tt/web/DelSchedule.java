package com.logisticsalliance.tt.web;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;

class DelSchedule implements Serializable {
	private static final long serialVersionUID = 10L;

	int delDay, shipDay, week;
	Date delDate, shipDate;
	Time delTimeFrom, delTimeTo;
	String cmdty, descr;
}
