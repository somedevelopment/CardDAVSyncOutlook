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

public class Appointment {

    public enum Status {

        CHANGED,
        UNCHANGED,
        DELETE,
        NEW,
        READIN
    }

    public enum Sensitivity {

        Normal,
        Personal,
        Private,
        Confidential;

        public static Sensitivity fromString(String x) {
            switch (new Integer(x)) {
                case 0:
                    return Normal;
                case 1:
                    return Personal;
                case 2:
                    return Private;
                case 3:
                    return Confidential;
            }

            return null;
        }
    }

    private String strEntryID;
    private String strSubject;
    private String strBody;
    private Sensitivity senSensitivity;
    private String strStartUTC;
    private String strEndUTC;
    private String strAllDayEvent;
    private Boolean bolIsRecurring;
    private String strLocation;
    private String strRequiredAttendees;
    private String strOptionalAttendees;
    private String strReminderMinutesBeforeStart;

    /**
     * Construction Section
     */
    public Appointment(String strEntryID, String strSubject, String strBody, Sensitivity senSensitivity, String strStartUTC,
            String strEndUTC, String strAllDayEvent, Boolean bolIsReurring, String strLocation, String strRequiredAttendees,
            String strOptionalAttendees, String strReminderMinutesBeforeStart) {
        this.strEntryID = strEntryID;
        this.strSubject = strSubject;
        this.strBody = strBody;
        this.senSensitivity = senSensitivity;
        this.strStartUTC = strStartUTC;
        this.strEndUTC = strEndUTC;
        this.strAllDayEvent = strAllDayEvent;
        this.bolIsRecurring = bolIsReurring;
        this.strLocation = strLocation;
        this.strRequiredAttendees = strReminderMinutesBeforeStart;
        this.strOptionalAttendees = strOptionalAttendees;
        this.strReminderMinutesBeforeStart = strReminderMinutesBeforeStart;
    }

    /**
     * Getter/Setter Section
     */
    public String getSubject() {
        return strSubject;
    }

    public void setSubject(String strSubject) {
        this.strSubject = strSubject;
    }

    public String getBody() {
        return strBody;
    }

    public void setBody(String strBody) {
        this.strBody = strBody;
    }

    public Sensitivity getSensitivity() {
        return senSensitivity;
    }

    public void setSensitivity(Sensitivity senSensitivity) {
        this.senSensitivity = senSensitivity;
    }

    public String getEntryID() {
        return strEntryID;
    }

    public void setEntryID(String strEntryID) {
        this.strEntryID = strEntryID;
    }

    public String getStartUTC() {
        return strStartUTC;
    }

    public void setStartUTC(String strStartUTC) {
        this.strStartUTC = strStartUTC;
    }

    public String getEndUTC() {
        return strEndUTC;
    }

    public void setEndUTC(String strEndUTC) {
        this.strEndUTC = strEndUTC;
    }

    public String getAllDayEvent() {
        return strAllDayEvent;
    }

    public void setAllDayEvent(String allDayEvent) {
        strAllDayEvent = allDayEvent;
    }

    public Boolean getIsRecurring() {
        return bolIsRecurring;
    }

    public void setIsRecurring(Boolean bolIsRecurring) {
        this.bolIsRecurring = bolIsRecurring;
    }

    public String getLocation() {
        return strLocation;
    }

    public void setLocation(String strLocation) {
        this.strLocation = strLocation;
    }

    public String getRequiredAttendees() {
        return strRequiredAttendees;
    }

    public void setRequiredAttendees(String strRequiredAttendees) {
        this.strRequiredAttendees = strRequiredAttendees;
    }

    public String getOptionalAttendees() {
        return strOptionalAttendees;
    }

    public void setOptionalAttendees(String strOptionalAttendees) {
        this.strOptionalAttendees = strOptionalAttendees;
    }

    public String getReminderMinutesBeforeStart() {
        return strReminderMinutesBeforeStart;
    }

    public void setReminderMinutesBeforeStart(String strReminderMinutesBeforeStart) {
        this.strReminderMinutesBeforeStart = strReminderMinutesBeforeStart;
    }
}
