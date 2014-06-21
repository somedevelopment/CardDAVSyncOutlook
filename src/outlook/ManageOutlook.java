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

public abstract class ManageOutlook {

    static {
        String workingDir = new File(System.getProperty("user.dir")).getAbsolutePath();
        String libDir = workingDir + File.separator + "lib";
        String bitness = System.getProperty("sun.arch.data.model");
        String libName;
        if (bitness.equals("64"))
            libName = "jacob-1.17-x64.dll";
        else
            libName = "jacob-1.17-x86.dll";
        try {
            System.load(libDir + File.separator + libName);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load.\n" + e);
            System.exit(1);
        }
    }

    static private ActiveXComponent axc;

    static protected Dispatch dipNamespace;
    static protected Dispatch dipOutlook;
    static protected String strWorkingDir;

    protected int intOutlookFolder;

    /**
     * Constructors
     */
    protected ManageOutlook(String strWorkingDir, int intOutlookFolder) {
        ManageOutlook.axc = null;
        ManageOutlook.dipNamespace = null;
        ManageOutlook.dipOutlook = null;

        ManageOutlook.strWorkingDir = strWorkingDir;
        this.intOutlookFolder = intOutlookFolder;
    }

    /**
     *
     * Public Section
     *
     */
    public Boolean openOutlook() {
        ComThread.InitMTA(true);

        Boolean bolOutlookOpen = false;

        if (ManageOutlook.axc == null) {
            ManageOutlook.axc = new ActiveXComponent("Outlook.Application");

            //Wait some sec to open Outlook
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ManageOutlook.dipOutlook = axc.getObject();
            if (ManageOutlook.dipOutlook != null) {
                bolOutlookOpen = true;
                ManageOutlook.dipNamespace = axc.getProperty("Session").toDispatch();

                Status.printStatusToConsole("Outlook opened");
            }
        } else {
            bolOutlookOpen = true;
        }

        return bolOutlookOpen;
    }

    public void closeOutlook() {
        StringSelection stringSelection = new StringSelection("");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);

        ManageOutlook.axc.invoke("Quit", new Variant[]{});

        ManageOutlook.dipOutlook.safeRelease();
        ManageOutlook.axc.safeRelease();
        ManageOutlook.dipNamespace.safeRelease();

        ComThread.Release();
        ComThread.quitMainSTA();

        ManageOutlook.dipOutlook = null;
        ManageOutlook.dipNamespace = null;
        ManageOutlook.axc = null;

        Status.printStatusToConsole("Outlook closed");
    }

    /**
     * Protected Section
     */
    protected Dispatch getOutlookItem(String strToUpdateItemID) {
        return Dispatch.call(ManageOutlook.dipNamespace, "GetItemFromID", strToUpdateItemID).toDispatch();
    }

    ;

    protected String getNewOutlookItem() {
        Dispatch dipItem = Dispatch.call(ManageOutlook.dipOutlook, "CreateItem", new Variant(2)).toDispatch();

        String strNewItemEntryID = Dispatch.get(dipItem, "EntryID").toString().trim();

        dipItem.safeRelease();

        return strNewItemEntryID;
    }

    protected void updateOutlookItem(Dispatch dipToUpdateItem) {
        Dispatch.call(dipToUpdateItem, "Save");

        dipToUpdateItem.safeRelease();
    }

    ;

    protected void deletOutlookItem(String strToDeleteItemID) {
        Dispatch dipItem = Dispatch.call(ManageOutlook.dipNamespace, "GetItemFromID", strToDeleteItemID).toDispatch();

        Dispatch.call(dipItem, "Delete");

        dipItem.safeRelease();
    }

    ;



    /**
     * Abstract Interfaces
     */

    abstract protected Dispatch generatePutDispatchContent(Object dataItem);

    abstract public void writeOutlookObjects(Object allContant);

    abstract public void loadContantFromOutlook(Object allContant);

}
