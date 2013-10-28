package com.logisticsalliance.sqla;

/**
 * This class creates a single instance for the {@link ConnectFactory} superclass.
 * The static method {@link #one()} returns the single instance of this object.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class ConnectFactory1 extends ConnectFactory {
	private static final long serialVersionUID = 10L;

	private static final ConnectFactory1 one = new ConnectFactory1();

	private ConnectFactory1() { }
	/**
	 * Returns the single instance of this object.
	 * @return ConnectFactory1 single instance
	 */
	public static ConnectFactory1 one() {
		return one;
	}

}
