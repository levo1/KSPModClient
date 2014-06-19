package ksp.modmanager;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import ksp.modmanager.api.ApiMod;

public class ModManagerGui extends JFrame {
	private ModManager modManager;
	
	public ModManagerGui(ModManager manager) {
		this.modManager = manager;
		setTitle("KSP Mod Manager");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(5, 5));

		add(new AddModPanel(modManager), BorderLayout.CENTER);

		pack();
	}
}
