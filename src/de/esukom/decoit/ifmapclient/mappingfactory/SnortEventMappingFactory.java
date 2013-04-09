/*
 * SnortEventMappingFactory.java 0.2 13/02/07
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

package de.esukom.decoit.ifmapclient.mappingfactory;

import de.esukom.decoit.ifmapclient.main.IfMapClient;

import de.fhhannover.inform.trust.ifmapj.metadata.EventType;
import de.fhhannover.inform.trust.ifmapj.metadata.Significance;

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
public abstract class SnortEventMappingFactory extends MappingFactory {

    // properties from mapping config file
    private HashMap<String, String> eventMap = null;
    private HashMap<String, String> significanceMap = null;

    // events to be processed
    private boolean logP2P = true;
    private boolean logCVE = true;
    private boolean logBotnetInfection = true;
    private boolean logWormInfection = true;
    private boolean logExcessiveFlows = true;
    private boolean logBehavioralChange = true;
    private boolean logPolicyViolation = true;
    private boolean logOther = true;

    /**
     * constructor
     * 
     * @param props properties object
     * @param data List of Hash-Maps containing data from file poller
     */
    public SnortEventMappingFactory(final Properties props, final ArrayList<HashMap<String, String>> data) {
        super(props, data);
    }

    @Override
    protected void initProperties(final Properties props) {
        // initialize event-type/classification mapping
        String[] behavioralChange;
        String[] botnetInfection;
        String[] cve;
        String[] excessiveFlows;
        String[] other;
        String[] p2p;
        String[] policyViolation;
        String[] wormInfection;

        if (!checkConfig(behavioralChange = props.getProperty("snort.eventmapping.behavioral_change").split(","))) {
            behavioralChange = null;
        }
        if (!checkConfig(botnetInfection = props.getProperty("snort.eventmapping.botnet_infection").split(","))) {
            botnetInfection = null;
        }
        if (!checkConfig(cve = props.getProperty("snort.eventmapping.cve").split(","))) {
            cve = null;
        }
        if (!checkConfig(excessiveFlows = props.getProperty("snort.eventmapping.excessive_flows").split(","))) {
            excessiveFlows = null;
        }
        if (!checkConfig(other = props.getProperty("snort.eventmapping.other").split(","))) {
            other = null;
        }
        if (!checkConfig(p2p = props.getProperty("snort.eventmapping.p2p").split(","))) {
            p2p = null;
        }
        if (!checkConfig(policyViolation = props.getProperty("snort.eventmapping.policy_violation").split(","))) {
            policyViolation = null;
        }
        if (!checkConfig(wormInfection = props.getProperty("snort.eventmapping.worm_infection").split(","))) {
            wormInfection = null;
        }

        // combine mapping-properties to new hashmap
        eventMap = combineEventArrays(behavioralChange, botnetInfection, cve, excessiveFlows, other, p2p,
                policyViolation, wormInfection);

        // initialize significance/priority mapping
        String[] critical = props.getProperty("snort.significancemapping.critical").split(",");
        String[] informational = props.getProperty("snort.significancemapping.informational").split(",");
        String[] important = props.getProperty("snort.significancemapping.important").split(",");

        // create hash-map from significance-values
        HashMap<String, String> significance = new HashMap<String, String>();
        for (int i = 0; i < critical.length; i++) {
            significance.put(critical[i], "critical");
        }
        for (int i = 0; i < informational.length; i++) {
            significance.put(informational[i], "informational");
        }
        for (int i = 0; i < important.length; i++) {
            significance.put(important[i], "important");
        }

        // set significance-map to config-class
        if (significance != null && significance.size() > 0) {
            significanceMap = significance;
        }

        // event to be processed/mapped
        logBehavioralChange = Boolean.parseBoolean(props.getProperty("snort.eventlog.behavioral_change", "true"));
        logBotnetInfection = Boolean.parseBoolean(props.getProperty("snort.eventlog.botnet_infection", "true"));
        logCVE = Boolean.parseBoolean(props.getProperty("snort.eventlog.cve", "true"));
        logExcessiveFlows = Boolean.parseBoolean(props.getProperty("snort.eventlog.excessive_flows", "true"));
        logOther = Boolean.parseBoolean(props.getProperty("snort.eventlog.other", "true"));
        logP2P = Boolean.parseBoolean(props.getProperty("snort.eventlog.p2p", "true"));
        logPolicyViolation = Boolean.parseBoolean(props.getProperty("snort.eventlog.policy_violation", "true"));
        logWormInfection = Boolean.parseBoolean(props.getProperty("snort.eventlog.worm_infection", "true"));
    }

    /**
     * check if array really contains values, if not set it to null this is necessary due to
     * problems with reading the properties from config!
     * 
     * @param propArray array to check
     * 
     * @return boolean indicating wheter array is null
     */
    private boolean checkConfig(final String[] propArray) {
        if (propArray != null && propArray.length > 0) {
            if (propArray[0].length() == 0) {
                return false;
            }
        }
        return true;
    }

    private HashMap<String, String> combineEventArrays(final String[] behavioralChange, final String[] botnetInfection,
            final String[] cve, final String[] excessiveFlows, final String[] other, final String[] p2p,
            final String[] policyViolation, final String[] wormInfection) {

        // build new eventmap from event-arrays
        HashMap<String, String> eventMap = new HashMap<String, String>();
        if (behavioralChange != null) {
            for (int i = 0; i < behavioralChange.length; i++) {
                eventMap.put(behavioralChange[i], "behavioral change");
            }
        }
        if (botnetInfection != null) {
            for (int i = 0; i < botnetInfection.length; i++) {
                eventMap.put(botnetInfection[i], "botnet infection");
            }
        }
        if (cve != null) {
            for (int i = 0; i < cve.length; i++) {
                eventMap.put(cve[i], "cve");
            }
        }
        if (excessiveFlows != null) {
            for (int i = 0; i < excessiveFlows.length; i++) {
                eventMap.put(excessiveFlows[i], "excessive flows");
            }

        }
        if (other != null) {
            for (int i = 0; i < other.length; i++) {
                eventMap.put(other[i], "other");
            }
        }
        if (p2p != null) {
            for (int i = 0; i < p2p.length; i++) {
                eventMap.put(p2p[i], "p2p");
            }
        }
        if (policyViolation != null) {
            for (int i = 0; i < policyViolation.length; i++) {
                eventMap.put(policyViolation[i], "policy violation");
            }
        }
        if (wormInfection != null) {
            for (int i = 0; i < wormInfection.length; i++) {
                eventMap.put(wormInfection[i], "worm infection");
            }
        }

        return eventMap;
    }

    /**
     * get the IF-MAP significance value for passed in snort-priority-value
     * 
     * @param int snortPriorityValue
     * 
     * @return significance value as string
     */
    protected Significance getSignificanceValue(final int snortPriorityValue) {
        String sigValue = null;
        Significance sign = Significance.informational; // default!

        if (significanceMap.containsKey(new Integer(snortPriorityValue).toString())) {
            sigValue = significanceMap.get(new Integer(snortPriorityValue).toString());
        }

        if (sigValue != null && sigValue.length() > 0) {
            if (sigValue.startsWith("important")) {
                sign = Significance.important;
            }
            else if (sigValue.startsWith("informational")) {
                sign = Significance.informational;
            }
            else if (sigValue.startsWith("critical")) {
                sign = Significance.critical;
            }
        }

        return sign;
    }

    /**
     * get the IF-MAP Event name for passed in Snort-Signature-Name (see
     * config/snort/mapping.properties)
     * 
     * @param sigName snort signature name
     * @return corresponding IF-MAP Signature Name
     */
    protected EventType getEventMappingForSignatureName(final String sigName) {
        String mappedEvent = null;
        if (eventMap.containsKey(sigName)) {
            mappedEvent = eventMap.get(sigName);
        }
        else {
            mappedEvent = "other";
        }

        EventType evntype = EventType.other; // default
        if (mappedEvent != null && mappedEvent.length() > 0) {
            if (mappedEvent.startsWith("behavioral change")) {
                evntype = EventType.behavioralChange;
            }
            else if (mappedEvent.startsWith("botnet infection")) {
                evntype = EventType.botnetInfection;
            }
            else if (mappedEvent.startsWith("cve")) {
                evntype = EventType.cve;
            }
            else if (mappedEvent.startsWith("excessive flows")) {
                evntype = EventType.excessiveFlows;
            }
            else if (mappedEvent.startsWith("other")) {
                evntype = EventType.other;
            }
            else if (mappedEvent.startsWith("p2p")) {
                evntype = EventType.p2p;
            }
            else if (mappedEvent.startsWith("policy violation")) {
                evntype = EventType.policyViolation;
            }
            else if (mappedEvent.startsWith("worm infection")) {
                evntype = EventType.wormInfection;
            }
            else {
                IfMapClient.LOGGER
                        .config("could not map event-type from read in mapping-result: could not find match for "
                                + evntype + " ...using default-type (other)");
            }
        }
        else {
            IfMapClient.LOGGER
                    .config("could not map event-type from read in mapping-result: empty or non-existing input...using default-type (other)");
        }

        return evntype;
    }

    /**
     * get event type from passed in string and decide if current message should be converted to
     * IF-MAP Events, depending on Properties from Settings in config/snort/mapping.properties
     * 
     * @param eventType type of the event
     * 
     * @return flag indicating whether the message should be convertet or not
     */
    protected boolean doConvert(final EventType eventType) {
        // map snort-event-type to if map event
        if (eventType == EventType.behavioralChange && !logBehavioralChange) {
            return false;
        }
        else if (eventType == EventType.botnetInfection && !logBotnetInfection) {
            return false;
        }
        else if (eventType == EventType.botnetInfection && !logCVE) {
            return false;
        }
        else if (eventType == EventType.excessiveFlows && !logExcessiveFlows) {
            return false;
        }
        else if (eventType == EventType.other && !logOther) {
            return false;
        }
        else if (eventType == EventType.p2p && !logP2P) {
            return false;
        }
        else if (eventType == EventType.policyViolation && !logPolicyViolation) {
            return false;
        }
        else if (eventType == EventType.wormInfection && !logWormInfection) {
            return false;
        }
        else {
            return true;
        }
    }
}