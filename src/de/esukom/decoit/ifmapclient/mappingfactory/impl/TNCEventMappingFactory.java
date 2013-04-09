/*
 * TNCEventMappingFactory.java 0.2 13/02/04
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

import de.esukom.decoit.ifmapclient.mappingfactory.SimpleEventMappingFactory;
import de.esukom.decoit.ifmapclient.mappingfactory.result.EventMappingResult;
import de.esukom.decoit.ifmapclient.messaging.SOAPMessageSender;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import java.util.ArrayList;
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
public class TNCEventMappingFactory extends SimpleEventMappingFactory {

    /**
     * constructor
     * 
     * @param props properties object
     * @param data List of Hash-Maps containing data from file poller
     */
    public TNCEventMappingFactory(final Properties props, final ArrayList<HashMap<String, String>> data) {
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
    protected void createMappingResult(final Properties props, final ArrayList<HashMap<String, String>> res) {
        for (HashMap<String, String> hashMap : res) {
            EventMappingResult event = new EventMappingResult();
            String currentDate = Toolbox.getNowDateAsString("yyyy-MM-dd");

            Matcher timestampMatcher = Toolbox.getRegExPattern("regex.timestamp").matcher(hashMap.get("0"));
            Matcher allowOrDenyMatcher = Toolbox.getRegExPattern("regex.allowdeny").matcher(hashMap.get("0"));
            Matcher ipMatcher = Toolbox.getRegExPattern("regex.ip4").matcher(hashMap.get("0"));

            if ((timestampMatcher != null && timestampMatcher.find())
                    && (allowOrDenyMatcher != null && allowOrDenyMatcher.find())
                    && (ipMatcher != null && ipMatcher.find())) {

                // discovered time = todays date + timestamp from result-entry
                event.setDiscoveredTime(currentDate + "T" + timestampMatcher.group() + "Z");
                event.setName("TNC-Server " + allowOrDenyMatcher.group() + " access for IP " + ipMatcher.group());
                event.setDiscovererId(SOAPMessageSender.getInstance().getIfMapPublisherId());
                event.setIp(ipMatcher.group());

                // set predefined mapping-values from config
                event.setSignificance(mSignificanceDefault);
                event.setEventMessageType(mEventtypeDefault);
                event.setConfidence(mConfidenceDefault);
                event.setMagnitude(mMagnitudeDefault);
                event.setIpType("IPv4");
                super.mapResult.add(event);
            }
        }
    }
}