/*
 * IFMAPMessagingFacade.java 0.2 13/02/16
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

package de.esukom.decoit.ifmapclient.messaging;

import de.esukom.decoit.ifmapclient.config.GeneralConfig;
import de.esukom.decoit.ifmapclient.iptables.IPTablesFacade;
import de.esukom.decoit.ifmapclient.iptables.PollResultChecker;
import de.esukom.decoit.ifmapclient.iptables.Rules;
import de.esukom.decoit.ifmapclient.iptables.RulesExecutor;
import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.mappingfactory.result.EventMappingResult;
import de.esukom.decoit.ifmapclient.mappingfactory.result.MappingResult;

import de.fhhannover.inform.trust.ifmapj.channel.ARC;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapErrorResult;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapException;
import de.fhhannover.inform.trust.ifmapj.exception.InitializationException;
import de.fhhannover.inform.trust.ifmapj.messages.PollResult;
import de.fhhannover.inform.trust.ifmapj.messages.ResultItem;
import de.fhhannover.inform.trust.ifmapj.messages.SearchResult;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Facade for performing IFMAP-communication related tasks
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class IFMAPMessagingFacade implements Observer {

    // singleton-pattern
    private static IFMAPMessagingFacade sInstance = new IFMAPMessagingFacade();

    // ip-address of the MAP-Server
    public String mapServerIP;
    
    // IFMAP-Message-Sender
    public final SOAPMessageSender sender;

    // polling-client list
    public PollingClientList pollingClientList = new PollingClientList();
    
    // IFMAP-Message-Poller
    private IFMAPMessagePollingThread mPoller;
    private boolean mIsARCPollingThreadActive = false;

    // sequence number for subscription-name, will
    // be increased with every subscription
    private int mSubscribeIndex;

    

    private IFMAPMessagingFacade() {
        // initialize MessageSender
        sender = SOAPMessageSender.getInstance();

        // initialize polling-client-list
        pollingClientList = new PollingClientList();

        // initialize messaging-channels
        initSRC();

        // initialize arc if it is enabled in general-configuration
        if (GeneralConfig.ARCPOLLING_ENABLED) {
            initARC();
        }
        else {
            IfMapClient.LOGGER.config("arc-polling is deactivated in configuration...not initializing arc-channel");
        }

        // initialize subscribe index
        mSubscribeIndex = 0;
    }

    /**
     * return an instance of IFMAPMessagingFacade
     * 
     * @return instance of IFMAPMessagingFacade
     */
    public synchronized static IFMAPMessagingFacade getInstance() {
        return sInstance;
    }

    /**
     * initialize properties
     * 
     * @param main reference to main-class
     */
    public void init(String serverIP) {
        // set MAP-Server ip
        mapServerIP = serverIP;

        // start ARC-Polling-Thread
        activateSOAPMessagePollingThread();
    }

    /**
     * initialize SRC
     */
    private void initSRC() {
        IfMapClient.LOGGER.info("initializing src-channel...");
        try {
            sender.prepareSSRC();
        } catch (InitializationException e) {
            e.printStackTrace();
            IfMapClient.exit("could not initialize messaging facade...preparation of src-channel failed!");
        }
    }

    /**
     * initialize ARC
     */
    private void initARC() {
        if (sender != null) {
            final ARC arcChannel = sender.getARC();
            if (arcChannel != null) {
                IfMapClient.LOGGER.info("initializing arc-channel...");
                mPoller = new IFMAPMessagePollingThread(arcChannel);
                mPoller.sleepTime = 20;
                mPoller.addObserver(this);
            }
            else {
                IfMapClient.exit("could not initialize messaging facade...preparation of arc-channel failed!");
            }
        }
    }

    /**
     * get the current IFMAP-Publisher ID
     * 
     * @return publisher
     */
    public String getIFMAPPublisherId() {
        return sender.getIfMapPublisherId();
    }

    /**
     * send a new-session request to map-server
     */
    public void sendNewSessionRequest() {
        try {
            IfMapClient.LOGGER.info("sending out new-session request over src...");
            sender.newSession();
        } catch (IfmapErrorResult e) {
            IfMapClient.exit("ifmap-error-result while sending new-session request: " + e);
        } catch (IfmapException e) {
            IfMapClient.exit("ifmap-exception while sending new-session request: " + e);
        }
    }

    /**
     * send a purge-publish request to map-server
     */
    public void sendPurgePublishRequest() {
        try {
            IfMapClient.LOGGER.info("sending out purge-publish request over src...");
            sender.purgePublisher();
        } catch (IfmapErrorResult e) {
            IfMapClient.exit("ifmap-error-result while sending purge-publish request: " + e);
        } catch (IfmapException e) {
            IfMapClient.exit("ifmap-exception while sending purge-publish request: " + e);
        }
    }

    /**
     * send a end-session request to map-server
     */
    public void sendEndSessionRequest() {
        try {
            IfMapClient.LOGGER.info("sending out end-session request over src...");
            sender.endSession();
        } catch (IfmapErrorResult e) {
            IfMapClient.exit("ifmap-error-result while sending end-session request: " + e);
        } catch (IfmapException e) {
            IfMapClient.exit("ifmap-exception while sending end-session request: " + e);
        }
    }

    /**
     * send a publish update request using the data from passed in result-list
     * 
     * @param resultList data to be mapped and published
     */
    public void sendPublishRequest(MappingResult[] resultList) {
        // esukom-specific-metadata
        if (GeneralConfig.SNORT_MODE.equalsIgnoreCase("esukom")) {
            // snort-client metadata
            if (GeneralConfig.MAPPINGCONFIG_CLASSNAME
                    .equals("de.esukom.decoit.ifmapclient.mappingfactory.SnortFileEventMappingFactory")
                    || GeneralConfig.MAPPINGCONFIG_CLASSNAME
                            .equals("de.esukom.decoit.ifmapclient.mappingfactory.SnortBarnyardFileEventMappingFactory")
                    || GeneralConfig.MAPPINGCONFIG_CLASSNAME
                            .equals("de.esukom.decoit.ifmapclient.mappingfactory.SnortSqlEventMappingFactory")) {
                sender.publishEsukomSpecificSnortEventUpdate(resultList);
            }
        }

        // openvpn-specific data (no event-publishing!)
        if (GeneralConfig.MAPPINGCONFIG_CLASSNAME
                .equals("de.esukom.decoit.ifmapclient.mappingfactory.impl.OpenVPNMappingFactory")) {
            sender.publishOpenVpnData(resultList);
        }

        // "standard"-metadata
        else {
            sender.publishEventUpdate(resultList);
        }
    }

    /**
     * (re)activate soap-message-polling thread
     */
    public void activateSOAPMessagePollingThread() {
        if (mPoller != null) {
            if (!mIsARCPollingThreadActive) {
                if (pollingClientList.getAllSubscribedEntrys() != null
                        && pollingClientList.getAllSubscribedEntrys().size() > 0) {
                    IfMapClient.LOGGER.config("activating arc-polling-thread...");
                    mPoller.running = true;
                    mPoller.pausing = false;
                    new Thread(mPoller).start();
                    mIsARCPollingThreadActive = true;
                }
                else {
                    IfMapClient.LOGGER
                            .info("not activiating arc-polling-thread because there are no subscribtions found");
                }
            }
            else {
                IfMapClient.LOGGER.info("not activiating arc-polling-thread because it is already set active");
            }
        }
    }

    public void activateSOAPMessagePollingThreadForAlertEvents() {
        if (mPoller != null) {
            if (!mIsARCPollingThreadActive) {

                IfMapClient.LOGGER.config("activating arc-polling-thread...");
                mPoller.running = true;
                mPoller.pausing = false;
                new Thread(mPoller).start();
                mIsARCPollingThreadActive = true;

            }
            else {
                IfMapClient.LOGGER.info("not activiating arc-polling-thread because it is already set active");
            }
        }
    }

    /**
     * check client-list for un-subscribed entries and subscribe them to the MAP-Server
     */
    public void updateSubscribtions() {
        IfMapClient.LOGGER.info("updating subscriptions...");
        if (pollingClientList != null && pollingClientList.getClientList().size() > 0) {
            for (int i = 0; i < pollingClientList.getClientList().size(); i++) {
                if (!pollingClientList.getClientList().get(i).isSubscribed()) {
                    if (sender != null) {
                        // subscribe current client
                        IfMapClient.LOGGER.info("subscribing client: "
                                + pollingClientList.getClientList().get(i).getIPAddress()
                                + ", current subscribe-index is: " + mSubscribeIndex);
                        sender.publishSubscription(pollingClientList.getClientList().get(i).getIPAddress(),
                                mSubscribeIndex);

                        // set new states to current client-list-entry
                        pollingClientList.getClientList().get(i).setSubscribed(true);
                        pollingClientList.getClientList().get(i).setSubscriptionName(mSubscribeIndex);
                        mSubscribeIndex++;

                        // (re)-activate IFMAPMessage-Polling-Thread
                        IfMapClient.LOGGER.info("subscription published...trying to activate ARC-Poller");
                        activateSOAPMessagePollingThread();
                    }
                    else {
                        IfMapClient.exit("error while initializing Subscription: SOAPMessageSender cannot be null!");
                    }
                }
            }
        }
        else {
            IfMapClient.LOGGER.info("no entries found in subscription-list...no subscriptions to be published");
        }
    }

    /**
     * - walk through all entries in polling client list - if a
     */
    public void checkEntriesForAllowance(MappingResult[] resultList) {
        boolean checkClient = false;
        // if we have read-out some ip-addresses...
        if (resultList != null) {
            for (int i = 0; i < resultList.length; i++) {
                EventMappingResult currentEventResult = (EventMappingResult) resultList[i];
                String currentIp = currentEventResult.getIp();

                // check if client is already in client-list, if not add it
                PollingClientListEntry newEntry = new PollingClientListEntry(currentIp);

                // check if current IP is already in client list if not, insert it
                if (pollingClientList.insertEntryIfIPNotExists(newEntry)) {
                    IfMapClient.LOGGER.info("new client has been added to client list: " + currentIp);
                    checkClient = true;
                }

                // entry is already in client-list, check if it is already allowed or blocked
                else {
                    IfMapClient.LOGGER.info("client is already in client-list...checking current state now");
                    if (IFMAPMessagingFacade.getInstance().pollingClientList.isClientBlockedOrAllowed(currentIp)) {
                        IfMapClient.LOGGER.info("client has been already blocked or allowed: " + currentIp
                                + "...not checking allowance");
                        continue;
                    }
                    else {
                        // check new client for allowance if necessary
                        checkClient = true;
                    }
                }

                // perform allowance-check
                if (checkClient && sender != null) {
                    IfMapClient.LOGGER.info("allowance-checking for client: " + currentIp);
                    if (executeSearchRequestAndCheckAllowance(currentIp)) {
                        RulesExecutor ipTablesRulesExecutor = RulesExecutor.getInstance();

                        // execute iptables-allow-rule for input-table
                        IfMapClient.LOGGER.info("executing allowance rule for input table for client " + currentIp);
                        ipTablesRulesExecutor.executePredefinedRule(
                                Rules.PREDEFINED_RULE_INSERT_INPUT_APPEND_ALLOW__IP, new String[] { currentIp });

                        if (GeneralConfig.IPTABLES_GATEWAYMODE) {
                            // also execute iptables-allow-rule for forward-table
                            IfMapClient.LOGGER.info("executing allowance rule for forward table for client "
                                    + currentIp);
                            ipTablesRulesExecutor.executePredefinedRule(
                                    Rules.PREDEFINED_RULE_INSERT_FORWARD_APPEND_ALLOW__IP, new String[] { currentIp });
                        }

                        IfMapClient.LOGGER.info("client has been allowed: " + currentIp
                                + "...setting allowed-state for entry");
                        pollingClientList.getEntryByIpString(currentIp).setAllowed(true);
                    }
                    else {
                        IfMapClient.LOGGER.info("allowance check: " + currentIp + " has not been allowed");
                    }
                }
                else {
                    IfMapClient.exit("error while performing allowance-check: SOAPMessageSender cannot be null!");
                }
            }
        }
    }

    public boolean executeSearchRequestAndCheckAllowance(String ip) {
        // check the "skip allowance" flag
        if (IPTablesFacade.getInstance().mSkipAllowClientCheck) {
            IfMapClient.LOGGER.info("skip allowance check is set, client with " + ip + " has been allowed!");
            return true;
        }

        /*
         * check if client is already inside "internal network"
         * IfMapClient.LOGGER.info("------------------------------------------------------------------"
         * ); IfMapClient.LOGGER.info("execute check for internal network for client " + ip);
         * IfMapClient
         * .LOGGER.info("------------------------------------------------------------------");
         * de.fhhannover.inform.trust.ifmapj.messages.SearchResult result =
         * sender.publishSearchRequest(ip);
         * IfMapClient.LOGGER.info("incoming search-result from search request for ip " + ip); //
         * get result items if (result != null) { List<ResultItem> resultItems =
         * result.getResultItems(); boolean isOk = false; for (int i = 0; i < resultItems.size();
         * i++) { // output identifiers if (resultItems.get(i).getIdentifier1() != null) {
         * IfMapClient.LOGGER.fine("entry[" + i + "] - Identifier 1 Name:" +
         * resultItems.get(i).getIdentifier1().toString()); } if
         * (resultItems.get(i).getIdentifier2() != null) { IfMapClient.LOGGER.fine("entry[" + i +
         * "] - Identifier 2 Name:" + resultItems.get(i).getIdentifier2().toString()); }
         * 
         * // check if (resultItems.get(i).getMetadata() != null) { Collection<Document> mresult =
         * resultItems.get(i).getMetadata(); for (Document currentDoc : mresult) { NodeList list =
         * currentDoc.getChildNodes(); isOk =
         * IPTablesFacade.getInstance().checkPollSearchMetadataForAllowance(list,
         * PollResultChecker.RESULTCHECK_TYPE_SEARCHRESULT); if (isOk) {
         * IfMapClient.LOGGER.info("client with " + ip + " has been allowed!"); return true; } } } }
         * }
         */

        // if client is not in "internal" network, execute Allowance-Check
        IfMapClient.LOGGER.info("------------------------------------------------------------------");
        IfMapClient.LOGGER.info("executeSearchRequestAndCheckAllowance : String " + ip);
        IfMapClient.LOGGER.info("------------------------------------------------------------------");
        de.fhhannover.inform.trust.ifmapj.messages.SearchResult myResult = sender.publishSearchRequest(ip);
        IfMapClient.LOGGER.info("incoming search-result from search request for ip " + ip);
        // get result items
        if (myResult != null) {
            List<ResultItem> resultItems = myResult.getResultItems();
            boolean isOk = false;
            for (int i = 0; i < resultItems.size(); i++) {
                // output identifiers
                if (resultItems.get(i).getIdentifier1() != null) {
                    IfMapClient.LOGGER.fine("entry[" + i + "] - Identifier 1 Name:"
                            + resultItems.get(i).getIdentifier1().toString());
                }
                if (resultItems.get(i).getIdentifier2() != null) {
                    IfMapClient.LOGGER.fine("entry[" + i + "] - Identifier 2 Name:"
                            + resultItems.get(i).getIdentifier2().toString());
                }

                // get result items
                if (resultItems.get(i).getMetadata() != null) {
                    Collection<Document> mresult = resultItems.get(i).getMetadata();
                    for (Document currentDoc : mresult) {
                        NodeList list = currentDoc.getChildNodes();
                        isOk = IPTablesFacade.getInstance().checkPollSearchMetadataForAllowance(list,
                                PollResultChecker.RESULTCHECK_TYPE_SEARCHRESULT);
                        if (isOk) {
                            IfMapClient.LOGGER.info("client with " + ip + " has been allowed!");
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public void subscribeToAlertEvents() {
        IfMapClient.LOGGER.info("subscribing to alert-events...");
        sender.publishAlertSubscribtion();
        activateSOAPMessagePollingThreadForAlertEvents();
    }

    /**
     * Method for handling callbacks/notifies from IFMAP-Message-Polling-Thread
     */
    public synchronized void update(Observable o, Object arg) {
        IfMapClient.LOGGER.info("ifmap-messaging-facade detected callback from arc-poller...");
        if (o != null) {
            // TODO: check casting...
            PollResult tmpResult = (PollResult) arg;
            checkPollResult(tmpResult);
        }
        else {
            IfMapClient.LOGGER
                    .info("result from " + GeneralConfig.POLLINGCONFIG_CLASSNAME + " is null, nothing to do!");
        }

        // (re)-activate polling thread
        activateSOAPMessagePollingThread();
    }

    /**
     * check incoming poll result and perform different tasks depending on content of result
     * 
     * @param resultList list containing result-items
     */
    public void checkPollResult(PollResult result) {
        IfMapClient.LOGGER.info("checking incoming poll result...");

        // check poll-search-result. this will contain the results of an initial
        // poll-response after first subscription was made
        if (result != null && result.getSearchResults().size() > 0) {
            SearchResult search = result.getSearchResults().iterator().next();
            // default mode: check for allowance by predefined checking-rules
            // inside
            // iptables-allowance config.properties
            if (search != null && GeneralConfig.IPTABLES_MODE.equalsIgnoreCase("default")) {
                IPTablesFacade.getInstance().checkPollResultForIPTablesEvent(search.getResultItems(),
                        PollResultChecker.RESULTCHECK_TYPE_SEARCHRESULT);
            }
        }

        // check poll-update-result. this will contain the result of updated
        // meta-data and identifiers from poll-response
        if (result != null && result.getUpdateResults().size() > 0) {
            Collection<SearchResult> update = result.getUpdateResults();
            for (Iterator<SearchResult> i = update.iterator(); i.hasNext();) {
                SearchResult sr = i.next();
                // esukom-specific approach for enforcment-check
                if (GeneralConfig.IPTABLES_MODE.equalsIgnoreCase("esukom")) {
                    IPTablesFacade.getInstance().checkPollResultForIPTablesAlert(sr.getResultItems(),
                            PollResultChecker.RESULTCHECK_TYPE_UPDATERESULT);
                }
                // default approach for enforcment-check
                else {
                    IPTablesFacade.getInstance().checkPollResultForIPTablesEvent(sr.getResultItems(),
                            PollResultChecker.RESULTCHECK_TYPE_UPDATERESULT);
                }

            }
        }
    }
}