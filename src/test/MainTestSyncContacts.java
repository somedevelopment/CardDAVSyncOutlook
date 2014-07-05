///*
// * Copyright (c) 2014 Swen Walkowski.
// * All rights reserved. Originator: Swen Walkowski.
// * Get more information about CardDAVSyncOutlook at http://sourceforge.net/projects/carddavsyncoutlook
// *
// * This program is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 2.1 of the License.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public
// * License along with this program; if not, write to the Free Software
// * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
// */
//package test;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//
//import main.Status;
//import outlook.ManageOutlookContacts;
//import contact.Contacts;
//
//public class MainTestSyncContacts {
//
//    static String strCardDAVUrl = "";
//    static String strHost = "";
//    static String strUser = "";
//    static String strPass = "";
//    static String strWorkingDir = "";
//
//    static int intOutlookFolder = 10;
//
//    public static void main(String[] args) throws Exception {
//        Status.print("Start");
//
//        //WorkingDir
//        strWorkingDir = System.getProperty("user.dir");
//        strWorkingDir = strWorkingDir + File.separator + "workingdir" + File.separator;
//
//        //Load config
//        Status.print("Load Config");
//        try {
//            File file = new File("conf\\config.txt");
//
//            if (file.exists()) {
//                BufferedReader in = new BufferedReader(new FileReader(file));
//
//                strUser = in.readLine();
//                strPass = in.readLine();
//                strHost = in.readLine();
//                strCardDAVUrl = in.readLine();
//
//                in.close();
//            }
//        } catch (IOException e1) {
//            e1.printStackTrace();
//        }
//
//        //Build Addressbooks
//        Contacts allContacts = new Contacts(strWorkingDir);
//
//        //Load Outlook Contacts
//        ManageOutlookContacts outlookContacts = new ManageOutlookContacts(strWorkingDir, intOutlookFolder);
//        outlookContacts.openOutlook();
//        outlookContacts.loadContantFromOutlook(allContacts);
//        outlookContacts.writeOutlookObjects(allContacts);
//
//        //Load WebDav Contacts
//        //ManageContactsWebDAV webDAVConnection = new ManageContactsWebDAV();
//        //webDAVConnection.connectHTTP(strUser, strPass, strHost);
//        //webDAVConnection.loadContactsFromWebDav(strHost+strCardDAVUrl, allContacts, strWorkingDir);
//        //Compare and modify Contacts
//        //allContacts.printStatus();
//        //System.out.println("----------------------");
//        //allContacts.compareAdressbooks();
//        //allContacts.printStatus();
//        //Write Data
//        //outlookContacts.writeContacts(allContacts, intOutlookFolder, strWorkingDir);
//        //webDAVConnection.writeContacts(strHost+strCardDAVUrl, allContacts);
//        //Save last Sync Uids
//        //Status.printStatusToConsole("Save last Sync UIDs");
//        //allContacts.saveUidsToFile(strWorkingDir);
//        //Delete Tmp Contact Pictures
//        //allContacts.deleteTmpContactPictures();
//        //Status.printStatusToConsole("Temporary Contact Pictures Files deleted");
//        //Close
//        outlookContacts.closeOutlook();
//
//        Status.print("End");
//    }
//}
