/*
 * BasicPropertiesReader.java 0.2 13/02/16
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

package de.esukom.decoit.ifmapclient.config;

import de.esukom.decoit.ifmapclient.main.IfMapClient;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Read properties from file
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class BasicPropertiesReader {

    private static Properties sProps;
    private static boolean sFlag = false;
    private static String sPath;

    /**
     * Sets the path to the properties file
     * 
     * @param path path to property file
     */
    public static void setPath(final String path) {
        BasicPropertiesReader.sPath = path;
    }

    /**
     * Load the properties to memory
     * 
     * @param path path to property file
     */
    public static void loadProperties(final String path) {
        if (sProps == null) {
            sProps = new Properties();
        }
        FileInputStream in;
        try {
            in = new FileInputStream(path);
            sProps.load(in);
            in.close();
            sFlag = true;
        } catch (Exception e) {
            IfMapClient.exit("error while loading basic properties from: " + path);
        }
    }

    /**
     * Get Properties
     * 
     * @return Properties
     */
    public static Properties getProperties() {
        if (!sFlag) {
            loadProperties(BasicPropertiesReader.sPath);
        }
        return sProps;
    }
}