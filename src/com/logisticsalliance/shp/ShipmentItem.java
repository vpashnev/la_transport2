package com.logisticsalliance.shp;

import java.io.Serializable;
import java.sql.Date;
/**
 * This class represents a line (order) in roadnet.up file with the four properties (parameters):
 * order #, pallets, units, weight, cube. ShipmentData contains the related ShipmentItems taken
 * from lines of roadnet.up file.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class ShipmentItem implements Serializable {
	private static final long serialVersionUID = 10L;

	String orderN, lw, firstUserFile, nextUserFile,
		dsFirstUserFile, dsNextUserFile;
	double pallets, units, weight, cube;
	Date dsShipDate;

}
