package com.logisticsalliance.sn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

import org.apache.log4j.Logger;

import com.logisticsalliance.util.SupportTime;

public class Notify1 {

	public static void updateNotifyEndingTime(Connection con1, String sql,
		Timestamp t) throws Exception {
		PreparedStatement timeSt = con1.prepareStatement(sql);
		timeSt.setTimestamp(1, t);
		timeSt.executeUpdate();
		con1.commit();
		timeSt.close();
	}
	public static Timestamp getNextTime(Connection con, String selSql, String insSql,
		Timestamp t0, Timestamp t1, Timestamp nextTime, long period, long timeAheadInMins,
		Logger log) throws Exception {
		PreparedStatement st = con.prepareStatement(selSql);
		ResultSet rs = st.executeQuery();
		if (rs.next()) {
			if (t0 == null) { t0 = rs.getTimestamp(1);}
			st.close();
		}
		else {
			st.close();
			st = con.prepareStatement(insSql);
			st.setNull(1, Types.TIMESTAMP);
			st.executeUpdate();
			con.commit();
			st.close();
		}
		if (t0 == null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(t1);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			t0 = new Timestamp(cal.getTime().getTime()+timeAheadInMins*60000);
			return t0;
		}
		else {
			long mins = (t0.getTime() - t1.getTime())/60000;
			if (mins < timeAheadInMins) { // 24-30 hours
				t0.setTime(t0.getTime()+period); // plus 30 or 15 minutes
				return t0;
			}
			if (!t0.equals(nextTime)) {
				log.debug("NEXT "+SupportTime.dd_MM_yyyy_HH_mm_Format.format(t0)+"\r\n");
				nextTime.setTime(t0.getTime());
			}
		}
		return null;
	}
}
