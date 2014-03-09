package com.logisticsalliance.tt.web;

import java.io.Serializable;

public class Alert implements Serializable {
	private static final long serialVersionUID = 10L;

	String[] comm = new String[4];
	boolean[] cmdty = new boolean[7];
	String checkMsg;

	public Object clone() {
		Alert a = new Alert();
		for (int i = 0; i != comm.length; i++) {
			a.comm[i] = comm[i];
		}
		for (int i = 0; i != cmdty.length; i++) {
			a.cmdty[i] = cmdty[i];
		}
		return a;
	}
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
