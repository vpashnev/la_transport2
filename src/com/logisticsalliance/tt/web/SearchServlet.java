package com.logisticsalliance.tt.web;

import java.io.PrintWriter;
import java.sql.Date;
import java.util.Calendar;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import com.logisticsalliance.sa.TrackingNote;
import com.logisticsalliance.util.SupportTime;

public class SearchServlet extends HServlet {
	private static final long serialVersionUID = 10L;

	static final String month1 = "month", day1 = "day", year1 = "year",
		cmdty1 = "cmdty", table1 = "table";

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
		for (Iterator<TrackingNote> it1 = d.list.iterator(); it1.hasNext();) {
			TrackingNote e = it1.next();
			HRow r = new HRow(table);
			r.setStyle("display:block;");
			r.setOnClick("selRow(this)");
			HCell c0 = new HCell(r, null);
			new HAnchor(c0, "-");
			new HCell(r, e.delDate == null ? null : SupportTime.MMM_dd_yy_Format.format(e.delDate));
			new HCell(r, e.arrivalTime);
			HCell c = new HCell(r, e.pallets);
			c.setClass("num");
			c = new HCell(r, e.cmdtyList);
			c.setStyle("text-wrap:none;");
			addAlert(r, c0, e);
		}
	}
	private void addAlert(HRow r, HCell c0, TrackingNote tn) {
		boolean ext = tn.items.size() > 1, first = true;
		for (Iterator<AlertItem> it2 = tn.items.iterator(); it2.hasNext();) {
			AlertItem ai = it2.next();
			new HCell(r, ai.status);
			new HCell(r, ai.comment);
			HCell c = new HCell(r, null);
			c.setStyle("display:none;");
			new HParam(c, ai.reason);
			if (first) {
				addOthers(c, tn);
			}
			if (ext && it2.hasNext()) {
				if (first) {
					c0.setStyle("padding:0px;vertical-align:middle;");
					HDiv n = new HDiv(c0);
					n.setClass("circle");
					n.setTextValue("+");
					first = false;
					n.setOnClick("unfold(this);");
				}
				r = new HRow(table);
				new HAnchor(new HCell(r, null), "-");
				new HCell(r, null); new HCell(r, null);
				new HCell(r, null); new HCell(r, null);
				r.setStyle("display:none;");
				r.setOnClick("selRow(this)");
			}
		}
	}
	private static void addOthers(HCell c, TrackingNote tn) {
		new HParam(c, tn.serviceTime);
		new HParam(c, tn.carrier);
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
			d.date = new Date(System.currentTimeMillis());
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
