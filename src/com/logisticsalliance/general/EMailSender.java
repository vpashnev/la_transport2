package com.logisticsalliance.general;

import java.io.File;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;


/**
 * This class sends email messages.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class EmailSender {

	private static Logger log = Logger.getLogger(EmailSender.class);

	public static Session send(Session s, EmailSent es, String recipients,
		String subject, String content, File attachment, int[] trials) {
		if (s == null) {
			s = getSession(es);
		}
		try {
			Message m = getMessage(s, es, recipients, subject);

			MimeBodyPart bp = new MimeBodyPart();
			bp.setContent(content, "text/html");
			Multipart mp = new MimeMultipart();
			mp.addBodyPart(bp);

			if (attachment != null) {
				bp = new MimeBodyPart();
				bp.attachFile(attachment);
				mp.addBodyPart(bp);
			}

			m.setContent(mp);
			Transport.send(m);
		}
		catch (Throwable ex) {
			handle(ex, trials);
			s = null;
		}
		return s;
	}
	private static Session getSession(final EmailSent es) {
		Properties props = new Properties();
		props.put("mail.smtp.host", es.host);
		props.put("mail.smtp.socketFactory.port", es.port);
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", es.port);
		Session s = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(es.user, es.password);
			}
		});
		return s;
	}
	private static Message getMessage(Session s, EmailSent es, String recipients,
		String subject) throws Exception {
		Message m = new MimeMessage(s);
		m.setFrom(new InternetAddress(es.email));
		if (es.emailSentOnlyToBbc == null && recipients != null) {
			m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
		}
		if (es.sentToBbc != null) {
			m.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(es.sentToBbc));
		}
		m.setSubject(subject);
		return m;
	}
	private static void handle(Throwable ex, int[] trials) {
		if (++trials[0] >= 20) {
			ex.printStackTrace();
			log.error(ex);
		}
		String trl = "Trial "+trials[0]+" to send email";
		System.out.println(trl);
		log.debug(trl);
	}
	public static Session send(Session s, EmailSent es, String recipients,
		String subject, String content, int[] trials) {
		if (s == null) {
			s = getSession(es);
		}
		try {
			Message m = getMessage(s, es, recipients, subject);
			m.setText(content);
			Transport.send(m);
		}
		catch (Throwable ex) {
			handle(ex, trials);
			s = null;
		}
		return s;
	}
}
