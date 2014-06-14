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

import contact.Contacts;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;
import outlook.ManageContactsOutlook;
import webdav.ManageContactsWebDAV;

public class Userinterface {

    private JFrame frame;
    private JPasswordField passwordField;
    private JTextField usernameField;
    private JTextField textHostURL;

    static private JTextPane textPane;
    static private StyledDocument docTextPane;

    //Start Worker Thread for Update Text Area
    Runnable syncWorker = new Runnable() {
        @Override
        public void run() {
            boolean run = true;

            int intOutlookFolder = 10;

            String strWorkingdir = System.getProperty("user.dir");
            strWorkingdir = strWorkingdir + File.separator + "workingdir" + File.separator;

            while (run) {
                textPane.setText("");

                Status.printStatusToConsole("Start");

                //Build Addressbooks
                Contacts allContacts = new Contacts(strWorkingdir);

                //Get Outlook instance
                ManageContactsOutlook outlookContacts = new ManageContactsOutlook();
                
                String id = outlookContacts.openOutlook();
                if (id != null) {

                    URL host;
                    try {
                        host = new URL(textHostURL.getText().trim());
                    } catch (MalformedURLException e) {
                        Status.printStatusToConsole("Invalid host URL");
                        e.printStackTrace();
                        run = false;
                        continue;
                    }
                    String server = host.getAuthority();
                    String fullPath = server + "/" + host.getPath();
                    
                    //Connect WebDAV
                    ManageContactsWebDAV webDAVConnection = new ManageContactsWebDAV();
                    webDAVConnection.connectHTTP(usernameField.getText().trim(), 
                            String.valueOf(passwordField.getPassword()).trim(), 
                            server);

                    //Load WebDAV Contacts, if connection true proceed
                    if (webDAVConnection.loadContactsFromWebDav(fullPath, allContacts, strWorkingdir)) {

                        //Load Outlook Contacts
                        outlookContacts.loadContacts(allContacts, intOutlookFolder, strWorkingdir);

                        //Compare and modify Contacts
                        Status.printStatusToConsole("Compare Adressbooks");
                        allContacts.compareAdressbooks();
                        //allContacts.printStatus();

                        //Write Data
                        outlookContacts.writeContacts(allContacts, intOutlookFolder, strWorkingdir);
                        webDAVConnection.writeContacts(fullPath, allContacts);

                        //Save last Sync Uids
                        Status.printStatusToConsole("Save last Sync UIDs");
                        allContacts.saveUidsToFile(strWorkingdir);

                        //Delete Tmp Contact Pictures
                        allContacts.deleteTmpContactPictures();
                        Status.printStatusToConsole("Temporary Contact Pictures Files deleted");
                    } else {
                        Status.printStatusToConsole("WebDAV Connection not established");
                    }

                    //Close
                    outlookContacts.closeOutlook();
                } else {
                    Status.printStatusToConsole("Can't open Outlook");
                }

                Status.printStatusToConsole("End");

                run = false;
            }
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
        initialize();
    }

    private void saveConfig() {
        String confDir = "conf";
        new File(confDir).mkdir();
        File file = new File(confDir + File.separator + "config.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(usernameField.getText());
            writer.write(System.getProperty("line.separator"));
            writer.write(passwordField.getPassword());
            writer.write(System.getProperty("line.separator"));
            writer.write(textHostURL.getText());
            writer.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Calibri", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        
        docTextPane = textPane.getStyledDocument();

        //textPane.getCaret().
        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        frame = new JFrame();
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

        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(new Font("Calibri", Font.BOLD, 12));
        
        usernameField = new JTextField();
        usernameField.setFont(new Font("Calibri", Font.PLAIN, 12));
        usernameField.setColumns(14);
        
        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(new Font("Calibri", Font.BOLD, 12));

        passwordField = new JPasswordField();
        passwordField.setEchoChar('*');
        passwordField.setFont(new Font("Calibri", Font.PLAIN, 12));
        passwordField.setColumns(14);

        textHostURL = new JTextField();
        textHostURL.setFont(new Font("Calibri", Font.PLAIN, 12));

        JLabel lblHost = new JLabel("CardDAV calendar address:");
        lblHost.setFont(new Font("Calibri", Font.BOLD, 12));

        //Load config
        Status.printStatusToConsole("Load Config");
        File file = new File("conf\\config.txt");
        if (file.exists()) {
            try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                usernameField.setText(in.readLine());
                passwordField.setText(in.readLine());
                textHostURL.setText(in.readLine());
            } catch (IOException e1) {
                e1.printStackTrace();
            } 
        }

        JButton btnSync = new JButton("Start Synchronization");
        btnSync.setFont(new Font("Calibri", Font.PLAIN, 12));
        btnSync.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(syncWorker).start();
            }
        });

        JLabel lblStatus = new JLabel("Status:");
        lblStatus.setFont(new Font("Calibri", Font.BOLD, 12));

        Panel northPanel = new Panel();
        northPanel.setLayout(new GridLayout(0, 1, 0, 0));
        northPanel.add(lblHost);
        northPanel.add(textHostURL);
        Panel accountPanel = new Panel();
        accountPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        accountPanel.add(lblUsername);
        accountPanel.add(usernameField);
        accountPanel.add(lblPassword);
        accountPanel.add(passwordField);
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
