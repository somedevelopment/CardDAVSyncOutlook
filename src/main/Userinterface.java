/*
 * Copyright (c) 2014 Swen Walkowski.
 * All rights reserved. Originator: Swen Walkowski.
 * Get more information about CardDAVSyncOutlook at http://sourceforge.net/projects/carddavsyncoutlook
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
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

    private WebFrame frame;
    private WebPasswordField passwordField;
    private WebTextField textUsername;
    private WebTextField urlField;
    private WebCheckBox insecureSSLBox;
    private WebLabel lblContactNumbers;
    private WebCheckBox savePasswordBox;
    private WebCheckBox initModeBox;

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

            Status.print("Start");

            //Build Addressbooks
            Contacts allContacts = new Contacts(strWorkingdir);

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
                outlookContacts.closeOutlook();
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
            outlookContacts.closeOutlook();

            Status.print("End");
        }
    };
    private WebLabel lblNumbersOfContacts;
    private JSeparator separator;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
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
            public void windowClosing(WindowEvent e){
                Userinterface.this.saveConfig();
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
                aboutPanel.add(new WebLabel("CardDAVSyncOutlook v0.03"));
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

        savePasswordBox = new WebCheckBox("Save Password");
        savePasswordBox.setFont(new Font("Calibri", Font.BOLD, 12));
        String tooltipText = "Save the password in configuration file as plaintext(!)";
        TooltipManager.addTooltip(savePasswordBox, tooltipText);

        insecureSSLBox = new WebCheckBox("Allow insecure SSL");
        insecureSSLBox.setFont(new Font("Calibri", Font.BOLD, 12));
        tooltipText = "Do not check the SSL certificate. Needed when the server uses a self-signed certifcate";
        TooltipManager.addTooltip(insecureSSLBox, tooltipText);

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

        initModeBox = new WebCheckBox("Initialization Mode");
        initModeBox.setFont(new Font("Calibri", Font.BOLD, 12));
        tooltipText = "Compare contacts by all fields. Useful on the first run";
        TooltipManager.addTooltip(initModeBox, tooltipText);

        WebLabel lblStatus = new WebLabel("Status:");

        lblNumbersOfContacts = new WebLabel("# of loaded Contacts:");
        lblNumbersOfContacts.setFont(new Font("Calibri", Font.BOLD, 12));
        lblContactNumbers = new WebLabel("");
        lblContactNumbers.setFont(new Font("Calibri", Font.PLAIN, 12));

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
        accountPanel.add(savePasswordBox);
        accountPanel.add(insecureSSLBox);
        northPanel.add(accountPanel);
        WebPanel numberPanel = new WebPanel();
        numberPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        numberPanel.add(lblNumbersOfContacts);
        numberPanel.add(lblContactNumbers);
        northPanel.add(numberPanel);
        WebPanel runPanel = new WebPanel();
        runPanel.add(btnSync, BorderLayout.CENTER);
        runPanel.add(initModeBox, BorderLayout.EAST);
        northPanel.add(runPanel);
        frame.getContentPane().add(northPanel, BorderLayout.NORTH);
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
        File file = new File("conf\\config.txt");
        if (file.exists()) {
            try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                textUsername.setText(in.readLine());
                passwordField.setText(in.readLine());
                urlField.setText(in.readLine());
                insecureSSLBox.setSelected(Boolean.valueOf(in.readLine()));
                savePasswordBox.setSelected(Boolean.valueOf(in.readLine()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void saveConfig() {
        String confDir = "conf";
        new File(confDir).mkdir();
        File file = new File(confDir + File.separator + "config.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(textUsername.getText());
            writer.write(System.getProperty("line.separator"));
            if (savePasswordBox.isSelected())
                writer.write(passwordField.getPassword());
            writer.write(System.getProperty("line.separator"));
            writer.write(urlField.getText());
            writer.write(System.getProperty("line.separator"));
            writer.write(Boolean.toString(insecureSSLBox.isSelected()));
            writer.write(System.getProperty("line.separator"));
            writer.write(Boolean.toString(savePasswordBox.isSelected()));
            writer.write(System.getProperty("line.separator"));

            writer.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
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
