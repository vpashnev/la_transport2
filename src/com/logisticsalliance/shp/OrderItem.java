package com.logisticsalliance.shp;

import java.io.Serializable;
import java.sql.Date;

/**
 * This class represents a line (order) in roadnet.up file with the 2 properties (parameters):
 * order #, pallets.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class OrderItem implements Serializable {
	private static final long serialVersionUID = 10L;

	public String orderN, lw, firstUserFile, nextUserFile,
		dsFirstUserFile, dsNextUserFile;
	public double pallets;
	public Date dsShipDate;
}
