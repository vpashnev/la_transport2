package com.logisticsalliance.tp;

import java.util.ArrayList;

public class FillGridDB {

	static String
	SQL_SEL =
	"SELECT " +
	"sd.store_n, sd.cmdty, sd.del_date, route_n, stop_n, dc," +
	"dc_depart_time, prev_distance, prev_travel_time, arrival_time, service_time," +
	"total_service_time, total_travel_time," +
	"order_n, pallets, units, weight, cube, del_time_from, del_time_to," +
	"spec_instructs, lh_carrier_id, lh_service, del_carrier_id, del_service," +
	"sd.first_user_file, sd.next_user_file, rno.first_user_file, rno.next_user_file," +
	"sts.first_user_file, sts.next_user_file, sts.ship_date " +

	"FROM " +
	"la.hstore_schedule sts,la.hstore_profile sp " +

	"WHERE " +
	"sts.store_n=sp.store_n AND sp.local_dc=? AND sts.cmdty IN () " +

	"ORDER BY " +
	"1,6,4,2";

	static ArrayList<ShipmentRow> process(SearchInput si) {
		ArrayList<ShipmentRow> al = new ArrayList<ShipmentRow>(512);
		return al;
	}
}
