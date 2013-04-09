/*
 * SnortBarnyardFileEventMappingFactory.java 0.2 13/02/16
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

import de.esukom.decoit.ifmapclient.config.GeneralConfig;
import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.mappingfactory.SnortEventMappingFactory;
import de.esukom.decoit.ifmapclient.mappingfactory.result.EventMappingResult;
import de.esukom.decoit.ifmapclient.messaging.SOAPMessageSender;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import de.fhhannover.inform.trust.ifmapj.metadata.EventType;

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
public class SnortBarnyardFileEventMappingFactory extends SnortEventMappingFactory {

    // flag detecting whether to skip ip6 addresses or not
    private final boolean mSkipIP6 = true;

    /**
     * constructor
     * 
     * @param props properties object
     * @param data List of Hash-Maps containing data from file poller
     */
    public SnortBarnyardFileEventMappingFactory(final Properties props, final ArrayList<HashMap<String, String>> data) {
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
            EventType msgType = null;
            String discTime = null;

            // get classification <-> event message type
            Matcher classMatcher = Toolbox.getRegExPattern("regex.class").matcher(hashMap.get("0"));
            if (classMatcher.find()) {
                msgType = getEventMappingForSignatureName(classMatcher.group().replace("Classification: ", ""));
            }
            else {
                // use default classification type
                msgType = EventType.other;
                IfMapClient.LOGGER.warning("could not find classification for entry]...using default (other)");
            }

            // discovered time
            Matcher timestampMatcher = Toolbox.getRegExPattern("regex.timestamp").matcher(hashMap.get("0"));
            if (timestampMatcher.find()) {
                discTime = Toolbox.getNowDateAsString("yyyy") + "/" + timestampMatcher.group();

                // check of current entry has a timestamp that was before the IF-MAP-Client started
                if (!GeneralConfig.MESSAGING_SENDOLD) {
                    if (!Toolbox.getCalendarFromString(discTime, "yyyy/MM/dd-HH:mm:ss", null).after(
                            Toolbox.getCalendarFromString(Toolbox.sClientStartTime, "yyyy-MM-dd HH:mm:ss", null))) {
                        discTime = null;
                    }
                }

            }
            else {
                IfMapClient.LOGGER.warning("could not find timestamp for entry...skipping entry!");
            }

            // check if event should be converted and send to server
            if (msgType != null && discTime != null && doConvert(msgType)) {
                EventMappingResult event = new EventMappingResult();

                // set event message-type
                event.setEventMessageType(msgType);

                // set discovered time
                event.setDiscoveredTime(Toolbox.convertTimestampToIfMapFormat(discTime, "/", "-"));

                // check (and maybe set) source-ip
                Matcher ipMatcher = Toolbox.getRegExPattern("regex.ip4").matcher(hashMap.get("0"));
                if (ipMatcher.find()) {
                    event.setIp(ipMatcher.group());
                    event.setIpType("IPv4");
                }
                else {
                    // if no IPv4-Address, check for IPv6-address, in case that
                    // IPv6-Addresses should also be send to server
                    // TODO: this is currently detected by class-field. should
                    // be integrated into config-file soon(er or later)
                    if (!mSkipIP6) {
                        Matcher ip6Matcher = Toolbox.getRegExPattern("regex.ip6").matcher(hashMap.get("0"));
                        if (ip6Matcher.find()) {
                            event.setIp(Toolbox.convertIP6AddressToIFMAPIP6AddressPattern(ip6Matcher.group()));
                            event.setIpType("IPv6");
                        }
                    }
                }

                // only continue if a source-ip was found
                if (event.getIp() != null) {

                    // set event name
                    Matcher typeMatcher = Toolbox.getRegExPattern("regex.type").matcher(hashMap.get("0"));
                    if (typeMatcher.find()) {
                        event.setName(typeMatcher.group().replace("] ", "").replace(" [", ""));
                    }
                    else {
                        IfMapClient.LOGGER.warning("could not find event name for entry");
                    }

                    // priority
                    Matcher priorityMatcher = Toolbox.getRegExPattern("regex.priority").matcher(hashMap.get("0"));
                    if (priorityMatcher.find()) {
                        event.setSignificance(getSignificanceValue(new Integer(priorityMatcher.group().replace(
                                "Priority: ", "")).intValue()));
                    }
                    else {
                        IfMapClient.LOGGER.warning("could not find priority for entry");
                    }

                    // vulnerabilty-url
                    if (hashMap.get("0") != null) {
                        Matcher vulnaribilityMatcher = Toolbox.getRegExPattern("regex.vulnaribilityuri").matcher(
                                hashMap.get("0"));
                        if (vulnaribilityMatcher.find()) {
                            event.setVulnerabilityUri(vulnaribilityMatcher.group().replace("[Xref => ", "")
                                    .replace("]h", " ; h").replace("]", ""));
                            event.setVulnerabilityUri("http://cve.mitre.org/cgi-bin/cvename.cgi?name=2005-0068");
                        }
                    }

                    // discoverer id
                    event.setDiscovererId(SOAPMessageSender.getInstance().getIfMapPublisherId());

                    // dummy values for now...
                    event.setConfidence("100");
                    event.setMagnitude("45");

                    // add new event-result-entry to final result
                    super.mapResult.add(event);
                    IfMapClient.LOGGER.fine("mapped snort event has been added to result list: "
                            + event.showOnConsole());
                }
            }
        }
    }
}