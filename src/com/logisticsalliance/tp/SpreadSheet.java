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
	private static void addHead1(FileWriter w) throws Exception {
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
			"WH 10 Plates,WHS.30 Totes,Event Route,Event Pallets,2 Event Route," +
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
	private static void fill(SearchInput si, String day,
		HashMap<String,ArrayList<ShipmentRow>> m) throws Exception {
		for (Iterator<Map.Entry<String,ArrayList<ShipmentRow>>> it = m.entrySet().iterator();
			it.hasNext();) {
			Map.Entry<String,ArrayList<ShipmentRow>> e = it.next();
			String cmdty = e.getKey();
			if (cmdty.equals(FillGridDB.missing)) {
				continue;
			}
			boolean dcx = cmdty.equals(CommonConstants.DCX),
				fs = cmdty.equals(CommonConstants.FS);
			int  i = 0, ltli = 200,
				maxStops = dcx ? 20 : (fs && si.dc.equals(CommonConstants.DC10) ? 6 : 0);
			ArrayList<ShipmentRow> al = e.getValue();
			ShipmentRow r0 = null;
			for (Iterator<ShipmentRow> it1 = al.iterator(); it1.hasNext();) {
				ShipmentRow r = it1.next();
				if (r0 != null) {
					r.sameGroup = r0.group == r.group;
					r.sameCar = r0.carrier == r.carrier ||
						r0.carrier != null && r0.carrier.equals(r.carrier);
					r.samePC = r0.postCode.equals(r.postCode);
				}
				boolean ltlFs = CommonConstants.LTL.equals(r.carrierType) &&
					CommonConstants.FS.equals(cmdty);
				if (r.aRoutePerGroup) {
					if (!r.sameGroup || !r.sameCar) {
						if (ltlFs) {
							ltli++;
						}
						else { i++;}
					}
				}
				else {
					if (ltlFs) {
						ltli++;
					}
					else { i++;}
				}
				r.route = getRoute(r, cmdty, i, ltli);
				r0 = r;
			}
			Collections.sort(al);
			int stop = 0; i = 0;
			boolean sameRoute = false;
			r0 = null;
			for (Iterator<ShipmentRow> it1 = al.iterator(); it1.hasNext();) {
				ShipmentRow r = it1.next();
				if (maxStops > 0) {
					r.route += i;
				}
				if (r0 != null) {
					if (r.route1 != null) {
						System.out.println(r);
					}
					sameRoute = r0.route == r.route ||
						r0.route1 != null && r0.route1.equals(r.route1);
				}
				if (sameRoute) {
					stop++;
				}
				else { stop = 1;}
				if (maxStops > 0 && stop > maxStops) {
					r.sameGroup = false;
					r.route++;
					stop = 1;
					i++;
				}
				r.stop = stop;
				r0 = r;
			}
		}
	}
	static void fill(FileWriter w, SearchInput si, String day, int dc20,
		HashMap<String,ArrayList<ShipmentRow>> m) throws Exception {
		w.write("DC"); w.write(si.dc); w.write(',');
		w.write(day);
		w.write('\r'); w.write('\n');
		if (si.test) {
			addHead(w);
		}
		else { addHead1(w);}
		fill(si, day, m);
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
			boolean first = true;
			for (Iterator<ShipmentRow> it1 = al.iterator(); it1.hasNext();) {
				ShipmentRow r = it1.next();
				if (!first && !r.sameGroup) {
					w.write('\r'); w.write('\n');
				}
				String v = si.test ? r.getCsvRow(dc20) : r.getCsvRow1(dc20);
				w.write(v);
				if (first) {
					first = false;
				}
			}
			w.write('\r'); w.write('\n');
		}
		if (al0 != null) {
			w.write(FillGridDB.missing); w.write('\r'); w.write('\n');
			for (Iterator<ShipmentRow> it1 = al0.iterator(); it1.hasNext();) {
				ShipmentRow r = it1.next();
				String v = si.test ? r.getCsvRow(dc20) : r.getCsvRow1(dc20);
				w.write(v);
			}
		}
	}
	private static int getRoute(ShipmentRow r, String cmdty, int i, int ltli) {
		int sdn = r.shipDay+1, n = 0;
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
			n = sdn*1000+300;
			break;
		}
		n += i;
		return n;
	}
}
