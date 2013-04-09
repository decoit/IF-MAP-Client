/*
 * GeneralConfig.java 0.2 13/02/16
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

/**
 * Class for holding application configuration parameters
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public final class GeneralConfig {

    // application version number string
    public static String VERSION;

    // pathes to "sub-config-files"
    public static String POLLINGCONFIG_PATH;
    public static String MAPPINGCONFIG_PATH;
    public static String REGEXCONFIG_PATH;

    // name for associated polling and mapping classes
    public static String POLLINGCONFIG_CLASSNAME;
    public static String MAPPINGCONFIG_CLASSNAME;

    // renew session parameters
    public static boolean RENEWSESSION_ENABLED;
    public static int RENEWSESSION_INTERVALL;

    // arc polling parameters
    public static boolean ARCPOLLING_ENABLED;

    // IF-MAP-Message Type to be send to MAP server
    public static String MESSAGING_TYPE;

    // flag detecting if old entries should also be send to MAP-Server
    public static boolean MESSAGING_SENDOLD;

    // polling thread interval
    public static int POLLING_INTERVAL;

    // ip-address of the machine this clients runs on
    public static String IPADDRESS;

    // snort-publishing mode
    public static String SNORT_MODE;

    // ip-tables-mode
    public static String IPTABLES_MODE;

    // ip-tables-startup-rules-script
    public static String IPTABLES_STARTSCRIPT;

    // is iptables component running in gateway mode
    public static boolean IPTABLES_GATEWAYMODE;

    // enable/disable sending of "datastream-detected"-events
    public static boolean IPTABLES_SENDDATASTREAMDETECTEDEVENT;

    // map-server configuration
    public static String MAPSERVER_URL;
    public static String MAPSERVER_KEYSTORE_PATH;
    public static String MAPSERVER_KEYSTORE_PASSWORD;
    public static String MAPSERVER_TRUSTSTORE_PATH;
    public static String MAPSERVER_TRUSTSTORE_PASSWORD;
    public static boolean MAPSERVER_BASICAUTH_ENABLED;
    public static String MAPSERVER_BASICAUTH_USER;
    public static String MAPSERVER_BASICAUTH_PASSWORD;
}