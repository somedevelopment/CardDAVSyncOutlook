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

import java.io.File;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author Alexander Bikadorov <abiku@cs.tu-berlin.de>
 */
public final class Config extends PropertiesConfiguration {

    private static Config INSTANCE = null;

    public final static String ACC_USER = "acc.user";
    public final static String ACC_PASS = "acc.pass";
    public final static String ACC_SAVE_PASS = "acc.save_pass";
    public final static String ACC_URL = "acc.url";
    public final static String ACC_SSL = "acc.ssl";
    public final static String ACC_OUTLOOK_FOLDER = "acc.outlook_folder";
    public final static String GLOB_CLOSE = "glob.close";
    public final static String GLOB_CORRECT_NUMBERS = "glob.clear_region";
    public final static String GLOB_REGION_CODE = "glob.region_code";

    private Config() {
        super();
    }

    public void saveToFile() {
        try {
            this.save();
        } catch (ConfigurationException e) {
            System.err.println("can't save configuration");
            e.printStackTrace();
        }
    }

    private static void initialize(String filePath) {
        INSTANCE = new Config();
        INSTANCE.setListDelimiter((char) 9);

        //String confDir = "conf";
        //new File(confDir).mkdir();
        INSTANCE.setFileName(filePath);
        File configFile = new File(filePath);
        try {
            INSTANCE.load(filePath);
        } catch (ConfigurationException e) {
            System.out.println("Configuration file not found: "
                    + configFile.getAbsolutePath()
                    + "\n  (Using default values)");
        }
    }

    public static void setFile(String filePath) {
        if (INSTANCE != null) {
            System.err.println("configuration file already loaded");
            return;
        }
        initialize(filePath);
    }

    public static Config getInstance() {
        if (INSTANCE == null) {
            String filePath = "conf" + File.separator + "config.properties";
            initialize(filePath);
        }
        return INSTANCE;
    }
}

