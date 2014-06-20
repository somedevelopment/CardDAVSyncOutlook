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

package test;

import java.io.File;

import webdav.ManageWebDAV;

public class MainTestSyncCalender {
	static String strCalDAVUrl = "";
	static String strHost = "";
	static String strUser = "";
	static String strPass = "";

	public static void main(String[] args) {
		
		ManageWebDAV tmp = new ManageWebDAV();
		
		String strPathToXMLRequest = System.getProperty("user.dir");
		strPathToXMLRequest = strPathToXMLRequest+File.separator+"conf"+File.separator+"CalDavRequest.xml";
		
		tmp.connectWebDAVServer(strHost, 20, strUser, strPass);
		//tmp.test(strHost+strCalDAVUrl, strPathToXMLRequest);
		
//		int intOutlookFolder = 9;
//
//		String strWorkingdir = System.getProperty("user.dir");
//		strWorkingdir = strWorkingdir+File.separator+"workingdir"+File.separator;
//		
//		//Build Addressbooks
//		Appointments allAppointments = new Appointments();
//		
//		
//		//Get CalDav instance
//		
//		ManageCalendersWebDAV test = new ManageCalendersWebDAV();
//		
//		test.connectHTTP(strUser, strPass, strHost, strCalDAVUrl);
		
		//Get Outlook instance
//		ManageOutlookAppointments outlookAppointments = new ManageOutlookAppointments(strWorkingdir, intOutlookFolder);
//		if (outlookAppointments.openOutlook()) {
//			
//			outlookAppointments.loadContantFromOutlook(allAppointments);
//			
//			//allAppointments.printAppointments();
//			
//			outlookAppointments.closeOutlook();
//		}
	}
}
