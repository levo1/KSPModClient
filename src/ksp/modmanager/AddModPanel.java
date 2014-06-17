package ksp.modmanager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import ksp.modmanager.api.ApiMod;
import ksp.modmanager.api.SearchResult;
import ksp.modmanager.api.SearchUrl;

public class AddModPanel extends JPanel {
	private final Pattern CURSE_PATTERN = Pattern
			.compile(
					"^(?:http|www|kerbal|curse).+?/ksp-mods/(?:kerbal/)?(\\d+)",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
							| Pattern.MULTILINE);

	JTextField modname = new JTextField();
	JButton install = new JButton("Search");
	JModList modList = new JModInstallList(new ModInfoChecker() {
		
		@Override
		public boolean isModInstalled(ApiMod mod) {
			return true;
		}

		@Override
		public boolean isModEnabled(ApiMod mod) {
			return true;
		}

		@Override
		public boolean isUpdateAvailable(ApiMod mod) {
			return true;
		}
	});

	public AddModPanel() {
		setLayout(new BorderLayout(5, 5));
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout(5, 5));

		Dimension size = modname.getPreferredSize();
		size.width = 200;
		modname.setPreferredSize(size);

		topPanel.add(modname, BorderLayout.CENTER);
		topPanel.add(install, BorderLayout.EAST);

		add(topPanel, BorderLayout.NORTH);
		add(new JScrollPane(modList), BorderLayout.CENTER);

		install.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				install.setEnabled(false);
				String mod = modname.getText();
				Matcher matcher = CURSE_PATTERN.matcher(mod);

				if (matcher.find()) {
					searchMod(Integer.parseInt(matcher.group(1)));
					System.out.println(matcher.group(1));
				} else { // Search api
					searchMod(mod);
				}
			}
		});
	}

	private void searchMod(final String query) {
		System.out.println("Start");
		new SwingWebWorker<SearchResult>(new SearchUrl(query), SearchResult.class) {

			@Override
			protected void done() {
				install.setEnabled(true);
				try {
					SearchResult result = get();
					for(ApiMod mod : result) {
						modList.getModel().add(mod);
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}.execute();
	}

	private void searchMod(int id) {

	}
}