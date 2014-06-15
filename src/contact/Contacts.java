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
package contact;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import main.Status;

public class Contacts {

    public enum Addressbook {
        WEBDAVADDRESSBOOK,
        OUTLOOKADDRESSBOOK
    }

    private final HashMap<String, Contact> davContacts;
    private final HashMap<String, Contact> outlookContacts;
    private final List<String> listSyncContacts;

    /**
     * Constructor
     */
    public Contacts(String strWorkingDir) {
        davContacts = new HashMap();
        outlookContacts = new HashMap();
        listSyncContacts = new ArrayList();

        this.loadUidsFromFile(strWorkingDir);
    }

    /**
     * Private
     */
    private void loadUidsFromFile(String strWorkingDir) {
        Status.printStatusToConsole("Load last Sync UIDs");
        File file = new File((strWorkingDir + "lastSync.txt"));
        if (file.exists()) {
            try (BufferedReader in = new BufferedReader(
                    new FileReader(strWorkingDir + "lastSync.txt"))) {
                String line;
                while ((line = in.readLine()) != null) {
                    this.listSyncContacts.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Public
     */
    public void addContact(Addressbook whichAdressbook, Contact conContact) {
        switch (whichAdressbook) {
            case WEBDAVADDRESSBOOK:
                davContacts.put(conContact.getUid(), conContact);
                break;
            case OUTLOOKADDRESSBOOK:
                outlookContacts.put(conContact.getUid(), conContact);
                break;
        }
    }

    public void removeContact(Addressbook whichAdressbook, String strUidKey) {
        switch (whichAdressbook) {
            case WEBDAVADDRESSBOOK:
                davContacts.get(strUidKey).deleteTmpContactPictureFile();
                davContacts.remove(strUidKey);
                break;
            case OUTLOOKADDRESSBOOK:
                outlookContacts.get(strUidKey).deleteTmpContactPictureFile();
                outlookContacts.remove(strUidKey);
                break;
        }
    }

    public HashMap<String, Contact> getAddressbook(Addressbook whichAdressbook) {
        switch (whichAdressbook) {
            case WEBDAVADDRESSBOOK:
                return davContacts;
            case OUTLOOKADDRESSBOOK:
                return outlookContacts;
        }

        return null;
    }

    public Contact getContact(Addressbook whichAdressbook, String strUidSearchContact) {
        switch (whichAdressbook) {
            case WEBDAVADDRESSBOOK:
                return davContacts.get(strUidSearchContact);
            case OUTLOOKADDRESSBOOK:
                return outlookContacts.get(strUidSearchContact);
            default:
                return null;
        }
    }

    public void saveUidsToFile(String strWorkingDir) {
        File file = new File(strWorkingDir + "lastSync.txt");
        FileWriter writer;
        try {
            writer = new FileWriter(file);
            for (Entry<String, Contact> entry : davContacts.entrySet()) {
                writer.write(entry.getValue().getUid());
                writer.write(System.getProperty("line.separator"));
            }

            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteTmpContactPictures() {
        Iterator<Entry<String, Contact>> iter = davContacts.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, Contact> entry = iter.next();
            entry.getValue().deleteTmpContactPictureFile();

        }

        iter = outlookContacts.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, Contact> entry = iter.next();
            entry.getValue().deleteTmpContactPictureFile();

        }
    }

    public void compareAdressbooks() {
        List<Contact> listNewOutlookContacts = new ArrayList();
        List<Contact> listNewDAVContacts = new ArrayList();
        List<Contact> listDelOutlookContacts = new ArrayList();
        List<Contact> listDelDAVContacts = new ArrayList();

        // look for contacts in both address books that were present during last
        // sync and were deleted in one address book. Mark them as
        // "to delete" in the other one
        for (String currentUID : listSyncContacts){
            if (davContacts.get(currentUID) == null) {
                if (outlookContacts.get(currentUID) != null) {
                    outlookContacts.get(currentUID).setStatus(Contact.Status.DELETE);
                }
            }

            if (outlookContacts.get(currentUID) == null) {
                if (davContacts.get(currentUID) != null) {
                    davContacts.get(currentUID).setStatus(Contact.Status.DELETE);
                }
            }
        }

        //Leading Outlook
        for (Entry<String, Contact> currentOutlookEntry : outlookContacts.entrySet()) {
            Contact currentOutlookContact = currentOutlookEntry.getValue();
            String currentOutlookKey = currentOutlookEntry.getKey();

            if (currentOutlookContact.getStatus() != Contact.Status.READIN &&
                    currentOutlookContact.getStatus() != Contact.Status.UIDADDED) {
                continue;
            }

            Contact currentDAVContact = davContacts.get(currentOutlookKey);
            if (currentDAVContact == null) {
                // corresponding dav contact does not exist, insert it
                Contact newContact = new Contact(currentOutlookContact, Contact.Status.NEW);
                listNewDAVContacts.add(newContact);
                continue;
            }

            if (currentOutlookContact.equalTo(currentDAVContact)) {
                currentOutlookContact.setStatus(Contact.Status.UNCHANGED);
                currentDAVContact.setStatus(Contact.Status.UNCHANGED);
                continue;
            }

            if (currentOutlookContact.getLastModificationTime().getTime() >
                    currentDAVContact.getLastModificationTime().getTime()) {
                // outlook contact is newer then dav contact, dav contact will
                // be replaced
                listDelDAVContacts.add(currentDAVContact);

                Contact newContact = new Contact(currentOutlookContact,
                        Contact.Status.CHANGED);
                listNewDAVContacts.add(newContact);

                currentOutlookContact.setStatus(Contact.Status.UNCHANGED);
                currentDAVContact.setStatus(Contact.Status.DELETE);
                continue;
            }

            // dav contact is newer then outlook contact, outlook contact will
            // be replaced
            listDelOutlookContacts.add(currentOutlookContact);

            Contact newContact = new Contact(currentDAVContact, Contact.Status.CHANGED);
            newContact.setEntryID(currentOutlookContact.getEntryID());
            listNewOutlookContacts.add(newContact);

            currentDAVContact.setStatus(Contact.Status.UNCHANGED);
            currentOutlookContact.setStatus(Contact.Status.DELETE);

            //System.out.println("\n6. "+ currentDAVContact.getContactAsString());
            //System.out.println("\n7. "+ newContact.getContactAsString());
            //System.out.println("\n8. "+ currentOutlookContact.getContactAsString());
        }

        //Leading WebDav
        for (Entry<String, Contact> currentDAVEntry : davContacts.entrySet()) {
            Contact currentDAVContact = currentDAVEntry.getValue();
            String currentDAVKey = currentDAVEntry.getKey();

            if (currentDAVContact.getStatus() == Contact.Status.READIN ||
                    (currentDAVContact.getStatus() == Contact.Status.UIDADDED)) {
                Contact currentOutlookContact = outlookContacts.get(currentDAVKey);

                if (currentOutlookContact != null) {
                    if (currentDAVContact.equalTo(currentOutlookContact)) {
                        currentOutlookContact.setStatus(Contact.Status.UNCHANGED);
                        currentDAVContact.setStatus(Contact.Status.UNCHANGED);

                        //System.out.println("\n10. "+ currentDAVContact.getContactAsString());
                        //System.out.println("\n11. "+ currentOutlookContact.getContactAsString());
                    } else {
                        if (currentOutlookContact.getLastModificationTime().getTime() >
                                currentDAVContact.getLastModificationTime().getTime()) {
                            listDelDAVContacts.add(currentDAVContact);

                            Contact newContact = new Contact(currentOutlookContact, Contact.Status.CHANGED);
                            listNewDAVContacts.add(newContact);

                            currentOutlookContact.setStatus(Contact.Status.UNCHANGED);
                            currentDAVContact.setStatus(Contact.Status.DELETE);

                            //System.out.println("\n12. "+ currentDAVContact.getContactAsString());
                            //System.out.println("\n13. "+ newContact.getContactAsString());
                            //System.out.println("\n14. "+ currentOutlookContact.getContactAsString());
                        } else {
                            listDelOutlookContacts.add(currentOutlookContact);

                            Contact newContact = new Contact(currentDAVContact, Contact.Status.CHANGED);
                            newContact.setEntryID(currentOutlookContact.getEntryID());
                            listNewOutlookContacts.add(newContact);

                            currentDAVContact.setStatus(Contact.Status.UNCHANGED);
                            currentOutlookContact.setStatus(Contact.Status.DELETE);

                            //System.out.println("\n15. "+ currentDAVContact.getContactAsString());
                            //System.out.println("\n16. "+ newContact.getContactAsString());
                            //System.out.println("\n17. "+ currentOutlookContact.getContactAsString());
                        }
                    }
                } else {
                    Contact newContact = new Contact(currentDAVContact, Contact.Status.NEW);
                    listNewOutlookContacts.add(newContact);

                    //System.out.println("\n18. "+ newContact.getContactAsString());
                }
            }
        }

        //Made Changes out of temporary Lists
        for (Contact currentContact : listDelDAVContacts) {
            this.removeContact(Addressbook.WEBDAVADDRESSBOOK, currentContact.getUid());
        }

        for (Contact currentContact : listDelOutlookContacts) {
            this.removeContact(Addressbook.OUTLOOKADDRESSBOOK, currentContact.getUid());
        }

        for (Contact currentContact : listNewDAVContacts) {
            this.addContact(Contacts.Addressbook.WEBDAVADDRESSBOOK, currentContact);
        }

        for (Contact currentContact : listNewOutlookContacts) {
            this.addContact(Contacts.Addressbook.OUTLOOKADDRESSBOOK, currentContact);
        }
    }

    public void printStatus() {
        Status.printStatusToConsole("Outlook Contacts: ");
        this.print(outlookContacts);
        Status.printStatusToConsole("DAV Contacs: ");
        this.print(davContacts);;
    }

    private void print(HashMap<String, Contact> addressBook) {
       for (Contact contact : addressBook.values()) {
           String s = contact.getFirstName() + "," +
                   contact.getLastName() + ": " +
                   contact.getStatus();
           Status.printStatusToConsole(s);
       }
    }
}
