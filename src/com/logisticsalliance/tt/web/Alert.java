package com.logisticsalliance.tt.web;

import java.io.Serializable;

class Alert implements Serializable {
	private static final long serialVersionUID = 10L;

	String commId, checkMsg;
	boolean[] cmdty = new boolean[6];
	int noteType;

}
