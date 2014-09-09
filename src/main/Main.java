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

import contact.Contacts;
import ezvcard.util.org.apache.commons.codec.binary.Hex;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import outlook.ManageOutlookContacts;
import utilities.Log;
import webdav.ManageWebDAVContacts;

/**
 *
 * @author Alexander Bikadorov <abiku@cs.tu-berlin.de>
 */
public class Main {

    private ServerSocket run = null;
    private Userinterface window;
    private Thread worker = null;

    Main() {
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
    }

    void run() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    window = new Userinterface(Main.this);
                    window.setVisible();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void shutdown() {
        try {
            this.run.close();
        } catch (IOException e2) {
            System.out.println("can't close socket");
            e2.printStackTrace();
        }
    }

    public void performSync(final String url,
            final boolean clearNumbers,
            final String region,
            final String username,
            final String password,
            final boolean insecureSSL,
            final boolean closeOutlook,
            final boolean initMode) {
        //Start Worker Thread for Update Text Area
        Runnable syncWorker = new Runnable() {
            @Override
            public void run() {
                int intOutlookFolder = 10;

                URL host;
                try {
                    host = new URL(url);
                } catch (MalformedURLException e) {
                    Status.print("Invalid host URL");
                    e.printStackTrace();
                    return;
                }
                String server = host.getProtocol() + "://" + host.getAuthority();
                String fullPath = server + "/" + host.getPath();

                if (clearNumbers) {
                    if (region.length() == 0) {
                        Status.print("Please set region code (two letter code)");
                        return;
                    }
                }

                // working dir
                String strWorkingdir = System.getProperty("user.dir");
                strWorkingdir = strWorkingdir + File.separator + "workingdir" + File.separator;
                new File(strWorkingdir).mkdir();

                // path to sync file
                String serverPart = host.getAuthority().replace(".", "&");
                byte[] hashBytes;
                try {
                     hashBytes = MessageDigest.getInstance("MD5").digest(host.getPath().getBytes());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return;
                }
                String hostPathHash = Hex.encodeHexString(hashBytes).substring(0, 8);
                String syncFilePath = strWorkingdir + "lastSync_" + serverPart + "_" + hostPathHash + ".txt";

                Status.print("Start");

                //Build Addressbooks
                Contacts allContacts = new Contacts(syncFilePath, region, clearNumbers);

                //Get Outlook instance
                ManageOutlookContacts outlookContacts = new ManageOutlookContacts(strWorkingdir, intOutlookFolder);
                boolean opened = outlookContacts.openOutlook();
                if (!opened) {
                    Status.print("Can't open Outlook");
                    return;
                }

                // TODO
                //outlookContacts.listContactFolders();
                //if (true) return;

                //Connect WebDAV
                ManageWebDAVContacts webDAVConnection = new ManageWebDAVContacts();
                webDAVConnection.connectHTTP(username,
                        password,
                        server,
                        insecureSSL);

                //Load WebDAV Contacts, if connection true proceed
                boolean loaded = webDAVConnection.loadContactsFromWebDav(fullPath, allContacts, strWorkingdir);
                if (!loaded) {
                    Status.print("Could not load WebDAV contacts");
                    outlookContacts.closeOutlook(closeOutlook);
                    return;
                }

                int contactNumberWebDAV = allContacts.numberOfContacts(Contacts.Addressbook.WEBDAVADDRESSBOOK);
                window.setContactNumbers(contactNumberWebDAV + " WebDAV");

                //Load Outlook Contacts
                outlookContacts.loadContentFromOutlook(allContacts);

                window.setContactNumbers(contactNumberWebDAV + " WebDAV / " + allContacts.numberOfContacts(Contacts.Addressbook.OUTLOOKADDRESSBOOK).toString() + " Outlook");

                //Compare and modify Contacts
                Status.print("Compare Adress Books");
                allContacts.compareAddressBooks(initMode);
                //allContacts.printStatus();

                //Write Data
                outlookContacts.writeOutlookObjects(allContacts);
                webDAVConnection.writeContacts(fullPath, allContacts);

                //Save last Sync Uids
                Status.print("Save last Sync UIDs");
                allContacts.saveUidsToFile();

                //Delete Tmp Contact Pictures
                allContacts.deleteTmpContactPictures();
                Status.print("Temporary Contact Pictures Files deleted");

                //Close
                outlookContacts.closeOutlook(closeOutlook);

                Status.print("End");
            }
        };

        if (worker != null && worker.isAlive()) {
            // already running
            return;
        }
        worker = new Thread(syncWorker);
        worker.start();
    }


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
        Main main = new Main();
        main.run();
    }
}
