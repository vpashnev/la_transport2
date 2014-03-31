package com.logisticsalliance.tp;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.glossium.ui.html.HFrame;
import com.glossium.ui.html.HNode;
import com.glossium.web.HServlet;
import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.SupportGeneral;

public class PlanServlet extends HServlet {
	private static final long serialVersionUID = 10L;

	static final String month1 = "month", day1 = "day", year1 = "year",
		days1 = "store", dc1 = "dc", holidays1 = "holidays",
		session = "tpSesion";
	static final String[] cmdty1 = {CommonConstants.DCB, CommonConstants.DCV,
		CommonConstants.DCX, CommonConstants.DCF, CommonConstants.EVT,
		CommonConstants.EVT2, CommonConstants.RX};

	private HNode month, day, year, days, dc, holidays;
	private HNode[] cmdty;

	static void configureLog4j(File appDir) {
		SupportGeneral.configureLog4j(appDir, "log4jTPWeb.properties");
	}

	public void init(ServletConfig config) throws ServletException {
		String appDir1 = config.getInitParameter("appDir"),
				pwd1 = config.getInitParameter("pwd1");
		File appDir = new File(appDir1);
		new File(appDir, "logweb").mkdir();
		configureLog4j(appDir);
		Properties appProps = new Properties();
		try {
			appProps.load(new FileReader(new File(appDir, "app.properties")));
		}
		catch (IOException e) {
			throw new ServletException(e);
		}
		SupportGeneral.makeDataSource1I5(appProps, pwd1, null);
		HFrame html = getHtml(getClass().getResourceAsStream("/html/tp/plan.html"));
		setControls(html);
		setHtml(html);
		super.init(config);
	}
	private void setControls(HFrame html) {
		HNode[] ns = html.getNodes(month1, day1, year1, days1, dc1, holidays1);
		cmdty = html.getNodes(cmdty1);
		month = ns[0];
		day = ns[1];
		year = ns[2];
		days = ns[3];
		dc = ns[4];
		holidays = ns[5];
		SupportGeneral.setMonth(month);
	}

	@Override
	protected void fillHtml(Object data) {
	}

	@Override
	protected Object processForHtml(HttpServletRequest req, HttpServletResponse res,
		PrintWriter w, boolean post) throws Exception {
		HttpSession s = req.getSession();
		Data d = (Data)s.getAttribute(session);
		if (d == null) {
			d = new Data();
			s.setAttribute(session, d);
		}
		return d;
	}

	private class Data {
	}
}
