/*
 * EventMappingResult.java 0.2 13/02/13
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

package de.esukom.decoit.ifmapclient.mappingfactory.result;

import de.fhhannover.inform.trust.ifmapj.metadata.EventType;
import de.fhhannover.inform.trust.ifmapj.metadata.Significance;

/**
 * concrete implementation of mapping-result. represents an IFMAP-Event
 * 
 * @version 0.2
 * @author Dennis Dunekacke, Decoit GmbH
 */
public class EventMappingResult extends MappingResult implements Cloneable {

    private String mIp;
    private String mDiscoveredTime;
    private String mDiscovererId;
    private String mMagnitude;
    private String mConfidence;
    private Significance mSignificance;
    private EventType mEventMessageType;
    private String mName;
    private String mIpType;
    private String mVulnerabilityUri;
    private String mIdentity;

    /**
     * (empty) constructor
     */
    public EventMappingResult() {
        // move along, nothing to see here...
    }

    /**
     * clone
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
     * @return the ip
     */
    public String getIp() {
        return mIp;
    }

    /**
     * @param ip the ip to set
     */
    public void setIp(final String ip) {
        this.mIp = ip;
    }

    /**
     * @return the discoveredTime
     */
    public String getDiscoveredTime() {
        return mDiscoveredTime;
    }

    /**
     * @param discoveredTime the discoveredTime to set
     */
    public void setDiscoveredTime(final String discoveredTime) {
        this.mDiscoveredTime = discoveredTime;
    }

    /**
     * @return the discovererId
     */
    public String getDiscovererId() {
        return mDiscovererId;
    }

    /**
     * @param discovererId the discovererId to set
     */
    public void setDiscovererId(final String discovererId) {
        this.mDiscovererId = discovererId;
    }

    /**
     * @return the magnitude
     */
    public String getMagnitude() {
        return mMagnitude;
    }

    /**
     * @param magnitude the magnitude to set
     */
    public void setMagnitude(final String magnitude) {
        this.mMagnitude = magnitude;
    }

    /**
     * @return the confidence
     */
    public String getConfidence() {
        return mConfidence;
    }

    /**
     * @param confidence the confidence to set
     */
    public void setConfidence(final String confidence) {
        this.mConfidence = confidence;
    }

    /**
     * @return the significance
     */
    public Significance getSignificance() {
        return mSignificance;
    }

    /**
     * @param significance the significance to set
     */
    public void setSignificance(final Significance significance) {
        this.mSignificance = significance;
    }

    /**
     * @return the eventMessageType
     */
    public EventType getEventMessageType() {
        return mEventMessageType;
    }

    /**
     * @param eventMessageType the eventMessageType to set
     */
    public void setEventMessageType(final EventType eventMessageType) {
        this.mEventMessageType = eventMessageType;
    }

    /**
     * @return the name
     */
    public String getName() {
        return mName;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.mName = name;
    }

    /**
     * @return the ipType
     */
    public String getIpType() {
        return mIpType;
    }

    /**
     * @param ipType the ipType to set
     */
    public void setIpType(final String ipType) {
        this.mIpType = ipType;
    }

    /**
     * @return the vulnerabilityUri
     */
    public String getVulnerabilityUri() {
        return mVulnerabilityUri;
    }

    /**
     * @param vulnerabilityUri the vulnerabilityUri to set
     */
    public void setVulnerabilityUri(final String vulnerabilityUri) {
        this.mVulnerabilityUri = vulnerabilityUri;
    }

    /**
     * @return the identity
     */
    public String getIdentity() {
        return mIdentity;
    }

    /**
     * @param identity the identity to set
     */
    public void setIdentity(final String identity) {
        this.mIdentity = identity;
    }

    @Override
    public String showOnConsole() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--> EventMappingResult \n");
        sb.append("\tidentity: ");
        sb.append(getIdentity());
        sb.append("\n");
        sb.append("\tip-address: ");
        sb.append(getIp());
        sb.append("\n");
        sb.append("\tdiscovered-time: ");
        sb.append(getDiscoveredTime());
        sb.append("\n");
        sb.append("\tdiscoverer-id: ");
        sb.append(getDiscovererId());
        sb.append("\n");
        sb.append("\tmagnitude: ");
        sb.append(getMagnitude());
        sb.append("\n");
        sb.append("\tconfidence: ");
        sb.append(getConfidence());
        sb.append("\n");
        sb.append("\tsignificance: ");
        sb.append(getSignificance().toString());
        sb.append("\n");
        sb.append("\tevent-msg-type: ");
        sb.append(getEventMessageType().toString());
        sb.append("\n");
        sb.append("\tname: ");
        sb.append(getName());
        sb.append("\n");
        sb.append("\tip-type: ");
        sb.append(getIpType());
        sb.append("\n");
        sb.append("\tvulnerabilitiy-uri: ");
        sb.append(getVulnerabilityUri());
        sb.append("\tidentity: ");
        sb.append(getIdentity());
        return sb.toString();
    }
}
