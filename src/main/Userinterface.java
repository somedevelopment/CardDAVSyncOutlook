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

import com.alee.laf.WebLookAndFeel;
import com.alee.laf.button.WebButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebPasswordField;
import com.alee.laf.text.WebTextField;
import com.alee.laf.text.WebTextPane;
import contact.Contacts;
import contact.Contacts.Addressbook;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;
import outlook.ManageOutlookContacts;
import webdav.ManageContactsWebDAV;

public class Userinterface {

    private WebFrame frame;
    private WebPasswordField passwordField;
    private WebTextField textUsername;
    private WebTextField textHostURL;
    private WebCheckBox insecureSSLBox;
    private WebLabel lblContactNumbers;

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
                host = new URL(textHostURL.getText().trim());
            } catch (MalformedURLException e) {
                Status.printStatusToConsole("Invalid host URL");
                e.printStackTrace();
                return;
            }
            String server = host.getProtocol() + "://" + host.getAuthority();
            String fullPath = server + "/" + host.getPath();

            Status.printStatusToConsole("Start");

            //Build Addressbooks
            Contacts allContacts = new Contacts(strWorkingdir);

            //Get Outlook instance
            ManageOutlookContacts outlookContacts = new ManageOutlookContacts(strWorkingdir, intOutlookFolder);
            boolean opened = outlookContacts.openOutlook();
            if (!opened) {
                Status.printStatusToConsole("Can't open Outlook");
                return;
            }

            //Connect WebDAV
            ManageContactsWebDAV webDAVConnection = new ManageContactsWebDAV();
            webDAVConnection.connectHTTP(textUsername.getText().trim(),
                    String.valueOf(passwordField.getPassword()).trim(),
                    server,
                    insecureSSLBox.isSelected());

            //Load WebDAV Contacts, if connection true proceed
            boolean loaded = webDAVConnection.loadContactsFromWebDav(fullPath, allContacts, strWorkingdir);
            if (!loaded) {
                Status.printStatusToConsole("Could not load WebDAV contacts");
                outlookContacts.closeOutlook();
                return;
            }

            lblContactNumbers.setText(allContacts.numberOfContacts(Addressbook.WEBDAVADDRESSBOOK).toString() + " WebDAV");

            //Load Outlook Contacts
            outlookContacts.loadContantFromOutlook(allContacts);

            lblContactNumbers.setText(lblContactNumbers.getText() + " / " + allContacts.numberOfContacts(Addressbook.OUTLOOKADDRESSBOOK).toString() + " Outlook");

            //Compare and modify Contacts
            Status.printStatusToConsole("Compare Adressbooks");
            allContacts.compareAdressbooks();
            //allContacts.printStatus();

            //Write Data
            outlookContacts.writeOutlookObjects(allContacts);
            webDAVConnection.writeContacts(fullPath, allContacts);

            //Save last Sync Uids
            Status.printStatusToConsole("Save last Sync UIDs");
            allContacts.saveUidsToFile(strWorkingdir);

            //Delete Tmp Contact Pictures
            allContacts.deleteTmpContactPictures();
            Status.printStatusToConsole("Temporary Contact Pictures Files deleted");

            //Close
            outlookContacts.closeOutlook();

            Status.printStatusToConsole("End");
        }
    };
    private WebLabel lblNumbersOfContacts;

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
        textPane.setEditable(false);

        scrollPane = new WebScrollPane(textPane);

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

        WebLabel lblUsername = new WebLabel("Username:");

        textUsername = new WebTextField();
        textUsername.setColumns(10);

        WebLabel lblPassword = new WebLabel("Password:");

        passwordField = new WebPasswordField();
        passwordField.setEchoChar('*');

        insecureSSLBox = new WebCheckBox("Allow insecure SSL");

        textHostURL = new WebTextField();
        textHostURL.setColumns(10);

        WebLabel lblHost = new WebLabel("Host URL:");

        //Load config
        Status.printStatusToConsole("Load Config");

        File file = new File("conf\\config.txt");

        if (file.exists()) {
            try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                textUsername.setText(in.readLine());
                passwordField.setText(in.readLine());
                textHostURL.setText(in.readLine());
                insecureSSLBox.setSelected(Boolean.valueOf(in.readLine()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        WebButton btnSync = new WebButton("Start Synchronization");
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

        lblNumbersOfContacts = new WebLabel("# of loaded Contacts:");
        lblContactNumbers = new WebLabel("");

        WebPanel northPanel = new WebPanel();
        northPanel.setLayout(new GridLayout(0, 1, 0, 0));
        northPanel.add(lblHost);
        northPanel.add(textHostURL);
        WebPanel accountPanel = new WebPanel();
        accountPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        accountPanel.add(lblUsername);
        accountPanel.add(textUsername);
        accountPanel.add(lblPassword);
        accountPanel.add(passwordField);
        accountPanel.add(insecureSSLBox);
        northPanel.add(accountPanel);
        WebPanel numberPanel = new WebPanel();
        numberPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        numberPanel.add(lblNumbersOfContacts);
        numberPanel.add(lblContactNumbers);
        northPanel.add(numberPanel);
        northPanel.add(btnSync);
        frame.add(northPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.getContentPane().setFocusTraversalPolicy(
                new FocusTraversalOnArray(
                        new Component[]{
                            textUsername,
                            passwordField,
                            textHostURL,
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
                            textHostURL,
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

    }

    private void saveConfig() {
        String confDir = "conf";
        new File(confDir).mkdir();
        File file = new File(confDir + File.separator + "config.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(textUsername.getText());
            writer.write(System.getProperty("line.separator"));
            writer.write(passwordField.getPassword());
            writer.write(System.getProperty("line.separator"));
            writer.write(textHostURL.getText());
            writer.write(System.getProperty("line.separator"));
            writer.write(Boolean.toString(insecureSSLBox.isSelected()));
            writer.write(System.getProperty("line.separator"));

            writer.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        } 
        Status.printStatusToConsole("Config Saved");

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
