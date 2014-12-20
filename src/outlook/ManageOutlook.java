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

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.concurrent.TimeUnit;
import main.Status;
import utilities.Utilities;

public abstract class ManageOutlook<T1, T2> {

    private ActiveXComponent axc = null;

    protected Dispatch dipNamespace = null;
    protected Dispatch dipOutlook = null;
    protected String strWorkingDir;

    /**
     * Constructors
     */
    protected ManageOutlook(String strWorkingDir) {

        this.strWorkingDir = strWorkingDir;

        // add directory with jacob library (loaded later) to library path
        String workingDir = new File(System.getProperty("user.dir")).getAbsolutePath();
        String libDir = workingDir + File.separator + "lib";
        Utilities.addLibraryPath(libDir);
    }

    /**
     *
     * Public Section
     *
     */
    public Boolean openOutlook() {
        try {
            ComThread.InitMTA(true);
        } catch (UnsatisfiedLinkError e) {
            Status.print("Cannot open com library");
            e.printStackTrace();
            return false;
        }

        Boolean bolOutlookOpen = false;

        if (this.axc == null) {
            this.axc = new ActiveXComponent("Outlook.Application");

            //Wait some sec to open Outlook
            // why? set to 2sec
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.dipOutlook = axc.getObject();
            if (this.dipOutlook != null) {
                bolOutlookOpen = true;
                this.dipNamespace = axc.getProperty("Session").toDispatch();

                Status.print("Outlook opened");
            }
        } else {
            bolOutlookOpen = true;
        }

        return bolOutlookOpen;
    }

    public void closeOutlook(Boolean bolCloseOutlook) {
        StringSelection stringSelection = new StringSelection("");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);

        if (bolCloseOutlook)
            this.axc.invoke("Quit", new Variant[]{});

        this.dipOutlook.safeRelease();
        this.axc.safeRelease();
        this.dipNamespace.safeRelease();

        ComThread.Release();
        ComThread.quitMainSTA();

        this.dipOutlook = null;
        this.dipNamespace = null;
        this.axc = null;

        Status.print("Outlook closed");
    }

    /**
     * Protected Section
     */
    protected Dispatch getOutlookItem(String strToUpdateItemID) {
        return Dispatch.call(this.dipNamespace, "GetItemFromID", strToUpdateItemID).toDispatch();
    }

    protected void updateOutlookItem(Dispatch dipToUpdateItem) {
        Dispatch.call(dipToUpdateItem, "Save");

        dipToUpdateItem.safeRelease();
    }

    protected void deleteOutlookItem(String strToDeleteItemID) {
        Dispatch dipItem = Dispatch.call(this.dipNamespace, "GetItemFromID", strToDeleteItemID).toDispatch();

        Dispatch.call(dipItem, "Delete");

        dipItem.safeRelease();
    }


    /**
     * Abstract Interfaces
     */

    abstract protected Dispatch generatePutDispatchContent(T1 dataItem);

    abstract public void writeOutlookObjects(T2 allContent);
}
