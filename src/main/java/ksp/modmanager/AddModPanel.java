package ksp.modmanager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import ksp.modmanager.api.ApiMod;
import ksp.modmanager.api.ModSearch;
import ksp.modmanager.api.SearchResult;
import ksp.modmanager.api.SearchUrl;

public class AddModPanel extends JPanel {
	private final Pattern CURSE_PATTERN = Pattern
			.compile(
					"^(?:http|www|kerbal|curse).+?/ksp-mods/(?:kerbal/)?(\\d+)",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
							| Pattern.MULTILINE);

	JTextField modname = new JTextField();
	JButton search = new JButton("Search");
	JModList modList;

	public AddModPanel(ModManager checker) {
		modList = new JModInstallList(checker);

		setLayout(new BorderLayout(5, 5));
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout(5, 5));

		Dimension size = modname.getPreferredSize();
		size.width = 200;
		modname.setPreferredSize(size);

		topPanel.add(modname, BorderLayout.CENTER);
		topPanel.add(search, BorderLayout.EAST);

		add(topPanel, BorderLayout.NORTH);
		add(new JScrollPane(modList), BorderLayout.CENTER);

		modname.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (search.isEnabled())
						search.doClick();
				}
			}
		});

		search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				search.setEnabled(false);
				String mod = modname.getText();
				Matcher matcher = CURSE_PATTERN.matcher(mod);

				if (matcher.find()) {
					searchMod(Long.parseLong(matcher.group(1)));
				} else { // Search api
					searchMod(mod);
				}
			}
		});

		try {
			String clipboard = (String) Toolkit.getDefaultToolkit()
					.getSystemClipboard().getData(DataFlavor.stringFlavor);
			Matcher matcher = CURSE_PATTERN.matcher(clipboard);
			if (matcher.find()) {
				modname.setText(clipboard);
				search.doClick();
			}
		} catch (HeadlessException | UnsupportedFlavorException | IOException e1) {
			e1.printStackTrace();
		}

	}

	private void searchMod(final String query) {
		new SwingWebWorker<SearchResult>(new SearchUrl(query),
				SearchResult.class) {

			@Override
			protected void done() {
				search.setEnabled(true);
				try {
					SearchResult result = get();
					List<ApiMod> mods = modList.getModel().getModList();
					mods.clear();
					mods.addAll(result);
					modList.getModel().fireTableDataChanged();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}.execute();
	}

	private void searchMod(long id) {
		new SwingWebWorker<ApiMod>(new ModSearch(id), ApiMod.class) {

			@Override
			protected void done() {
				search.setEnabled(true);
				try {
					ApiMod result = get();
					List<ApiMod> mods = modList.getModel().getModList();
					mods.clear();
					mods.add(result);
					modList.getModel().fireTableDataChanged();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}.execute();
	}
}