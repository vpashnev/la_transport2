package com.logisticsalliance.sa;

import java.io.Serializable;
import java.sql.Timestamp;

import com.logisticsalliance.util.SupportTime;

class AlertItem implements Serializable {
	private static final long serialVersionUID = 10L;

	String status, reasonID, reason, comment;
	boolean exception;
	Timestamp ts;

	private StringBuilder getStringBuilder(boolean descr) {
		StringBuilder b = new StringBuilder(status.length()+
			reasonID.length()+reason.length()+comment.length()+20);
		b.append(status);
		b.append(' ');
		b.append(reasonID);
		if (descr) {
			b.append('('); b.append(reason); b.append(')');
			b.append(' '); b.append(':');
		}
		b.append(' ');
		b.append(comment);
		return b;
	}
	@Override
	public String toString() {
		StringBuilder b = getStringBuilder(true);
		b.append('\r'); b.append('\n');
		b.append(SupportTime.dd_MM_yyyy_HH_mm_Format.format(ts));
		return b.toString();
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AlertItem) {
			AlertItem a = (AlertItem)obj;
			if (status.equals(a.status) &&
				reasonID.equals(a.reasonID) &&
				comment.equals(a.comment)) {
				return true;
			}
		}
		return false;
	}
	@Override
	public int hashCode() {
		StringBuilder b = getStringBuilder(false);
		return b.toString().toUpperCase().hashCode();
	}
}
