package com.logisticsalliance.shp;

import java.sql.Date;
import java.sql.Time;
import java.text.DecimalFormat;
import java.util.ArrayList;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.util.SupportTime;

class Functions {

	private static final DecimalFormat hourFormat = new DecimalFormat("#.##");

	static String getGroupID(ShipmentData sd, int leg) {
		return sd.dc.equals("20") && sd.dcx ? "" : sd.routeN;
	}
	static String getDelService(ShipmentData sd, String service) {
		if (CommonConstants.CCS.equals(sd.delCarrier)) {
			if (sd.equipSize.startsWith("60H")) {//52TG
				return "HWYT";
			}
			if (sd.equipSize.startsWith("60")) {
				return "SGCT";
			}
			if (sd.cmdty.equals(CommonConstants.DCF)) {
				if (sd.equipSize.equals("24")) {
					return "STFC";
				}
				//else if (sd.equipSize.equals("30") || sd.equipSize.equals("48")) {
					return "FFS";
				//}
			}
			else {
				if (sd.equipSize.equals("24")) {
					return "STG";
				}
			}
			return "SGL";
		}
		String v = cut(service, 4);
		if (v.isEmpty()) {
			if (sd.cmdty.equals(CommonConstants.DCF)) {
				v = "FFS";
			}
			else { v = "LTL";}
		}
		return v;
	}
	static String cut(String v, int len) {
		if (v == null) {
			v = "";
		}
		else if (v.length() > len) {
			v = v.substring(0, len).trim();
		}
		return v;
	}
	static void putRefs(ArrayList<Ref> refs, ShipmentData sd, int leg) {
		String a = "";
		if (leg == 2) { a = ".";}
		refs.clear();
		refs.add(new Ref("CR", sd.ordN));
		refs.add(new Ref("SG", sd.routeN));
		refs.add(new Ref("RDES", sd.routeN+a));

		int srvMins = toMins(sd.serviceTime);
		if (sd.lhCarrier == null || leg == 2) {
			refs.add(new Ref("PD", String.valueOf(toMins(sd.prevTravelTime))+a));
			refs.add(new Ref("PM", String.valueOf(sd.prevDistance)+a));
			refs.add(new Ref("PW", String.valueOf(srvMins)+a));
			if (leg == 1) {
				if (CommonConstants.CCS.equals(sd.delCarrier)) {
					refs.add(new Ref("TTIM", String.valueOf(toDouble(sd.prevTravelTime))));
				}
				refs.add(new Ref("SST", SupportTime.Hmm_Format.format(sd.dcDepartTime)));
			}
			else {
				refs.add(new Ref("SST", "1200"+a));
			}
			refs.add(new Ref("ESTA", SupportTime.HHmm_Format.format(sd.arrivalTime)+a));
			Time t = new Time(sd.arrivalTime.getTime()+srvMins*60000);//depart time
			refs.add(new Ref("ESTD", SupportTime.HHmm_Format.format(t)+a));
			refs.add(new Ref("TW1O", SupportTime.Hmm_Format.format(sd.delTimeFrom)+a));
			refs.add(new Ref("TW1C", SupportTime.Hmm_Format.format(sd.delTimeTo)+a));
		}
		else if (leg == 1) {
			refs.add(new Ref("SST", SupportTime.Hmm_Format.format(sd.dcDepartTime)));
		}

		refs.add(new Ref("ET", sd.equipSize+a));
	}
	static int toMins(Time t) {
		String s = SupportTime.HHmm_Format.format(t);
		int v = Integer.parseInt(s.substring(0, 2));
		v = Integer.parseInt(s.substring(2))+v*60;
		return v;
	}
	static int toInt(Date d) {
		String s = SupportTime.yyMMdd_Format.format(d);
		int v = 1000000+Integer.parseInt(s);
		return v;
	}
	static double toDouble(Time t) {
		String s = SupportTime.HHmm_Format.format(t);
		double v = Double.parseDouble(s.substring(0, 2));
		v += Double.parseDouble(s.substring(2))/60;
		s = hourFormat.format(v);
		return Double.parseDouble(s);
	}
	static class Ref {
		String name, value;

		private Ref(String name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return name+'='+value;
		}
	}
}
