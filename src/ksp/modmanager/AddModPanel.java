package ksp.modmanager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class AddModPanel extends JPanel {
	private final Pattern CURSE_PATTERN = Pattern
			.compile(
					"^(?:http|www|kerbal|curse).+?/ksp-mods/(?:kerbal/)?(\\d+)",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
							| Pattern.MULTILINE);

	JTextField modname = new JTextField();
	JButton install = new JButton("Install");

	public AddModPanel() {
		setLayout(new BorderLayout(5, 5));
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout(5, 5));
		
		Dimension size = modname.getPreferredSize();
		size.width = 200;
		modname.setPreferredSize(size);
		
		topPanel.add(modname, BorderLayout.CENTER);
		topPanel.add(install, BorderLayout.EAST);
		
		ModList modList = new ModList();
		
		add(topPanel, BorderLayout.NORTH);
		add(new JScrollPane(modList), BorderLayout.CENTER);

		install.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String mod = modname.getText();
				Matcher matcher = CURSE_PATTERN.matcher(mod);

				if (matcher.find()) {
					System.out.println(matcher.group(1));
				} else { // Search api
					System.out.println(mod);
				}
			}
		});
	}
}