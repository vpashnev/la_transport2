package com.logisticsalliance.tt.web;

import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.glossium.ui.html.HCell;
import com.glossium.ui.html.HFrame;
import com.glossium.ui.html.HNode;
import com.glossium.ui.html.HRow;
import com.glossium.web.HServlet;
import com.logisticsalliance.util.SupportTime;

public class ScheduleServlet extends HServlet {
	private static final long serialVersionUID = 10L;

	static final String reg1 = "reg", hld1 = "hld", table1 = "table";

	private HNode reg, hld, table;

	public void init(ServletConfig config) throws ServletException {
		HFrame html = getHtml(getClass().getResourceAsStream("/html/tt/schedule.html"));
		setControls(html);
		setHtml(html);
		super.init(config);
	}
	private void setControls(HFrame html) {
		HNode[] ns = html.getNodes(reg1, hld1, table1);
		reg = ns[0];
		hld = ns[1];
		table = ns[2];
	}

	@Override
	protected void fillHtml(Object data) {
		TtSession.ScheduleData d = (TtSession.ScheduleData)data;
		table.clearChildren();
		if (d.type == 2) {
			hld.setChecked(true);
			reg.setChecked(false);
			for (Iterator<DelSchedule> it = d.list.iterator(); it.hasNext();) {
				DelSchedule e = it.next();
				HRow r = new HRow(table);
				new HCell(r, e.delDate == null ? null : SupportTime.MMM_dd_yy_Format.format(e.delDate));
				new HCell(r, SupportTime.HH_mm_Format.format(e.delTimeFrom));
				new HCell(r, SupportTime.HH_mm_Format.format(e.delTimeTo));
				new HCell(r, SupportTime.MMM_dd_yy_Format.format(e.shipDate));
				new HCell(r, e.week);
				new HCell(r, e.cmdty);
				new HCell(r, e.descr);
			}
		}
		else if (d.type == 1) {
			reg.setChecked(true);
			hld.setChecked(false);
			for (Iterator<DelSchedule> it = d.list.iterator(); it.hasNext();) {
				DelSchedule e = it.next();
				HRow r = new HRow(table);
				new HCell(r, e.delDay == -1 ? null : SupportTime.getDayOfWeek(e.delDay));
				new HCell(r, SupportTime.HH_mm_Format.format(e.delTimeFrom));
				new HCell(r, SupportTime.HH_mm_Format.format(e.delTimeTo));
				new HCell(r, SupportTime.getDayOfWeek(e.shipDay));
				new HCell(r, e.week);
				new HCell(r, e.cmdty);
			}
		}
	}

	@Override
	protected Object processForHtml(HttpServletRequest req, HttpServletResponse res,
		PrintWriter w, boolean post) throws Exception {
		TtSession a = _Support.getSession(req, res);
		if (a == null) {
			return null;
		}
		TtSession.ScheduleData d = a.scheduleData;
		if (!post) {
			if (d.type == 0) {
				reg.setChecked(false);
				hld.setChecked(false);
			}
			return d;
		}
		String v = req.getParameter("schedule");
		d.type = hld1.equals(v) ? 2 : (reg1.equals(v) ? 1 : 0);
		d.list = ScheduleDB.select(a.store, d.type);
		return d;
	}

}
