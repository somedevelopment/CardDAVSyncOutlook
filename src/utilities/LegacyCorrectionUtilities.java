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
package utilities;

public class LegacyCorrectionUtilities {

    static public Boolean bodyHasUID(String strBody) {
        if (strBody != "" && strBody != null) {
            if (strBody.contains("---_Start_Do_Not_Delete_or_Change_Required_for_CardDAVSyncOutlook_---")) 
                return true;
        }
        
        return false;
    }

    static public String getBodyUID(String strBody) {
        if (strBody.contains("---_Start_Do_Not_Delete_or_Change_Required_for_CardDAVSyncOutlook_---")) {
            String[] result = strBody.split("\n");
            for (int i = 0; i < result.length; i++) {
                if (result[i].contains("---_Start_Do_Not_Delete_or_Change_Required_for_CardDAVSyncOutlook_---"))
                    return result[i + 1].trim();
            }
        }
        return "";
    }

    static public String cleanBodyFromUID(String strBody) {
        if (strBody.contains("---_Start_Do_Not_Delete_or_Change_Required_for_CardDAVSyncOutlook_---")) {
            StringBuilder strBuilder = new StringBuilder();
            String[] result = strBody.split("\n");

            for (int i = 0; i < result.length; i++) {
                if (result[i].contains("---_Start_Do_Not_Delete_or_Change_Required_for_CardDAVSyncOutlook_---")) {
                    i = i + 2;
                } else {
                    strBuilder.append(result[i]);
                }
            }

            return strBuilder.toString();
        }

        return strBody;
    }
}
