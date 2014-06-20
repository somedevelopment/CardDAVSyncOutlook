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
package webdav;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import main.Status;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import contact.Contact;
import contact.Contacts;
import contact.Contacts.Addressbook;

public class ManageContactsWebDAV {

    private HostConfiguration hostConfig = null;
    private HttpConnectionManager connectionManager = null;
    private HttpConnectionManagerParams connectionManagerParams = null;

    private HttpClient client = null;
    private Credentials creds = null;

    private int intMaxConnections = 20;

    /**
     *
     * Private Section
     *
     */
    private String generateWebDavUriFilename(Contact currentConntact, String strCardDavUrl) {
        if (currentConntact.getWebDavUriFilename() == null) {
            currentConntact.setWebDavUriFilename(strCardDavUrl + "/" + currentConntact.getUid() + ".vcf");
        }

        return currentConntact.getWebDavUriFilename();
    }

    private MultiStatusResponse[] getContentToRessource(String strResourcePath) {
        DavMethod pFind = null;
        MultiStatusResponse[] responses = null;

        try {
            pFind = new PropFindMethod(strResourcePath, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_INFINITY);
            this.client.executeMethod(pFind);
            MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();
            responses = multiStatus.getResponses();
            pFind.releaseConnection();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DavException e) {
            e.printStackTrace();
        }

        return responses;
    }

    private String readVCardsFromWebDAV(String strUriFile) {
        try {
            GetMethod httpMethod = new GetMethod(strUriFile);
            this.client.executeMethod(httpMethod);

            StringWriter strWriter = new StringWriter();

            if (httpMethod.getResponseContentLength() > 0) {
                InputStream inputStream = httpMethod.getResponseBodyAsStream();

                IOUtils.copy(inputStream, strWriter, "UTF-8");
                inputStream.close();
            }

            httpMethod.releaseConnection();
            return strWriter.toString();
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    private void uploadVCardsToWebDAV(String strUriFile, String strContent) {
        try {
            PutMethod httpMethod = new PutMethod(strUriFile);
            httpMethod.setRequestBody(strContent);
            this.client.executeMethod(httpMethod);
            httpMethod.releaseConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteVCardsFromWebDAV(String strUriFile) {
        try {
            DeleteMethod httpMethod = new DeleteMethod(strUriFile);
            this.client.executeMethod(httpMethod);
            httpMethod.releaseConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * Public Section
     *
     */
    public void connectHTTP(String strUser, String strPass, String strHost) {
        //Connect WebDAV with credentials
        hostConfig = new HostConfiguration();
        hostConfig.setHost(strHost);
        connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManagerParams = new HttpConnectionManagerParams();
        connectionManagerParams.setMaxConnectionsPerHost(hostConfig, this.intMaxConnections);
        connectionManager.setParams(connectionManagerParams);
        client = new HttpClient(connectionManager);
        creds = new UsernamePasswordCredentials(strUser, strPass);
        client.getState().setCredentials(AuthScope.ANY, creds);
        client.setHostConfiguration(hostConfig);

        Status.printStatusToConsole("WebDav Connection generated");
    }

    public Boolean loadContactsFromWebDav(String strCardDAVUrl, Contacts allContacts, String strWorkingDir) {
        MultiStatusResponse[] responses = getContentToRessource(strCardDAVUrl);

        if (responses != null) {
            try {
                MultiStatusResponse currResponse;
                for (int i = 0; i < responses.length; i++) {
                    currResponse = responses[i];

                    String strFileToDownload = hostConfig.getHost() + currResponse.getHref();

                    if (strFileToDownload.contains(".vcf")) { //Pr�fen ob es sich wirklich um ein Kontakt handelt - nicht wirklich sch�n
                        String strNewContact = readVCardsFromWebDAV(strFileToDownload);
                        if (strNewContact != null) {
                            Contact tmpContact;
                            allContacts.addContact(Addressbook.WEBDAVADDRESSBOOK, tmpContact = new Contact(strNewContact, strFileToDownload, strWorkingDir));

                            Status.printStatusToConsole("Load WebDav Contact " + tmpContact.getFirstName() + ", " + tmpContact.getLastName());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        } else {
            return false;
        }
    }

    public void writeContacts(String strCardDAVUrl, Contacts allContacts) {
        List<Contact> listDelDAVContacts = new ArrayList<Contact>();

        Iterator<Entry<String, Contact>> iterDavContacts = allContacts.getAddressbook(Addressbook.WEBDAVADDRESSBOOK).entrySet().iterator();

        while (iterDavContacts.hasNext()) {
            Entry<String, Contact> currentOutlookEntry = iterDavContacts.next();

            switch (currentOutlookEntry.getValue().getStatus()) {
                case CHANGED:
                    Status.printStatusToConsole("Write Changed Contact to WebDAV " + currentOutlookEntry.getValue().getFirstName() + ", " + currentOutlookEntry.getValue().getLastName());
                    uploadVCardsToWebDAV(generateWebDavUriFilename(currentOutlookEntry.getValue(), strCardDAVUrl), currentOutlookEntry.getValue().getContactAsString());
                    break;
                case NEW:
                    Status.printStatusToConsole("Write New Contact to WebDAV " + currentOutlookEntry.getValue().getFirstName() + ", " + currentOutlookEntry.getValue().getLastName());
                    uploadVCardsToWebDAV(generateWebDavUriFilename(currentOutlookEntry.getValue(), strCardDAVUrl), currentOutlookEntry.getValue().getContactAsString());
                    break;
                case DELETE:
                    Status.printStatusToConsole("Delete Contact from WebDAV " + currentOutlookEntry.getValue().getFirstName() + ", " + currentOutlookEntry.getValue().getLastName());
                    deleteVCardsFromWebDAV(generateWebDavUriFilename(currentOutlookEntry.getValue(), strCardDAVUrl));
                    listDelDAVContacts.add(currentOutlookEntry.getValue());
                    break;
                case READIN:
                    //Do nothing
                    break;
                case UIDADDED:
                    Status.printStatusToConsole("Write Changed Contact to WebDAV  " + currentOutlookEntry.getValue().getFirstName() + ", " + currentOutlookEntry.getValue().getLastName());
                    uploadVCardsToWebDAV(generateWebDavUriFilename(currentOutlookEntry.getValue(), strCardDAVUrl), currentOutlookEntry.getValue().getContactAsString());
                    break;
                case UNCHANGED:
                    //Do nothing
                    break;
            }
        }

        //Delete deleted Contacts
        if (!listDelDAVContacts.isEmpty()) {
            Iterator<Contact> iter = listDelDAVContacts.iterator();
            while (iter.hasNext()) {
                Contact currentContact = iter.next();

                allContacts.removeContact(Addressbook.WEBDAVADDRESSBOOK, currentContact.getUid());
            }
        }
    }

}
