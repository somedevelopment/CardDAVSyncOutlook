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
package outlook;

import appointment.Appointment;
import appointment.Appointment.Sensitivity;
import appointment.Appointments;
import com.jacob.com.Dispatch;
import main.Status;

public class ManageOutlookAppointments extends ManageOutlook<Appointment, Appointments> {

    private final Sensitivity senSensitivity;

    public ManageOutlookAppointments(String strWorkingDir, int intOutlookFolder) {
        super(strWorkingDir, intOutlookFolder);

        this.senSensitivity = null;
    }

    public ManageOutlookAppointments(String strWorkingDir, int intOutlookFolder, Sensitivity senSensitivity) {
        super(strWorkingDir, intOutlookFolder);

        this.senSensitivity = senSensitivity;
    }

    @Override
    public Dispatch generatePutDispatchContent(Appointment appointment) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeOutlookObjects(Appointments appointments) {
		// TODO Auto-generated method stub

    }

    @Override
    public void loadContentFromOutlook(Appointments appointments) {

        Dispatch dipAppointmentsFolder = Dispatch.call(ManageOutlook.dipNamespace, "GetDefaultFolder", (Object) super.intOutlookFolder).toDispatch();
        Dispatch dipAppointmentsItems = Dispatch.get(dipAppointmentsFolder, "items").toDispatch();

        @SuppressWarnings("deprecation")
        int count = Dispatch.call(dipAppointmentsItems, "Count").toInt();

        for (int i = 1; i <= count; i++) {
            Dispatch dipAppointment;
            dipAppointment = Dispatch.call(dipAppointmentsItems, "Item", i).toDispatch();

            Sensitivity sensitivity = Sensitivity.fromString(Dispatch.get(dipAppointment, "Sensitivity").toString().trim());

            if ((this.senSensitivity != null) && (this.senSensitivity != sensitivity)) {
                dipAppointment.safeRelease();
            } else {
                String strEntryID = Dispatch.get(dipAppointment, "EntryID").toString().trim();
                String strSubject = Dispatch.get(dipAppointment, "Subject").toString().trim();
                String strBody = Dispatch.get(dipAppointment, "Body").toString().trim();
                String strStartUTC = Dispatch.get(dipAppointment, "StartUTC").toString().trim();
                String strEndUTC = Dispatch.get(dipAppointment, "EndUTC").toString().trim();
                String strAllDayEvent = Dispatch.get(dipAppointment, "AllDayEvent").toString().trim();
                Boolean bolIsRecurring = Boolean.valueOf(Dispatch.get(dipAppointment, "IsRecurring").toString().trim());
                String strLocation = Dispatch.get(dipAppointment, "Location").toString().trim();
                String strRequiredAttendees = Dispatch.get(dipAppointment, "RequiredAttendees").toString().trim();
                String strOptionalAttendees = Dispatch.get(dipAppointment, "OptionalAttendees").toString().trim();
                String strReminderMinutesBeforeStart = Dispatch.get(dipAppointment, "ReminderMinutesBeforeStart").toString().trim();

                appointments.addAppointment(Appointments.Calenders.OUTLOOKCALENDER, new Appointment(
                        strEntryID, strSubject, strBody, sensitivity, strStartUTC, strEndUTC, strAllDayEvent,
                        bolIsRecurring, strLocation, strRequiredAttendees, strOptionalAttendees, strReminderMinutesBeforeStart));

                Status.print("Load appointment from Outlook " + strSubject);
            }

            dipAppointment.safeRelease();
        }

        dipAppointmentsItems.safeRelease();
        dipAppointmentsFolder.safeRelease();
    }
}
