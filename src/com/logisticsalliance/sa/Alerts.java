package com.logisticsalliance.sa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

public class Alerts implements Serializable {
	private static final long serialVersionUID = 10L;

	public int pallets;
	public ArrayList<AlertItem> items = new ArrayList<AlertItem>(2);
	HashSet<AlertItem> hset = new HashSet<AlertItem>(2, .5f);

	void updateItems() {
		items.addAll(hset);
		Collections.sort(items);
		hset = null;
	}
	public String toString() {
		StringBuilder sb = new StringBuilder(64);
		for (Iterator<AlertItem> it = items.iterator(); it.hasNext();) {
			AlertItem v = it.next();
			sb.append(v.toString());
			sb.append('\r'); sb.append('\n');
		}
		return sb.toString();
	}
}
