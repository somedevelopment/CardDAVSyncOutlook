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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import main.Status;

public class Contacts {
	
	public enum Addressbook {
		WEBDAVADDRESSBOOK,
		OUTLOOKADDRESSBOOK
	}
	
	static private Hashtable<String, Contact> hasTabDAVContacts = null;
	static private Hashtable<String, Contact> hasTabOutlookContacts = null;
	
	private List<String> listSyncContacts = null;
	
	/**
	 * 
	 * Construction Section
	 * 
	 */	
	public Contacts(String strWorkingDir) {
		hasTabDAVContacts = new Hashtable<String, Contact>();
		hasTabOutlookContacts = new Hashtable<String, Contact>();
		listSyncContacts = new ArrayList<String>();
		
		this.loadUidsFromFile(strWorkingDir);
	}

	/**
	 * Private
	 * 
	 */
	private void loadUidsFromFile(String strWorkingDir) {
		try {
			Status.printStatusToConsole("Load last Sync UIDs");
			File file = new File ((strWorkingDir + "lastSync.txt"));
			
			if (file.exists()) {
				BufferedReader in = new BufferedReader(new FileReader(strWorkingDir + "lastSync.txt"));
				
				String line = null;
				while ((line = in.readLine()) != null) {
					this.listSyncContacts.add(line);
				}
				line = null;

				in.close();
				in = null;
			}
			
			file = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Public
	 * 
	 */
	
	public Integer numberOfContacts(Addressbook whichAdressbook) {
		int size = 0;
		switch (whichAdressbook) {
			case WEBDAVADDRESSBOOK:
				size = hasTabDAVContacts.size();
				break;
			case OUTLOOKADDRESSBOOK:
				size = hasTabOutlookContacts.size();
				break;
		}
		
		return new Integer(size);
	}
	
	public void addContact(Addressbook whichAdressbook, Contact conContact) {
		switch (whichAdressbook) {
			case WEBDAVADDRESSBOOK:
				hasTabDAVContacts.put(conContact.getUid(), conContact);
				break;
			case OUTLOOKADDRESSBOOK:
				hasTabOutlookContacts.put(conContact.getUid(), conContact);
				break;
		}
	}
	
	public void removeContact(Addressbook whichAdressbook, String strUidKey) {
		switch (whichAdressbook) {
		case WEBDAVADDRESSBOOK:
			hasTabDAVContacts.get(strUidKey).deleteTmpContactPictureFile();
			hasTabDAVContacts.remove(strUidKey);
			break;
		case OUTLOOKADDRESSBOOK:
			hasTabOutlookContacts.get(strUidKey).deleteTmpContactPictureFile();
			hasTabOutlookContacts.remove(strUidKey);
			break;
		}
	}	

	public Hashtable<String, Contact> getAddressbook(Addressbook whichAdressbook) {
		switch (whichAdressbook) {
			case WEBDAVADDRESSBOOK:
				return hasTabDAVContacts;
			case OUTLOOKADDRESSBOOK:
				return hasTabOutlookContacts;
		}
		
		return null;
	}
	
	public Contact getContact(Addressbook whichAdressbook, String strUidSearchContact) {
		switch (whichAdressbook) {
			case WEBDAVADDRESSBOOK:
				return hasTabDAVContacts.get(strUidSearchContact);
			case OUTLOOKADDRESSBOOK:
				return hasTabOutlookContacts.get(strUidSearchContact);
			default:
				return null;
		}
	}

	public void saveUidsToFile(String strWorkingDir) {
		File file = new File(strWorkingDir + "lastSync.txt");
		FileWriter writer;
		try {
			writer = new FileWriter(file);
	
			Iterator<Entry<String, Contact>> iter = hasTabDAVContacts.entrySet().iterator();
			
			while(iter.hasNext()) {
				Entry<String, Contact> entry = iter.next();
				writer.write(entry.getValue().getUid());
				writer.write(System.getProperty("line.separator"));
			}
			
			writer.flush();
			writer.close();
			
			iter = null;
			writer = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteTmpContactPictures() {
		Iterator<Entry<String, Contact>> iter = hasTabDAVContacts.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<String, Contact> entry = iter.next();
			entry.getValue().deleteTmpContactPictureFile();
			
			entry = null;
		}
		
		iter = hasTabOutlookContacts.entrySet().iterator();	
		while(iter.hasNext()) {
			Entry<String, Contact> entry = iter.next();
			entry.getValue().deleteTmpContactPictureFile();
			
			entry = null;
		}
		
		iter = null;
	}
	
	public void compareAdressbooks() {
		List<Contact> listNewOutlookContacts = new ArrayList<Contact>();
		List<Contact> listNewDAVContacts = new ArrayList<Contact>();
		List<Contact> listDelOutlookContacts = new ArrayList<Contact>();
		List<Contact> listDelDAVContacts = new ArrayList<Contact>();
		
		//Pr�fen ob ein Kontakt zu l�schen ist im Vergleich zum letzten mal
		if (!listSyncContacts.isEmpty()) {
			Iterator<String> iter = listSyncContacts.iterator();
			while(iter.hasNext()) {
				String currentUID = iter.next();
				
				if (hasTabDAVContacts.get(currentUID) == null) {
					if (hasTabOutlookContacts.get(currentUID) != null) {
						hasTabOutlookContacts.get(currentUID).setStatus(Contact.Status.DELETE);
					}
				}
				
				if (hasTabOutlookContacts.get(currentUID) == null) {
					if (hasTabDAVContacts.get(currentUID) != null) {
						hasTabDAVContacts.get(currentUID).setStatus(Contact.Status.DELETE);
					}
				}
					
			}
		}
		
		
		/** Compare Addressb�cher **/
		//Leading Outlook
		Iterator<Entry<String, Contact>> iterOutlookContacts = hasTabOutlookContacts.entrySet().iterator();
		while(iterOutlookContacts.hasNext()) {
				Entry<String, Contact> currentOutlookEntry = iterOutlookContacts.next();
				Contact currentOutlookContact = currentOutlookEntry.getValue();
				String currentOutlookKey = currentOutlookEntry.getKey();
				
				if ((currentOutlookContact.getStatus() == Contact.Status.READIN) || (currentOutlookContact.getStatus() == Contact.Status.UIDADDED)) {
					Contact currentDAVContact = hasTabDAVContacts.get(currentOutlookKey);
				
					if (currentDAVContact != null) {
						if (currentOutlookContact.compareContact(currentDAVContact)) {
							//System.out.println("\n1. "+ currentOutlookContact.getContactAsString());
							//System.out.println("\n2. "+ currentDAVContact.getContactAsString());
							currentOutlookContact.setStatus(Contact.Status.UNCHANGED);
							currentDAVContact.setStatus(Contact.Status.UNCHANGED);
						}
						else {
							if (currentOutlookContact.getLastModificationTime().getTime() > currentDAVContact.getLastModificationTime().getTime()) {						
								listDelDAVContacts.add(currentDAVContact);
								
								Contact newContact = new Contact(currentOutlookContact, Contact.Status.CHANGED);
								listNewDAVContacts.add(newContact);
								
								currentOutlookContact.setStatus(Contact.Status.UNCHANGED);
								currentDAVContact.setStatus(Contact.Status.DELETE);
								
								//System.out.println("\n3. "+ currentDAVContact.getContactAsString());
								//System.out.println("\n4. "+ newContact.getContactAsString());
								//System.out.println("\n5. "+ currentOutlookContact.getContactAsString());
							}
							else {
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
						}
					}
					else {
						Contact newContact = new Contact(currentOutlookContact, Contact.Status.NEW);
						listNewDAVContacts.add(newContact);
						//System.out.println("\n9. "+ newContact.getContactAsString());
						
					}
				}
		}
		
		//Leading WebDav
		Iterator<Entry<String, Contact>> iterDAVContacts = hasTabDAVContacts.entrySet().iterator();
		while(iterDAVContacts.hasNext()) {
			Entry<String, Contact> currentDAVEntry = iterDAVContacts.next();
			Contact currentDAVContact = currentDAVEntry.getValue();
			String currentDAVKey = currentDAVEntry.getKey();
			
			if (currentDAVContact.getStatus() == Contact.Status.READIN || (currentDAVContact.getStatus() == Contact.Status.UIDADDED)) {
				Contact currentOutlookContact = hasTabOutlookContacts.get(currentDAVKey);
			
				if (currentOutlookContact != null) {				
					if (currentDAVContact.compareContact(currentOutlookContact)) {
						currentOutlookContact.setStatus(Contact.Status.UNCHANGED);
						currentDAVContact.setStatus(Contact.Status.UNCHANGED);
						
						//System.out.println("\n10. "+ currentDAVContact.getContactAsString());
						//System.out.println("\n11. "+ currentOutlookContact.getContactAsString());
					}
					else {
						if (currentOutlookContact.getLastModificationTime().getTime() > currentDAVContact.getLastModificationTime().getTime()) {						
							listDelDAVContacts.add(currentDAVContact);
							
							Contact newContact = new Contact(currentOutlookContact, Contact.Status.CHANGED);
							listNewDAVContacts.add(newContact);
							
							currentOutlookContact.setStatus(Contact.Status.UNCHANGED);
							currentDAVContact.setStatus(Contact.Status.DELETE);
							
							//System.out.println("\n12. "+ currentDAVContact.getContactAsString());
							//System.out.println("\n13. "+ newContact.getContactAsString());
							//System.out.println("\n14. "+ currentOutlookContact.getContactAsString());
						}
						else {
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
				}
				else {
					Contact newContact = new Contact(currentDAVContact, Contact.Status.NEW);
					listNewOutlookContacts.add(newContact);
					
					//System.out.println("\n18. "+ newContact.getContactAsString());
				}
			}
		}
		
		//Made Changes out of temporary Lists
		if (!listDelDAVContacts.isEmpty()) {
			Iterator<Contact> iter = listDelDAVContacts.iterator();
			while(iter.hasNext()) {
				Contact currentContact = iter.next();
				
				this.removeContact(Addressbook.WEBDAVADDRESSBOOK, currentContact.getUid());
			}
		}	
		
		if (!listDelOutlookContacts.isEmpty()) {
			Iterator<Contact> iter = listDelOutlookContacts.iterator();
			while(iter.hasNext()) {
				Contact currentContact = iter.next();
				
				this.removeContact(Addressbook.OUTLOOKADDRESSBOOK, currentContact.getUid());
			}
		}
		
		if (!listNewDAVContacts.isEmpty()) {
			Iterator<Contact> iter = listNewDAVContacts.iterator();
			while(iter.hasNext()) {
				Contact currentContact = iter.next();
				
				this.addContact(Contacts.Addressbook.WEBDAVADDRESSBOOK, currentContact);
			}
		}
		
		if (!listNewOutlookContacts.isEmpty()) {
			Iterator<Contact> iter = listNewOutlookContacts.iterator();
			while(iter.hasNext()) {
				Contact currentContact = iter.next();
				
				this.addContact(Contacts.Addressbook.OUTLOOKADDRESSBOOK, currentContact);
			}
		}
	}
	
	public void printStatus() {
		Iterator<Entry<String, Contact>> iterContacts = hasTabDAVContacts.entrySet().iterator();
		
		while(iterContacts.hasNext()) {
			Entry<String, Contact> entry = iterContacts.next();
			Contact currentContact = entry.getValue();
			
			Status.printStatusToConsole("DAV Contact: "+currentContact.getFirstName()+","+currentContact.getLastName()+": "+ currentContact.getStatus());
		}
		
		iterContacts = hasTabOutlookContacts.entrySet().iterator();
		
		while(iterContacts.hasNext()) {
			Entry<String, Contact> entry = iterContacts.next();
			Contact currentContact = entry.getValue();
			
			Status.printStatusToConsole("Outlook Contact: "+currentContact.getFirstName()+","+currentContact.getLastName()+": "+ currentContact.getStatus());
		}
	}
}
