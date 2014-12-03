/*
 * Copyright (c) 2014 Swen Walkowski.
 * All rights reserved. Originator: Swen Walkowski.
 * Get more information about CardDAVSyncOutlook at https://github.com/somedevelopment/CardDAVSyncOutlook/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package main;

import com.alee.extended.label.WebLinkLabel;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.button.WebButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuBar;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebPasswordField;
import com.alee.laf.text.WebTextField;
import com.alee.laf.text.WebTextPane;
import com.alee.managers.tooltip.TooltipManager;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;
import utilities.Config;

//TODO split up user interface for Outlook, DAV and general configuration information
public class Userinterface {

    private Main control;

    private WebFrame frame;
    private WebPasswordField passwordField;
    private WebTextField textUsername;
    private WebTextField urlField;
    private WebCheckBox insecureSSLBox;
    private WebLabel lblContactNumbers;
    private WebCheckBox savePasswordBox;
    private WebCheckBox initModeBox;
    private WebCheckBox outlookCheckBox;
    private WebCheckBox clearNumbersBox;
    private WebTextField txtRegion;
    private WebCheckBox syncOutlookCheckBox;
    private WebCheckBox iCalCheckBox;

    static private WebTextPane textPane;
    static private WebScrollPane scrollPane;
    static private StyledDocument docTextPane;
    private WebLabel lblNumbersOfContacts;
    private JSeparator separator;
    private TrayIcon trayIcon;

    /**
     * Create the application.
     */
    public Userinterface(final Main main) {

        control = main;

        WebLookAndFeel.install();

        textPane = new WebTextPane();
        textPane.setFont(new Font("Calibri", Font.PLAIN, 12));
        textPane.setEditable(false);

        scrollPane = new WebScrollPane(textPane);
        scrollPane.setDarkBorder(Color.LIGHT_GRAY);
        scrollPane.setBorderColor(Color.LIGHT_GRAY);

        docTextPane = textPane.getStyledDocument();

        //textPane.getCaret().
        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        frame = new WebFrame();
        frame.setBounds(100, 100, 630, 430);
        frame.setResizable(false);
        frame.setTitle("CardDAVSyncOutlook");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Userinterface.this.shutdown();
            }
        });

        // menu
        WebMenuBar menubar = new WebMenuBar();

        WebMenu fileMenu = new WebMenu("File");
        WebMenuItem exitMenuItem = new WebMenuItem("Exit");
        exitMenuItem.setToolTipText("Exit application");
        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        });
        fileMenu.add(exitMenuItem);
        menubar.add(fileMenu);

        WebMenu helpMenu = new WebMenu("Help");
        WebMenuItem aboutMenuItem = new WebMenuItem("About");
        aboutMenuItem.setToolTipText("About...");
        aboutMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                WebPanel aboutPanel = new WebPanel();
                aboutPanel.add(new WebLabel("CardDAVSyncOutlook v0.04 (Beta)"));
                WebLinkLabel linkLabel = new WebLinkLabel();
                linkLabel.setLink("https://github.com/somedevelopment/CardDAVSyncOutlook/");
                linkLabel.setText("Visit the developer site");
                aboutPanel.add(linkLabel, BorderLayout.SOUTH);
                WebOptionPane.showMessageDialog(frame,
                        aboutPanel,
                        "About",
                        WebOptionPane.INFORMATION_MESSAGE);
            }
        });
        helpMenu.add(aboutMenuItem);
        menubar.add(helpMenu);

        frame.setJMenuBar(menubar);

        WebLabel lblHost = new WebLabel("CardDAV calendar address: ");
        lblHost.setVerticalAlignment(SwingConstants.BOTTOM);
        lblHost.setMargin(new Insets(0, 3, 0, 0));
        lblHost.setFont(new Font("Calibri", Font.BOLD, 12));

        urlField = new WebTextField();
       // urlField.addMouseListener(new MouseAdapter() {
        	//@Override
        	//public void mouseExited(MouseEvent arg0) {
        	//	if (urlField.getText().trim().startsWith("https://")) {
        	//		insecureSSLBox.setSelected(true);
        	//		urlField.setText("http" + urlField.getText().substring(5));
        	//		Status.print("Activated insecure SSL");
        	//	}
        	//}
       // });
        urlField.setFont(new Font("Calibri", Font.PLAIN, 12));
        //textHostURL.setColumns(10)
        urlField.setInputPrompt("http://<server-name>/owncloud/remote.php/carddav/addressbooks/<user_name>/<addr_book_name>");
        urlField.setHideInputPromptOnFocus(false);

        WebLabel lblUsername = new WebLabel("Username:");
        lblUsername.setFont(new Font("Calibri", Font.BOLD, 12));

        textUsername = new WebTextField();
        textUsername.setFont(new Font("Calibri", Font.PLAIN, 12));
        textUsername.setColumns(10);

        WebLabel lblPassword = new WebLabel("Password:");
        lblPassword.setFont(new Font("Calibri", Font.BOLD, 12));

        passwordField = new WebPasswordField();
        passwordField.setColumns(10);
        passwordField.setFont(new Font("Calibri", Font.PLAIN, 11));
        passwordField.setEchoChar('*');

        WebButton btnSync = new WebButton("Start Synchronization");
        btnSync.setFont(new Font("Calibri", Font.BOLD, 12));
        btnSync.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Userinterface.this.callSync();
            }
        });

        WebLabel lblStatus = new WebLabel("Status:");

        // layout
        WebPanel northPanel = new WebPanel();
        northPanel.setBorderColor(Color.LIGHT_GRAY);
        northPanel.setMargin(new Insets(0, 5, 0, 5));
        northPanel.setLayout(new GridLayout(0, 1, 0, 0));
        northPanel.add(lblHost);
        northPanel.add(urlField);
        WebPanel accountPanel = new WebPanel();
        accountPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        accountPanel.add(lblUsername);
        accountPanel.add(textUsername);
        accountPanel.add(lblPassword);
        accountPanel.add(passwordField);

        separator = new JSeparator();
        accountPanel.add(separator);
        northPanel.add(accountPanel);

        savePasswordBox = new WebCheckBox("Save Password");
        accountPanel.add(savePasswordBox);
        savePasswordBox.setFont(new Font("Calibri", Font.BOLD, 12));
        String tooltipText = "Save the password in configuration file as plaintext(!)";
        TooltipManager.addTooltip(savePasswordBox, tooltipText);

        WebPanel optionPanel = new WebPanel();
        northPanel.add(optionPanel);
        optionPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

        insecureSSLBox = new WebCheckBox("Allow insecure SSL");
        optionPanel.add(insecureSSLBox);
        insecureSSLBox.setFont(new Font("Calibri", Font.BOLD, 12));
        tooltipText = "Do not check the SSL certificate. Needed when the server uses a self-signed certificate";
        TooltipManager.addTooltip(insecureSSLBox, tooltipText);

        JSeparator separator_1 = new JSeparator();
        optionPanel.add(separator_1);

        outlookCheckBox = new WebCheckBox("Close Outlook?");
        optionPanel.add(outlookCheckBox);
        outlookCheckBox.setText("Close Outlook?");
        outlookCheckBox.setFont(new Font("Calibri", Font.BOLD, 12));
        tooltipText = "Close Outlook after synchronization is finished.";
        TooltipManager.addTooltip(outlookCheckBox, tooltipText);
        
        WebPanel webPanel = new WebPanel();
        northPanel.add(webPanel);
        webPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        
        WebLabel webLabel = new WebLabel("Default region:");
        webLabel.setText("Default region");
        webLabel.setFont(new Font("Calibri", Font.BOLD, 12));
        webPanel.add(webLabel);
        
        txtRegion = new WebTextField();
        txtRegion.setText("");
        txtRegion.setInputPromptPosition(2);
        txtRegion.setInputPrompt("DE");
        txtRegion.setFont(new Font("Calibri", Font.PLAIN, 12));
        txtRegion.setColumns(2);
        webPanel.add(txtRegion);
        
        JSeparator separator_4 = new JSeparator();
        webPanel.add(separator_4);
        
        clearNumbersBox = new WebCheckBox("Number Correction?");
        clearNumbersBox.setText("International number correction?");
        clearNumbersBox.setSelected(false);
        clearNumbersBox.setFont(new Font("Calibri", Font.BOLD, 12));
        webPanel.add(clearNumbersBox);

        WebPanel internationalNumberPanel = new WebPanel();
        northPanel.add(internationalNumberPanel);
        internationalNumberPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        
        syncOutlookCheckBox = new WebCheckBox("Sync Contacts");
        syncOutlookCheckBox.setSelected(false);
        syncOutlookCheckBox.setFont(new Font("Calibri", Font.BOLD, 12));
        internationalNumberPanel.add(syncOutlookCheckBox);
        tooltipText = "Check if you want to sync your outlook contacts with your webdav installation.";
        TooltipManager.addTooltip(syncOutlookCheckBox, tooltipText);

        JSeparator separator_3 = new JSeparator();
        internationalNumberPanel.add(separator_3);
        
        iCalCheckBox= new WebCheckBox("Export Outlook calender to iCAL");
        iCalCheckBox.setSelected(false);
        iCalCheckBox.setFont(new Font("Calibri", Font.BOLD, 12));
        internationalNumberPanel.add(iCalCheckBox);
        tooltipText = "Check if you want to exort your outlook calender to your file system (iCAL format; to date - one month";
        TooltipManager.addTooltip(iCalCheckBox, tooltipText);

        WebPanel runPanel = new WebPanel();
        runPanel.add(btnSync, BorderLayout.CENTER);
        northPanel.add(runPanel);
        frame.getContentPane().add(northPanel, BorderLayout.NORTH);

        WebPanel southPanel = new WebPanel();
        frame.getContentPane().add(southPanel, BorderLayout.SOUTH);
        southPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

        lblNumbersOfContacts = new WebLabel("# of loaded Contacts:");
        southPanel.add(lblNumbersOfContacts);
        lblNumbersOfContacts.setFont(new Font("Calibri", Font.BOLD, 12));

        JSeparator separator_5 = new JSeparator();
        southPanel.add(separator_5);
        lblContactNumbers = new WebLabel("");
        southPanel.add(lblContactNumbers);
        lblContactNumbers.setFont(new Font("Calibri", Font.PLAIN, 12));
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        frame.getContentPane().setFocusTraversalPolicy(
                new FocusTraversalOnArray(
                        new Component[]{
                            textUsername,
                            passwordField,
                            urlField,
                            btnSync,
                            textPane,
                            scrollPane,
                            lblStatus,
                            lblHost,
                            lblPassword,
                            lblUsername
                        }
                )
        );
        frame.setFocusTraversalPolicy(
                new FocusTraversalOnArray(
                        new Component[]{
                            textUsername,
                            passwordField,
                            urlField,
                            btnSync,
                            textPane,
                            lblStatus,
                            lblHost,
                            lblPassword,
                            frame.getContentPane(),
                            scrollPane,
                            lblUsername
                        }
                )
        );

        //Load config
        Status.print("Load Config");
        Config config = Config.getInstance();
        textUsername.setText(config.getString(Config.ACC_USER, ""));
        passwordField.setText(config.getString(Config.ACC_PASS, ""));
        urlField.setText(config.getString(Config.ACC_URL, ""));
        insecureSSLBox.setSelected(config.getBoolean(Config.ACC_SSL, false));
        savePasswordBox.setSelected(config.getBoolean(Config.ACC_SAVE_PASS, false));
        outlookCheckBox.setSelected(config.getBoolean(Config.GLOB_CLOSE, false));

        JSeparator separator_2 = new JSeparator();
        optionPanel.add(separator_2);

        initModeBox = new WebCheckBox("Initialization Mode");
        optionPanel.add(initModeBox);
        initModeBox.setFont(new Font("Calibri", Font.BOLD, 12));
        tooltipText = "Compare contacts by all fields. Useful on the first run";
        TooltipManager.addTooltip(initModeBox, tooltipText);

        this.setTray();
    }

    public void setVisible() {
        frame.setVisible(true);
    }

    public void setContactNumbers(String text) {
        lblContactNumbers.setText(text);
    }

    private void callSync() {
        textPane.setText("");
        String url = urlField.getText().trim();
        boolean clearNumbers = clearNumbersBox.isSelected();
        String region = txtRegion.getText();
        String username = textUsername.getText().trim();
        String password = String.valueOf(passwordField.getPassword()).trim();
        boolean insecureSSL = insecureSSLBox.isSelected();
        boolean closeOutlook = outlookCheckBox.isSelected();
        boolean initMode = initModeBox.isSelected();
        boolean syncContacts = syncOutlookCheckBox.isSelected();
        boolean exportICAL = iCalCheckBox.isSelected();
        control.performSync(url, clearNumbers, region, username, password, insecureSSL, closeOutlook, initMode, syncContacts, exportICAL);
    }

    private void saveConfig() {
        Config config = Config.getInstance();
        config.setProperty(Config.ACC_USER, textUsername.getText());
        if (savePasswordBox.isSelected())
            config.setProperty(Config.ACC_PASS, new String(passwordField.getPassword()));
        config.setProperty(Config.ACC_URL, urlField.getText());
        config.setProperty(Config.ACC_SSL, insecureSSLBox.isSelected());
        config.setProperty(Config.ACC_SAVE_PASS, savePasswordBox.isSelected());
        config.setProperty(Config.GLOB_CLOSE, outlookCheckBox.isSelected());

        config.saveToFile();
        Status.print("Config Saved");
    }

    private void toggleState() {
        if (frame.getState() == Frame.NORMAL) {
            frame.setState(Frame.ICONIFIED);
            frame.setVisible(false);
        } else {
            frame.setState(Frame.NORMAL);
            frame.setVisible(true);
        }
    }

    void shutdown() {
        this.saveConfig();
        frame.setVisible(false);
        frame.dispose();
        control.shutdown();
    }

    private void setTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("tray icon not supported");
            return;
        }

        if (trayIcon != null)
            // already set
            return;

        // load image
        Image image = new BufferedImage(22, 22, Image.SCALE_SMOOTH);
        //image = image.getScaledInstance(22, 22, Image.SCALE_SMOOTH);

        // TODO popup menu
        final WebPopupMenu popup = new WebPopupMenu();
        WebMenuItem syncItem = new WebMenuItem("Sync");
        syncItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Userinterface.this.callSync();
            }
        });
        popup.add(syncItem);
        WebMenuItem quitItem = new WebMenuItem("Quit");
        quitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Userinterface.this.shutdown();
            }
        });
        popup.add(quitItem);

        // create an action listener to listen for default action executed on the tray icon
        MouseListener listener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                check(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1)
                    Userinterface.this.toggleState();
                else
                    check(e);
            }
            private void check(MouseEvent e) {
                if (!e.isPopupTrigger())
                    return;

                // TODO ugly
                popup.setLocation(e.getX() - 20, e.getY() - 40);
                popup.setInvoker(popup);
                popup.setVisible(true);
            }
        };

        trayIcon = new TrayIcon(image, "CardDAVSyncOutlook" /*, popup*/);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(listener);

        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    static public void setTextInTextPane(String strText) {
        try {
            if (docTextPane.getLength() > 0) {
                docTextPane.insertString(docTextPane.getLength(), strText + "\n", null);
            } else {
                docTextPane.insertString(0, strText + "\n", null);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
