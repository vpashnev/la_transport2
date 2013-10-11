package com.logisticsalliance.sn;

import java.io.Serializable;
import java.sql.Date;

/**
 * This class represents an item of delivery that should be sent to store by email. Each
 * item has order #, commodity, pallets.
 * 
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class DeliveryItem implements Serializable {
	private static final long serialVersionUID = 10L;

	String cmdty, orderN, firstUserFile, nextUserFile,
		dsFirstUserFile, dsNextUserFile;
	double pallets;
	Date dsShipDate;
}
