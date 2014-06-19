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

package utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import mainPackage.Status;

public class Utilities {

	static public String loadFileInString(String strDescriptionFile, String strFileToLoad) {
		try {
			String strFile = null;
			
			Status.printStatusToConsole("Load: "+ strDescriptionFile);
			File file = new File (strFileToLoad);
			
			if (file.exists()) {
				BufferedReader in = new BufferedReader(new FileReader(strFileToLoad));
				
				String line = null;
				while ((line = in.readLine()) != null) {
					strFile = strFile + line;
				}
				line = null;

				in.close();
				in = null;
			}
			
			file = null;
			
			return strFile;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	static public void saveStringToFile(String strDescriptionFile, String strFileToSave, String strDestinationFile) {
		try {
			Status.printStatusToConsole("Save: "+ strDescriptionFile);
			
			File file = new File(strDestinationFile);
			FileWriter writer;

			writer = new FileWriter(file);	
			writer.write(strFileToSave);
			
			writer.flush();
			writer.close();
			
			writer = null;
			file = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
