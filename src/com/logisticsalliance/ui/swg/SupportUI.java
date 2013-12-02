package com.logisticsalliance.ui.swg;

import java.util.Enumeration;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * This class provides several helpful methods to manage GUI components.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class SupportUI {

	public static int show(JDialog dlg, JOptionPane op) {
		dlg.setVisible(true);
		Object selectedValue = op.getValue();
		if (selectedValue != null) {
			Object[] opts = op.getOptions();
			for (int i = 0; i < opts.length; i++) {
				if(opts[i].equals(selectedValue)) { return i;}
			}
		}
		return JOptionPane.CLOSED_OPTION;
	}
	public static void setDefaultFontSize(float size){
		Enumeration<?> ks = UIManager.getDefaults().keys();
		while (ks.hasMoreElements()) {
			Object k = ks.nextElement();
			Object v = UIManager.get(k);
			if (v instanceof FontUIResource) {
				FontUIResource f = (FontUIResource)v;
				UIManager.put(k, f.deriveFont(size));
			}
		}
	} 
}
