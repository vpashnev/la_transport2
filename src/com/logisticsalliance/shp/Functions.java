package com.logisticsalliance.shp;

import java.sql.Date;
import java.sql.Time;
import java.text.DecimalFormat;
import java.util.ArrayList;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.util.SupportTime;

public class Functions {

	private static final DecimalFormat hourFormat = new DecimalFormat("#.##");

	static String getService(ShipmentData sd, String service, int leg) {
		if (CommonConstants.CCS.equals(sd.delCarrier)) {
			if (sd.equipSize.startsWith("60")) {//52TG
				return "HWYT";
			}
			if (sd.cmdty.equals(CommonConstants.DCF)) {
				if (sd.equipSize.startsWith("24")) {
					return "STFC";
				}
				else if (sd.equipSize.startsWith("30") || sd.equipSize.startsWith("48")) {
					return "FFS";
				}
			}
			else {
				if (sd.equipSize.startsWith("24")) {
					return "STG";
				}
			}
			return "SGL";
		}
		return cut(service, 4);
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
		refs.clear();
		refs.add(new Ref("CR", sd.ordN));
		refs.add(new Ref("SG", sd.routeN));

		refs.add(new Ref("ESTA", SupportTime.HHmm_Format.format(sd.arrivalTime)));
		int srvMins = toMins(sd.serviceTime);
		Time t = new Time(sd.arrivalTime.getTime()+srvMins*60000);//depart time
		refs.add(new Ref("ESTD", SupportTime.HHmm_Format.format(t)));
		refs.add(new Ref("ET", sd.equipSize));
		refs.add(new Ref("PD", String.valueOf(toMins(sd.prevTravelTime))));
		refs.add(new Ref("PM", String.valueOf(sd.prevDistance/10d)));
		refs.add(new Ref("PW", String.valueOf(srvMins)));
		refs.add(new Ref("TTIM", String.valueOf(toDouble(sd.prevTravelTime))));

		refs.add(new Ref("RDES", sd.routeN));
		refs.add(new Ref("SST", SupportTime.Hmm_Format.format(sd.dcDepartTime)));
		refs.add(new Ref("TW1O", SupportTime.Hmm_Format.format(sd.delTimeFrom)));
		refs.add(new Ref("TW1C", SupportTime.Hmm_Format.format(sd.delTimeTo)));
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
			super();
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return name+'='+value;
		}
	}
}
