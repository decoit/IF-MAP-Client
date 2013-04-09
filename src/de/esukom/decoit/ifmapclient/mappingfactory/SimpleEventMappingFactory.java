/*
 * EventMappingFactory.java 0.2 13/02/07
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.util.Toolbox;
import de.fhhannover.inform.trust.ifmapj.metadata.EventType;
import de.fhhannover.inform.trust.ifmapj.metadata.Significance;

/**
 * abstract base-class for simple event-mapping-factories
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public abstract class SimpleEventMappingFactory extends MappingFactory {

    // keys for values from mapping.properties
    public static final String MAPPINGKEYS_EVENTTYPE = "eventmapping.eventtype";
    public static final String MAPPINGKEYS_MAGNITUDE = "eventmapping.magnitude";
    public static final String MAPPINGKEYS_CONFIDENCE = "eventmapping.confidence";
    public static final String MAPPINGKEYS_SIGNIFICANCE = "eventmapping.significance";

    // valid values from mapping.properties
    public static final String MAPPINGVALUES_EVENTTYPE_CHANGE = "behavioral-change";
    public static final String MAPPINGVALUES_EVENTTYPE_BOTNET = "botnet-infection";
    public static final String MAPPINGVALUES_EVENTTYPE_CVE = "cve";
    public static final String MAPPINGVALUES_EVENTTYPE_FLOW = "excessive-flows";
    public static final String MAPPINGVALUES_EVENTTYPE_OTHER = "other";
    public static final String MAPPINGVALUES_EVENTTYPE_P2P = "p2p";
    public static final String MAPPINGVALUES_EVENTTYPE_POLICY = "policy-violation";
    public static final String MAPPINGVALUES_EVENTTYPE_WORM = "worm-infection";
    public static final String MAPPINGVALUES_SIGNIFICANCE_INFO = "informational";
    public static final String MAPPINGVALUES_SIGNIFICANCE_IMPORTANT = "important";
    public static final String MAPPINGVALUES_SIGNIFICANCE_CRITICAL = "critical";

    // read out properties initialized from mapping.properties
    protected EventType mEventtypeDefault;
    protected String mMagnitudeDefault;
    protected String mConfidenceDefault;
    protected Significance mSignificanceDefault;

    /**
     * constructor
     * 
     * @param props properties object
     * @param data List of Hash-Maps containing data from file-poller
     */
    public SimpleEventMappingFactory(final Properties props, final ArrayList<HashMap<String, String>> data) {
        super(props, data);
    }

    @Override
    protected void initProperties(final Properties props) {
        mEventtypeDefault = getEventTypeFromProperties(props);
        if (mEventtypeDefault == null) {
            IfMapClient.exit("could not load " + MAPPINGKEYS_EVENTTYPE + " from mapping.properties");
        }

        mSignificanceDefault = getSignificanceFromProperties(props);
        if (mSignificanceDefault == null) {
            IfMapClient.exit("could not load " + MAPPINGKEYS_SIGNIFICANCE + " from mapping.properties");
        }

        mMagnitudeDefault = getNummericValueFromPropertiesAsString(MAPPINGKEYS_MAGNITUDE, props);
        if (mMagnitudeDefault == null) {
            IfMapClient.exit("could not load " + MAPPINGKEYS_MAGNITUDE + " from mapping.properties");
        }

        mConfidenceDefault = getNummericValueFromPropertiesAsString(MAPPINGKEYS_CONFIDENCE, props);
        if (mConfidenceDefault == null) {
            IfMapClient.exit("could not load " + MAPPINGKEYS_CONFIDENCE + "from mapping.properties");
        }
    }

    /**
     * get the event-type from passed in properties
     * 
     * @param props properties object
     * 
     * @return the related IFMAP-event-type
     */
    private EventType getEventTypeFromProperties(final Properties props) {
        String eventTypeFromConfig = Toolbox.getStringProperty(MAPPINGKEYS_EVENTTYPE, props, false);
        if (Toolbox.isNullOrEmpty(eventTypeFromConfig)) {
            return null;
        }
        else {
            if (eventTypeFromConfig.equalsIgnoreCase(MAPPINGVALUES_EVENTTYPE_BOTNET)) {
                return EventType.botnetInfection;
            }
            else if (eventTypeFromConfig.equalsIgnoreCase(MAPPINGVALUES_EVENTTYPE_CVE)) {
                return EventType.cve;
            }
            else if (eventTypeFromConfig.equalsIgnoreCase(MAPPINGVALUES_EVENTTYPE_FLOW)) {
                return EventType.excessiveFlows;
            }
            else if (eventTypeFromConfig.equalsIgnoreCase(MAPPINGVALUES_EVENTTYPE_OTHER)) {
                return EventType.other;
            }
            else if (eventTypeFromConfig.equals(MAPPINGVALUES_EVENTTYPE_P2P)) {
                return EventType.p2p;
            }
            else if (eventTypeFromConfig.equalsIgnoreCase(MAPPINGVALUES_EVENTTYPE_POLICY)) {
                return EventType.policyViolation;
            }
            else if (eventTypeFromConfig.equalsIgnoreCase(MAPPINGVALUES_EVENTTYPE_WORM)) {
                return EventType.wormInfection;
            }
            else if (eventTypeFromConfig.equalsIgnoreCase(MAPPINGVALUES_EVENTTYPE_CHANGE)) {
                return EventType.behavioralChange;
            }
            else {
                return null;
            }
        }
    }

    /**
     * get the significance-type from passed in properties
     * 
     * @param props properties object
     * 
     * @return the related IFMAP-significance-type
     */
    private Significance getSignificanceFromProperties(final Properties props) {
        String significance = Toolbox.getStringProperty(MAPPINGKEYS_SIGNIFICANCE, props, false);
        if (Toolbox.isNullOrEmpty(significance)) {
            return null;
        }
        else {
            if (significance.equalsIgnoreCase(MAPPINGVALUES_SIGNIFICANCE_IMPORTANT)) {
                return Significance.important;
            }
            else if (significance.equalsIgnoreCase(MAPPINGVALUES_SIGNIFICANCE_CRITICAL)) {
                return Significance.critical;
            }
            else if (significance.equalsIgnoreCase(MAPPINGVALUES_SIGNIFICANCE_INFO)) {
                return Significance.informational;
            }
            else {
                return null;
            }
        }
    }

    /**
     * get numerical value from passed in properties as string
     * 
     * @param mappingKey key for desired value from properties
     * @param props properties object
     * 
     * @return detected value as string
     */
    private String getNummericValueFromPropertiesAsString(final String mappingKey, final Properties props) {
        String returnValue = null;
        try {
            return String.valueOf(new Integer(Toolbox.getStringProperty(mappingKey, props, false)));
        } catch (NumberFormatException nfe) {
            IfMapClient.exit("could not load value from mapping.properties for " + mappingKey);
        }
        return returnValue;
    }
}