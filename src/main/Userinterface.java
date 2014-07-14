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
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebPasswordField;
import com.alee.laf.text.WebTextField;
import com.alee.laf.text.WebTextPane;
import com.alee.managers.tooltip.TooltipManager;
import contact.Contacts;
import contact.Contacts.Addressbook;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;
import outlook.ManageOutlookContacts;
import webdav.ManageWebDAVContacts;

public class Userinterface {

    private static ServerSocket run = null;

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

    private Thread worker = null;

    static private WebTextPane textPane;
    static private WebScrollPane scrollPane;
    static private StyledDocument docTextPane;

    //Start Worker Thread for Update Text Area
    Runnable syncWorker = new Runnable() {
        @Override
        public void run() {
            int intOutlookFolder = 10;

            String strWorkingdir = System.getProperty("user.dir");
            strWorkingdir = strWorkingdir + File.separator + "workingdir" + File.separator;
            new File(strWorkingdir).mkdir();

            textPane.setText("");

            URL host;
            try {
                host = new URL(urlField.getText().trim());
            } catch (MalformedURLException e) {
                Status.print("Invalid host URL");
                e.printStackTrace();
                return;
            }
            String server = host.getProtocol() + "://" + host.getAuthority();
            String fullPath = server + "/" + host.getPath();
            
            if (clearNumbersBox.isSelected()) {
                if (txtRegion.getText().length() == 0) {
                    Status.print("Please set region code (two letter code)");
                    return;
                }
            }

            Status.print("Start");
            
            //Build Addressbooks
            Contacts allContacts = new Contacts(strWorkingdir, txtRegion.getText(), clearNumbersBox.isSelected());

            //Get Outlook instance
            ManageOutlookContacts outlookContacts = new ManageOutlookContacts(strWorkingdir, intOutlookFolder);
            boolean opened = outlookContacts.openOutlook();
            if (!opened) {
                Status.print("Can't open Outlook");
                return;
            }

            //Connect WebDAV
            ManageWebDAVContacts webDAVConnection = new ManageWebDAVContacts();
            webDAVConnection.connectHTTP(textUsername.getText().trim(),
                    String.valueOf(passwordField.getPassword()).trim(),
                    server,
                    insecureSSLBox.isSelected());

            //Load WebDAV Contacts, if connection true proceed
            boolean loaded = webDAVConnection.loadContactsFromWebDav(fullPath, allContacts, strWorkingdir);
            if (!loaded) {
                Status.print("Could not load WebDAV contacts");
                outlookContacts.closeOutlook(outlookCheckBox.isSelected());
                return;
            }

            lblContactNumbers.setText(allContacts.numberOfContacts(Addressbook.WEBDAVADDRESSBOOK).toString() + " WebDAV");

            //Load Outlook Contacts
            outlookContacts.loadContantFromOutlook(allContacts);

            lblContactNumbers.setText(lblContactNumbers.getText() + " / " + allContacts.numberOfContacts(Addressbook.OUTLOOKADDRESSBOOK).toString() + " Outlook");

            //Compare and modify Contacts
            Status.print("Compare Adress Books");
            allContacts.compareAddressBooks(initModeBox.isSelected());
            //allContacts.printStatus();

            //Write Data
            outlookContacts.writeOutlookObjects(allContacts);
            webDAVConnection.writeContacts(fullPath, allContacts);

            //Save last Sync Uids
            Status.print("Save last Sync UIDs");
            allContacts.saveUidsToFile(strWorkingdir);

            //Delete Tmp Contact Pictures
            allContacts.deleteTmpContactPictures();
            Status.print("Temporary Contact Pictures Files deleted");

            //Close
            outlookContacts.closeOutlook(outlookCheckBox.isSelected());

            Status.print("End");
        }
    };
    private WebLabel lblNumbersOfContacts;
    private JSeparator separator;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {

        // set up logging
        try {
            Log.init();
        } catch (IOException e) {
            System.out.println("can't set up logging");
            e.printStackTrace();
        }
        System.out.println("START");

        // check if already running
        try {
            InetAddress addr = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
            run = new ServerSocket(9872, 10, addr);
        } catch(java.net.BindException e){
            System.out.println("already running");
            System.exit(2);
        } catch(IOException e){
            System.out.println("can't create socket");
            e.printStackTrace();
        }

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Userinterface window = new Userinterface();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public Userinterface() {

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
                Userinterface.this.saveConfig();
                try {
                    Userinterface.run.close();
                } catch (IOException e2) {
                    System.out.println("can't close socket");
                    e2.printStackTrace();
                }
                frame.setVisible(false);
                frame.dispose();
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
                if (worker != null && worker.isAlive()) {
                    // already running
                    return;
                }
                worker = new Thread(syncWorker);
                worker.start();
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
        tooltipText = "Do not check the SSL certificate. Needed when the server uses a self-signed certifcate";
        TooltipManager.addTooltip(insecureSSLBox, tooltipText);

        JSeparator separator_1 = new JSeparator();
        optionPanel.add(separator_1);
        tooltipText = "Compare contacts by all fields. Useful on the first run";

        outlookCheckBox = new WebCheckBox("Close Outlook?");
        optionPanel.add(outlookCheckBox);
        outlookCheckBox.setText("Close Outlook?");
        outlookCheckBox.setFont(new Font("Calibri", Font.BOLD, 12));
        tooltipText = "Close Outlook after synchronization is finished.";
        TooltipManager.addTooltip(outlookCheckBox, tooltipText);
        
        WebPanel internationalNumberPanel = new WebPanel();
        northPanel.add(internationalNumberPanel);
        internationalNumberPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        
        WebLabel regionLabel = new WebLabel("Default region:");
        regionLabel.setText("Default region");
        internationalNumberPanel.add(regionLabel);
        regionLabel.setFont(new Font("Calibri", Font.BOLD, 12));
        
        txtRegion = new WebTextField();
        txtRegion.setInputPromptPosition(2);
        txtRegion.setInputPrompt("DE");
        internationalNumberPanel.add(txtRegion);
        txtRegion.setText("");
        txtRegion.setFont(new Font("Calibri", Font.PLAIN, 12));
        txtRegion.setColumns(2);
        
        JSeparator separator_3 = new JSeparator();
        internationalNumberPanel.add(separator_3);
        
        clearNumbersBox = new WebCheckBox("Number Correction?");
        internationalNumberPanel.add(clearNumbersBox);
        clearNumbersBox.setText("International number correction?");
        clearNumbersBox.setSelected(false);
        clearNumbersBox.setFont(new Font("Calibri", Font.BOLD, 12));
        tooltipText = "e.g. +49 89 1234567";
        TooltipManager.addTooltip(clearNumbersBox, tooltipText);
        
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
        TooltipManager.addTooltip(initModeBox, tooltipText);
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

    static public void setTextinTextPane(String strText) {
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
