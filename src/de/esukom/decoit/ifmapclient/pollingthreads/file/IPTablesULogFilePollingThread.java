/*
 * IPTablesULogFilePollingThread.java 0.2 13/02/07
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

import de.esukom.decoit.ifmapclient.config.GeneralConfig;
import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * Thread for polling the ulog-File for new iptables-events
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DDECOIT GmbH
 */
public class IPTablesULogFilePollingThread extends FilePollingThread {

    // ip-addresses contained here will not be processed
    private String[] mIpBlacklist;

    // log-skipped-entries-flag
    private boolean mLogSkippedEntries;

    /**
     * constructor
     * 
     * @param path path of snort-log-file
     */
    public IPTablesULogFilePollingThread(final Properties pr) {
        initProperties(pr);
    }

    @Override
    protected void initProperties(final Properties props) {
        super.initProperties(props);

        // ip-blacklist initialization
        mIpBlacklist = props.getProperty("iptables.iplist.blacklist").split(",");
        IfMapClient.LOGGER.config("reading in ip-blacklist from enforcement.properties");
        if (mIpBlacklist == null || mIpBlacklist.length < 1) {
            IfMapClient.LOGGER.config("no ip-addresses defined in blacklist");
        }
        for (int i = 0; i < mIpBlacklist.length; i++) {
            IfMapClient.LOGGER.config("entry [" + i + "] on blacklist: " + mIpBlacklist[i]);
        }

        // log skipped entries flag
        mLogSkippedEntries = Toolbox.getBoolPropertyWithDefault("iptables.log.skippedentries", false, props);
    }

    /**
     * read and parse alert-log-file
     * 
     * @return list containing all read in entries from ulog-file
     */
    @Override
    protected ArrayList<HashMap<String, String>> readFile() {
        int currentLineInSingleEntry = 0;
        int currentCycleLineNumber = 0;

        BufferedReader input = null;
        ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> tempEventData = new HashMap<String, String>();

        try {
            input = new BufferedReader(new FileReader(file), 1);
            String line = null;
            String entryDate = null;
            boolean skipCurrentEntry = false;

            while ((line = input.readLine()) != null) {
                if (isFirstStart) {
                    lastEntryLineNumber++;
                }

                currentCycleLineNumber++;
                if (isFirstStart | currentCycleLineNumber > lastEntryLineNumber) {
                    if (!skipCurrentEntry) {
                        // convert last entry
                        if (!tempEventData.isEmpty()) {
                            result.add((HashMap<String, String>) tempEventData.clone());
                        }
                    }
                    else {
                        // reset skip-current-entry-flag
                        skipCurrentEntry = false;
                    }

                    // reset entries from last cycle
                    tempEventData.clear();

                    // reset current entry line counter
                    currentLineInSingleEntry = 0;

                    // get ip of current entry and perform blacklist-check
                    Matcher ip4Matcher = Toolbox.getRegExPattern("regex.ip4").matcher(line);
                    if (ip4Matcher.find()) {
                        if (inBlacklist(ip4Matcher.group())) {
                            skipCurrentEntry = true;
                            if (mLogSkippedEntries) {
                                IfMapClient.LOGGER.warning("entry is inside ip-blacklist [" + ip4Matcher.group()
                                        + "]...skipping");
                            }

                        }
                    }
                    else {
                        skipCurrentEntry = true;
                        IfMapClient.LOGGER.warning("could not find IP4-address in current entry...skipping");
                    }

                    if (!skipCurrentEntry) {
                        // get timestamp of current entry
                        Matcher timestampMatcher = Toolbox.getRegExPattern("regex.ulogtimestamp").matcher(line);
                        if (timestampMatcher.find()) {
                            // convert entry to "ifmap-compatible" format to perform date check
                            entryDate = rearrangeDate(Toolbox.getNowDateAsString("yyyy") + "/"
                                    + timestampMatcher.group());
                            // replace current entries date with new format
                            String newLine = line.replaceFirst(Toolbox.getRegExPattern("regex.ulogtimestamp")
                                    .toString(), entryDate);
                            line = newLine;
                        }
                        else {
                            skipCurrentEntry = true;
                            IfMapClient.LOGGER
                                    .warning("could not find timestamp in current entry...will now skip this entry");
                        }

                        // if "sendold"-option is disabled, skip entries which time-stamp is before
                        // the clients startup-time. Will be done here (instead of Mapping-Factory)
                        // because the ulog-file can really grow big an we don't want to pass all
                        // these non-used entries through the mapping factory due to performance
                        if (!GeneralConfig.MESSAGING_SENDOLD) {
                            if (Toolbox.getCalendarFromString(entryDate, "yyyy-MM-dd HH:mm:ss", null)
                                    .after(Toolbox.getCalendarFromString(Toolbox.sClientStartTime,
                                            "yyyy-MM-dd HH:mm:ss", null))) {
                                tempEventData.put(new Integer(currentLineInSingleEntry).toString(), line);
                                currentLineInSingleEntry++;
                            }
                        }
                        else {
                            // add current entry without time-stamp-check
                            tempEventData.put(new Integer(currentLineInSingleEntry).toString(), line);
                        }
                    }
                }
            }

            // loop over, add last remaining entry
            if (!tempEventData.isEmpty() & !skipCurrentEntry) {
                result.add(tempEventData);
            }

            if (!isFirstStart) {
                // set new last entry index
                lastEntryLineNumber = currentCycleLineNumber;
            }
            else {
                // first cycle finished
                isFirstStart = false;
            }
        } catch (FileNotFoundException ex) {
            IfMapClient.exit("could not find ip-tables ulog-file.");
        } catch (IOException ex) {
            IfMapClient.exit("I/O error while reading iptables ulog-file");
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                IfMapClient.exit("error while closing input buffer");
            }
        }

        return result;
    }

    /**
     * rearrange passed in date (as parsed from alertlog-file) to fit IF-MAP Timestamp format
     * 
     * @param String currentDate [YYYY/MONTH(3-chars)_DD_HH:MM:SS]
     * 
     * @return timestamp in IF-MAP format
     */
    private String rearrangeDate(String currentDate) {
        String[] date = new String[3];
        date[0] = currentDate.substring(0, 4); // Year
        date[1] = Toolbox.getAplhaNumericMonthMap().get(currentDate.substring(5, 8));

        // if the date consist only of one digit, u-log doesn't fill it up with a leading
        // zero (e.g 02.08.2011), instead it leaves a space (e.g. 2.08.2011)
        date[2] = currentDate.substring(9, 11); // Day
        if (date[2].startsWith(" ")) {
            date[2] = date[2].replaceFirst(" ", "0");
        }

        // YYYY/MM/DD-hh:mm:ss.S => [0]Date [1]Time
        String timestamp = currentDate.substring(12, currentDate.length()); // Day

        // return new timestamp-string
        return date[0] + "-" + date[1] + "-" + date[2] + " " + timestamp;
    }

    /**
     * check if passed in ip-address-string is contained inside ip-blacklist
     * 
     * @param ip ip-address to be checked
     * 
     * @return true, if passed i address contained in blacklist
     */
    private boolean inBlacklist(String ip) {
        for (int i = 0; i < mIpBlacklist.length; i++) {
            if (mIpBlacklist[i].equals(ip)) {
                return true;
            }
        }
        return false;
    }
}