/*
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
import de.esukom.decoit.ifmapclient.mappingfactory.result.EventMappingResult;
import de.esukom.decoit.ifmapclient.util.Toolbox;
import de.fhhannover.inform.trust.ifmapj.metadata.EventType;
import de.fhhannover.inform.trust.ifmapj.metadata.Significance;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

/**
 * 
 * @author Marcel Jahnke, DECOIT GmbH
 * 
 */
public class RadiusSQLEventMappingFactory extends RadiusEventMappingFactory {

    // Columnnames for the radacct table
    private final String mDbColumnEventId = "radacctid";
    private final String mDbColumnSessionId = "acctsessionid";
    private final String mDbColumnUsername = "username";
    private final String mDbColumnIfAddress = "nasipaddress";
    private final String mDbColumnAcctStarttime = "acctstarttime";
    private final String mDbColumnAcctStoptime = "acctstoptime";
    private final String mDbColumnSessionTimeDB_COLUMN_SESSION_TIME = "acctsessiontime";

    // Columnnames for the radpostauth table
    private boolean firstStart = true;
    private final String mDbColumnId = "id";
    private final String mDbColumnReply = "reply";
    private final String mDbColumnAuthenticationDate = "authdate";

    private EventType mEventtypeDefault;
    private String mMagnitudeDefault;
    private String mConfidenceDefault;
    private Significance mSignificanceDefault;

    public RadiusSQLEventMappingFactory(Properties props, ArrayList<HashMap<String, String>> data) {
        super(props, data);
    }

    @Override
    protected void createMappingResult(Properties props, ArrayList<HashMap<String, String>> data) {

        initProperties(props);

        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).containsKey(mDbColumnAcctStarttime)) {
                System.out.print(data.get(i).get(mDbColumnEventId) + " ");
                System.out.print(data.get(i).get(mDbColumnUsername) + " ");
                System.out.print(data.get(i).get(mDbColumnSessionId) + " ");
                System.out.print(data.get(i).get(mDbColumnIfAddress) + " ");
                System.out.print(data.get(i).get(mDbColumnAcctStarttime).replace(".0", "") + " ");

                if (data.get(i).get(mDbColumnAcctStoptime) != null) {
                    System.out.print(data.get(i).get(mDbColumnAcctStoptime).replace(".0", "") + " ");
                }

                System.out.println(data.get(i).get(mDbColumnSessionTimeDB_COLUMN_SESSION_TIME));
            }
            else {
                System.out.print(data.get(i).get(mDbColumnId) + " ");
                System.out.print(data.get(i).get(mDbColumnUsername) + " ");
                System.out.print(data.get(i).get(mDbColumnReply) + " ");
                System.out.println(data.get(i).get(mDbColumnAuthenticationDate).replace(".0", ""));
            }
        }

        if (!firstStart) {
            for (int i = 0; i < data.size(); i++) {
                SimpleDateFormat ifmapTimeStyle = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss'Z'");
                SimpleDateFormat parseStringDate = new SimpleDateFormat("yyyy-MM-DD kk:mm:ss");
                EventMappingResult event = new EventMappingResult();
                if (data.get(i).containsKey(mDbColumnAcctStoptime)) {
                    if (data.get(i).get(mDbColumnAcctStoptime) == null) {
                        event.setName("Accounting Start");
                        Date startTime = null;
                        try {
                            startTime = parseStringDate
                                    .parse(data.get(i).get(mDbColumnAcctStarttime).replace(".0", ""));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        event.setDiscoveredTime(ifmapTimeStyle.format(startTime).toString());

                    }
                    else {
                        Date stopTime = null;
                        try {
                            if (data.get(i).get(mDbColumnAcctStoptime) != null) {
                                stopTime = parseStringDate.parse(data.get(i).get(mDbColumnAcctStoptime)
                                        .replace(".0", ""));
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        event.setName("Accounting Stop");

                        event.setDiscoveredTime(ifmapTimeStyle.format(stopTime).toString());

                    }
                    // System.out.println(data.get(i).get(DB_COLUMN_USERNAME));
                    event.setIdentity(data.get(i).get(mDbColumnUsername));
                    if (event.getIdentity().equalsIgnoreCase(""))
                        event.setIdentity("Unknown");
                    event.setIp(data.get(i).get(mDbColumnIfAddress));
                    event.setIpType("IPv4");
                    event.setConfidence(mConfidenceDefault);
                    event.setMagnitude(mMagnitudeDefault);
                    event.setSignificance(mSignificanceDefault);
                    event.setEventMessageType(mEventtypeDefault);
                }
                else {
                    event.setName("Authentication: " + data.get(i).get(mDbColumnReply));
                    event.setIdentity(data.get(i).get(mDbColumnUsername));

                    Date authenticationTime = null;
                    try {
                        authenticationTime = parseStringDate.parse(data.get(i).get(mDbColumnAuthenticationDate)
                                .replace(".0", ""));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    event.setDiscoveredTime(ifmapTimeStyle.format(authenticationTime).toString());

                    if (data.get(i).get(mDbColumnReply).equalsIgnoreCase("Access-Accept")) {
                        event.setSignificance(Significance.critical);
                    }
                    else {
                        event.setSignificance(Significance.informational);
                    }

                    event.setIp("0.0.0.0");
                    event.setConfidence(mConfidenceDefault);
                    event.setMagnitude(mMagnitudeDefault);
                    event.setSignificance(mSignificanceDefault);
                    event.setEventMessageType(mEventtypeDefault);
                }

                super.mapResult.add(event);
            }
        }
        else {
            firstStart = false;
        }
        System.out.println("test");

    }

    /**
     * initialize config from property-file
     * 
     * @param conf properties-object
     */
    public void initProperties(Properties props) {
        boolean criticalErrorOcured = false;

        // event to be processed/mapped
        mEventtypeDefault = EventType.behavioralChange; // default!

        String eventTypeFromConfig = Toolbox.getStringProperty("radius.eventmapping.eventtype", props, false);

        if (mEventtypeDefault == null) {
            criticalErrorOcured = true;
        }

        // map event-type-string to ifmaplibj EventType-type
        if (eventTypeFromConfig == "botnet-infection") {
            mEventtypeDefault = EventType.botnetInfection;

        }
        else if (eventTypeFromConfig == "cve") {
            mEventtypeDefault = EventType.cve;

        }
        else if (eventTypeFromConfig == "excessive-flows") {
            mEventtypeDefault = EventType.excessiveFlows;

        }
        else if (eventTypeFromConfig == "other") {
            mEventtypeDefault = EventType.other;

        }
        else if (eventTypeFromConfig == "p2p") {
            mEventtypeDefault = EventType.p2p;

        }
        else if (eventTypeFromConfig == "policy-violation") {
            mEventtypeDefault = EventType.policyViolation;

        }
        else if (eventTypeFromConfig == "worm-infection") {
            mEventtypeDefault = EventType.wormInfection;

        }
        else {
            IfMapClient.LOGGER
                    .config("could not load default event-type value from radius mapping config...using default (behavioral-change)");
        }

        // magnitude mapping
        mMagnitudeDefault = Toolbox.getStringProperty("radius.eventmapping.magnitude", props, false);
        if (mMagnitudeDefault == null) {
            criticalErrorOcured = true;
        }

        // confidence mapping
        mConfidenceDefault = Toolbox.getStringProperty("radius.eventmapping.confidence", props, false);
        if (mConfidenceDefault == null) {
            criticalErrorOcured = true;
        }

        // significance mapping
        mSignificanceDefault = Significance.informational; // default!
        String significanceFromConfig = Toolbox.getStringProperty("radius.eventmapping.significance", props, false);

        // map significance-string to ifmaplibj significance datatype
        if (significanceFromConfig == "important") {
            mSignificanceDefault = Significance.important;

        }
        else if (significanceFromConfig == "critical") {
            mSignificanceDefault = Significance.critical;

        }
        else {
            IfMapClient.LOGGER
                    .config("could not load default significance value from radius mapping config...using default (informational)");
        }

        if (criticalErrorOcured) {
            IfMapClient.exit("error while initializing RadiusEventMappingFactory");
        }
    }

}
