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

import contact.Contact;
import contact.Contacts;
import contact.Contacts.Addressbook;
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
import java.util.Date;
import java.util.List;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.httpclient.protocol.Protocol;
import outlook.ManageOutlookAppointments;
import outlook.ManageOutlookContacts;
import ui.Userinterface;
import utilities.Config;
import utilities.Log;
import webdav.EasySSLProtocolSocketFactory;
import webdav.ManageWebDAVContacts;

/**
 *
 * @author Alexander Bikadorov <abiku@cs.tu-berlin.de>
 */
public class Main {
    public static final String VERSION = "0.04";

    private ServerSocket run = null;
    private Userinterface window;
    private Thread worker = new Thread();

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

    void run(final boolean singleRun) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    window = new Userinterface(Main.this);
                    window.setVisible();
                    // TODO UI not visible during single run
                    if (singleRun) {
                        window.runAndShutDown();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void shutdown() {
        try {
            worker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            this.run.close();
        } catch (IOException e2) {
            System.out.println("can't close socket");
            e2.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * Sync a CardDAV address book (specified by URL) with a local Outlook
     * address book.
     */
    public void syncContacts(final String url,
            final boolean clearNumbers,
            final String region,
            final String username,
            final String password,
            final String outlookFolder,
            final boolean insecureSSL,
            final boolean closeOutlook,
            final boolean initMode) {
        //Start Worker Thread for Update Text Area
        Runnable syncWorker = new Runnable() {
            @Override
            public void run() {
                Userinterface.resetTextPane();

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

                if (clearNumbers && region.length() == 0) {
                    Status.print("Please set region code (two letter code)");
                    return;
                }

                String workingDir = getWorkingDir();

                // path to sync file
                String serverPart = host.getAuthority().replace(".", "&");
                MessageDigest md;
                try {
                    md = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return;
                }
                byte[] hashBytes = md.digest(host.getPath().getBytes());
                String hash = Hex.encodeHexString(hashBytes).substring(0, 8);
                String syncFilePath = workingDir + "lastSync_" + serverPart;
                if (!outlookFolder.isEmpty())
                    syncFilePath += "_" + outlookFolder;
                syncFilePath += "_" + hash + ".txt";

                Status.print("Starting contact synchronization");

                // initialize contact storage
                Contacts allContacts = new Contacts(syncFilePath, region, clearNumbers);

                //Connect WebDAV
                ManageWebDAVContacts webDAVConnection = new ManageWebDAVContacts();
                webDAVConnection.connectHTTP(username,
                        password,
                        server,
                        insecureSSL);

                //Load WebDAV Contacts, if connection true proceed
                boolean loaded = webDAVConnection.loadContactsFromWebDav(fullPath, allContacts, workingDir);
                if (!loaded) {
                    Status.print("Could not load WebDAV contacts");
                    return;
                }

                int contactNumberWebDAV = allContacts.numberOfContacts(Contacts.Addressbook.WEBDAVADDRESSBOOK);
                window.setContactNumbers(contactNumberWebDAV + " WebDAV");

                //Get Outlook instance for Contacts
                ManageOutlookContacts outlookManager = new ManageOutlookContacts(workingDir, outlookFolder);
                boolean opened = outlookManager.openOutlook();
                if (!opened) {
                    Status.print("Can't open Outlook");
                    return;
                }

                //Load Outlook Contacts
                List<Contact> outlookContacts = outlookManager.loadOutlookContacts();
                if (outlookContacts == null) {
                    Status.print("Can't load Outlook contacts");
                    outlookManager.closeOutlook(closeOutlook);
                    return;
                }
                for(Contact contact: outlookContacts)
                    allContacts.addContact(Addressbook.OUTLOOKADDRESSBOOK, contact);

                window.setContactNumbers(contactNumberWebDAV +
                        " WebDAV / " +
                        allContacts.numberOfContacts(Addressbook.OUTLOOKADDRESSBOOK).toString() +
                        " Outlook");

                //Compare and modify Contacts
                Status.print("Compare Adress Books");
                allContacts.compareAddressBooks(initMode);
                //allContacts.printStatus();

                //Write Data
                outlookManager.writeOutlookObjects(allContacts);
                webDAVConnection.writeContacts(fullPath, allContacts);

                //Save last Sync Uids
                Status.print("Save last Sync UIDs");
                allContacts.saveUidsToFile();

                //Delete Tmp Contact Pictures
                allContacts.deleteTmpContactPictures();
                Status.print("Temporary Contact Pictures Files deleted");

                //Close
                outlookManager.closeOutlook(closeOutlook);

                Status.print("End");
            }
        };

        this.runWorkerThreader(syncWorker);
    }

    /**
     * @author Swen Walkowski
     */
    public void syncAppointments() {
        Runnable syncWorker = new Runnable() {
            @Override
            public void run() {
                Userinterface.resetTextPane();

                String workingDir = getWorkingDir();

                // TODO Abfrage Sync Appointments
                Status.print("Sync appointments");

                //Get Outlook instance for Appointments
                ManageOutlookAppointments outlookAppointments = new ManageOutlookAppointments(workingDir);
                boolean opened = outlookAppointments.openOutlook();
                if (!opened) {
                    Status.print("Can't open Outlook");
                    return;
                }

                //Build Schedule
                //Appointments allAppointments = new Appointments();

                //Load outlook appointments
                //outlookAppointments.loadContentFromOutlook(allAppointments);

                //Close
                //outlookAppointments.closeOutlook(closeOutlook);

                String user = "xxx";
                String password = "xxx";

                URL url;
                try {
                    url = new URL("https", "192.168.178.20", "");
                    Protocol lEasyHttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
                    Protocol.registerProtocol("https", lEasyHttps);
        //            CalDavCalendarStore store = new CalDavCalendarStore("-//MacTI//WOCal//EN", url, PathResolver.GCAL);
        //            store.connect(user, password.toCharArray());
        //            CalDavCalendarCollection  collection = store.getCollection("/owncloud/remote.php/caldav/");
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        this.runWorkerThreader(syncWorker);
    }

    /**
     * @author Swen Walkowski
     */
    public void exportICAL() {
        Runnable syncWorker = new Runnable() {
            @Override
            public void run() {
                Userinterface.resetTextPane();

                // TODO Abfrage Save as iCal
                Status.print("Save Outlook as iCal");

                String strWorkingdir = getWorkingDir();

                //Get Outlook instance for Appointments
                ManageOutlookAppointments outlookAppointments = new ManageOutlookAppointments(strWorkingdir);
                boolean opened = outlookAppointments.openOutlook();
                if (!opened) {
                    Status.print("Can't open Outlook");
                    return;
                }

                //Save Outlook calender to iCal
                Date dateToday = new Date();
                Date dataLastMonth = new Date();
                dataLastMonth.setMonth(dateToday.getMonth()-1);
                outlookAppointments.saveAsICalender(strWorkingdir, dataLastMonth.toString(), dateToday.toString());
                Status.print("Export done");
            }
        };
        this.runWorkerThreader(syncWorker);
    }

    /**
     * Get a list of Outlook Contact Folders and add them to the userinterface.
     */
    public void listContactFolders() {
        //Start Worker Thread for Update Text Area
        Runnable syncWorker = new Runnable() {
            @Override
            public void run() {
                Userinterface.resetTextPane();

                //Get Outlook instance for Contacts
                ManageOutlookContacts outlookManager = new ManageOutlookContacts(getWorkingDir());
                boolean opened = outlookManager.openOutlook();
                if (!opened) {
                    Status.print("Can't open Outlook");
                    return;
                }

                List<String> contactFolders = outlookManager.getContactFolders();
                Userinterface.setContactFolderItems(contactFolders);
            }
        };
        this.runWorkerThreader(syncWorker);
    }

    private void runWorkerThreader(Runnable runnable) {
        if (worker.isAlive()) {
            // a thread is already already running
            return;
        }
        worker = new Thread(runnable);
        worker.start();
    }

    private static String getWorkingDir() {
        String strWorkingdir = System.getProperty("user.dir");
        strWorkingdir = strWorkingdir + File.separator + "workingdir" + File.separator;
        new File(strWorkingdir).mkdir();
        return strWorkingdir;
    }

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar CardDAVSyncOutlook.jar", options, true);
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

        // parse args
        Options options = new Options();
        options.addOption("h", "help", false, "show this help message");
        options.addOption("s", "singlerun", false, "single synchronization mode");
        options.addOption(OptionBuilder.withArgName("config_file")
                .hasArg()
                .withDescription("use given config file")
                .withLongOpt("config")
                .create("c")
        );

        CommandLineParser parser = new BasicParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("can't parse arguments: "+e.getMessage());
            showHelp(options);
            return;
        }
        if (cmd.hasOption("h")) {
            showHelp(options);
            return;
        }
        boolean singleRun = cmd.hasOption("s");
        if (cmd.hasOption("c")) {
            Config.setFile(cmd.getOptionValue("c"));
        }

        System.out.println("START - " +VERSION);

        Main main = new Main();
        main.run(singleRun);
    }
}
