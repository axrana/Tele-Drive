//
// Copyright Aliaksei Levin (levlam@telegram.org), Arseny Smirnov (arseny30@gmail.com) 2014-2026
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.drinkless.tdlib;

/**
 * Class for TDLib log manipulation.
 */
public final class Log {
    /**
     * Sets the verbosity level of the internal logging of TDLib.
     *
     * @param verbosityLevel New value of the verbosity level for logging. Value 0 corresponds to fatal errors,
     *                       value 1 corresponds to errors, value 2 corresponds to warnings and debug info,
     *                       value 3 corresponds to verbose debug info, value 4 and higher can be used for internal
     *                       TDLib logging. Default value is 5.
     * @return true on success.
     */
    public static native boolean setVerbosityLevel(int verbosityLevel);

    /**
     * Sets the path to the file to where the internal TDLib log will be written.
     * By default, TDLib writes logs to stderr or an OS specific log.
     *
     * @param path Path to the file to where the internal TDLib log will be written.
     * @return true on success.
     */
    public static native boolean setFilePath(String path);

    /**
     * Sets the maximum size of the file to where the internal TDLib log is written before the file will be rotated.
     *
     * @param maxFileSize The maximum size of the log file to be rotated, in bytes.
     */
    public static native void setMaxFileSize(long maxFileSize);
}
