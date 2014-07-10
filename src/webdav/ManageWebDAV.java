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

/* Currently test class for Calender Entries */
package webdav;

import java.io.IOException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.OptionsMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

public class ManageWebDAV {

    protected HttpClient client;

    /*
     * Public section
     */
    public void connectWebDAVServer(String strUri, int intMaxConnections, String strUserId, String strPassword) {
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(strUri);

        HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setMaxConnectionsPerHost(hostConfig, intMaxConnections);
        connectionManager.setParams(params);

        this.client = new HttpClient(connectionManager);
        client.setHostConfiguration(hostConfig);
        Credentials creds = new UsernamePasswordCredentials(strUserId, strPassword);
        client.getState().setCredentials(AuthScope.ANY, creds);
    }

    public void printHttpOptionsToConsole(String strUri) {
        try {
            OptionsMethod optMethod = new OptionsMethod(strUri);
            this.client.executeMethod(optMethod);
            int intLength = optMethod.getResponseHeaders().length;

            System.out.println(optMethod.getStatusLine());
            for (int i = 0; i < intLength; i++) {
                System.out.println(optMethod.getResponseHeaders()[i].getName() + ": " + optMethod.getResponseHeaders()[i].getValue());
            }

            optMethod.releaseConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printPropFindMethodToConsole(String strUri) {
        try {
            PropFindMethod pMethod = new PropFindMethod(strUri, DavConstants.PROPFIND_ALL_PROP_INCLUDE, DavConstants.DEPTH_INFINITY);
            this.client.executeMethod(pMethod);

            MultiStatus multiStatus = pMethod.getResponseBodyAsMultiStatus();

            if (multiStatus != null) {
                MultiStatusResponse[] responses = multiStatus.getResponses();
                for (MultiStatusResponse i : responses) {
                    System.out.println("Response Href: " + i.getHref() + " - Description: " + i.getResponseDescription());
                }
            }

            pMethod.releaseConnection();
        } catch (DavException e) {
            e.printStackTrace();
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//	public void test(String strUri, String strXMLFile) {
//		  try {
//		   Document docXMLRequest = XMLUtilities.loadXMLFile(strXMLFile);
//		   ReportInfo repInfo = new ReportInfo(docXMLRequest.getDocumentElement(), DavConstants.DEPTH_1);
//
//		   ReportMethod repMethod = new ReportMethod(strUri, repInfo);
//		  } catch (DavException | IOException e) {
//		   e.printStackTrace();
//		  }
//	}
}
