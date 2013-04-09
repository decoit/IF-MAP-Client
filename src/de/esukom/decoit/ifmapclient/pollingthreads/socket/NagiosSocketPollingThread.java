/*
 * NagiosSocketPollingThread.java 0.2 13/02/08
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

package de.esukom.decoit.ifmapclient.pollingthreads.socket;

import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

/**
 * Thread for listening and receiving nagios-events over a socket connection
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class NagiosSocketPollingThread extends SocketPollingThread {

    /**
     * constructor
     * 
     * @param props properties-object containing values for initialization
     */
    public NagiosSocketPollingThread(final Properties pr) {
        super.initProperties(pr);
    }

    @Override
    protected HashMap<String, String> parseLine(final String line) {
        /*
         * MAKRONAME:VALUE;MAKRONAME:VALUE;MAKRONAME:VALUE;... ONE LINE!
         * 
         * possible makro-values for hosts: hostname, hostalias, hostaddress, hoststate,
         * hoststatetype, hostoutput
         * 
         * possible makro-values for services:
         * 
         * servicestate, servicestatetype, serviceattempt, servicedescription serviceoutput,
         * servicelatency, serviceduration, servicedowntime servicenotes
         */
        // current validation rule: minimum of two chars separated by ":" and ending with ";"
        if (line != null && Toolbox.getRegExPattern("regex.valid").matcher(line).find()) {
            IfMapClient.LOGGER.fine("incomming line to parse: " + line);

            // initialize temporary nagios-makro-list
            HashMap<String, String> makrosList = new HashMap<String, String>();

            // get "makro:value" pair as array
            Scanner scanner = new Scanner(line);
            scanner.useDelimiter(";");
            while (scanner.hasNext()) {
                String pair = scanner.next();
                String[] parsedpair = pair.split("\\=");
                makrosList.put(parsedpair[0], parsedpair[1]);
            }
            return makrosList;
        }
        else {
            IfMapClient.LOGGER.warning("nagios polling-thread detected a unparsable line: " + line);
            return null;
        }
    }
}
