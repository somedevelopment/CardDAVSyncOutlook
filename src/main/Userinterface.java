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
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import java.awt.Font;

import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;

import outlook.ManageOutlookContacts;
import webdav.ManageContactsWebDAV;
import contact.Contacts;
import contact.Contacts.Addressbook;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import net.miginfocom.swing.MigLayout;
import javax.swing.SwingConstants;

public class Userinterface {

    private JFrame frame;
    private JPasswordField passwordField;
    private JTextField textUsername;
    private JTextField textHostURL;

    private JLabel lblContactNumbers;

    static private JTextPane textPane;
    static private JScrollPane scrollPane;
    static private StyledDocument docTextPane;

    //Start Worker Thread for Update Text Area
    Runnable syncWorker = new Runnable() {
        @Override
        public void run() {
            boolean run = true;

            int intOutlookFolder = 10;

            String strWorkingdir = System.getProperty("user.dir");
            strWorkingdir = strWorkingdir + File.separator + "workingdir" + File.separator;
            new File(strWorkingdir).mkdir();

            while (run) {
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
                if (outlookContacts.openOutlook()) {

                    //Connect WebDAV
                    ManageContactsWebDAV webDAVConnection = new ManageContactsWebDAV();
                    webDAVConnection.connectHTTP(textUsername.getText().trim(), String.valueOf(passwordField.getPassword()).trim(), server);

                    //Load WebDAV Contacts, if connection true proceed
                    if (webDAVConnection.loadContactsFromWebDav(fullPath, allContacts, strWorkingdir)) {

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
    private JLabel lblNumbersOfContacts;

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
        try {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(textUsername.getText());
                writer.write(System.getProperty("line.separator"));
                writer.write(passwordField.getPassword());
                writer.write(System.getProperty("line.separator"));
                writer.write(textHostURL.getText());
                writer.write(System.getProperty("line.separator"));

                writer.flush();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        Status.printStatusToConsole("Config Saved");

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

        frame = new JFrame();
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

        //Load config
        Status.printStatusToConsole("Load Config");
        try {
            File file = new File("conf\\config.txt");

            if (file.exists()) {
                try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                    textUsername.setText(in.readLine());
                    passwordField.setText(in.readLine());
                    textHostURL.setText(in.readLine());
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
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
        lblStatus.setVerticalAlignment(SwingConstants.BOTTOM);
        lblStatus.setHorizontalAlignment(SwingConstants.LEFT);
        lblStatus.setFont(new Font("Calibri", Font.BOLD, 12));
        frame.getContentPane().setLayout(new MigLayout("", "[96px][250px][250px][250px]", "[22px][22px][22px][22px][22px][22px][222px]"));
        frame.getContentPane().add(lblHost, "cell 0 2,alignx left,aligny center");
        frame.getContentPane().add(textHostURL, "cell 1 2 3 1,growx,aligny top");

        lblNumbersOfContacts = new JLabel("# of loaded Contacts:");
        lblNumbersOfContacts.setFont(new Font("Calibri", Font.BOLD, 12));
        frame.getContentPane().add(lblNumbersOfContacts, "cell 0 4");

        lblContactNumbers = new JLabel("");
        lblContactNumbers.setFont(new Font("Calibri", Font.PLAIN, 12));
        frame.getContentPane().add(lblContactNumbers, "cell 1 4 2 1,grow");
        frame.getContentPane().add(scrollPane, "cell 0 6 4 1,grow");
        frame.getContentPane().add(lblUsername, "cell 0 0,alignx left,aligny center");
        frame.getContentPane().add(lblPassword, "cell 0 1,alignx left,aligny center");
        frame.getContentPane().add(lblStatus, "cell 0 5,growx,aligny top");
        frame.getContentPane().add(textUsername, "cell 1 0 2 1,growx,aligny top");
        frame.getContentPane().add(passwordField, "cell 1 1 2 1,growx,aligny top");
        frame.getContentPane().add(btnSync, "cell 3 0 1 2,grow");

        frame.getContentPane().setFocusTraversalPolicy(new FocusTraversalOnArray(new Component[]{textUsername, passwordField, textHostURL, btnSync, textPane, scrollPane, lblStatus, lblHost, lblPassword, lblUsername}));
        frame.setFocusTraversalPolicy(new FocusTraversalOnArray(new Component[]{textUsername, passwordField, textHostURL, btnSync, textPane, lblStatus, lblHost, lblPassword, frame.getContentPane(), scrollPane, lblUsername}));
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
