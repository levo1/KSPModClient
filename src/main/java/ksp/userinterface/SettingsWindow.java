package ksp.userinterface;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import ksp.modmanager.Config;
import ksp.modmanager.ModManager;

public class SettingsWindow extends JFrame {
	private String kspLocation;
	private ModManager modManager;
    private JLabel kspLoc;

    public SettingsWindow() {
    	kspLocation = Config.get.getKspDirectory();
    	modManager = new ModManager();
        initUI();
    }

	private void initUI() {
        setLayout(new GridBagLayout());
        setTitle("Settings");
        setSize(600, 200);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        JButton resetLoc = new JButton("Change KSP Directory");
        
        kspLoc = new JLabel(new String("Current KSP Location: " + kspLocation));
        
        resetLoc.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				kspLocation = modManager.manualKspPath(false, kspLocation);
				Config.get.setKspDirectory(kspLocation).save();
				kspLoc.setText("Current KSP Location: " + kspLocation);
			}
		});
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.gridheight = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(kspLoc, gbc);

        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        add(resetLoc, gbc);
	}
}
