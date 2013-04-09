/* 
 * SnortSqlEventMappingFactory.java        0.1.4 12/02/16
 *  
 * DEVELOPED BY DECOIT GMBH WITHIN THE ESUKOM-PROJECT:
 * http://www.decoit.de/
 * http://www.esukom.de/cms/front_content.php?idcat=10&lang=1
 * 
 * DERIVED FROM  THE DHCP-IFMAP-CLIENT-IMPLEMENTATION DEVELOPED BY 
 * FHH/TRUST WITHIN THE IRON-PROJECT:
 * http://trust.inform.fh-hannover.de/joomla/
 * 
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file 
 * distributed with this work for additional information 
 * regarding copyright ownership.  The ASF licenses this file 
 * to you under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance 
 * with the License.  You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations 
 * under the License. 
 */
package de.esukom.decoit.ifmapclient.mappingfactory.impl;

import de.esukom.decoit.ifmapclient.config.GeneralConfig;
import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.mappingfactory.SnortEventMappingFactory;
import de.esukom.decoit.ifmapclient.mappingfactory.result.EventMappingResult;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * concrete implementation of abstract mapping-factory for mapping values from
 * polling-threads to event objects that can be send to MAP-Server
 * 
 * @version 0.1.4
 * @author Dennis Dunekacke, Decoit GmbH
 */
public class SnortSqlEventMappingFactory extends SnortEventMappingFactory {

    // predefined names of DB-columns to be used for getting data from DB-result
    private final String mColumnSignature = "sig_name";
    private final String mColumnEventId = "cid";
    private final String mColumnReftag = "ref_tag";
    private final String mColumnIp = "inet_ntoa(iphdr.ip_src)";
    private final String mColumnTimestamp = "timestamp";
    private final String mColumnSensorId = "sid";
    private final String mColumnIpVersion = "ip_ver";
    private final String mColumnPriority = "sig_priority";
    private final String mColumnSignatureClassName = "sig_class_name";
    private final String mColumnRefSystemName = "ref_system_name";

    public SnortSqlEventMappingFactory(Properties props, ArrayList<HashMap<String, String>> data) {
        super(props, data);
    }

    @Override
    protected void createMappingResult(Properties props, ArrayList<HashMap<String, String>> res) {

        // initialize properties
        super.initProperties(props);

        int lastEventCID = -1;
        EventMappingResult lastEntry = null;

        for (int i = 0; i < res.size(); i++) {
            boolean isFollowUp = false;
            // not(!) the first event-entry in result-set
            if (lastEventCID != -1) {
                // is the current entry a "follow up" entry (same cid)
                if (new Integer(res.get(i).get(mColumnEventId)).intValue() == lastEventCID) {
                    isFollowUp = true;
                }
            }

            // "follow up" entry - just add the next vulnerability-uri
            // to last event (...if there is one) separated by ";"
            if (isFollowUp && lastEntry != null) {
                // FeatureNotYetImplemented-Exception ;-)
            }

            // new entry
            else {
                // check if following event should be converted
                if (doConvert(getEventMappingForSignatureName(res.get(i).get(
                        mColumnSignatureClassName)))) {

                    // if there is a last entry, add it to the result list
                    // because there are no more "follow up" entries
                    if (lastEntry != null) {
                        // check time before adding new entry to result list
                        // check time
                        if (GeneralConfig.MESSAGING_SENDOLD) {
                            super.mapResult.add(lastEntry);
                            IfMapClient.LOGGER
                                    .fine("mapped snort event has been added to result list: "
                                            + lastEntry.showOnConsole());
                        } else {
                            if (Toolbox.getCalendarFromString(lastEntry.getDiscoveredTime(),"yyyy-MM-dd HH:mm:ss", null).after(
                                    Toolbox.getCalendarFromString(Toolbox.sClientStartTime,"yyyy-MM-dd HH:mm:ss", null))) {
                                super.mapResult.add(lastEntry);
                            }
                        }
                    }

                    EventMappingResult event = new EventMappingResult();

                    // dummy values for now...
                    event.setConfidence("100");
                    event.setMagnitude("45");

                    // real values, yo
                    /*
                     * There is a Problem here: There are some Snort-Event
                     * Messages that only differs in the source/target port. The
                     * Problem is that no Port-Number can be added to IP-Address
                     * (as defined in IF-MAP Specification), like
                     * XXX.XXX.XXX.XXX.YYYY Don't know where to put the
                     * Port-Number yet...so there might be some Event-Entries
                     * that seems to be identical when converted to IF-MAP
                     * Format due to missing Port-Number Entry...but in
                     * "reality" they are not!
                     */
                    event.setIpType("IPv" + res.get(i).get(mColumnIpVersion).toString());
                    event.setIp(res.get(i).get(mColumnIp));

                    event.setDiscoveredTime(convertDateToIFMAPDate(res.get(i).get(mColumnTimestamp)));
                    event.setDiscovererId(res.get(i).get(mColumnSensorId));

                    // map significance from database to significance-datatype
                    // from
                    // ifmaplibj
                    event.setSignificance(getSignificanceValue(new Integer(res.get(i).get(
                            mColumnPriority)).intValue()));
                    event.setName(res.get(i).get(mColumnSignature));
                    event.setEventMessageType(getEventMappingForSignatureName(res.get(i).get(
                            mColumnSignatureClassName)));
                    
                    // set created object to lastObject, so it can be
                    // checked for follow up entries during next loop-cycle
                    lastEntry = (EventMappingResult) event.clone();
                }

                // set the last CID -> used to determine follow up
                // entries in next loop-cycle
                lastEventCID = new Integer(res.get(i).get(mColumnEventId)).intValue();

            }
        }

        // add last entry to list
        if (lastEntry != null) {
            if (GeneralConfig.MESSAGING_SENDOLD) {
                super.mapResult.add(lastEntry);
            } else {
                if (Toolbox.getCalendarFromString(lastEntry.getDiscoveredTime(),"yyyy-MM-dd HH:mm:ss", null).after(
                        Toolbox.getCalendarFromString(Toolbox.sClientStartTime,"yyyy-MM-dd HH:mm:ss", null))) {
                    super.mapResult.add(lastEntry);
                    IfMapClient.LOGGER.fine("mapped snort event has been added to result list: "
                            + lastEntry.showOnConsole());
                }
            }
        }
    }

    public String getColumnReftag() {
        return mColumnReftag;
    }
    
    /**
     * convert passed in string to string in IFMAP-Timestamp format
     * 
     * @param String
     *            currentDate (YYYY-MM-DD hh:mm:ss.S)
     * 
     * @return timestamp in IF-MAP format
     */

    private String convertDateToIFMAPDate(String currentDate) {
        // YYYY-MM-DD hh:mm:ss.S => [0]Date [1]Time
        String[] timestamp = currentDate.split(" ");

        // YYYY-MM-DD => [0]Year [1] Month [2] Day
        String[] date = timestamp[0].split("-");

        // fomrat according to ifmap-specification
        String newDate = date[0] + "-" + date[1] + "-" + date[2] + "T" + timestamp[1];
        return newDate.substring(0, newDate.length() - 2) + "Z";
    }
}