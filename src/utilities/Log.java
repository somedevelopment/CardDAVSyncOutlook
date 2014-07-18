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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.PrintStream;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Set up logging globally.
 *
 * @author Alexander Bikadorov
 */
public class Log {

    private static final Level STDOUT = new StdOutErrLevel("STDOUT", Level.INFO.intValue()+53);
    private static final Level STDERR = new StdOutErrLevel("STDERR", Level.INFO.intValue()+54);
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * Write stdout and stderr to console and log file at the same time.
     * Based on https://blogs.oracle.com/nickstephen/entry/java_redirecting_system_out_and
     * and http://stackoverflow.com/a/10288881
     * @throws IOException
     */
    public static void init() throws IOException {

        LogManager logManager = LogManager.getLogManager();
        logManager.reset();

        Handler fileHandler = new FileHandler("log.txt", 10000, 1, true);

        fileHandler.setFormatter(new Formatter(){
            @Override
            public String format(LogRecord record) {
                return record.getMessage() + LINE_SEPARATOR;
            }
        });
        Logger.getLogger("").addHandler(fileHandler);

        // now rebind stdout/stderr to logger
        Logger logger;
        LoggingOutputStream los;

        logger = Logger.getLogger("stdout");
        los = new LoggingOutputStream(logger, Log.STDOUT);
        System.setOut(new TeeStream(System.out, new PrintStream(los, true)));

        logger = Logger.getLogger("stderr");
        los = new LoggingOutputStream(logger, Log.STDERR);
        System.setErr(new TeeStream(System.err, new PrintStream(los, true)));
    }

    private static class StdOutErrLevel extends Level {

        private StdOutErrLevel(String name, int value) {
            super(name, value);
        }

        protected Object readResolve()
            throws ObjectStreamException {
            if (this.intValue() == STDOUT.intValue())
                return STDOUT;
            if (this.intValue() == STDERR.intValue())
                return STDERR;
            throw new InvalidObjectException("Unknown instance :" + this);
        }
    }

    private static class LoggingOutputStream extends ByteArrayOutputStream {
        private final Logger logger;
        private final Level level;

        LoggingOutputStream(Logger logger, Level level) {
            super();
            this.logger = logger;
            this.level = level;
        }

        @Override
        public void flush() throws IOException {
            String record;
            synchronized(this) {
                super.flush();
                record = this.toString();
                super.reset();

                if (record.length() == 0 || record.equals(LINE_SEPARATOR)) {
                    // avoid empty records
                    return;
                }
                logger.logp(level, "", "", record);
            }
        }
    }

    private static class TeeStream extends PrintStream {
        private final PrintStream out2;

        TeeStream(PrintStream out1, PrintStream out2) {
            super(out1);
            this.out2 = out2;
        }

        @Override
        public void write(byte buf[], int off, int len) {
            try {
                super.write(buf, off, len);
                out2.write(buf, off, len);
            } catch (Exception e) {
            }
        }

        @Override
        public void flush() {
            super.flush();
            out2.flush();
        }
    }
}
