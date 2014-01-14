package com.logisticsalliance.tt.web;

import java.io.Serializable;

public class Alert implements Serializable {
	private static final long serialVersionUID = 10L;

	String[] comm = new String[4];
	boolean[] cmdty = new boolean[6];
	String checkMsg;

	public String[] getComm() {
		return comm;
	}
	public boolean[] getCmdty() {
		return cmdty;
	}
	public String getCheckMsg() {
		return checkMsg;
	}

}
