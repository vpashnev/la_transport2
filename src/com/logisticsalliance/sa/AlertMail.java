package com.logisticsalliance.sa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.mail.Session;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.EMailSender;
import com.logisticsalliance.general.ScheduledWorker.EmailSent;
import com.logisticsalliance.tt.web.Alert;
import com.logisticsalliance.util.SupportTime;

/**
 * This class creates and sends the messages of deliveries to stores.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class AlertMail {

	private static Logger log = Logger.getLogger(AlertMail.class);

	private static void addAddress(StringBuilder rb, TrackingNote tn, int startIdx) {
		Alert[] arr = SendAlertDb.alertPrefs.get(tn.storeN);
		int count = startIdx + 2;
		for (int n = 0; n != arr.length; n++) {
			Alert a = arr[n];
			String[] comm = a.getComm();
			boolean[] b = a.getCmdty();
			for (int j = 0; j != b.length; j++) {
				boolean has = false;
				switch (j) {
				case 0:
					if (b[j] && tn.cmdtyPallets.containsKey(CommonConstants.DCB)) { has = true;}
					break;
				case 1:
					if (b[j] && tn.cmdtyPallets.containsKey(CommonConstants.DCV)) { has = true;}
					break;
				case 2:
					if (b[j] && tn.cmdtyPallets.containsKey(CommonConstants.DCX)) { has = true;}
					break;
				case 3:
					if (b[j] && tn.cmdtyPallets.containsKey(CommonConstants.DCF)) { has = true;}
					break;
				case 4:
					if (b[j] && tn.cmdtyPallets.containsKey(CommonConstants.EVT)) { has = true;}
					break;
				case 5:
					if (b[j] && tn.cmdtyPallets.containsKey(CommonConstants.EVT2)) { has = true;}
					break;
				}
				if (has) {
					for (int i = startIdx; i != count; i++) {
						String c = comm[i];
						if (c != null && !c.isEmpty()) {
							rb.append(c);
							rb.append(',');
						}
					}
					break;
				}
			}
		}
	}
	static Session send(Session s, EmailSent es, ArrayList<TrackingNote> al,
		String interval, boolean alertStoresByPhone) throws Exception {
		for (Iterator<TrackingNote> it = al.iterator(); it.hasNext();) {
			TrackingNote tn = it.next();
			if (!tn.exception) {
				continue;
			}
			int count = alertStoresByPhone ? 4 : 2;
			for (int i = 0; i != count; i += 2) {
				StringBuilder rb = new StringBuilder(256);
				addAddress(rb, tn, i);
				if (rb.length() == 0) {
					continue;
				}
				int[] trials = {0};
				String sbj = "DC Delivery Status Update - Exception";
				if (i == 2) {
					String msg = getMessage(tn, null, true);
					s = EMailSender.send(s, es, rb.toString(), sbj, msg, trials);
					while (s == null && trials[0] < 20) {
						Thread.sleep(20000);
						s = EMailSender.send(s, es, rb.toString(), sbj, msg, trials);
					}
				}
				else {
					String msg = getMessage(tn, interval, false);
					s = EMailSender.send(s, es, rb.toString(), sbj, msg, null, trials);
					while (s == null && trials[0] < 20) {
						Thread.sleep(20000);
						s = EMailSender.send(s, es, rb.toString(), sbj, msg, null, trials);
					}
				}
				if (s == null) {
					log.error("Unable to send email for the delivery: "+tn.storeN+", "+
						tn.delDate+", "+tn.route+", "+tn.carrier+", "+tn.cmdtyPallets);
				}
			}
		}
		return s;
	}

	private static String getMessage(TrackingNote tn, String interval, boolean sms) {
		String delDate = SupportTime.yyyy_MM_dd_Format.format(tn.delDate);
		StringBuilder cb;
		if (sms) {
			cb = new StringBuilder(200);
			cb.append("\r\nDelivery : ");
			cb.append(delDate);
			cb.append(" - ");
			cb.append(getCmdty(tn));
			for (Iterator<AlertItem> it = tn.items.iterator(); it.hasNext();) {
				AlertItem ai = it.next();
				cb.append("\r\n\r\n");
				cb.append(ai.reason);
				cb.append(" :\r\n");
				cb.append(ai.comment);
			}
		}
		else {
			cb = new StringBuilder(1024);
			cb.append("<html>\n");
			cb.append("<body>\n");
			cb.append("Store Number : "+ tn.storeN + "<br> ");
			cb.append("<p>\n");
			cb.append("<b>***NOTE: A French version of this message follows the English version below***</b><br>\n");
			cb.append("<b>***NOTE: Une version fran�ais de ce message suit la version anglaise ci-dessous***</b>\n");
			cb.append("</p>\n");
			cb.append("<b>Attention:</b> Associate and Front Store Manager<br><br>\n");
			cb.append("The following status update has been made to the <b> ");
			cb.append(getCmdty(tn));
			cb.append(" </b> delivery that your store is scheduled to receive on <b>");
			cb.append(delDate+"</b>\n");
			cb.append("<ul>\n");
			for (Iterator<AlertItem> it = tn.items.iterator(); it.hasNext();) {
				AlertItem ai = it.next();
				cb.append("<li>\n");
				cb.append(ai.reason);
				cb.append("<br>");
				cb.append(ai.comment);
				cb.append("</li>\n");
			}
			cb.append("</ul>\n");
			cb.append("If this update changes the estimated time of arrival (ETA) of the delivery at your store, \n");
			cb.append("you will receive another update with the new ETA once it is confirmed.<br><br>\n");
			cb.append("Contact the Marketing Call Centre if you have any exceptions during the delivery process<p>\n");
			cb.append("All details for your delivery can be found on the Store On-line Delivery Status Tool at :<br>\n");
			cb.append("www.putthewebaddresshere.com<br><br>\n");

			cb.append("<hr><br>\n");//

			cb.append("<b>Destinataires:</b> Pharmacien-propri�taire et g�rant du Magasin<br>\n");

			cb.append("\n<br><br><div style='font-size:10px;color:LightGray;'>");
			cb.append(tn.storeN); cb.append('-'); cb.append(tn.route);
			cb.append(' '); cb.append(':'); cb.append(' ');
			cb.append(interval);
			cb.append("</div>\n");
			cb.append("</body>\n");
			cb.append("</html>");
		}
		return cb.toString();
	}
	private static String getCmdty(TrackingNote tn) {
		HashSet<String> cmdty = new HashSet<String>(4, .5f);
		for (Iterator<AlertItem> it = tn.items.iterator(); it.hasNext();) {
			AlertItem ai = it.next();
			ai.addCmdtyTo(cmdty);
		}
		return TrackingNote.getCmdtyList(cmdty);
	}
}
