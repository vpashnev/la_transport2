package com.logisticsalliance.tt.web;

import java.sql.Date;
import java.util.ArrayList;

public class TtSession {

	int store;
	SearchData searchData = new SearchData();
	ScheduleData scheduleData = new ScheduleData();
	Alert[] alerts;

	static class SearchData {
		Date date;
		String cmdty;
		int store;
		ArrayList<Delivery> list;
		boolean firstTime = true;
	}
	static class ScheduleData {
		int type;
		ArrayList<DelSchedule> list;
	}
}
