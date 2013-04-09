/*
 * OpenVPNMappingResult.java 0.2 13/02/13
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

/**
 * concrete implementation of mapping-result. holds data from open-vpn-log-file
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class OpenVPNMappingResult extends MappingResult {

    private String username;
    private String vpnIPAddress;
    private String ispIpAddress;
    private int arCounter;
 
    /**
     * (empty) constructor
     */
    public OpenVPNMappingResult() {
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
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * @return the vpnIPAddress
     */
    public String getVpnIPAddress() {
        return vpnIPAddress;
    }

    /**
     * @param vpnIPAddress the vpnIPAddress to set
     */
    public void setVpnIPAddress(final String vpnIPAddress) {
        this.vpnIPAddress = vpnIPAddress;
    }

    /**
     * @return the ispIpAddress
     */
    public String getIspIpAddress() {
        return ispIpAddress;
    }

    /**
     * @param ispIpAddress the ispIpAddress to set
     */
    public void setIspIpAddress(final String ispIpAddress) {
        this.ispIpAddress = ispIpAddress;
    }

    /**
     * @return the arCounter
     */
    public int getArCounter() {
        return arCounter;
    }

    /**
     * @param arCounter the arCounter to set
     */
    public void setArCounter(int arCounter) {
        this.arCounter = arCounter;
    }

    @Override
    public String showOnConsole() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--> OpenVPNMappingResult \n");
        sb.append("\tusername: ");
        sb.append(getUsername());
        sb.append("\n");   
        sb.append("\tvpn-ip-address: ");
        sb.append(getVpnIPAddress());
        sb.append("\n");
        sb.append("\tisp-ip-address: ");
        sb.append(getIspIpAddress());
        return sb.toString();
    }
}