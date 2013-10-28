package com.logisticsalliance.sqla;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * This class represents a factory for connections to the physical data source.
 * It produces connections using the {@code DriverManger}.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class ConnectFactory implements Serializable {
	private static final long serialVersionUID = 10L;

	private String driver, url,
		user, password = "";

	public ConnectFactory() { }
	/**
	 * Constructs {@code ConnectFactory} with the specified driver and
	 * database URL.
	 * @param driver the driver class to load for {@code DriverManger}
	 * @param url the database URL in the form jdbc:subprotocol:subname
	 */
	public ConnectFactory(String driver, String url) {
		setDriver(driver);
		this.url = url;
	}
	/**
	 * Constructs {@code ConnectFactory} with the specified driver,
	 * database URL, user and password.
	 * @param driver the driver class to load for {@code DriverManger}
	 * @param url the database URL in the form jdbc:subprotocol:subname
	 * @param user the database user
	 * @param password the user's password 
	 */
	public ConnectFactory(String driver, String url,
		String user, String password) {
		this(driver, url);
		setUser(user);
		setPassword(password);
	}
	/**
	 * Returns driver class which was loaded by {@code DriverManger}
	 * @return Driver class
	 */
	public String getDriver() {
		return driver;
	}
	/**
	 * Loads the specified driver class by {@code DriverManger}
	 * @param v the driver class to load
	 */
	public void setDriver(String v) {
		if (v != null) {
			v = v.trim();
			try {
				Driver dr = (Driver)Class.forName(v).newInstance();
				DriverManager.registerDriver(dr);
//				Class.forName(v);
			}
			catch (Throwable ex) {
				throw new RuntimeException("Failed to load the JDBC driver '"+v+"'", ex);
			}
		}
		driver = v;
	}
	/**
	 * Returns the database URL the {@code DriverManger} uses to establish
	 * connections
	 * @return Database URL
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * Sets the specified database URL the {@code DriverManger} should
	 * use to establish connections.
	 * @param v the database URL in the form jdbc:subprotocol:subname
	 */
	public void setUrl(String v) {
		url = v;
	}
	/**
	 * Returns the database user on whose behalf the {@code DriverManger}
	 * creates connections
	 * @return Database user
	 */
	public String getUser() {
		return user;
	}
	/**
	 * Sets the new database user on whose behalf the {@code DriverManger}
	 * should create connections
	 * @param v the database user to set
	 */
	public void setUser(String v) {
		user = v == null ? null : v.trim();
	}
	/**
	 * Returns the user's password
	 * @return Password
	 */
	public final String getPassword() {
		return password;
	}
	/**
	 * Sets the new user's password
	 * @param v the password to set
	 */
	public void setPassword(String v) {
		password = v == null ? "" : v;
	}
	/**
	 * Creates connection. Connection is created either by DataSource factory
	 * (if present) or by Driver manager using URL, user, password properties
	 * @return Connection
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		Connection c;
		String u = user;
		if (u == null) {
			c = DriverManager.getConnection(url);
			c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			c.setAutoCommit(false);
		}
		else {
			String p = password;
			c = getConnection(u, p);
		}
		return c;
	}
	/**
	 * Creates connection on behalf of the specified user and password.
	 * Connection is created either by DataSource factory (if present) or by
	 * Driver manager using URL property and the specified user and password.
	 * @param user the database user
	 * @param password the user's password
	 * @return Connection
	 * @throws SQLException
	 */
	public Connection getConnection(String user,
		String password) throws SQLException {
		Connection c = DriverManager.getConnection(url, user, password);
		c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		c.setAutoCommit(false);
		return c;
	}
	/**
	 * Creates connection with the method {@link #getConnection()} and makes
	 * the connection ReadOnly 
	 * @return Connection
	 * @throws SQLException
	 */
	public Connection getReadOnlyConnection() throws SQLException {
		Connection c = getConnection();
		c.setReadOnly(true);
		return c;
	}
	/**
	 * Creates connection with the method {@link #getConnection(String,String)
	 * getConnection(user,password)} and makes the connection ReadOnly 
	 * @param user the database user
	 * @param password the user's password
	 * @return Connection
	 * @throws SQLException
	 */
	public Connection getReadOnlyConnection(String user,
		String password) throws SQLException {
		Connection c = getConnection(user, password);
		c.setReadOnly(true);
		return c;
	}
	/**
	 * Undoes all changes made in the current transaction of the specified
	 * connection
	 * @param con the connection for the current transaction
	 */
	public static void rollback(Connection con){
		try {
			con.rollback();
		}
		catch (SQLException ex) { }
	}
	/**
	 * Closes the specified connection
	 * @param con the connection to close
	 */
	public static void close(Connection con){
		try {
			if (con == null || con.isClosed()) { return;}
			con.rollback();
			con.close();
		}
		catch (SQLException e) {
			try {
				con.close();
			}
			catch (SQLException ex) {
				// comment throw new RuntimeException(ex);
				throw new RuntimeException(e);
			}
		}
	}

}
