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

import ksp.modmanager.ModManager;
import ksp.modmanager.ModManagerGui;

public class MainWindow extends JFrame {
	private ModManager modManager = new ModManager(); 
	
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

        ImageIcon settingsIcon = new ImageIcon(getClass().getResource("/res/settings2.png"));
        JButton settingsButton = new JButton("Settings", settingsIcon);
        
        
        
        toolbar.add(launchButton);
        toolbar.add(addButton);
        toolbar.add(updateButton);
        toolbar.add(settingsButton);

        add(toolbar, BorderLayout.NORTH);

        // mod list
        String[] columnNames = {"Mod Title", "Size", "Last Updated"};
        Object[][] data = {
                {"Space Stuff", "500mb", "1 day ago"},
                {"Weeeeee", "35mb", "Today"},
                {"More Space Things", "5mb", "3 weeks ago"},
                {"STAR TREK: THE MOD", "120mb", "2 days ago"},
                {"Yup", "260mb", "3 days ago"}
        };

        JTable table = new JTable(data, columnNames);

        // description box
        JPanel rightPane = new JPanel();
        //rightPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.PAGE_AXIS));

        String descriptionHeader = "<html><body width='150px'><b>Title</b>: MechJeb<br/><b>Author</b>: Jeb</body></html>";
        JLabel rightLabel = new JLabel(descriptionHeader);
        
        String descriptionContents = "By default, a text area does not wrap lines that are too long for the display area. Instead, it uses one line for all the text between newline characters and — if the text area is within a scroll pane — allows itself to be scrolled horizontally. This example turns line wrapping on with a call to the setLineWrap method and then calls the setWrapStyleWord method to indicate that the text area should wrap lines at word boundaries rather than at character boundaries. By default, a text area does not wrap lines that are too long for the display area. Instead, it uses one line for all the text between newline characters and — if the text area is within a scroll pane — allows itself to be scrolled horizontally. This example turns line wrapping on with a call to the setLineWrap method and then calls the setWrapStyleWord method to indicate that the text area should wrap lines at word boundaries rather than at character boundaries.";
        JTextArea message = new JTextArea(descriptionContents);

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
        pane.add(table, gbc);

        gbc.weightx = 0;
        gbc.weighty = 1.0;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        pane.add(rightPane, gbc);
        add(pane, BorderLayout.CENTER);
    }
}
