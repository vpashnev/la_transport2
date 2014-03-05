package com.logisticsalliance.general;

import java.io.File;

import org.apache.log4j.Logger;

import com.glossium.net.ObjectResponse;

public class UserAuth {

	private static Logger log = Logger.getLogger(UserAuth.class);
	private static ObjectResponse res = new ObjectResponse() {

		@Override
		public void log(Exception e) throws Exception {
			log(e);
		}
		
	};
	static boolean ok;
	
	static void process(final File appDir, final String pwd,
		final int serverPort, final Object response) {
		if (pwd == null) { return;}
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					ok = true;
					File f = new File(appDir, "addFiles/la_keystore");
					res.communicate(f, pwd, serverPort, response);
				}
				catch (Exception e) {
					ok = false;
					log.error(e);
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
}
