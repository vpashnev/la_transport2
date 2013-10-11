package com.logisticsalliance.general;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * The class provides a way to load a Java properties file and then access a property value
 * by the key. Each property has key and value.<br>
 * Roadnet.up file is a plain text file where each line represents shipment order. The data of line
 * has multiple cells separated by the character '|'. Property value represents the position of a
 * cell column. It has the following format: {@code offset;length}. Offset and Length are separated
 * with semicolon. Offset starts from 0. For example, 10;5 means offset is 10 and the length is 5.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class RnColumns {

	public static final String
	STORE_N = "storeN",
	CONSIGNEE = "consignee",
	ROUTE_N = "routeN",
	STOP_N = "stopN",
	DC = "dc",
	COMMODITY = "commodity",
	LW = "lw",
	SHIP_DATE = "shipDate",
	DC_DEPART_TIME = "dcDepartTime",
	PREV_DISTANCE = "prevDistance",
	PREV_TRAVEL_TIME = "prevTravelTime",
	ARRIVAL_TIME = "arrivalTime",
	SERVICE_TIME = "serviceTime",
	TOTAL_SERVICE_TIME = "totalServiceTime",
	TOTAL_TRAVEL_TIME = "totalTravelTime",
	EQUIP_SIZE = "equipSize",
	ORDER_N = "orderN",
	ORDER_TYPE = "orderType",
	PALLETS = "pallets",
	UNITS = "units",
	WEIGHT = "weight",
	CUBE = "cube";

	private HashMap<String,ColPosition> map = new HashMap<String,ColPosition>();
	/**
	 * Constructs the RnColumns object for the Java properties file with the name
	 * {@code RnColumns.properties} and its parent {@code folder}.
	 * @param folder the folder which contains the file {@code RnColumns.properties}.
	 * @throws IOException
	 */
	public RnColumns(File folder) throws IOException {
		File f = folder.getParentFile();
		f = new File(f, "RnColumns.properties");
		Properties ps = load(f);
		for (Iterator<Map.Entry<Object,Object>> it = ps.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Object,Object> me = it.next();
			ColPosition cp = new ColPosition(me.getValue().toString().trim());
			map.put(me.getKey().toString(), cp);
		}
	}
	/**
	 * Returns the position of cell column in the table of Roadnet.up orders. The name of column is
	 * the property key. All properties are stored in Java properties file which is loaded when this
	 * object is constructed.
	 * @param key the property key, that is also the name of cell column.
	 * @return Column position as ColPosition object 
	 */
	public ColPosition getPosition(String key) {
		return map.get(key);
	}
	private Properties load(File f) throws IOException {
		Properties ps = new Properties();
		if (!f.exists()) {
			ps.setProperty(STORE_N, "4;6");
			ps.setProperty(CONSIGNEE, "4;6");
			ps.setProperty(ROUTE_N, "19;4");
			ps.setProperty(STOP_N, "24;3");
			ps.setProperty(DC, "28;2");
			ps.setProperty(COMMODITY, "30;2");
			ps.setProperty(LW, "30;2");
			ps.setProperty(SHIP_DATE, "680;10");
			ps.setProperty(DC_DEPART_TIME, "1297;5");
			ps.setProperty(PREV_DISTANCE, "600;10");
			ps.setProperty(PREV_TRAVEL_TIME, "611;6");
			ps.setProperty(ARRIVAL_TIME, "191;5");
			ps.setProperty(SERVICE_TIME, "618;5");
			ps.setProperty(TOTAL_SERVICE_TIME, "640;5");
			ps.setProperty(TOTAL_TRAVEL_TIME, "633;6");
			ps.setProperty(EQUIP_SIZE, "1331;15");
			ps.setProperty(ORDER_N, "28;15");
			ps.setProperty(ORDER_TYPE, "11;1");
			ps.setProperty(PALLETS, "95;7");
			ps.setProperty(UNITS, "50;5");
			ps.setProperty(WEIGHT, "44;5");
			ps.setProperty(CUBE, "13;5");
			ps.store(new FileWriter(f), null);
		}
		ps.load(new FileReader(f));
		return ps;
	}

	public static class ColPosition {
/**
 * The class represents a cell column position  in the table of Roadnet.up orders.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
*/
		private int startIndex, endIndex;
		/**
		 * Constructs the ColPosition object for the given position with the format:
		 * {@code offset;length}. For example, 10;5 means offset is 10 and the length is 5.
		 * It converts the values to the {@code StartIndex} (offset) and {@code EndIndex}
		 * (offset+length).
		 * @param pos the column position
		 */
		public ColPosition(String pos) {
			int i = pos.indexOf(';');
			startIndex = Integer.parseInt(pos.substring(0, i).trim());
			endIndex = startIndex + Integer.parseInt(pos.substring(i+1).trim());
		}
		/**
		 * Returns the starting index of cell column
		 * @return Starting index
		 */
		public int getStartIndex() {
			return startIndex;
		}
		/**
		 * Returns the ending index of cell column
		 * @return Ending index
		 */
		public int getEndIndex() {
			return endIndex;
		}
	}
}
