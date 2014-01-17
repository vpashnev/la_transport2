package com.logisticsalliance.tt.web;

import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.glossium.ui.html.HFrame;
import com.glossium.web.HServlet;

public class FeedbackServlet extends HServlet {
	private static final long serialVersionUID = 10L;

	static final String store1 = "store", table1 = "table";

	public void init(ServletConfig config) throws ServletException {
		HFrame html = getHtml(getClass().getResourceAsStream("/html/tt/feedback.html"));
		setControls(html);
		setHtml(html);
		super.init(config);
	}
	private void setControls(HFrame html) {
		//HNode[] ns = html.getNodes(month1, day1, year1, cmdty1, store1, table1);
	}

	@Override
	protected void fillHtml(Object data) {
	}

	@Override
	protected Object processForHtml(HttpServletRequest req, HttpServletResponse res,
		PrintWriter w, boolean post) throws Exception {
		TtSession a = _Support.getSession(req, res);
		if (a == null) {
			return null;
		}
		Data d = new Data();
		return d;
	}

	private class Data {
	}
}
