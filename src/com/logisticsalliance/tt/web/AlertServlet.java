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

	static final String[]
		comm1 =
		{"comm1", "comm2", "comm31", "comm32", "comm33", "comm34",
		"comm41", "comm42", "comm43", "comm44"},
		note1 =
		{"note11", "note12", "note21", "note22", "note31", "note32", "note41", "note42"};
	static final String[][] cmdty1 = {
		{"DCB1", "DCV1", "DCX1", "DCF1", "EVT1", "EVT21"},
		{"DCB2", "DCV2", "DCX2", "DCF2", "EVT2", "EVT22"},
		{"DCB3", "DCV3", "DCX3", "DCF3", "EVT3", "EVT23"},
		{"DCB4", "DCV4", "DCX4", "DCF4", "EVT4", "EVT24"},
	};
	static final String msg1 = "msg",
		check = "alert('Please check your email and phone for updates confirmation')";

	private HNode[] comm, note;
	private HNode[][] cmdty = new HNode[4][0];
	private HNode msg;

	public void init(ServletConfig config) throws ServletException {
		HFrame html = getHtml(getClass().getResourceAsStream("/html/alert.html"));
		setControls(html);
		setHtml(html);
		super.init(config);
	}
	private void setControls(HFrame html) {
		comm = html.getNodes(comm1);
		note = html.getNodes(note1);
		cmdty[0] = html.getNodes(cmdty1[0]);
		cmdty[1] = html.getNodes(cmdty1[1]);
		cmdty[2] = html.getNodes(cmdty1[2]);
		cmdty[3] = html.getNodes(cmdty1[3]);
		msg = html.getNodes(msg1)[0];
	}

	@Override
	protected void fillHtml(Object data) {
		Alert[] d = (Alert[])data;
		for (int i = 0; i != d.length; i++) {
			Alert a = d[i];
			if (a.commId == null || a.commId.trim().isEmpty()) {
				if (i < 2) {
					comm[i].setValue(null);
				}
				else if (i == 2) {
					comm[i].setValue(null); comm[i+1].setValue(null);
					comm[i+2].setValue(null); comm[i+3].setValue(null);
				}
				else {
					comm[i+3].setValue(null); comm[i+4].setValue(null);
					comm[i+5].setValue(null); comm[i+6].setValue(null);
				}
			}
			else {
				if (i < 2) {
					comm[i].setValue(a.commId);
				}
				else if (i == 2) {
					comm[i].setValue(a.commId.substring(0, 3));
					comm[i+1].setValue(a.commId.substring(3, 6));
					comm[i+2].setValue(a.commId.substring(6, 10));
					comm[i+3].setValue(a.commId.substring(11));
				}
				else {
					comm[i+3].setValue(a.commId.substring(0, 3));
					comm[i+4].setValue(a.commId.substring(3, 6));
					comm[i+5].setValue(a.commId.substring(6, 10));
					comm[i+6].setValue(a.commId.substring(11));
				}
			}
			int ii = i<<1;
			switch (a.noteType) {
			case (0):
				note[ii].setChecked(false);
				note[ii+1].setChecked(false);
				break;
			case (1):
				note[ii].setChecked(true);
				note[ii+1].setChecked(false);
				break;
			case (2):
				note[ii].setChecked(false);
				note[ii+1].setChecked(true);
			case (3):
				note[ii].setChecked(true);
				note[ii+1].setChecked(true);
			}
			for (int j = 0; j != a.cmdty.length; j++) {
				cmdty[i][j].setChecked(a.cmdty[j]);
			}
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
			d = new Alert[]{ new Alert(), new Alert(), new Alert(), new Alert()};
			a.alerts = d;
			AlertDB.select(a.store, d);
			d[0].checkMsg = null;
		}
		else if (post) {
			setData(d, req);
			AlertDB.update(a.store, d);
			AlertDB.select(a.store, d);
			TestComm.send(LoginServlet.emailSent, d[0].commId,
				d[1].commId, d[2].commId, d[3].commId, a.store);
			d[0].checkMsg = check;
		}
		else { d[0].checkMsg = null;}
		return d;
	}
	private static void setData(Alert[] d, HttpServletRequest req) {
		for (int i = 0; i != d.length; i++) {
			Alert a = d[i];
			if (i < 2) {
				a.commId = req.getParameter(comm1[i]);
			}
			else if (i == 2) {
				a.commId = getPhone(req, i, i+1, i+2, i+3);
			}
			else {
				a.commId = getPhone(req, i+3, i+4, i+5, i+6);
			}
			if (a.commId == null) {
				a.commId = "";
			}
			else {
				a.commId = a.commId.trim();
			}
			AlertDB.reset(a);
			if (a.commId.isEmpty()) {
				continue;
			}
			int ii = i<<1;
			boolean t1 = req.getParameter(note1[ii]) != null,
				t2 = req.getParameter(note1[ii+1]) != null;
			if (t1 && t2) {
				a.noteType = 3;
			}
			else if (t1) {
				a.noteType = 1;
			}
			else if (t2) {
				a.noteType = 2;
			}
			else {
				a.noteType = 0;
			}
			if (a.noteType != 0) {
				for (int j = 0; j != a.cmdty.length; j++) {
					a.cmdty[j] = req.getParameter(cmdty1[i][j]) != null;
				}
			}
		}
	}
	private static String getPhone(HttpServletRequest req, int i1, int i2, int i3, int i4) {
		StringBuilder b = new StringBuilder(40);
		b.append(req.getParameter(comm1[i1]));
		b.append(req.getParameter(comm1[i2]));
		b.append(req.getParameter(comm1[i3]));
		b.append('@');
		b.append(req.getParameter(comm1[i4]));
		return b.length() < 12 ? null : b.toString();
	}

}
