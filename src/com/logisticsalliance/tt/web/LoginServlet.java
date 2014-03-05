package com.logisticsalliance.tt.web;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.glossium.sqla.ConnectFactory;
import com.glossium.ui.html.HFrame;
import com.glossium.ui.html.HNode;
import com.glossium.web.HServlet;
import com.logisticsalliance.general.EmailSent1;
import com.logisticsalliance.general.SupportGeneral;

public class LoginServlet extends HServlet {
	private static final long serialVersionUID = 10L;

	static final String user1 = "user", pwd = "pwd", error1 = "error", error21 = "error2",
		session = "ttSesion", invalidLogin = "Invalid user's name or password",
		signIn = "Before making request, please",
		signIn2 = "sign in with your store account";

	private static Logger log = Logger.getLogger(LoginServlet.class);
	
	private HNode user, error, error2;

	private static File appDir;
	static ConnectFactory connectFactoryI5;
	static EmailSent1 emailSent1;

	static void configureLog4j(File appDir) {
		SupportGeneral.configureLog4j(appDir, "log4jTTWeb.properties");
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		String appDir1 = config.getInitParameter("appDir"),
			pwd1 = config.getInitParameter("pwd1"),
			pwdI5 = config.getInitParameter("pwdI5"),
			emailPwd = config.getInitParameter("emailPwd");
		appDir = new File(appDir1);
		new File(appDir, "logweb").mkdir();
		configureLog4j(appDir);
		Properties appProps = new Properties();
		try {
			appProps.load(new FileReader(new File(appDir, "app.properties")));
		}
		catch (IOException e) {
			throw new ServletException(e);
		}
		ConnectFactory cf = SupportGeneral.makeDataSource1I5(appProps, pwd1, pwdI5);
		//cf = new ConnectFactory(cf.getDriver(),
		//	"jdbc:as400:tmsodev.nulogx.com;prompt=false", cf.getUser(), cf.getPassword());
		connectFactoryI5 = cf;
		try {
			UserDB.fill();
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
		emailSent1 = new EmailSent1(appProps, emailPwd);
		HFrame html = getHtml(getClass().getResourceAsStream("/html/tt/login.html"));
		setControls(html);
		setHtml(html);
		super.init(config);
	}
	private void setControls(HFrame html) {
		HNode[] ns = html.getNodes(user1, error1, error21);
		user = ns[0];
		error = ns[1];
		error2 = ns[2];
	}

	protected void fillHtml(Object data) {
		Data d = (Data)data;
		user.setValue(d.user);
		error.setTextValue(d.errMsg);
		error2.setTextValue(d.errMsg2);
	}

	@Override
	protected Object processForHtml(HttpServletRequest req, HttpServletResponse res,
		PrintWriter w, boolean post) throws Exception {
		HttpSession s = req.getSession();
		TtSession a = (TtSession)s.getAttribute(session);
		if (a == null || a.store > 0) {
			a = new TtSession();
			s.setAttribute(session, a);
		}
		Data d = new Data();
		String u = req.getParameter(user1), p = req.getParameter(pwd);
		if (u == null || u.trim().isEmpty()) {
			d.user = null;
			String err = null;
			if (a.store < 0) {
				err = signIn;
				d.errMsg2 = signIn2;
				a.store = 0;
			}
			d.errMsg = err;
			return d;
		}
		u = u.trim();
		log.debug("start");
		int store = UserDB.login(u, p);
		log.debug("end");
		if (store == 0) {
			d.user = u;
			d.errMsg = invalidLogin;
			return d;
		}
		a.store = store;
		RequestDispatcher rd = req.getRequestDispatcher("search");
		rd.forward(req, res);
		return null;
	}

	private class Data {
		private String user, errMsg, errMsg2;
	}

}
