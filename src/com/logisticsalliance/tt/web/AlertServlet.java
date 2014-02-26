package com.logisticsalliance.tt.web;

import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.glossium.ui.html.HFrame;
import com.glossium.ui.html.HNode;
import com.glossium.web.HServlet;

public class AlertServlet extends HServlet {
	private static final long serialVersionUID = 10L;

	static final String[][]
		comm1 = {
		{"email1", "email21", "phone11", "phone12", "phone13", "phone14",
		"phone211", "phone212", "phone213", "phone214"},
		{"email2", "email22", "phone21", "phone22", "phone23", "phone24",
		"phone221", "phone222", "phone223", "phone224"},
		{"email3", "email23", "phone31", "phone32", "phone33", "phone34",
		"phone231", "phone232", "phone233", "phone234"},
		},
		cmdty1 = {
		{"DCB1", "DCV1", "DCX1", "DCF1", "EVT1", "EVT21"},
		{"DCB2", "DCV2", "DCX2", "DCF2", "EVT2", "EVT22"},
		{"DCB3", "DCV3", "DCX3", "DCF3", "EVT3", "EVT23"},
		};
	static final String[] select1 = {"select1", "select2", "select3"};
	private static final String msg1 = "msg",
		check = "alert('Please check your email and phone. " +
			"A confirmation should be received within a few minutes.')";

	private HNode[][] comm = new HNode[4][0];
	private HNode[][] cmdty = new HNode[4][0];
	private HNode[] sel;
	private HNode msg;

	public void init(ServletConfig config) throws ServletException {
		HFrame html = getHtml(getClass().getResourceAsStream("/html/tt/alert.html"));
		setControls(html);
		setHtml(html);
		super.init(config);
	}
	private void setControls(HFrame html) {
		comm[0] = html.getNodes(comm1[0]);
		comm[1] = html.getNodes(comm1[1]);
		comm[2] = html.getNodes(comm1[2]);
		cmdty[0] = html.getNodes(cmdty1[0]);
		cmdty[1] = html.getNodes(cmdty1[1]);
		cmdty[2] = html.getNodes(cmdty1[2]);
		sel = html.getNodes(select1);
		msg = html.getNodes(msg1)[0];
	}

	@Override
	protected void fillHtml(Object data) {
		Alert[] d = (Alert[])data;
		for (int n = 0; n != d.length; n++) {
			Alert a = d[n];
			for (int i = 0; i != a.comm.length; i++) {
				if (a.comm[i] == null || a.comm[i].trim().isEmpty()) {
					if (i < 2) {
						comm[n][i].setValue(null);
					}
					else if (i == 2) {
						comm[n][i].setValue(null); comm[n][i+1].setValue(null);
						comm[n][i+2].setValue(null); comm[n][i+3].setValue(null);
					}
					else {
						comm[n][i+3].setValue(null); comm[n][i+4].setValue(null);
						comm[n][i+5].setValue(null); comm[n][i+6].setValue(null);
					}
				}
				else {
					if (i < 2) {
						comm[n][i].setValue(a.comm[i]);
					}
					else if (i == 2) {
						comm[n][i].setValue(a.comm[i].substring(0, 3));
						comm[n][i+1].setValue(a.comm[i].substring(3, 6));
						comm[n][i+2].setValue(a.comm[i].substring(6, 10));
						comm[n][i+3].setValue(a.comm[i].substring(11));
					}
					else {
						comm[n][i+3].setValue(a.comm[i].substring(0, 3));
						comm[n][i+4].setValue(a.comm[i].substring(3, 6));
						comm[n][i+5].setValue(a.comm[i].substring(6, 10));
						comm[n][i+6].setValue(a.comm[i].substring(11));
					}
				}
			}
			boolean has = false;
			for (int i = 0; i != a.cmdty.length; i++) {
				cmdty[n][i].setChecked(a.cmdty[i]);
				if (!has && a.cmdty[i]) {
					has = true;
				}
			}
			sel[n].setChecked(has);
		}
		msg.setTextValue(d[0].checkMsg);
	}

	@Override
	protected Object processForHtml(HttpServletRequest req, HttpServletResponse res,
		PrintWriter w, boolean post) throws Exception {
		TtSession a = _Support.getSession(req, res);
		if (a == null) {
			return null;
		}
		Alert[] d = a.alerts;
		if (d == null) {
			d = new Alert[]{ new Alert(), new Alert(), new Alert()};
			a.alerts = d;
			AlertDB.select(a.store, d, true);
			d[0].checkMsg = null;
		}
		else if (post) {
			Alert[] d0 = new Alert[d.length];
			for (int i = 0; i != d.length; i++) {
				d0[i] = (Alert)d[i].clone();
			}
			setData(d, req);
			AlertDB.update(a.store, d);
			TestComm.send(LoginServlet.emailSent, d0, d, a.store);
			d[0].checkMsg = check;
		}
		else { d[0].checkMsg = null;}
		return d;
	}
	private static void setData(Alert[] d, HttpServletRequest req) {
		for (int n = 0; n != d.length; n++) {
			Alert a = d[n];
			boolean has = false;
			for (int i = 0; i != a.comm.length; i++) {
				if (i < 2) {
					a.comm[i] = req.getParameter(comm1[n][i]);
				}
				else if (i == 2) {
					a.comm[i] = getPhone(req, n, i, i+1, i+2, i+3);
				}
				else {
					a.comm[i] = getPhone(req, n, i+3, i+4, i+5, i+6);
				}
				a.comm[i] = a.comm[i].trim();
				if (!has && !a.comm[i].isEmpty()) {
					has = true;
				}
			}
			AlertDB.reset(a);
			if (!has) {
				continue;
			}
			for (int i = 0; i != a.cmdty.length; i++) {
				a.cmdty[i] = req.getParameter(cmdty1[n][i]) != null;
			}
		}
	}
	private static String getPhone(HttpServletRequest req, int n, int i1, int i2, int i3, int i4) {
		StringBuilder b = new StringBuilder(40);
		b.append(req.getParameter(comm1[n][i1]));
		b.append(req.getParameter(comm1[n][i2]));
		b.append(req.getParameter(comm1[n][i3]));
		if (b.length() < 10) { return "";}
		b.append('@');
		b.append(req.getParameter(comm1[n][i4]));
		return b.length() < 12 ? "" : b.toString();
	}

}
