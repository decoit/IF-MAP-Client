/*
 * GeneralPropertiesReader.java 0.2 13/02/16
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
import de.esukom.decoit.ifmapclient.messaging.SOAPMessageSender;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Reads properties from configuration file
 * 
 * @version 0.2
 * @author Tobias, FHH/TRUST
 * @author Dennis Dunekacke, Decoit GmbH
 */
public class GeneralPropertiesReader {

    // default values for properties
    private static final int POLLINGINTERVAL_DEFAULT = 10;
    private static final boolean RENEWSESSION_DEFAULT = false;
    private static final boolean ARCPOLLING_DEFAULT = false;
    private static final int RENEWSESSIONINTERVAL_DEFAULT = 30;
    private static final boolean BASICAUTHENABLED_DEFAULT = true;
    private static final boolean MESSAGING_SENDOLD = false;

    private static Properties sProps;
    private static boolean sFlag = false;
    private static String sPath = "config/config.properties";

    /**
     * Sets the path to properties file
     */
    public static void setPath(final String path) {
        GeneralPropertiesReader.sPath = path;
    }

    /**
     * Loads the properties to memory
     * 
     * @param path path to property file
     */
    public static void loadProperties(String path) {
        sProps = new Properties();
        FileInputStream in;
        try {
            in = new FileInputStream(path);
            sProps.load(in);
            in.close();
            initConfig(sProps);
            sFlag = true;
        } catch (Exception e) {
            e.printStackTrace();
            IfMapClient.exit("error while loading general properties from " + path);
        }
    }

    /**
     * initialize configuration from property-file
     * 
     * @param conf properties-object
     */
    private static void initConfig(Properties conf) {
        if (!initConfigFiles(conf) || !initApplicationConfig(conf) || !initMapServerConfig(conf)) {
            IfMapClient.exit("error while initializing configuration");
        }
    }

    /**
     * get config-properties from configuration file and set them to GeneralConfig-Class.
     * 
     * @param conf properties-object
     * 
     * @return true if successful, false otherwise
     */
    private static boolean initConfigFiles(Properties conf) {
        // path to polling configuration
        GeneralConfig.POLLINGCONFIG_PATH = Toolbox.getStringProperty("application.pollingconfig.path", conf, true);
        if (GeneralConfig.POLLINGCONFIG_PATH == null) {
            return false;
        }

        // path to mapping configuration
        GeneralConfig.MAPPINGCONFIG_PATH = Toolbox.getStringProperty("application.mappingconfig.path", conf, true);
        if (GeneralConfig.MAPPINGCONFIG_PATH == null) {
            return false;
        }

        // class-name for polling class
        GeneralConfig.POLLINGCONFIG_CLASSNAME = Toolbox.getStringProperty("application.pollingconfig.classname", conf,
                false);
        if (GeneralConfig.POLLINGCONFIG_CLASSNAME == null) {
            return false;
        }

        // class-name for mapping class
        GeneralConfig.MAPPINGCONFIG_CLASSNAME = Toolbox.getStringProperty("application.mappingconfig.classname", conf,
                false);
        if (GeneralConfig.MAPPINGCONFIG_CLASSNAME == null) {
            return false;
        }

        // path to regular expressions configuration
        GeneralConfig.REGEXCONFIG_PATH = Toolbox.getStringProperty("application.regexconfig.path", conf, false);
        if (GeneralConfig.REGEXCONFIG_PATH == null) {
            return false;
        }

        return true;
    }

    /**
     * get application-properties from configuration file and set them to GeneralConfig-Class.
     * 
     * @param conf properties-object
     * 
     * @return true if successful, false otherwise
     */
    private static boolean initApplicationConfig(Properties conf) {
        // application version string
        GeneralConfig.VERSION = Toolbox.getStringProperty("application.version", conf, false);

        // polling interval
        GeneralConfig.POLLING_INTERVAL = Toolbox.getIntPropertyWithDefault("application.polling.interval",
                POLLINGINTERVAL_DEFAULT, conf, true);

        // renew session enabled flag
        GeneralConfig.RENEWSESSION_ENABLED = Toolbox.getBoolPropertyWithDefault("application.renewsession.enabled",
                RENEWSESSION_DEFAULT, conf);

        // arc-polling enabled flag
        GeneralConfig.ARCPOLLING_ENABLED = Toolbox.getBoolPropertyWithDefault("application.arcpolling.enabled",
                ARCPOLLING_DEFAULT, conf);

        // skip/post "old" entries flag
        GeneralConfig.MESSAGING_SENDOLD = Toolbox.getBoolPropertyWithDefault("application.messaging.sendold",
                MESSAGING_SENDOLD, conf);

        // renew session interval
        GeneralConfig.RENEWSESSION_INTERVALL = Toolbox.getIntPropertyWithDefault("application.renewsession.intervall",
                RENEWSESSIONINTERVAL_DEFAULT, conf, true);

        // ip-address of the machine this client runs on
        GeneralConfig.IPADDRESS = Toolbox.getStringPropertyWithDefault("application.ipaddress", conf, "");

        // snort-publishing mode
        GeneralConfig.SNORT_MODE = Toolbox.getStringPropertyWithDefault("application.snort.mode", conf, "default");

        // iptables-mode
        GeneralConfig.IPTABLES_MODE = Toolbox
                .getStringPropertyWithDefault("application.iptables.mode", conf, "default");

        // iptables-startup-rules-script to be executed
        GeneralConfig.IPTABLES_STARTSCRIPT = Toolbox.getStringPropertyWithDefault("application.iptables.startscript",
                conf, "config/iptables/intialize_rules.sh");

        // iptables-sending of Datastream-Detected-Events
        GeneralConfig.IPTABLES_SENDDATASTREAMDETECTEDEVENT = Toolbox.getBoolPropertyWithDefault(
                "application.iptables.senddatastreamevents", false, conf);

        // gateway mode for ip-tables-component
        GeneralConfig.IPTABLES_GATEWAYMODE = Toolbox.getBoolPropertyWithDefault("application.iptables.gateway", false,
                conf);

        // IF-MAP messaging type
        GeneralConfig.MESSAGING_TYPE = Toolbox.getStringProperty("application.messaging.type", conf, false);
        if (GeneralConfig.MESSAGING_TYPE == null
                || (!GeneralConfig.MESSAGING_TYPE.equalsIgnoreCase("update") && !GeneralConfig.MESSAGING_TYPE
                        .equalsIgnoreCase("notify"))) {
            return false;
        }
        // set publish type
        else {
            if (GeneralConfig.MESSAGING_TYPE.equalsIgnoreCase("update")) {
                SOAPMessageSender.publishMode = SOAPMessageSender.PUBLISH_TYPE_UPDATE;
            }
            else {
                SOAPMessageSender.publishMode = SOAPMessageSender.PUBLISH_TYPE_NOTIFY;
            }
        }

        return true;
    }

    /**
     * get mapserver-properties from configuration file and set them to GeneralConfig-Class.
     * 
     * @param conf properties-object
     * 
     * @return true if successful, false otherwise
     */
    private static boolean initMapServerConfig(Properties conf) {
        // map-server url
        GeneralConfig.MAPSERVER_URL = Toolbox.getStringProperty("mapserver.url", conf, false);
        if (GeneralConfig.MAPSERVER_URL == null) {
            return false;
        }

        // mapserver keystore path
        GeneralConfig.MAPSERVER_KEYSTORE_PATH = Toolbox.getStringProperty("mapserver.keystore.path", conf, true);
        if (GeneralConfig.MAPSERVER_KEYSTORE_PATH == null) {
            return false;
        }

        // mapserver keystore password
        GeneralConfig.MAPSERVER_KEYSTORE_PASSWORD = Toolbox.getStringProperty("mapserver.keystore.password", conf,
                false);
        if (GeneralConfig.MAPSERVER_KEYSTORE_PASSWORD == null) {
            return false;
        }

        // mapserver truststore path
        GeneralConfig.MAPSERVER_TRUSTSTORE_PATH = Toolbox.getStringProperty("mapserver.truststore.path", conf, true);
        if (GeneralConfig.MAPSERVER_TRUSTSTORE_PATH == null) {
            return false;
        }

        // mapserver truststore password
        GeneralConfig.MAPSERVER_TRUSTSTORE_PASSWORD = Toolbox.getStringProperty("mapserver.truststore.password", conf,
                false);
        if (GeneralConfig.MAPSERVER_TRUSTSTORE_PASSWORD == null) {
            return false;
        }

        // basic authorization flag
        GeneralConfig.MAPSERVER_BASICAUTH_ENABLED = Toolbox.getBoolPropertyWithDefault("mapserver.basicauth.enabled",
                BASICAUTHENABLED_DEFAULT, conf);

        // basic authorization user
        GeneralConfig.MAPSERVER_BASICAUTH_USER = Toolbox.getStringProperty("mapserver.basicauth.user", conf, false);
        if (GeneralConfig.MAPSERVER_BASICAUTH_USER == null) {
            return false;
        }

        // basic authorization password
        GeneralConfig.MAPSERVER_BASICAUTH_PASSWORD = Toolbox.getStringProperty("mapserver.basicauth.password", conf,
                false);
        if (GeneralConfig.MAPSERVER_BASICAUTH_PASSWORD == null) {
            return false;
        }

        return true;
    }

    /**
     * Gets a specific property
     * 
     * @param ident The property name
     * @return String the value of a property
     */

    public static String getProperty(String ident) {
        if (!sFlag) {
            loadProperties(GeneralPropertiesReader.sPath);
        }
        String value = sProps.getProperty(ident);
        return value;
    }
}