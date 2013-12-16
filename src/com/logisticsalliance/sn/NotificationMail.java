package com.logisticsalliance.sn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.mail.Session;

import org.apache.log4j.Logger;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.general.EMailSender;
import com.logisticsalliance.general.ScheduledWorker.EmailSent;
import com.logisticsalliance.util.SupportTime;

/**
 * This class creates and sends the messages of deliveries to stores.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class NotificationMail {

	private static Logger log = Logger.getLogger(NotificationDb.class);

	static Session send(Session s, EmailSent es, HashSet<Integer> storeSubset,
		ArrayList<DeliveryNote> al, String interval) throws Exception {
		for (Iterator<DeliveryNote> it = al.iterator(); it.hasNext();) {
			DeliveryNote dn = it.next();
			if (storeSubset != null && !storeSubset.contains(dn.storeN)) { continue;}
			StringBuilder rb = new StringBuilder(256);
			if (dn.province != null && (dn.province.trim().equalsIgnoreCase("PQ") ||
				dn.province.trim().equalsIgnoreCase("QC"))) {
				String sn = String.valueOf(dn.storeN);
				while (sn.length() < 3) {
					sn = "0" + sn;
				}
	        	rb.append(es.qcStorePrefix1); rb.append(sn);
	        	rb.append('@'); rb.append(es.qcRcptHost);
	        	rb.append(',');
	        	rb.append(es.qcStorePrefix2); rb.append(sn);
	        	rb.append('@'); rb.append(es.qcRcptHost);
	        }
	        else {
	        	rb.append(es.storePrefix1); rb.append(dn.storeN);
	        	rb.append('@'); rb.append(es.rcptHost);
	        	rb.append(',');
	        	rb.append(es.storePrefix2); rb.append(dn.storeN);
	        	rb.append('@'); rb.append(es.rcptHost);
	        }
			String delDate = SupportTime.yyyy_MM_dd_Format.format(dn.delDate),
				cmdtyList = dn.getCmdtyList(false),
				delTimeFrom = SupportTime.HH_mm_Format.format(dn.delTimeFrom),
				delTimeTo = SupportTime.HH_mm_Format.format(dn.delTimeTo),
				arrivalTime = dn.arrivalTime == null ? CommonConstants.N_A :
					SupportTime.HH_mm_Format.format(dn.arrivalTime),
				serviceTime = dn.serviceTime == null ? CommonConstants.N_A :
					SupportTime.HH_mm_Format.format(dn.serviceTime);
			StringBuilder cb = new StringBuilder(2048);
			cb.append("<html>\n");
			cb.append("<body>\n");
			//cb.append("Store Number : "+ dn.storeN + "<br>");
			cb.append("<p>\n");//
			cb.append("<b>***NOTE: A French version of this message follows the English version below***</b><br>\n");
			cb.append("<b>***NOTE: Une version fran�ais de ce message suit la version anglaise ci-dessous***</b>\n");
			cb.append("</p>\n");//
			//cb.append("<table>\n");
			//cb.append("<tr>\n");
			//cb.append("<td style='padding-top:14px;'>\n");
			cb.append("Attention: Associate and Front Store Manager<br>\n");
			cb.append("<br>\n");
			cb.append("Please be advised that a delivery will be made to your store from the DC on <b>");
			cb.append(delDate+"</b> containing the following orders:<br>\n");
			cb.append("<b>"+cmdtyList+"</b><br><br>\n");
			cb.append("Delivery Details:<br>\n");
			cb.append("<ul><li>	Delivery Window : <b>"+delTimeFrom+"</b> - <b>"+delTimeTo+"</b></li>\n");
			cb.append("<li>	Estimated Arrival Time : <b>"+arrivalTime+"</b></li>\n");
			cb.append("<li>	Estimated Unload Time (in HH:MM) : <b>"+serviceTime+"</b></li>\n");
			cb.append("<li>	Estimated # of Pallets (including totes) :  <b>"+dn.totalPallets+"</b></li>\n");
			cb.append("<li>	Planned delivery carrier : <b>"+dn.delCarrier+"</b></li></ul>\n");
			cb.append("**NOTE: This is only an estimate of pallets. It represents the floor positions on the delivery ");
			cb.append("truck for your order and should be used for planning purposes only. Additional totes and/or ");
			cb.append("loading adjustments at the DC may cause this number to change.  As per the established standards, ");
			cb.append("please use the information provided on the BOL(s) that arrive with the delivery as the source for ");
			cb.append("validating that you have received a complete order.<br>\n");
			cb.append("\n<br>");
			cb.append("Reminder :<br>\n");
			cb.append("In order to prepare for an efficient and effective delivery, please ensure the following established standards continue to be implemented in your store:</li>\n");
			cb.append("<ul><li>	The Receiving area, both inside and outside the store, is safe, cleared, and prepared for your Driver prior to your scheduled delivery window</li>\n");
			cb.append("<li>	Prepare product returns/claims prior to the arrival of the Driver. The Driver can only accept returns for authorized claims.</li>\n");
			cb.append("<li>	The Narcotic QPIC Report is completed correctly and signed prior to the arrival of the Driver</li>\n");
			cb.append("<li>	Stack empty totes (separating gray/black and beige) and pallets in a clear dry area within Receiving ready for the Driver</li></ul>\n");
			cb.append("<br>\n");
			cb.append("Contact the Marketing Call Centre if you have any exceptions during the delivery process.\n");
			//cb.append("</td>\n");
			cb.append("<br><br><br>\n");//
			cb.append("<hr>\n");
			cb.append("<br><br>\n");//
			//cb.append("<td style='padding-top:2px;padding-left:60px;'>\n");
			cb.append("Destinataires : Pharmacien-propri�taire et g�rant du Magasin<br>\n");
			cb.append("<br>\n");
			cb.append("Soyez avis�s qu'une livraison du CD sera effectu�e � votre magasin le <b>");
			cb.append(delDate+"</b> incluant les commandes suivantes : <br>\n");
			cb.append("<b>"+cmdtyList+"</b><br>\n<br>\n");
			cb.append("D�tails :<br>\n");
			cb.append("<ul><li>	P�riode de livraison : <b>"+delTimeFrom+" - "+delTimeTo+"</b></li>\n");
			cb.append("<li>	Temps d'arriv�e estim� : <b>"+arrivalTime+"</b></li>\n");
			cb.append("<li>	Temps estim� de d�chargement (en HH:MM) : <b>"+serviceTime+"</b></li>\n");
			cb.append("<li>	Votre livraison sera compos�e d'environ <b>"+dn.totalPallets+"</b> palettes** incluant les bacs.</li>\n");
			cb.append("<li>	Le Transporteur Planifi� de la livraison : <b>"+dn.delCarrier+"</b></li></ul>\n");
			cb.append("** Prenez note qu'il ne s'agit que d'un estim� du nombre de palettes. Il repr�sente les positions ");
			cb.append("de plancher que votre commande occupera dans le camion de livraison, et devrait �tre utilis� pour ");
			cb.append("votre planification. Des bacs additionnels et/ou le chargement au CD peut faire varier ce nombre. ");
			cb.append("Veuillez utiliser l'information fournie sur le connaissement accompagnant la livraison comme base ");
			cb.append("pour valider que vous avez re�u votre commande au complet.<br>\n");
			cb.append("<br>\n");
			cb.append("Rappel :<br>\n");
			cb.append("Pr�parez-vous pour une livraison efficace et prompte, en vous assurant de ce qui suit :<br>\n");
			cb.append("<ul><li>	Que l'aire de r�ception, aussi bien � l'int�rieur qu'� l'ext�rieur du magasin, ");
			cb.append("est s�curitaire, nettoy�e et pr�par�e pour votre chauffeur avant la p�riode de ");
			cb.append("livraison pr�vue</li>\n");
			cb.append("<li>	Pr�parez les retours/r�clamations avant l'arriv�e du chauffeur. ");
			cb.append("Le chauffeur ne peut accepter que des retours autoris�s;</li>\n");
			cb.append("<li>	Compl�tez correctement et signez avant l'arriv�e du chauffeur le rapport de produits narcotiques du responsable qualifi�;</li> \n");
			cb.append("<li>	Empilez les bacs vides (en s�parant les gris des beiges) et les palettes dans un ");
			cb.append("secteur sec de votre aire de r�ception pr�ts pour votre chauffeur.</li></ul>\n");
			cb.append("\n<br>");
			cb.append("Communiquez avec le Centre t�l�phonique ? Marketing si vous constatez des exceptions durant la livraison.");
			//cb.append("</td>\n");
			//cb.append("</tr>\n");
			//cb.append("</table>\n");
			cb.append("\n<br>\n<br>");
			cb.append("\n<div style='font-size:10px;color:LightGray;'>");
			cb.append(dn.storeN); cb.append('-'); cb.append(dn.id);
			cb.append(' '); cb.append(':'); cb.append(' ');
			cb.append(interval);
			cb.append("</div>\n");
			cb.append("</body>\n");
			cb.append("</html>");
			String sbj = "DC Delivery Planning Notification / Notification de Planification Remise C.D.";
			int[] trials = {0};
			s = EMailSender.send(s, es, rb.toString(), sbj, cb.toString(), null, trials);
			while (s == null && trials[0] < 20) {
				Thread.sleep(20000);
				s = EMailSender.send(s, es, rb.toString(), sbj, cb.toString(), null, trials);
			}
			if (s == null) {
				log.error("Unable to send email for the delivery: "+dn.storeN+", "+
					dn.cmdtyList+", "+dn.delDate+", "+dn.nextUserFile);
				it.remove();
			}
		}
		return s;
	}

}
