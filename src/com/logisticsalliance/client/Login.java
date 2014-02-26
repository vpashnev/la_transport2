package com.logisticsalliance.client;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.logisticsalliance.net.SupportNet;
import com.logisticsalliance.sqla.ConnectFactory;
import com.logisticsalliance.ui.swg.SupportUI;

public class Login {

	static ConnectFactory connectFactory;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String[] login = new String[2];
		if (args.length > 2) {
			login[0] = args[2];
		}
		SupportUI.setDefaultFontSize(14);
		showDialog(login);
		connectFactory = SupportNet.getConnectFactory(args[0], args[1], login);
		connectFactory.getConnection().close();
		JOptionPane.showMessageDialog(null, "SSL and database connections successfull !");
		System.exit(0);
	}
	private static void showDialog(String[] login) {
		JTextField uf = new JTextField(20);
		uf.setText(login[0]);
		JPasswordField pf = new JPasswordField(20);
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.FIRST_LINE_START;
		gbc.insets.left = 5; gbc.insets.bottom = 5;
		gbc.gridx = 0; gbc.gridy = 0;
		p.add(new JLabel("User"), gbc);
		gbc.gridx = 1; gbc.gridy = 0;
		p.add(uf, gbc);
		gbc.gridx = 0; gbc.gridy = 1;
		p.add(new JLabel("Password"), gbc);
		gbc.gridx = 1; gbc.gridy = 1;
		p.add(pf, gbc);
		JOptionPane op = new JOptionPane(p);
		op.setOptions(new String[]{"OK", "Cancel"});
		JDialog dlg = op.createDialog(null, "Login");
		dlg.pack();
		int i = SupportUI.show(dlg, op);
		if (i != 0) {
			System.exit(0);
		}
		login[0] = uf.getText().trim();
		login[1] = new String(pf.getPassword());
	}
}
