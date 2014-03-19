package com.logisticsalliance.tt.web;

import java.io.PrintWriter;
import java.sql.Date;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import com.glossium.ui.html.HAnchor;
import com.glossium.ui.html.HCell;
import com.glossium.ui.html.HDiv;
import com.glossium.ui.html.HFrame;
import com.glossium.ui.html.HNode;
import com.glossium.ui.html.HOption;
import com.glossium.ui.html.HParam;
import com.glossium.ui.html.HRow;
import com.glossium.web.HServlet;
import com.logisticsalliance.sa.AlertItem;
import com.logisticsalliance.sa.Alerts;
import com.logisticsalliance.sa.TrackingNote;
import com.logisticsalliance.util.SupportTime;

public class SearchServlet extends HServlet {
	private static final long serialVersionUID = 10L;

	static final String month1 = "month", day1 = "day", year1 = "year",
		cmdty1 = "cmdty", table1 = "table", nbsp = DatatypeConverter.parseString("\u00A0");

	private HNode month, day, year, cmdty, table;

	public void init(ServletConfig config) throws ServletException {
		HFrame html = getHtml(getClass().getResourceAsStream("/html/tt/search.html"));
		setControls(html);
		setHtml(html);
		super.init(config);
	}
	private void setControls(HFrame html) {
		HNode[] ns = html.getNodes(month1, day1, year1, cmdty1, table1);
		month = ns[0];
		day = ns[1];
		year = ns[2];
		cmdty = ns[3];
		table = ns[4];
		setMonth();
		setCmdty();
	}
	private void setMonth() {
		new HOption(month, "01", "Jan");
		new HOption(month, "02", "Feb");
		new HOption(month, "03", "Mar");
		new HOption(month, "04", "Apr");
		new HOption(month, "05", "May");
		new HOption(month, "06", "Jun");
		new HOption(month, "07", "Jul");
		new HOption(month, "08", "Aug");
		new HOption(month, "09", "Sep");
		new HOption(month, "10", "Oct");
		new HOption(month, "11", "Nov");
		new HOption(month, "12", "Dec");
	}
	private void setCmdty() {
		new HOption(cmdty, "0", "");
		new HOption(cmdty, "DCB", "DCB");
		new HOption(cmdty, "DCV", "DCV");
		new HOption(cmdty, "DCX", "DCX");
		new HOption(cmdty, "DCF", "DCF");
		new HOption(cmdty, "EVT", "EVT");
		new HOption(cmdty, "EVT2", "EVT2");
	}

	@Override
	protected void fillHtml(Object data) {
		TtSession.SearchData d = (TtSession.SearchData)data;
		boolean date0 = d.date == null;
		if (date0) { d.date = new Date(System.currentTimeMillis());}
		String v = SupportTime.yyyyMMdd_Format.format(d.date);
		month.setSelectedOption(v.substring(4, 6));
		day.setValue(date0 ? null : v.substring(6));
		year.setValue(v.substring(0, 4));
		cmdty.setSelectedOption(d.cmdty);
		table.clearChildren();
		for (Iterator<TrackingNote> it = d.list.iterator(); it.hasNext();) {
			TrackingNote tn = it.next();
			HRow r = newRow();
			HCell c0 = new HCell(r, null);
			setRowFocus(c0);
			new HCell(r, tn.newDelDate == null ? tn.delDate1 : tn.newDelDate1);
			new HCell(r, tn.newArrivalTime.length() == 0 ? tn.arrivalTime : tn.newArrivalTime);
			boolean[] first = {true};
			for (Iterator<Map.Entry<String,Alerts>> it1 = tn.cmdtyAlerts.entrySet().iterator();
				it1.hasNext();) {
				Map.Entry<String,Alerts> e = it1.next();
				String cmdty = e.getKey();
				Alerts as = e.getValue();
				HCell c = new HCell(r, as.pallets);
				c.setClass("num");
				new HCell(r, cmdty);
				addAlert(r, c0, tn, as, first);
				if (it1.hasNext()) {
					r = newRow();
					c0 = new HCell(r, null);
					setRowFocus(c0);
					new HCell(r, null); new HCell(r, null);
				}
			}
		}
	}
	private void addAlert(HRow r, HCell c0, TrackingNote tn, Alerts as, boolean[] first) {
		boolean ext = as.items.size() > 1, first1 = true;
		for (Iterator<AlertItem> it2 = as.items.iterator(); it2.hasNext();) {
			AlertItem ai = it2.next();
			new HCell(r, ai.status);
			new HCell(r, ai.comment);
			HCell c = new HCell(r, null);
			c.setStyle("display:none;");
			new HParam(c, ai.reasonEn);
			if (first[0]) {
				addOthers(c, tn);
				first[0] = false;
			}
			if (ext && it2.hasNext()) {
				if (first1) {
					c0.setStyle("padding:0px;vertical-align:middle;");
					HDiv n = new HDiv(c0);
					n.setClass("circle");
					n.setTextValue("+");
					first1 = false;
					n.setOnClick("unfold(this);");
				}
				r = newRow();
				r.setStyle("display:none;");
				setRowFocus(new HCell(r, null));
				new HCell(r, null); new HCell(r, null);
				new HCell(r, null); new HCell(r, null);
			}
		}
	}
	private HRow newRow() {
		HRow r = new HRow(table);
		r.setOnClick("selRow(this);");
		return r;
	}
	private static void setRowFocus(HCell c) {
		HAnchor a = new HAnchor(c, "-");
		a.setClass("rf");
		a.setTextValue(nbsp);
	}
	private static void addOthers(HCell c, TrackingNote tn) {
		new HParam(c, tn.serviceTime);
		new HParam(c, tn.carrier);
		new HParam(c, tn.stopN).setClass("num");
		new HParam(c, tn.delDate1);
		new HParam(c, tn.arrivalTime);
	}

	@Override
	protected Object processForHtml(HttpServletRequest req, HttpServletResponse res,
		PrintWriter w, boolean post) throws Exception {
		TtSession a = _Support.getSession(req, res);
		if (a == null) {
			return null;
		}
		TtSession.SearchData d = a.searchData;
		if (!post) {
			return d;
		}
		d.store = a.store;
		if (d.firstTime) {
			d.date = new Date(System.currentTimeMillis()+2*SupportTime.DAY);
			d.firstTime = false;
		}
		else {
			String m = req.getParameter("month"),
				d1 = req.getParameter("day"),
				y = req.getParameter("year");
			d.date = getDate(d, y, m, d1);
			d.cmdty = req.getParameter("cmdty");
			if ("0".equals(d.cmdty)) { d.cmdty = null;}
		}
		d.list = SearchDB.select(d.store, d.date, d.cmdty);
		return d;
	}
	private static Date getDate(TtSession.SearchData d, String y, String m, String d1) {
		try {
			int y1 = Integer.parseInt(y);
			int m1 = Integer.parseInt(m);
			int d11 = Integer.parseInt(d1);
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(y1, m1-1, d11);
			return new Date(c.getTimeInMillis());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

}
