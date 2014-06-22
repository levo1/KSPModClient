package ksp.userinterface;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;

import ksp.modmanager.Config;
import ksp.modmanager.JModList;
import ksp.modmanager.ModManager;
import ksp.modmanager.ModManager.ModEvent;
import ksp.modmanager.ModManager.ModEventListener;
import ksp.modmanager.ModManagerGui;
import ksp.modmanager.Start;
import ksp.modmanager.api.ApiMod;

public class MainWindow extends JFrame implements ModEventListener {
	private ModManager modManager = new ModManager();
	private JTextArea message = new JTextArea(new String(""));
	private JLabel rightLabel = new JLabel(
			new String(
					"<html><body width='150px'><b>Title</b>: <br/><b>Author</b>: </body></html>"));
	private JModList table;

	private void setModDescription(String title, String author,
			String description) {
		message.setText(description);
		rightLabel.setText("<html><body width='150px'><b>Title</b>: " + title
				+ "<br/><b>Author</b>: " + author + "</body></html>");
	}

	public MainWindow() throws IOException, BadLocationException {
		initUI();
		modManager.addListener(this);
	}

	private void initUI() throws IOException, BadLocationException {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		setLayout(new BorderLayout());
		setTitle("KSP Mod Manager - " + Start.getVersion());
		setSize(800, 600);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		// toolbar

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);

		ImageIcon launchIcon = new ImageIcon(getClass().getResource(
				"/res/laptop41.png"));
		JButton launchButton = new JButton("Launch", launchIcon);

		launchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// try {
				// Process p = Runtime.getRuntime().exec(
				// Config.get.getKspDirectory() + "/KSP");
				// } catch (IOException e1) {
				// // TODO Auto-generated catch block
				// e1.printStackTrace();
				// }

				try {
					File kspBinary = new File(Config.get.getKspDirectory(),
							"KSP");
					File kspWinBinary = new File(Config.get.getKspDirectory(),
							"KSP.exe");
					File kspMacBinary = new File(Config.get.getKspDirectory(),
							"KSP.app");
					if (kspMacBinary.exists())
						kspBinary = kspMacBinary;
					else if (kspWinBinary.exists())
						kspBinary = kspWinBinary;
					if (kspBinary.exists())
						Desktop.getDesktop().open(kspBinary);
					else
						JOptionPane
								.showMessageDialog(
										MainWindow.this,
										"Could not launch KSP! Please report this on the issue tracker, and be sure to mention your operating system and KSP directory layout");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		ImageIcon addIcon = new ImageIcon(getClass().getResource(
				"/res/add11.png"));
		JButton addButton = new JButton("Install Mod", addIcon);

		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						try {
							UIManager.setLookAndFeel(UIManager
									.getSystemLookAndFeelClassName());
						} catch (Throwable t) {

						}
						new ModManagerGui(modManager).setVisible(true);
					}
				});
			}
		});

		ImageIcon updateIcon = new ImageIcon(getClass().getResource(
				"/res/low27.png"));
		JButton updateButton = new JButton("Update Mods", updateIcon);

		updateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						setModDescription("Title!", "Rob",
								"Description goes in here");
					}
				});
			}
		});

		ImageIcon settingsIcon = new ImageIcon(getClass().getResource(
				"/res/settings2.png"));
		JButton settingsButton = new JButton("Settings", settingsIcon);

		settingsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new SettingsWindow().setVisible(true);
			}
		});

		toolbar.add(launchButton);
		toolbar.add(addButton);
		toolbar.add(updateButton);
		toolbar.add(settingsButton);

		add(toolbar, BorderLayout.NORTH);

		table = new JModList(modManager);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		table.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						System.out.println(e.toString());
						if (table.getSelectedRow() >= 0) {
							int row = table.getSelectedRow();
							ApiMod mod = table
									.getModel()
									.getModList()
									.get(table.getRowSorter()
											.convertRowIndexToModel(row));
							setModDescription(mod.getTitle(), mod.getAuthor(),
									mod.getDescription());
						}

					}
				});

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int r = table.rowAtPoint(e.getPoint());
				if (r >= 0 && r < table.getRowCount()) {
					table.setRowSelectionInterval(r, r);
				} else {
					table.clearSelection();
				}

				int rowindex = table.getSelectedRow();
				if (rowindex < 0)
					return;

				ApiMod mod = table.getModel().getModList()
						.get(table.getRowSorter().convertRowIndexToModel(r));

				if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
					JPopupMenu popup = createContextMenuFor(mod);
					popup.show(e.getComponent(), e.getX(), e.getY());
				}

			}
		});

		doRefreshModList();

		// description box
		JPanel rightPane = new JPanel();
		// rightPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.PAGE_AXIS));

		message.setWrapStyleWord(true);
		message.setLineWrap(true);
		message.setEditable(false);
		message.setFocusable(false);
		message.setOpaque(false);
		message.setPreferredSize(new Dimension(150, 0));
		Font font = new Font("Verdana", Font.PLAIN, 12);
		message.setFont(font);

		JScrollPane jsp = new JScrollPane(message,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		rightPane.add(rightLabel);
		rightPane.add(Box.createRigidArea(new Dimension(0, 5)));
		rightPane.add(jsp);
		rightPane.add(Box.createRigidArea(new Dimension(0, 5)));

		// gridlayout
		JPanel pane = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = GridBagConstraints.RELATIVE;
		gbc.gridheight = GridBagConstraints.RELATIVE;
		gbc.insets = new Insets(10, 10, 10, 10);

		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		pane.add(new JScrollPane(table), gbc);

		gbc.weightx = 0;
		gbc.weighty = 1.0;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.NORTH;
		pane.add(rightPane, gbc);
		add(pane, BorderLayout.CENTER);
	}

	private JPopupMenu createContextMenuFor(final ApiMod mod) {
		JPopupMenu menu = new JPopupMenu();

		JMenuItem visitSite = new JMenuItem("View on Curseforge");
		visitSite.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					java.awt.Desktop.getDesktop().browse(
							new URI(mod.getPageUrl()));
				} catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
		});

		menu.add(visitSite);

		JMenuItem uninstall = new JMenuItem("Uninstall");
		uninstall.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					modManager.uninstallMod(mod);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		menu.add(uninstall);
		return menu;
	}

	@Override
	public void onModEvent(ModEvent event) {
		doRefreshModList();
	}

	private void doRefreshModList() {
		List<ApiMod> mods = table.getModel().getModList();
		mods.clear();
		try {
			mods.addAll(modManager.getInstalledMods());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		table.getModel().fireTableDataChanged();
	}
}
