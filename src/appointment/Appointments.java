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
package appointment;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

import main.Status;

public class Appointments {

    public enum Calenders {

        WEBDAVCALENDER,
        OUTLOOKCALENDER
    }

    static private Hashtable<String, Appointment> hasTabDAVAppointments = null;
    static private Hashtable<String, Appointment> hasTabOutlookAppointments = null;

    /**
     *
     * Construction Section
     *
     */
    public Appointments() {
        hasTabDAVAppointments = new Hashtable<String, Appointment>();
        hasTabOutlookAppointments = new Hashtable<String, Appointment>();
    }

    /**
     * Public
     *
     */
    public void addAppointment(Calenders whichCalender, Appointment appAppointment) {
        switch (whichCalender) {
            case WEBDAVCALENDER:
                hasTabDAVAppointments.put(appAppointment.getEntryID(), appAppointment);
                break;
            case OUTLOOKCALENDER:
                hasTabOutlookAppointments.put(appAppointment.getEntryID(), appAppointment);
                break;
        }
    }

    public Hashtable<String, Appointment> getCalenders(Calenders whichAdressbook) {
        switch (whichAdressbook) {
            case WEBDAVCALENDER:
                return hasTabDAVAppointments;
            case OUTLOOKCALENDER:
                return hasTabOutlookAppointments;
        }

        return null;
    }

    public void printAppointments() {
        Iterator<Entry<String, Appointment>> iterAppointment = hasTabDAVAppointments.entrySet().iterator();

        while (iterAppointment.hasNext()) {
            Entry<String, Appointment> entry = iterAppointment.next();
            Appointment currentAppointment = entry.getValue();

            Status.print("DAV Appointment: " + currentAppointment.getSensitivity() + " " + currentAppointment.getSubject());
        }

        iterAppointment = hasTabOutlookAppointments.entrySet().iterator();

        while (iterAppointment.hasNext()) {
            Entry<String, Appointment> entry = iterAppointment.next();
            Appointment currentAppointment = entry.getValue();

            Status.print("Outlook Appointment: " + currentAppointment.getSensitivity() + " " + currentAppointment.getSubject());
        }
    }

}
