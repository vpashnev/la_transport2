package com.logisticsalliance.shp;

/**
 * This class represents a line (order) in roadnet.up file with the 5 properties (parameters):
 * order #, pallets, units, weight, cube. ShipmentData contains the related ShipmentItems taken
 * from lines of roadnet.up file.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class ShipmentItem extends OrderItem {
	private static final long serialVersionUID = 10L;

	double units, weight, cube;

}
