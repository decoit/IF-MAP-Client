/*
 * IfMapClient.java 0.2 13/02/16
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

package de.esukom.decoit.ifmapclient.main;

import de.esukom.decoit.ifmapclient.config.BasicPropertiesReader;
import de.esukom.decoit.ifmapclient.config.GeneralConfig;
import de.esukom.decoit.ifmapclient.config.GeneralPropertiesReader;
import de.esukom.decoit.ifmapclient.iptables.IPTablesFacade;
import de.esukom.decoit.ifmapclient.logging.Logging;
import de.esukom.decoit.ifmapclient.mappingfactory.MappingFactory;
import de.esukom.decoit.ifmapclient.mappingfactory.result.MappingResult;
import de.esukom.decoit.ifmapclient.messaging.IFMAPMessagingFacade;
import de.esukom.decoit.ifmapclient.pollingthreads.PollingThread;
import de.esukom.decoit.ifmapclient.pollingthreads.file.FilePollingThread;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Entry-Class. Here we initiate all components and stick them together...
 * 
 * @version 0.2
 * @author Tobias, FHH/TRUST
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class IfMapClient implements Observer {

    // logging
    public static final Logger LOGGER = Logging.getTheLogger();

    // ip-address of MAP-Server
    public static String sMapServerIP = null;

    // ip-address of this client
    public static String sClientIP = null;

    // thread for polling logs for new entries
    private PollingThread mPollingThread;

    // path to main configuration file
    private final String mDefaultConfigPath = "config/config.properties";

    // log-rotate date
    private Date lastLogRotateDate;

    /**
     * Constructor, initializes configuration-properties and the polling-thread-object
     * 
     * @param args command-line parameters, not used in this version
     */
    public IfMapClient(String[] args) {
        init();
    }

    /**
     * initialize all required application parameters and components
     * 
     * @param args parameters from command line
     */
    private void init() {
        // initialize configuration-files
        initConfigFiles();

        // parse the ip-address of map-server from configuration-file
        Matcher ipMatcher = Toolbox.getRegExPattern("regex.ip4").matcher(GeneralConfig.MAPSERVER_URL);
        if (ipMatcher.find()) {
            sMapServerIP = ipMatcher.group();
        }
        else {
            exit("error while parsing ip-address of map-server from general configuration");
        }

        // get ip-address of the machine this client runs on from
        sClientIP = GeneralConfig.IPADDRESS;
        if (!Toolbox.isNullOrEmpty(sClientIP)) {
            LOGGER.config("IP-Address for this Machine (as defined in config.properties) -> " + sClientIP);
        }
        else {
            exit("no ip-address for this machine found in config.properties");
        }

        // initialize messaging-facade
        initMessagingFacade();

        // set last log rotate date
        lastLogRotateDate = Toolbox.getNowDate("yyyy-MM-dd");

        // initialize log-polling threads
        initPollingThreads();

        // initialize the ip-tables-facade and execute ip-tables start-rules
        if (mPollingThread instanceof de.esukom.decoit.ifmapclient.pollingthreads.file.IPTablesULogFilePollingThread) {
            initIpTablesFacade();
            IPTablesFacade.getInstance().executeIPTableStartRules(IFMAPMessagingFacade.getInstance().mapServerIP);
        }

        // set shutdown exit-hook
        initShutdownHook();
    }

    /**
     * load and initialize configuration files
     */
    private void initConfigFiles() {
        if ((new File(mDefaultConfigPath)).exists()) {
            // load general properties
            GeneralPropertiesReader.loadProperties(mDefaultConfigPath);

            // load polling properties
            BasicPropertiesReader.loadProperties(GeneralConfig.POLLINGCONFIG_PATH);

            // load mapping properties
            BasicPropertiesReader.loadProperties(GeneralConfig.MAPPINGCONFIG_PATH);

            // load and initialize regular expressions
            BasicPropertiesReader.loadProperties(GeneralConfig.REGEXCONFIG_PATH);
            Toolbox.loadAndPrepareRegExFromFile(BasicPropertiesReader.getProperties());
        }
        else {
            exit("could not find main configuration file at: " + mDefaultConfigPath);
        }
    }

    /**
     * initialize the if-map-messaging-facade
     * 
     * @param sMapServerIP ip-address of the MAP-Server
     */
    private void initMessagingFacade() {
        IFMAPMessagingFacade.getInstance().init(sMapServerIP);
    }

    /**
     * initialize the ip-tables-facade
     */
    private void initIpTablesFacade() {
        BasicPropertiesReader.loadProperties("config/iptables/enforcement.properties");
        IPTablesFacade.getInstance().init(BasicPropertiesReader.getProperties());
    }

    /**
     * initialize polling threads
     */
    private void initPollingThreads() {
        // check polling properties from polling-config file
        if (BasicPropertiesReader.getProperties() == null) {
            exit("no properties found");
        }

        // initialize Polling-Thread from configuration-file
        Class<?> pollingClass = null;
        try {
            pollingClass = Class.forName(GeneralConfig.POLLINGCONFIG_CLASSNAME);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            exit("no valid polling-class found");
        }

        Constructor<?> constructor = null;
        try {
            constructor = pollingClass.getConstructor(new Class[] { Properties.class });
        } catch (Exception e) {
            e.printStackTrace();
            exit("could not get constructor for class " + GeneralConfig.POLLINGCONFIG_CLASSNAME);
        }

        try {
            mPollingThread = (PollingThread) constructor.newInstance(BasicPropertiesReader.getProperties());
        } catch (Exception e) {
            e.printStackTrace();
            exit("could not create new polling-class from " + GeneralConfig.POLLINGCONFIG_CLASSNAME);
        }

        // set thread sleep time for each cycle (in seconds!)
        mPollingThread.sleepTime = GeneralConfig.POLLING_INTERVAL * 1000;

        // add corresponding observer-entity
        mPollingThread.addObserver(this);
    }

    /**
     * set the shutdown-hook for send endSession()
     */
    private void initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    IFMAPMessagingFacade.getInstance().sendEndSessionRequest();
                } catch (Exception e) {
                    exit("error while initializing shutdown-hook!");
                }
            }
        });
    }

    /**
     * Starts the program
     */
    public void start() {
        // establish session with MAP-Server and send initial purgePublish
        LOGGER.info("trying to connect to MAP-Server at: " + GeneralConfig.MAPSERVER_URL + "...");
        IFMAPMessagingFacade.getInstance().sendNewSessionRequest();
        IFMAPMessagingFacade.getInstance().sendPurgePublishRequest();
        LOGGER.info("connection established, publisher-id: " + IFMAPMessagingFacade.getInstance().getIFMAPPublisherId());

        // start Polling-Thread
        if (mPollingThread != null) {
            mPollingThread.running = true;
            mPollingThread.pausing = false;
            new Thread(mPollingThread).start();
        }
        else {
            exit("polling-thread could not be started");
        }

        // if esukom-mode for ip-tables is activated, the arc-polling thread that listens for new
        // alert events (that can lead to an enforcement of a client) will be started here
        if (GeneralConfig.POLLINGCONFIG_CLASSNAME
                .equals("de.esukom.decoit.ifmapclient.pollingthreads.IPTablesULogFilePollingThread")
                && GeneralConfig.ARCPOLLING_ENABLED && GeneralConfig.IPTABLES_MODE.equalsIgnoreCase("esukom")) {
            IFMAPMessagingFacade.getInstance().subscribeToAlertEvents();
        }
    }

    /**
     * Is called when Polling-Thread has updated results. Pauses the Thread, maps the result with
     * help from the Mapping-Factory and passes the messages to SOAPControl-Class which finally
     * sends them to the map-server
     * 
     * @param o the observable (polling-thread) object that called update-method
     * @param arg result from polling-object (in form of a HashMap-List)
     */
    @Override
    public synchronized void update(final Observable o, final Object arg) {
        LOGGER.info("polling-thread notifies observer about new result!");

        // check if passed in object exists
        if (o != null) {
            ArrayList<HashMap<String, String>> tmpResultList = (ArrayList<HashMap<String, String>>) arg;
            if (tmpResultList != null) {
                MappingFactory mappingFactory = getMappingFactory(tmpResultList, GeneralConfig.MAPPINGCONFIG_CLASSNAME);

                // use mapping factory to map result from poller
                if (mappingFactory != null) {
                    MappingResult[] resultList = mappingFactory.getMappingResult();
                    if (resultList != null && resultList.length > 0) {
                        // mapping result from ip-tables-component
                        if (o instanceof de.esukom.decoit.ifmapclient.pollingthreads.file.IPTablesULogFilePollingThread) {
                            processIpTablesMappingResult(resultList);
                        }

                        // send mapping result from other components to map-server
                        else {
                            IFMAPMessagingFacade.getInstance().sendPublishRequest(resultList);
                            LOGGER.info("done sending data to MAP-Server at " + GeneralConfig.MAPSERVER_URL);
                        }
                    }
                    else {
                        LOGGER.info("mapping result is null or empty, not sending anything to server");
                    }
                }
            }
        }
        else {
            LOGGER.info("result from " + GeneralConfig.POLLINGCONFIG_CLASSNAME + " is null, nothing to send to server");
        }

        if (!checkLogRotate(o)) {
            mPollingThread.pausing = false;
        }
    }

    /**
     * get the mapping factory for converting result from polling-thread to Event-Message Object
     * 
     * @param data result from polling-object
     * 
     * @return MappingFactory mapping-factory-object containing the converted result
     */
    private MappingFactory getMappingFactory(final ArrayList<HashMap<String, String>> data,
            final String mappingConfigClassName) {
        if (data != null) {
            // initialize Mapping Factory from configuration-file
            Class<?> mappingClass = null;
            try {
                mappingClass = Class.forName(mappingConfigClassName);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                exit("could not find MappingClass-property in config.properties");
            }

            Constructor<?> constructor = null;
            try {
                constructor = mappingClass.getConstructor(new Class[] { Properties.class, ArrayList.class });
            } catch (Exception e) {
                e.printStackTrace();
                exit("could not get Constructor of MappingClass");
            }

            MappingFactory mappingFactory = null;
            try {
                mappingFactory = (MappingFactory) constructor.newInstance(BasicPropertiesReader.getProperties(), data);
            } catch (Exception e) {
                e.printStackTrace();
                exit("could not create MappingFactory");
            }

            return mappingFactory;
        }
        else {
            LOGGER.warning("could not create mapping factory from empty result");
            return null;
        }
    }

    /**
     * process iptables mapping result and send it to map-server
     * 
     * @param resultList result from mapping-factory to be processed
     */
    private void processIpTablesMappingResult(final MappingResult[] resultList) {
        if (GeneralConfig.ARCPOLLING_ENABLED) {
            // esukom-mode
            if (GeneralConfig.IPTABLES_MODE.equalsIgnoreCase("esukom")) {
                // check read-out clients for allowance
                IFMAPMessagingFacade.getInstance().checkEntriesForAllowance(resultList);
            }
            // default-mode
            else {
                // add new clients to client-list
                if (IFMAPMessagingFacade.getInstance().pollingClientList.addNewClientsToClientList(resultList)) {

                    // update subscriptions with new entries
                    IFMAPMessagingFacade.getInstance().updateSubscribtions();

                    // send new events to map-server
                    if (GeneralConfig.IPTABLES_SENDDATASTREAMDETECTEDEVENT) {
                        IFMAPMessagingFacade.getInstance().sendPublishRequest(resultList);
                    }
                }
            }
        }
    }

    /**
     * check for log rotate and re-initialize file-polling thread if necessary
     * 
     * @param o current active polling-thread
     */
    private boolean checkLogRotate(final Observable o) {
        if (o instanceof FilePollingThread) {
            FilePollingThread fpt = (FilePollingThread) o;
            if (fpt.isLogRotateActive()) {
                Date currentDate = Toolbox.getNowDate("yyyy-MM-dd");
                if (currentDate.after(lastLogRotateDate)) {
                    // re-initialize file-polling-thread
                    lastLogRotateDate = currentDate;
                    mPollingThread = null;
                    initPollingThreads();
                    mPollingThread.running = true;
                    mPollingThread.pausing = false;
                    new Thread(mPollingThread).start();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * entry-point for the application
     * 
     * @param args command-line parameters, not used in this version
     */
    public static void main(String[] args) {
        IfMapClient client = new IfMapClient(args);
        client.start();
    }

    /**
     * shut down application
     */
    public static void exit(final String errorMsg) {
        if (errorMsg != null) {
            LOGGER.severe("client ended due to error with message: " + errorMsg);
            System.exit(1);
        }
        else {
            LOGGER.info("client ended normaly...bye bye :-)");
            System.exit(0);
        }
    }
}