/*
 * OpenVPNEventMappingFactory.java 0.2 13/02/04
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

import de.esukom.decoit.ifmapclient.mappingfactory.MappingFactory;
import de.esukom.decoit.ifmapclient.mappingfactory.result.OpenVPNMappingResult;

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
public class OpenVPNMappingFactory extends MappingFactory {

    /**
     * constructor
     * 
     * @param props properties object
     * @param data List of Hash-Maps containing data from file poller
     */
    public OpenVPNMappingFactory(final Properties props, final ArrayList<HashMap<String, String>> data) {
        super(props, data);
    }

    @Override
    protected void initProperties(final Properties props) {
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
        boolean entriesFound = false;
        for (HashMap<String, String> hashMap : res) {
            if (hashMap.get("0").startsWith("CLIENT_LIST")) {
                entriesFound = true;
                String[] entries = hashMap.get("0").split(",");
                OpenVPNMappingResult mappingResult = new OpenVPNMappingResult();
                mappingResult.setUsername(entries[1]);
                mappingResult.setIspIpAddress(entries[2].split(":")[0]);
                mappingResult.setVpnIPAddress(entries[3]);
                super.mapResult.add(mappingResult);
            }
        }

        if (!entriesFound) {
            OpenVPNMappingResult mappingResult = new OpenVPNMappingResult();
            mappingResult.setIspIpAddress("999.999.999.999");
            super.mapResult.add(mappingResult);
        }
    }
}