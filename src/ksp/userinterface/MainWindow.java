package ksp.userinterface;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;

import ksp.modmanager.Config;
import ksp.modmanager.JModList;
import ksp.modmanager.ModManager;
import ksp.modmanager.ModManagerGui;
import ksp.modmanager.SwingWebWorker;
import ksp.modmanager.api.ApiMod;
import ksp.modmanager.api.SearchResult;
import ksp.modmanager.api.SearchUrl;

public class MainWindow extends JFrame {
	private ModManager modManager = new ModManager();
    private JTextArea message = new JTextArea(new String("By default, a text area does not wrap lines that are too long for the display area. Instead, "
            + "it uses one line for all the text between newline characters and � if the text area is within a scroll pane � allows "
            + "itself to be scrolled horizontally. This example turns line wrapping on with a call to the setLineWrap method and then "
            + "calls the setWrapStyleWord method to indicate that the text area should wrap lines at word boundaries rather than at "
            + "character boundaries. By default, a text area does not wrap lines that are too long for the display area. Instead, "
            + "it uses one line for all the text between newline characters and � if the text area is within a scroll pane � allows "
            + "itself to be scrolled horizontally. This example turns line wrapping on with a call to the setLineWrap method and "
            + "then calls the setWrapStyleWord method to indicate that the text area should wrap lines at word boundaries rather "
            + "than at character boundaries."));
    private JLabel rightLabel = new JLabel(new String("<html><body width='150px'><b>Title</b>: <br/><b>Author</b>: </body></html>"));

	private void setModDescription(String title, String author, String description) {
        message.setText(description);
	    rightLabel.setText("<html><body width='150px'><b>Title</b>: "
	            + title + "<br/><b>Author</b>: "
	            + author + "</body></html>");
	}

    public MainWindow() throws IOException, BadLocationException {
        initUI();
    }

    private void initUI() throws IOException, BadLocationException {



    	try {
    	    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    	} catch (Exception e) {
    	    e.printStackTrace();
    	}

        setLayout(new BorderLayout());
        setTitle("KSP Mod Manager");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // toolbar

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        ImageIcon launchIcon = new ImageIcon(getClass().getResource("/res/laptop41.png"));
        JButton launchButton = new JButton("Launch", launchIcon);

        launchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		        try {
					Process p = Runtime.getRuntime().exec(Config.get.getKspDirectory() + "/KSP.exe");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});

        ImageIcon addIcon = new ImageIcon(getClass().getResource("/res/add11.png"));
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
						new ModManagerGui().setVisible(true);
					}
				});
			}
		});



        ImageIcon updateIcon = new ImageIcon(getClass().getResource("/res/low27.png"));
        JButton updateButton = new JButton("Update Mods", updateIcon);


        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        setModDescription("Title!", "Rob", "Description goes in here");
                    }
                });
            }
        });

        ImageIcon settingsIcon = new ImageIcon(getClass().getResource("/res/settings2.png"));
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

        // mod list
        final JModList table = new JModList(modManager);
        new SwingWebWorker<SearchResult>(new SearchUrl("mech"), SearchResult.class) {

			@Override
			protected void done() {
				try {
					for(ApiMod mod : get()) {
						table.getModel().add(mod);
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}.execute();

        // description box
        JPanel rightPane = new JPanel();
        //rightPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
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
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
                );

        rightPane.add(rightLabel);
        rightPane.add(Box.createRigidArea(new Dimension(0,5)));
        rightPane.add(jsp);
        rightPane.add(Box.createRigidArea(new Dimension(0,5)));



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
}
