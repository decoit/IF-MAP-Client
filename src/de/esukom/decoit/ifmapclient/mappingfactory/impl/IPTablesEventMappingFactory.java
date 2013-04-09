/*
 * IPTablesEventMappingFactory.java 0.2 13/02/13
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

package de.esukom.decoit.ifmapclient.mappingfactory.impl;

import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.mappingfactory.SimpleEventMappingFactory;
import de.esukom.decoit.ifmapclient.mappingfactory.result.EventMappingResult;
import de.esukom.decoit.ifmapclient.messaging.SOAPMessageSender;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * concrete implementation of abstract mapping-factory for mapping values from polling-threads to
 * event objects that can be send to MAP-Server
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class IPTablesEventMappingFactory extends SimpleEventMappingFactory {

    // defined in mapping.properties
    private int mDuplicateEntriesDelay;

    /**
     * constructor
     * 
     * @param props properties object
     * @param data List of Hash-Maps containing data from file poller
     */
    public IPTablesEventMappingFactory(final Properties props, final ArrayList<HashMap<String, String>> data) {
        super(props, data);
    }

    @Override
    protected void initProperties(final Properties props) {
        super.initProperties(props);

        // time delay for events (with the same name and address) to be considered "equal"
        mDuplicateEntriesDelay = Toolbox.getIntPropertyWithDefault("iptables.eventmapping.timedelay", 0, props, false);
    }

    /**
     * map result-set from database-query to a list of attribute-value-pairs (realized as ArrayList
     * of HashMaps for now)
     * 
     * @param resultSet result-set of database query
     * 
     * @return ArrayList list containing the DB-Entries as HashMaps
     */
    @Override
    protected void createMappingResult(final Properties props, final ArrayList<HashMap<String, String>> res) {
        // needed for comparing potential "duplicate" entries in result
        boolean isFirstEntry = true;
        String prevDiscTime = null;

        for (HashMap<String, String> hashMap : res) {
            String discTime = null;
            EventMappingResult event = new EventMappingResult();

            // timestamp -> discovered time
            Matcher timestampMatcher = Toolbox.getRegExPattern("regex.ifmaptimestamp").matcher(hashMap.get("0"));
            if (timestampMatcher != null && timestampMatcher.find()) {
                discTime = timestampMatcher.group();
            }
            else {
                IfMapClient.LOGGER.warning("could not find timestamp for current entry, skipping entry");
                continue;
            }

            // get source and destination-ip and append it to event-name
            String srcIp, dstIp = null;
            Matcher srcMatcher = Toolbox.getRegExPattern("regex.ip4.src").matcher(hashMap.get("0"));
            if (srcMatcher != null && srcMatcher.find()) {
                srcIp = srcMatcher.group().replace("SRC=", "");
            }
            else {
                IfMapClient.LOGGER.warning("could not find src ip for current entry, skipping entry");
                continue;
            }
            event.setIp(srcIp);

            Matcher dstMatcher = Toolbox.getRegExPattern("regex.ip4.dst").matcher(hashMap.get("0"));
            if (dstMatcher != null && dstMatcher.find()) {
                dstIp = dstMatcher.group().replace("DST=", "");
            }
            else {
                IfMapClient.LOGGER.warning("could not find dst ip for current entry, skipping entry");
            }

            // do not publish datastreams-detected-Events between MAP-Server and IPTables-Client!
            if (srcIp.equals(IfMapClient.sMapServerIP) & dstIp.equals(IfMapClient.sClientIP)
                    || srcIp.equals(IfMapClient.sClientIP) & dstIp.equals(IfMapClient.sMapServerIP)) {
                continue;
            }

            // set event name
            event.setName("datastream detected from " + srcIp + " to " + dstIp);

            // check if current entry equals last entry, if so throw it away because its duplicate:
            boolean insertEntry = false;
            if (!isFirstEntry) {
                for (int j = 0; j < super.mapResult.size(); j++) {
                    EventMappingResult tempEvent = (EventMappingResult) super.mapResult.get(j);
                    // check for same name and ip
                    if (tempEvent.getName().equals(event.getName()) & tempEvent.getIp().equals(event.getIp())) {
                        // compare time-stamps of both objects
                        Calendar prevCalendar = Toolbox
                                .getCalendarFromString(prevDiscTime, "yyyy-MM-dd HH:mm:ss", null);
                        prevCalendar.add(Calendar.SECOND, mDuplicateEntriesDelay * (-1));
                        if (Toolbox.getCalendarFromString(discTime, "yyyy-MM-dd HH:mm:ss", null).after(prevCalendar)) {
                            super.mapResult.remove(j);
                            insertEntry = true;
                        }
                        else {
                            prevDiscTime = discTime;
                        }
                    }
                    else {
                        insertEntry = true;
                    }
                }
            }
            else {
                isFirstEntry = false;
                insertEntry = true;

            }

            // generate "datastream detected" - event
            if (insertEntry) {
                event.setDiscovererId(SOAPMessageSender.getInstance().getIfMapPublisherId());

                // set predefined mapping-values from config
                event.setSignificance(mSignificanceDefault);
                event.setEventMessageType(mEventtypeDefault);
                event.setConfidence(mConfidenceDefault);
                event.setMagnitude(mMagnitudeDefault);
                event.setIpType("IPv4");

                // set time (and store it for date-comparison in next loop)
                prevDiscTime = discTime;
                event.setDiscoveredTime(Toolbox.convertTimestampToIfMapFormat(discTime, "-", " "));

                // add event
                super.mapResult.add(event);
                IfMapClient.LOGGER.fine("iptables event has been added to result: " + event.showOnConsole());
            }
        }
    }
}