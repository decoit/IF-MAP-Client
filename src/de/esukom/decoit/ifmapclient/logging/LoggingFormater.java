/*
 * LoggingFormater.java 0.2 13/02/16
 * 
 * DEVELOPED BY DECOIT GMBH WITHIN THE ESUKOM-PROJECT: http://www.decoit.de/
 * http://www.esukom.de/cms/front_content.php?idcat=10&lang=1
 * 
 * DERIVED FROM THE DHCP-IFMAP-CLIENT-IMPLEMENTATION DEVELOPED BY FHH/TRUST WITHIN THE IRON-PROJECT:
 * http://trust.inform.fh-hannover.de/joomla/
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package de.esukom.decoit.ifmapclient.logging;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Class for formating log-messages
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class LoggingFormater extends Formatter {

    private final Date mDate = new Date();
    private final DateFormat mDateformater = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

    /**
     * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
     */
    @Override
    public String format(LogRecord record) {
        mDate.setTime(record.getMillis());

        StringBuffer sb = new StringBuffer();

        // Date
        sb.append(mDateformater.format(mDate));
        sb.append(" ");

        // Class
        if (record.getSourceClassName() != null) {
            sb.append(record.getSourceClassName());
        }
        else {
            sb.append(record.getLoggerName());
        }

        /* Method */
        if (record.getSourceMethodName() != null) {
            sb.append(" ");
            sb.append(record.getSourceMethodName());
            sb.append("() ");
        }

        // Level
        sb.append(record.getLevel().getName());
        sb.append(" - ");

        // Message
        sb.append(record.getMessage());

        // Newline
        sb.append(System.getProperty("line.separator"));

        return sb.toString();
    }
}
