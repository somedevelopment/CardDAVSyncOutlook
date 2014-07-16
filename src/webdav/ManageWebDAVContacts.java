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
package webdav;

import contact.Contact;
import contact.Contacts;
import contact.Contacts.Addressbook;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import javax.net.ssl.SSLHandshakeException;
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
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import utilities.LegacyCorrectionUtilities;

public class ManageWebDAVContacts {

    private HostConfiguration hostConfig = null;
    private HttpConnectionManager connectionManager = null;
    private HttpConnectionManagerParams connectionManagerParams = null;

    private HttpClient client = null;
    private Credentials creds = null;

    private final int intMaxConnections = 20;

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
        DavMethod pFind;
        MultiStatusResponse[] responses = null;

        try {
            pFind = new PropFindMethod(strResourcePath, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_INFINITY);
            this.client.executeMethod(pFind);
            MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();
            responses = multiStatus.getResponses();
            pFind.releaseConnection();
        } catch (IOException | DavException e) {
            if (e instanceof UnknownHostException)
                Status.print("Error: Can't find server");
            if (e instanceof DavException)
                Status.print("Error: Can't find address book on server");
            if (e instanceof SSLHandshakeException)
                Status.print("Error: SSL Certificate not accepted");
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
                try (InputStream inputStream = httpMethod.getResponseBodyAsStream()) {
                    IOUtils.copy(inputStream, strWriter, "UTF-8");
                }
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
    public void connectHTTP(String strUser, String strPass, String strHost, boolean insecure) {
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

        if (insecure) {
            Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
            Protocol.registerProtocol("https", easyhttps);
        }

        Status.print("WebDav Connection generated");
    }

    public boolean loadContactsFromWebDav(String strCardDAVUrl, Contacts allContacts, String strWorkingDir) {
        MultiStatusResponse[] responses = getContentToRessource(strCardDAVUrl);
        if (responses == null)
            return false;

        try {
            for (MultiStatusResponse response : responses) {
                String strFileToDownload = hostConfig.getHost() + response.getHref();
                //Pruefen ob es sich wirklich um ein Kontakt handelt - nicht wirklich schoen
                if (strFileToDownload.contains(".vcf")) {
                    String strNewContact = readVCardsFromWebDAV(strFileToDownload);
                    if (strNewContact != null) {
                        Contact tmpContact = new Contact(strNewContact, strFileToDownload, strWorkingDir);
                        allContacts.addContact(Addressbook.WEBDAVADDRESSBOOK, tmpContact);

                        Status.print("Load WebDav Contact " +
                                tmpContact.getFirstName() + ", " +
                                tmpContact.getLastName());
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //TODO: Naming of current Variable
    public void writeContacts(String strCardDAVUrl, Contacts allContacts) {
        List<Contact> listDelDAVContacts = new ArrayList();

        for (Entry<String, Contact> currentOutlookEntry : allContacts.getAddressbook(Addressbook.WEBDAVADDRESSBOOK).entrySet()) {

            //Legacy correction UID call
            if (LegacyCorrectionUtilities.bodyHasUID(currentOutlookEntry.getValue().getBody())) {
                currentOutlookEntry.getValue().setBody(LegacyCorrectionUtilities.cleanBodyFromUID(currentOutlookEntry.getValue().getBody()));
                if ((currentOutlookEntry.getValue().getStatus() == Contact.Status.READIN) ||
                        (currentOutlookEntry.getValue().getStatus() == Contact.Status.UIDADDED) ||
                        (currentOutlookEntry.getValue().getStatus() == Contact.Status.UNCHANGED)) {
                    currentOutlookEntry.getValue().setStatus(Contact.Status.CHANGED);
                }
            }
            
            //Correction of numbers INTERNATIONAL formating
            if (allContacts.getCorrectNumber()) {
                currentOutlookEntry.getValue().correctNumbers(allContacts.getDefaultRegion());
                if ((currentOutlookEntry.getValue().getStatus() == Contact.Status.READIN) ||
                        (currentOutlookEntry.getValue().getStatus() == Contact.Status.UIDADDED) ||
                        (currentOutlookEntry.getValue().getStatus() == Contact.Status.UNCHANGED)) {
                    currentOutlookEntry.getValue().setStatus(Contact.Status.CHANGED);
                }                
            }

            switch (currentOutlookEntry.getValue().getStatus()) {
                case CHANGED:
                    Status.print("Write Changed Contact to WebDAV " +
                            currentOutlookEntry.getValue().getFirstName() + ", " +
                            currentOutlookEntry.getValue().getLastName());
                    uploadVCardsToWebDAV(
                            generateWebDavUriFilename(
                                    currentOutlookEntry.getValue(),
                                    strCardDAVUrl),
                            currentOutlookEntry.getValue().getContactAsString());
                    break;
                case NEW:
                    Status.print("Write New Contact to WebDAV " +
                            currentOutlookEntry.getValue().getFirstName() + ", " +
                            currentOutlookEntry.getValue().getLastName());
                    uploadVCardsToWebDAV(
                            generateWebDavUriFilename(
                                    currentOutlookEntry.getValue(),
                                    strCardDAVUrl),
                            currentOutlookEntry.getValue().getContactAsString());
                    break;
                case DELETE:
                    Status.print("Delete Contact from WebDAV " +
                            currentOutlookEntry.getValue().getFirstName() + ", " +
                            currentOutlookEntry.getValue().getLastName());
                    deleteVCardsFromWebDAV(
                            generateWebDavUriFilename(
                                    currentOutlookEntry.getValue(),
                                    strCardDAVUrl));
                    listDelDAVContacts.add(currentOutlookEntry.getValue());
                    break;
                case READIN:
                	//Nothing to do
                	break;
                case UIDADDED:
                    Status.print("Write Contact with new UID to WebDAV " +
                            currentOutlookEntry.getValue().getFirstName() + ", " +
                            currentOutlookEntry.getValue().getLastName());
                    Status.print("WARNING: this should not happen!");
                    break;
                case UNCHANGED:
                	//Nothing to do
                    break;
            }
        }

        //Delete deleted Contacts
        for (Contact currentContact : listDelDAVContacts) {
            allContacts.removeContact(Addressbook.WEBDAVADDRESSBOOK, currentContact.getUid());
        }
    }

}
