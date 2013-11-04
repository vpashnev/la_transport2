package com.logisticsalliance.shp;

import java.util.ArrayList;

import com.logisticsalliance.general.CommonConstants;
import com.logisticsalliance.util.SupportTime;

public class Functions {

	static String getService(ShipmentData sd, String service, int leg) {
		if (CommonConstants.CCS.equals(sd.delCarrier)) {
			if (sd.equipSize.equals("60HWY")) {
				return "HWYT";
			}
			if (sd.cmdty.equals(CommonConstants.DCF)) {
				if (sd.equipSize.equals("24")) {
					return "STFC";
				}
				else if (sd.equipSize.equals("30") || sd.equipSize.equals("48")) {
					return "FFS";
				}
			}
			else {
				if (sd.equipSize.equals("24")) {
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
		refs.add(new Ref("RDES", sd.routeN));
		refs.add(new Ref("SG", sd.routeN));
		refs.add(new Ref("SST", SupportTime.HH_mm_Format.format(sd.serviceTime)));
		refs.add(new Ref("TW1O", SupportTime.HH_mm_Format.format(sd.serviceTime)));
		refs.add(new Ref("TW1C", SupportTime.HH_mm_Format.format(sd.serviceTime)));
	}
	static class Ref {
		String name, value;

		private Ref(String name, String value) {
			super();
			this.name = name;
			this.value = value;
		}
	}
}
