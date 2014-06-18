package ksp.modmanager;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import ksp.modmanager.JModList.ModListModel;
import ksp.modmanager.api.ApiMod;

public class JModInstallList extends JModList {
	public JModInstallList(ModManager checker) {
		super(checker);
	}

	protected void setupButtons() {
		new ModButtonColumn(this, new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Must install mod");
			}
		}, 2);
	}

	protected TableModel createDefaultDataModel() {
		return new ModListInstallModel();
	}

	public class ModListInstallModel extends ModListModel {
		public ModListInstallModel() {
			columns = new String[] { "Mod Title", "Size", "Status" };
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			ApiMod mod = mods.get(rowIndex);
			if (mod == null)
				return null;

			switch (columnIndex) {
			case 0:
				return mod.getTitle();
			case 1:
				return mod.getHumanReadableSize();
			case 2:
				return "Install";
			}
			return null;
		}

		@Override
		public Class<?> getColumnClass(int column) {
			return String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			if (column == 2 && row >= 0 && row < getRowCount()
					&& !checker.isModInstalled(mods.get(row)))
				return true;

			return false;
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
		}

	}

	public class ModButtonColumn extends ButtonColumn {
		private JLabel alreadyInstalled = new JLabel("Already installed");

		public ModButtonColumn(JTable table, Action action, int column) {
			super(table, action, column);
			alreadyInstalled.setOpaque(true);
		}

		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
			if (!checker.isModInstalled(getModel().mods.get(row)))
				return super.getTableCellEditorComponent(table, value,
						isSelected, row, column);
			return alreadyInstalled;
		}

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			if (!checker.isModInstalled(getModel().mods.get(row)))
				return super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);

			if (isSelected) {
				alreadyInstalled.setForeground(table.getSelectionForeground());
				alreadyInstalled.setBackground(table.getSelectionBackground());
			} else {
				alreadyInstalled.setForeground(table.getForeground());
				alreadyInstalled.setBackground(table.getBackground());
			}

			return alreadyInstalled;
		}
	}
}
