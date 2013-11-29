package com.logisticsalliance.general;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.apache.log4j.Logger;

public class UserAuth {

	private static Logger log = Logger.getLogger(UserAuth.class);
	
	static void process(final File appDir, final String pwd,
		final int serverPort, final Object response) {
		if (pwd == null) { return;}
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					process1(appDir, pwd, serverPort, response);
				}
				catch (Exception e) {
					log.error(e);
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
	private static void process1(File appDir, String pwd,
		int serverPort, Object response) throws Exception {
		File f = new File(appDir, "la_keystore");
		System.setProperty("javax.net.ssl.keyStore", f.getPath());  
		System.setProperty("javax.net.ssl.keyStorePassword", pwd);  
		SSLServerSocketFactory sf = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
		SSLServerSocket srv = (SSLServerSocket)sf.createServerSocket(serverPort);
		try {
			while (true) {
				SSLSocket clt = (SSLSocket)srv.accept();
				try {
					BufferedReader in = new BufferedReader(new InputStreamReader(clt.getInputStream()));
					String v = in.readLine();
					v = v+"";
					ObjectOutputStream out = new ObjectOutputStream(clt.getOutputStream());
					out.writeObject(response);
					Thread.sleep(100);
				}
				catch (Exception e) {
					log.error(e);
				}
				finally {
					clt.close();
				}
			}
		}
		finally {
			srv.close();
		}
	}
}
