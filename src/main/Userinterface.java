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

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.Font;

import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;

import outlook.ManageContactsOutlook;
import webdav.ManageContactsWebDAV;
import contact.Contacts;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.Component;

public class Userinterface {

    private JFrame frame;
    private JPasswordField passwordField;
    private JTextField textUsername;
    private JTextField textHostURL;
    private JTextField textOwncloudURL;

    static private JTextPane textPane;
    static private JScrollPane scrollPane;
    static private StyledDocument docTextPane;
    private JButton btnSaveConfig;

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

                    //Connect WebDAV
                    ManageContactsWebDAV webDAVConnection = new ManageContactsWebDAV();
                    webDAVConnection.connectHTTP(textUsername.getText().trim(), 
                            String.valueOf(passwordField.getPassword()).trim(), 
                            textHostURL.getText().trim());

                    //Load WebDAV Contacts, if connection true proceed
                    if (webDAVConnection.loadContactsFromWebDav(textHostURL.getText().trim() + 
                            textOwncloudURL.getText().trim(), 
                            allContacts, strWorkingdir)) {

                        //Load Outlook Contacts
                        outlookContacts.loadContacts(allContacts, intOutlookFolder, strWorkingdir);

                        //Compare and modify Contacts
                        Status.printStatusToConsole("Compare Adressbooks");
                        allContacts.compareAdressbooks();
                        //allContacts.printStatus();

                        //Write Data
                        outlookContacts.writeContacts(allContacts, intOutlookFolder, strWorkingdir);
                        webDAVConnection.writeContacts(textHostURL.getText().trim() + textOwncloudURL.getText().trim(), allContacts);

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
                    System.err.println(e);
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

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Calibri", Font.PLAIN, 12));

        scrollPane = new JScrollPane(textPane);

        docTextPane = textPane.getStyledDocument();

        //textPane.getCaret().
        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        btnSaveConfig = new JButton("Save Configuration");
        btnSaveConfig.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Write Config File
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
                    writer.write(textOwncloudURL.getText());

                    writer.flush();
                } catch (IOException e1) {
                    System.err.println(e1);
                }
                Status.printStatusToConsole("Config Saved");

            }
        });
        btnSaveConfig.setFont(new Font("Calibri", Font.PLAIN, 12));

        frame = new JFrame();
        frame.setBounds(100, 100, 617, 445);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(new Font("Calibri", Font.BOLD, 12));

        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(new Font("Calibri", Font.BOLD, 12));

        passwordField = new JPasswordField();
        passwordField.setEchoChar('*');
        passwordField.setFont(new Font("Calibri", Font.PLAIN, 12));

        textUsername = new JTextField();
        textUsername.setFont(new Font("Calibri", Font.PLAIN, 12));
        textUsername.setColumns(10);

        textHostURL = new JTextField();
        textHostURL.setFont(new Font("Calibri", Font.PLAIN, 12));
        textHostURL.setColumns(10);

        JLabel lblHost = new JLabel("Host URL:");
        lblHost.setFont(new Font("Calibri", Font.BOLD, 12));

        textOwncloudURL = new JTextField();
        textOwncloudURL.setFont(new Font("Calibri", Font.PLAIN, 12));
        textOwncloudURL.setColumns(10);

        JLabel lblOwncloudUrl = new JLabel("Owncloud URL:");
        lblOwncloudUrl.setFont(new Font("Calibri", Font.BOLD, 12));

        //Load config
        Status.printStatusToConsole("Load Config");

        File file = new File("conf\\config.txt");

        if (file.exists()) {
            try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                textUsername.setText(in.readLine());
                passwordField.setText(in.readLine());
                textHostURL.setText(in.readLine());
                textOwncloudURL.setText(in.readLine());
            } catch (IOException e1) {
                System.err.println(e1);
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

        GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
        groupLayout.setHorizontalGroup(
                groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                        .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                .addGroup(groupLayout.createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                                .addComponent(lblOwncloudUrl, GroupLayout.PREFERRED_SIZE, 96, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(lblHost, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE))
                                        .addGap(10)
                                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                                .addComponent(textHostURL, 459, 459, 459)
                                                .addComponent(textOwncloudURL, GroupLayout.PREFERRED_SIZE, 459, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnSaveConfig, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 144, GroupLayout.PREFERRED_SIZE)))
                                .addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
                                        .addGap(18)
                                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                                .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 565, GroupLayout.PREFERRED_SIZE)
                                                .addGroup(groupLayout.createSequentialGroup()
                                                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                                                .addComponent(lblUsername)
                                                                .addGroup(groupLayout.createSequentialGroup()
                                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                                        .addComponent(lblPassword))
                                                                .addComponent(lblStatus, GroupLayout.PREFERRED_SIZE, 96, GroupLayout.PREFERRED_SIZE))
                                                        .addGap(10)
                                                        .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                                                .addComponent(textUsername, GroupLayout.PREFERRED_SIZE, 267, GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, 267, GroupLayout.PREFERRED_SIZE))
                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                        .addComponent(btnSync, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                        .addContainerGap(18, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
                groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addGroup(groupLayout.createSequentialGroup()
                                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                                .addGroup(groupLayout.createSequentialGroup()
                                                        .addContainerGap()
                                                        .addComponent(textUsername, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addGroup(groupLayout.createSequentialGroup()
                                                        .addGap(14)
                                                        .addComponent(lblUsername)))
                                        .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                                .addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                        .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
                                                        .addGap(9)
                                                        .addComponent(lblPassword))))
                                .addGroup(groupLayout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(btnSync, GroupLayout.PREFERRED_SIZE, 52, GroupLayout.PREFERRED_SIZE)
                                        .addGap(60)))
                        .addGap(31)
                        .addComponent(lblStatus, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 222, GroupLayout.PREFERRED_SIZE)
                        .addGap(10))
                .addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
                        .addContainerGap(67, Short.MAX_VALUE)
                        .addGroup(groupLayout.createSequentialGroup()
                                .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                        .addGroup(groupLayout.createSequentialGroup()
                                                .addComponent(textHostURL, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
                                                .addGap(3))
                                        .addGroup(groupLayout.createSequentialGroup()
                                                .addGap(3)
                                                .addComponent(lblHost, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)
                                                .addGap(6)))
                                .addGap(3)
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                        .addComponent(textOwncloudURL, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblOwncloudUrl, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(btnSaveConfig)
                        .addGap(259))
        );
        frame.getContentPane().setLayout(groupLayout);
        frame.getContentPane().setFocusTraversalPolicy(
                new FocusTraversalOnArray(
                        new Component[]{
                            textUsername, 
                            passwordField, 
                            textHostURL, 
                            textOwncloudURL, 
                            btnSync, 
                            btnSaveConfig, 
                            textPane,
                            scrollPane, 
                            lblStatus, 
                            lblOwncloudUrl, 
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
                            textOwncloudURL,
                            btnSync, 
                            btnSaveConfig, 
                            textPane, 
                            lblStatus, 
                            lblOwncloudUrl, 
                            lblHost, 
                            lblPassword, 
                            frame.getContentPane(), 
                            scrollPane, 
                            lblUsername}
                )
        );
    }

    static public void setTextinTextPane(String strText) {
        try {
            if (docTextPane.getLength() > 0) {
                docTextPane.insertString(docTextPane.getLength(), strText + "\n", null);
            } else {
                docTextPane.insertString(0, strText + "\n", null);
            }
        } catch (BadLocationException e) {
            System.err.println(e);
        }
    }
}
