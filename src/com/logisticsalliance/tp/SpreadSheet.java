package com.logisticsalliance.tp;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.logisticsalliance.general.CommonConstants;

public class SpreadSheet {

	private static Cell addCell(Row r, int cell, String value) {
		Cell c = r.createCell(cell);
		c.setCellValue(value);
		return c;
	}
	private static void addHead(Row r1, Row r2) {
		addCell(r1, 9, "Poll");
	}
	static void fill(Sheet sh, SearchInput si, String day,
		HashMap<String,ArrayList<ShipmentRow>> m) {
		Row r = sh.createRow(0);
		Cell c = r.createCell(0);
		c.setCellValue("DC"+si.dc);
		c = r.createCell(1);
		c.setCellValue(day);
		r = sh.createRow(2);
		addHead(r, sh.createRow(3));
	}
	private static void addHead1(FileWriter w, boolean dc50) throws Exception {
		w.write(",,,,,,,,,Narc,,,,,,Polling,, Roadnet Run,,Bomb,,Selection,,RX Polling,," +
			"RX Roadnet Run,,RX Bomb,,RX Selection,, Shipping Time,,,,,,,,,,,,,,,,,,,,,,,,,,,,," +
			"Delivery,,,,,,,WHSE # 10,WHSE # 30,WHSE # 40,WHSE # 50,WHSE # 40,WHSE # 50," +
			"WHSE # 10,WHSE # 30,,,,,,Driver,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,," +
			",,,,,,,YYYY-MM-DD\r\n");
		w.write("Route,Stop,Store,Orig.Route,Name,Street #,Address1,City,DB Ship Day," +
			"Stop,Route,DCF,Carrier,Prov,Post Code,Day,Time,Day,Time,Day,Time,Day,Time," +
			"Day,Time,Day,Shift,Day,Shift,Day,Shift,Day,Load & Seal,Time,Depart & TRAIN," +
			"Load Close,RX Totes,Actual Dep,Late,Pallets,Weight kg,Cube,WHSE 10,WHSE 30," +
			"WHSE 55,WHSE 20,WHSE 25,WHSE 20 cube,WHSE 25 cube,WH20/25 cube,WHSE 60," +
			(dc50 ? "WHS 15" : "WHS 10")+
			",WHS.30 Totes,Event Route,Event Pallets,2 Event Route," +
			"2 Event Pallets,3 Event Route,3 Event Pallets,Load Ready,Day,Time Window,," +
			"Gatehouse Time,Truck Size,Max Truck Size,Special Instructions,Stage Lane," +
			"Stage Lane,Stage Lane,Stage Lane,Bombed,Bombed,Bombed,Bombed,Door #,Trailer #," +
			"Stage Lane,Verify Trailer #,City or Hwy Cost designation,Last Name,First Name," +
			"Hours Delayed,Receiving Hours,Comments,Agency Name,Cab Number,LH Carrier," +
			"Weight,Arrival time,Service Time,Route Start Time,Travel Time,Distance," +
			"Stop Arrival Date,Pre route Time,Stop Type,Post Route Time,Route Departure Time," +
			"Route Arrival Time,Route Complete,Route Name,,,,,,,,,,,,,,,,,,,,,,,,,,,,," +
			"LH Carrier,LH Group ID,Delivery Carrier,Delivery Group ID,Redirect Flag," +
			"Delivery Service Override,LH Service Override,Dallas BOL,Carrier ProBill," +
			"Depart Date,Delivery Date,Group");
		w.write('\r'); w.write('\n');
	}
	private static void addHead(FileWriter w) throws Exception {
		w.write(','); w.write(','); w.write(','); w.write(',');
		w.write("Narc"); w.write(','); w.write(','); w.write(',');
		w.write(','); w.write(','); w.write(','); w.write(','); w.write(',');
		w.write("Poll"); w.write(','); w.write(',');
		w.write("Ship"); w.write(','); w.write(',');
		w.write("Changed");
		w.write(','); w.write(',');
		w.write(','); w.write(','); w.write(','); w.write(',');
		w.write("Deliver");
		w.write('\r'); w.write('\n');
		w.write(',');
		w.write("Route"); w.write(',');
		w.write("Stop"); w.write(',');
		w.write("Store"); w.write(',');
		w.write("Route"); w.write(',');
		w.write("Stop"); w.write(',');
		w.write("Group"); w.write(',');
		w.write("City"); w.write(',');
		w.write("Carriers"); w.write(','); w.write(',');
		w.write("Prov"); w.write(',');
		w.write("Post Code"); w.write(',');
		w.write("Day"); w.write(',');
		w.write("Time"); w.write(',');
		w.write("Day"); w.write(',');
		w.write("Time"); w.write(',');
		w.write("Day"); w.write(',');
		w.write("Time"); w.write(',');
		w.write("Pallets"); w.write(',');
		w.write("Weight"); w.write(',');
		w.write("WHSE 60"); w.write(',');
		w.write("WH 10 plts"); w.write(',');
		w.write("Date"); w.write(',');
		w.write("Day"); w.write(',');
		w.write("From"); w.write(',');
		w.write("To"); w.write(',');
		w.write("DCX"); w.write(',');
		w.write("Ship Day"); w.write(',');
		w.write("LH Car."); w.write(',');
		w.write("LH Serv."); w.write(',');
		w.write("Del Car."); w.write(',');
		w.write("Del Serv."); w.write(',');
		w.write("Stg.Lane"); w.write(',');
		w.write("Spec.Instr"); w.write(',');
		w.write("Distance"); w.write(',');
		w.write("Truck Size"); w.write(',');
		w.write("Max Truck Size"); w.write(',');
		w.write("File"); w.write(',');
		w.write("Rel.File"); w.write(',');
		w.write("Replaced");
		w.write('\r'); w.write('\n');
	}
	private static int getDcn(String dc) {
		switch (dc) {
		case "10": return 9;
		case "20": return 8;
		case "30": return 3;
		case "50": return 5;
		default: return 7;
		}
	}
	private static void fill(SearchInput si, String day, boolean dc10,
		HashMap<String,ArrayList<ShipmentRow>> m) throws Exception {
		for (Iterator<Map.Entry<String,ArrayList<ShipmentRow>>> it = m.entrySet().iterator();
			it.hasNext();) {
			Map.Entry<String,ArrayList<ShipmentRow>> e = it.next();
			String cmdty = e.getKey();
			if (cmdty.equals(FillGridDB.missing)) {
				continue;
			}
			boolean dcx = cmdty.equals(CommonConstants.DCX),
				fs = cmdty.equals(CommonConstants.FS),
				rx = cmdty.equals(CommonConstants.RX),
				sameGroup = false, sameCar = false;
			int i = 0, ltli = 100,
				maxStops = dcx ? 20 : (fs && dc10 ? 6 : 0);
			ArrayList<ShipmentRow> al = e.getValue();
			ShipmentRow r0 = null;
			for (Iterator<ShipmentRow> it1 = al.iterator(); it1.hasNext();) {
				ShipmentRow r = it1.next();
				if (r0 != null) {
					sameGroup = r0.group == r.group;
					sameCar = r0.carrier == r.carrier ||
						r0.carrier != null && r0.carrier.equals(r.carrier);
				}
				boolean ltlFs = CommonConstants.LTL.equals(r.carrierType) &&
					CommonConstants.FS.equals(cmdty);
				if (r.aRoutePerGroup) {
					if (!sameGroup || !sameCar) {
						if (ltlFs) {
							ltli++;
						}
						else { i += (rx ? 5 : 1);}
					}
				}
				else {
					if (ltlFs) {
						ltli++;
					}
					else { i += (rx ? 5 : 1);}
				}
				r.route = getRoute(r, dc10, cmdty, i, ltli);
				if (r.stop1 != -1) {
					r.stop = r.stop1;
				}
				r0 = r;
			}
			Collections.sort(al);
			r0 = null;
			for (Iterator<ShipmentRow> it1 = al.iterator(); it1.hasNext();) {
				ShipmentRow r = it1.next();
				if (r0 != null) {
					r.sameGroup = r0.group == r.group;
					r.sameCar = r0.carrier == r.carrier ||
						r0.carrier != null && r0.carrier.equals(r.carrier);
				}
				r0 = r;
			}
			int stop = 0; i = 0;
			boolean sameRoute = false;
			r0 = null;
			for (Iterator<ShipmentRow> it1 = al.iterator(); it1.hasNext();) {
				ShipmentRow r = it1.next();
				r.route += i;
				if (r0 != null) {
					sameRoute = r0.route == r.route;
				}
				if (sameRoute) {
					stop++;
				}
				else { stop = 1;}
				if (maxStops > 0 && stop > maxStops || stop > 49) {
					r.sameGroup = false;
					r.route += (rx ? 5 : 1);
					i += (rx ? 5 : 1);
					stop = 1;
				}
				if (r.stop1 == -1) {
					r.stop = stop;
				}
				r0 = r;
			}
		}
	}
	static void fill(FileWriter w, SearchInput si, String day, boolean dc10, int dc20,
		boolean dc50, boolean dc70, HashMap<String,ArrayList<ShipmentRow>> m) throws Exception {
		w.write("DC"); w.write(si.dc); w.write(',');
		w.write(day);
		w.write('\r'); w.write('\n');
		if (si.test) {
			addHead(w);
		}
		else { addHead1(w, dc50);}
		fill(si, day, dc10, m);
		ArrayList<ShipmentRow> al0 = null;
		for (Iterator<Map.Entry<String,ArrayList<ShipmentRow>>> it =
			m.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String,ArrayList<ShipmentRow>> e = it.next();
			String cmdty = e.getKey();
			ArrayList<ShipmentRow> al = e.getValue();
			if (cmdty.equals(FillGridDB.missing)) {
				al0 = al;
				continue;
			}
			w.write(cmdty); w.write('\r'); w.write('\n');
			ShipmentRow r0 = null;
			boolean[] sonarDdctLtl = {false, false};
			int stops = 0;
			for (Iterator<ShipmentRow> it1 = al.iterator(); it1.hasNext();) {
				ShipmentRow r = it1.next();
				if (r0 != null && r.route != r0.route) {
					if (stops > 1 || !r.sameCar) {
						w.write('\r'); w.write('\n');
					}
					stops = 0;
				}
				addHeader(w, r, sonarDdctLtl);
				String v = si.test ? r.getCsvRow(dc20) : r.getCsvRow1(dc20, dc50, dc70);
				w.write(v);
				r0 = r;
				stops++;
			}
			w.write('\r'); w.write('\n');
		}
		if (al0 != null) {
			w.write(FillGridDB.missing); w.write('\r'); w.write('\n');
			for (Iterator<ShipmentRow> it1 = al0.iterator(); it1.hasNext();) {
				ShipmentRow r = it1.next();
				String v = si.test ? r.getCsvRow(dc20) : r.getCsvRow1(dc20, dc50, dc70);
				w.write(v);
			}
		}
	}
	private static void addHeader(FileWriter w, ShipmentRow r,
		boolean[] sonarDdctLtl) throws Exception {
		if (CommonConstants.SONAR.equals(r.delCarrier)) {
			if (!sonarDdctLtl[0] && CommonConstants.DDCT.equals(r.delService)) {
				w.write("SONAR DDCT:\r\n");
				sonarDdctLtl[0] = true;
			}
			if (!sonarDdctLtl[1] && CommonConstants.LTL.equals(r.delService)) {
				w.write("SONAR LTL:\r\n");
				sonarDdctLtl[1] = true;
			}
		}
	}
	private static int getRoute(ShipmentRow r, boolean dc10, String cmdty, int i, int ltli) {
		int sdn = r.shipDay, n = 0;
		if (!dc10) { sdn++;}
		switch (cmdty) {
		case CommonConstants.FS:
			n = sdn*1000;
			if (CommonConstants.LTL.equals(r.carrierType)) {
				return n+ltli;
			}
			break;
		case CommonConstants.DCX:
			sdn = r.relCmdtyShipDay+1;
			n = sdn*1000 + getDcn(r.localDc)*100;
			break;
		case CommonConstants.DCF:
			n = sdn*100;
			break;
		case CommonConstants.RX:
			n = sdn*1000+200;
			break;
		}
		n += i;
		return n;
	}
}
