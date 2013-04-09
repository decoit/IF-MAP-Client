/*
 * MappingFactory.java 0.2 12/02/16
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

import de.esukom.decoit.ifmapclient.mappingfactory.result.MappingResult;

/**
 * abstract base-class for mapping-factories
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public abstract class MappingFactory {

    protected ArrayList<MappingResult> mapResult = new ArrayList<MappingResult>();

    /**
     * constructor
     * 
     * @param props properties-object
     * @param data result-data
     */
    public MappingFactory(final Properties props, final ArrayList<HashMap<String, String>> data) {
        initProperties(props);
        createMappingResult(props, data);
    }

    /**
     * initialize mapping factory with the properties from related configuration-file
     * 
     * @param props mappingProperties
     */
    protected abstract void initProperties(final Properties props);

    /**
     * create mapping result from passed in value-list
     * 
     * @param props mappingProperties
     * @param data raw data from polling-thread
     */
    protected abstract void createMappingResult(final Properties props, final ArrayList<HashMap<String, String>> data);

    /**
     * get mapping result from passed in data
     * 
     * @return MappingResult[] mapped-result for desired result-type
     */
    public MappingResult[] getMappingResult() {
        if (mapResult != null) {
            return mapResult.toArray(new MappingResult[mapResult.size()]);
        }
        else {
            return null;
        }
    }
}