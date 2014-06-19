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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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
import outlook.ManageContactsOutlook;
import webdav.ManageContactsWebDAV;

public class Userinterface {

    private final String confDir = "conf";
    private final String confPath = confDir + File.separator + "config.txt";

    private WebPasswordField passwordField;
    private WebTextField usernameField;
    private WebTextField textHostURL;
    private WebCheckBox insecureSSLBox;

    static private WebTextPane textPane;
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

            Status.printStatusToConsole("Start");

            //Build Addressbooks
            Contacts allContacts = new Contacts(strWorkingdir);

            //Get Outlook instance
            ManageContactsOutlook outlookContacts = new ManageContactsOutlook();

            String id = outlookContacts.openOutlook();
            if (id == null) {
                Status.printStatusToConsole("Outlook connection failed");
                return;
            }

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

            //Connect WebDAV
            ManageContactsWebDAV webDAVConnection = new ManageContactsWebDAV();
            webDAVConnection.connectHTTP(usernameField.getText().trim(),
                    String.valueOf(passwordField.getPassword()).trim(),
                    server, insecureSSLBox.isSelected());

            //Load WebDAV Contacts, if connection true proceed
            boolean loaded = webDAVConnection.loadContactsFromWebDav(fullPath, allContacts, strWorkingdir);
            if (!loaded) {
                Status.printStatusToConsole("WebDAV Connection not established");
                return;
            }

            //Load Outlook Contacts
            outlookContacts.loadContacts(allContacts, intOutlookFolder, strWorkingdir);

            //Compare and modify Contacts
            Status.printStatusToConsole("Compare Adressbooks");
            allContacts.compareAdressbooks();
            allContacts.printStatus();

            //Write Data
            outlookContacts.writeContacts(allContacts, intOutlookFolder, strWorkingdir);
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

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Userinterface window = new Userinterface();
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

final WebFrame frame = new WebFrame();
        //frame.setBounds(100, 100, 617, 445);
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

        WebLabel lblHost = new WebLabel("CardDAV calendar address:");
        textHostURL = new WebTextField();

        WebLabel lblUsername = new WebLabel("Username:");

        usernameField = new WebTextField();
        usernameField.setColumns(14);

        WebLabel lblPassword = new WebLabel("Password:");

        passwordField = new WebPasswordField();
        passwordField.setColumns(14);

        insecureSSLBox = new WebCheckBox("Allow insecure SSL");

        // log pane
        textPane = new WebTextPane();
        textPane.setEditable(false);
        WebScrollPane scrollPane = new WebScrollPane(textPane);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        docTextPane = textPane.getStyledDocument();
        //textPane.getCaret().
        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        //Load config
        Status.printStatusToConsole("Load Config");
        File file = new File(confPath);
        if (file.exists()) {
            try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                usernameField.setText(in.readLine());
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
                new Thread(syncWorker).start();
            }
        });

        WebLabel lblStatus = new WebLabel("Status:");

        WebPanel northPanel = new WebPanel();
        northPanel.setLayout(new GridLayout(0, 1, 0, 0));
        northPanel.add(lblHost);
        northPanel.add(textHostURL);
        WebPanel accountPanel = new WebPanel();
        accountPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        accountPanel.add(lblUsername);
        accountPanel.add(usernameField);
        accountPanel.add(lblPassword);
        accountPanel.add(passwordField);
        accountPanel.add(insecureSSLBox);
        northPanel.add(accountPanel);
        northPanel.add(btnSync);
        frame.add(northPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.getContentPane().setFocusTraversalPolicy(
                new FocusTraversalOnArray(
                        new Component[]{
                            usernameField,
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
                            usernameField,
                            passwordField,
                            textHostURL,
                            btnSync,
                            textPane,
                            lblStatus,
                            lblHost,
                            lblPassword,
                            frame.getContentPane(),
                            scrollPane,
                            lblUsername}
                )
        );

        frame.pack();
        frame.setVisible(true);
    }

    private void saveConfig() {
        new File(confDir).mkdir();
        File file = new File(confPath);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(usernameField.getText());
            writer.write(System.getProperty("line.separator"));
            writer.write(passwordField.getPassword());
            writer.write(System.getProperty("line.separator"));
            writer.write(textHostURL.getText());
            writer.write(System.getProperty("line.separator"));
            writer.write(Boolean.toString(insecureSSLBox.isSelected()));
            writer.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
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
