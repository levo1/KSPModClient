package ksp.modmanager;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import ksp.modmanager.api.ApiMod;

public class JModList extends JTable {
	protected ModInfoChecker checker;

	public JModList(ModInfoChecker checker) {
		this.checker = checker;
		setupButtons();
	}

	protected void setupButtons() {
		new ModButtonColumn(this, new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Must update");
			}
		}, 3);
	}

	protected TableModel createDefaultDataModel() {
		return new ModListModel();
	}

	@Override
	public void setModel(TableModel model) {
		if (model != null && !(model instanceof ModListModel)) {
			throw new IllegalArgumentException("Not a ModListModel");
		}
		super.setModel(model);
	}

	@Override
	public ModListModel getModel() {
		return (ModListModel) super.getModel();
	}

	public class ModListModel extends AbstractTableModel {
		protected String[] columns = new String[] { "Enabled", "Mod Title",
				"Size", "Status" };
		protected List<ApiMod> mods = new ArrayList<>();

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			ApiMod mod = mods.get(rowIndex);
			if (mod == null)
				return null;

			switch (columnIndex) {
			case 0:
				return mod.enabled;
			case 1:
				return mod.getTitle();
			case 2:
				return mod.getHumanReadableSize();
			case 3:
				return "Update";
			}
			return null;
		}

		public void add(ApiMod mod) {
			int rowIndex = this.mods.size();
			this.mods.add(mod);
			fireTableRowsInserted(rowIndex, rowIndex);
		}

		@Override
		public String getColumnName(int column) {
			return columns[column];
		}

		@Override
		public Class<?> getColumnClass(int column) {
			if (column == 0) {
				return Boolean.class;
			}

			return String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			if (column == 0)
				return true;
			if (column == 3 && checker.isUpdateAvailable(mods.get(row)))
				return true;

			return false;
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (column != 0 || row < 0 || row >= getRowCount())
				return;

			mods.get(row).enabled = (Boolean) value;
		}

		@Override
		public int getRowCount() {
			return mods.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}
	}
	
	public class ModButtonColumn extends ButtonColumn {
		private JLabel upToDate = new JLabel("Up to date");

		public ModButtonColumn(JTable table, Action action, int column) {
			super(table, action, column);
			upToDate.setOpaque(true);
		}

		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
			if (checker.isUpdateAvailable(getModel().mods.get(row)))
				return super.getTableCellEditorComponent(table, value,
						isSelected, row, column);
			return upToDate;
		}

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			if (checker.isUpdateAvailable(getModel().mods.get(row)))
				return super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);

			if (isSelected) {
				upToDate.setForeground(table.getSelectionForeground());
				upToDate.setBackground(table.getSelectionBackground());
			} else {
				upToDate.setForeground(table.getForeground());
				upToDate.setBackground(table.getBackground());
			}

			return upToDate;
		}
	}
}
