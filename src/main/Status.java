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

import ui.Userinterface;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Status {

    static public void print(String strWhereIam) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        //System.out.println(strWhereIam + ": " + sdf.format(new Date()));
        Userinterface.setTextInTextPane(sdf.format(new Date()) + " - " + strWhereIam);
    }
}
