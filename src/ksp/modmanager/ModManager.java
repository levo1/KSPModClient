package ksp.modmanager;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class ModManager extends JFrame {

	public ModManager() {
		setTitle("KSP Mod Manager");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout(5, 5));

//		JTable table = new JTable() {
//			public boolean isCellEditable(int nRow, int nCol) {
//				return true;
//			}
//		};
//		table.setAutoCreateRowSorter(true);
//		DefaultTableModel contactTableModel = (DefaultTableModel) table
//				.getModel();
//		contactTableModel.setColumnIdentifiers(new String[] { "Enabled",
//				"Mod Title", "Size", "Status" });
//		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		add(new AddModPanel(), BorderLayout.CENTER);
//		add(new JScrollPane(table), BorderLayout.CENTER);

		pack();
	}
}
