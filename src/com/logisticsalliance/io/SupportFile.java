package com.logisticsalliance.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.PropertyConfigurator;

/**
 * This class provides several helpful methods to access and manage files.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class SupportFile {

	/**
	 * Returns the file extension. If the file has no extension, the null is to be returned.
	 * @param f the file, that has extension to return;
	 * @return File extension or null for the file that has no extension.
	 */
	public static String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');
		if (i > 0 && i < s.length()-1) {
			ext = s.substring(i+1).toLowerCase();
		}
		return ext;
	}
	/**
	 * 
	 * Configures Log4j output for the property file in the given parent {@code folder}.
	 * @param folder the folder, where property file is placed.
	 * @param propFile the property file name
	 */
	public static void configureLog4j(File folder, String propFile) {
		String fp = new File(folder, propFile).getPath();
		PropertyConfigurator.configure(fp);
	}

	public static void zip(File zip, File[] fs) throws Exception {
		byte[] buffer = new byte[4096];
		FileOutputStream fos = new FileOutputStream(zip);
		ZipOutputStream zos = new ZipOutputStream(fos);
		try {
			for (int i = 0; i != fs.length; i++) {
				File f = fs[i];
				if (!f.exists()) { continue;}
				String fp = f.getPath();
				ZipEntry ze = new ZipEntry(fp.charAt(0) == '.' ? f.getName() : fp);
				zos.putNextEntry(ze);
				FileInputStream in = new FileInputStream(f);
				int n;
				while ((n = in.read(buffer)) != -1) {
					zos.write(buffer, 0, n);
				}
				in.close();
			}
		}
		finally {
			zos.closeEntry();
			zos.close();
		}
	}
}
