/*
 * SnortFilePollingThread.java 0.2 13/02/07
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

package de.esukom.decoit.ifmapclient.pollingthreads.file;

import de.esukom.decoit.ifmapclient.main.IfMapClient;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * thread for polling the alertlog-file for new snort-events
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class OpenVPNFilePollingThread extends FilePollingThread {

    /**
     * constructor
     * 
     * @param path path of the log-file
     */
    public OpenVPNFilePollingThread(final Properties pr) {
        super.initProperties(pr);
    }

    /**
     * read and parse alert-log-file
     */
    @Override
    protected ArrayList<HashMap<String, String>> readFile() {
        BufferedReader input = null;
        ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();

        try {
            input = new BufferedReader(new FileReader(file), 1);
            String line = null;
            while ((line = input.readLine()) != null) {
                HashMap<String, String> currentEntry = new HashMap<String, String>();
                currentEntry.put("0", line);
                result.add(currentEntry);
            }
            isFirstStart = false;
        } catch (FileNotFoundException ex) {
            IfMapClient.exit("could not found log-file at " + filePath);
        } catch (IOException ex) {
            IfMapClient.exit("I/O error while reading log-file at " + filePath);
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                IfMapClient.exit("error while closing input buffer");
            }
        }
        return result;
    }
}