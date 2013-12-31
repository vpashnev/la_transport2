package com.logisticsalliance.tt.web;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

class _Support {

	static TtSession getSession(HttpServletRequest req, HttpServletResponse res) throws Exception {
		HttpSession s = req.getSession();
		TtSession a = (TtSession)s.getAttribute(LoginServlet.session);
		if (a == null || a.store <= 0) {
			a = new TtSession();
			a.store = -1;
			s.setAttribute(LoginServlet.session, a);
			RequestDispatcher rd = req.getRequestDispatcher("login");
			rd.forward(req, res);
			return null;
		}
		return a;
	}
}
