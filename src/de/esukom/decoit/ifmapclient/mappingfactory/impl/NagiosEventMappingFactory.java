/*
 * NagiosEventMappingFactory.java 0.2 13/02/08
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
import java.util.HashMap;
import java.util.Properties;

/**
 * concrete implementation of abstract mapping-factory for mapping values from polling-threads to
 * event objects that can be send to MAP-Server
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class NagiosEventMappingFactory extends SimpleEventMappingFactory {

    // name of keys for result-hashmap-list from file-poller
    private final String mTimestampKey = "timestamp";
    private final String mAddressKey = "address";
    private final String mEventsourceKey = "source";
    private final String mEventstateKey = "state";

    /**
     * constructor
     * 
     * @param props properties object
     * @param data List of Hash-Maps containing data from file poller
     */
    public NagiosEventMappingFactory(final Properties props, final ArrayList<HashMap<String, String>> data) {
        super(props, data);
    }

    /**
     * map result-set to a list of attribute-value-pairs
     * 
     * @param resultSet result-set of database query
     * 
     * @return ArrayList list containing the result-entries as hashmaps
     */
    @Override
    protected void createMappingResult(final Properties props, final ArrayList<HashMap<String, String>> data) {
        String missingValue = null;
        EventMappingResult event = new EventMappingResult();

        for (HashMap<String, String> hashMap : data) {
            if (hashMap.get(mTimestampKey) != null) {
                event.setDiscoveredTime(Toolbox.convertTimestampToIfMapFormat(hashMap.get(mTimestampKey), "/", "-"));
            }
            else {
                missingValue = "event.discovered_time";
                break;
            }

            // ip-address
            if (hashMap.get(mAddressKey) != null) {
                event.setIp(hashMap.get(mAddressKey));
            }
            else {
                missingValue = "event.ip";
                break;
            }

            // event-name
            if (hashMap.get(mEventsourceKey) != null && hashMap.get(mEventstateKey) != null) {
                String eventName = null;
                if (hashMap.get(mEventsourceKey).startsWith("host")) {
                    eventName = "Detected Host State: " + hashMap.get(mEventstateKey);
                }
                else if (hashMap.get(mEventsourceKey).startsWith("service")) {
                    eventName = "Detected Service State: " + hashMap.get(mEventstateKey);
                }
                else {
                    eventName = "Undefined Event Name";
                }
                event.setName(eventName);
            }
            else {
                missingValue = "event.name";
                break;
            }

            // set predefined values from mapping.properties
            event.setIpType("IPv4");
            event.setDiscovererId(SOAPMessageSender.getInstance().getIfMapPublisherId());
            event.setSignificance(mSignificanceDefault);
            event.setEventMessageType(mEventtypeDefault);
            event.setConfidence(mConfidenceDefault);
            event.setMagnitude(mMagnitudeDefault);
        }

        if (missingValue == null) {
            super.mapResult.add(event);
        }
        else {
            IfMapClient.LOGGER.warning("mapping of nagios-event failed - cannot find values" + missingValue);
            super.mapResult = null;
        }
    }
}