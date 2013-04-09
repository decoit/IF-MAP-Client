/*
 * SOAPMessageSender.java 0.2 13/02/27
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
import de.esukom.decoit.ifmapclient.logging.Logging;
import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.mappingfactory.result.EventMappingResult;
import de.esukom.decoit.ifmapclient.mappingfactory.result.MappingResult;
import de.esukom.decoit.ifmapclient.mappingfactory.result.OpenVPNMappingResult;
import de.esukom.decoit.ifmapclient.util.Toolbox;
import de.fhhannover.inform.trust.ifmapj.IfmapJ;
import de.fhhannover.inform.trust.ifmapj.IfmapJHelper;
import de.fhhannover.inform.trust.ifmapj.binding.IfmapStrings;
import de.fhhannover.inform.trust.ifmapj.channel.ARC;
import de.fhhannover.inform.trust.ifmapj.channel.SSRC;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapErrorResult;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapException;
import de.fhhannover.inform.trust.ifmapj.exception.InitializationException;
import de.fhhannover.inform.trust.ifmapj.identifier.AccessRequest;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifier;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.Identity;
import de.fhhannover.inform.trust.ifmapj.identifier.IdentityType;
import de.fhhannover.inform.trust.ifmapj.identifier.IpAddress;
import de.fhhannover.inform.trust.ifmapj.messages.MetadataLifetime;
import de.fhhannover.inform.trust.ifmapj.messages.PublishDelete;
import de.fhhannover.inform.trust.ifmapj.messages.PublishNotify;
import de.fhhannover.inform.trust.ifmapj.messages.PublishRequest;
import de.fhhannover.inform.trust.ifmapj.messages.PublishUpdate;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;
import de.fhhannover.inform.trust.ifmapj.messages.SearchRequest;
import de.fhhannover.inform.trust.ifmapj.messages.SubscribeRequest;
import de.fhhannover.inform.trust.ifmapj.metadata.EnforcementAction;
import de.fhhannover.inform.trust.ifmapj.metadata.EventType;
import de.fhhannover.inform.trust.ifmapj.metadata.Significance;
import de.fhhannover.inform.trust.ifmapj.metadata.StandardIfmapMetadataFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * This Class contains all methods that are necessary for the communication with the IF-MAP Server
 * 
 * @version 0.01 01 Dez 2009
 * @version 0.02 08 Dez 2009
 * @version 0.03 15 Dez 2009
 * @version 0.04 17 Dez 2009
 * @version 0.05 21 Dez 2009
 * @version 0.06 22 Dez 2009
 * @version 0.07 17 Mar 2010
 * @version 0.08 30 Mar 2010
 * @version 0.09 13 Apr 2010
 * @version 0.10 13 Apr 2010
 * @version 0.11 20 Apr 2010
 * @version 0.12 27 Apr 2010
 * @version 0.13 04 May 2010
 * @version 0.14 11 May 2010
 * @version 0.15 18 May 2010
 * @version 0.16 22 Jun 2010
 * @version 0.20 13 Feb 2013
 * 
 * @author Sebastian Kobert
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public final class SOAPMessageSender {

    // flags indicating whether to send events as update or notify
    public static final byte PUBLISH_TYPE_UPDATE = 0;
    public static final byte PUBLISH_TYPE_NOTIFY = 1;

    // publish mode, update is default
    public static byte publishMode = PUBLISH_TYPE_UPDATE;

    // some predefined values for esukom-specific.metadata
    final String OTHER_TYPE_DEFINITION = "32939:category";
    final String NAMESPACE = "http://www.esukom.de/2012/ifmap-metadata/1";
    final String NAMESPACE_PREFIX = "esukom";
    final String QUANT = "quantitive";
    final String ARBIT = "arbitrary";
    final String QUALI = "qualified";

    // represents our SSRC to the MAPS
    private SSRC mSSRC;

    // indicates whether a session is active or not.
    private volatile boolean mSessionActive;

    // document builder and factory
    private DocumentBuilderFactory mDocumentBuilderFactory = null;
    private DocumentBuilder mDocumentBuilder = null;

    // creates standard IF-MAP metadata, ip-mac in this case
    private StandardIfmapMetadataFactory mMetadataFactory;

    // flag used for adding delete-request to publish update after initial publish of metadata
    private boolean mInitialPublishDone = false;

    // singleton instance
    private static SOAPMessageSender mInstance;

    // client-list for connected openvpn-clients
    private ArrayList<OpenVPNMappingResult> mVpnClientList = new ArrayList<OpenVPNMappingResult>();

    /**
     * This is the private Constructor for the Singleton Pattern
     */
    private SOAPMessageSender() {
        mMetadataFactory = IfmapJ.createStandardMetadataFactory();
        mDocumentBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            mDocumentBuilder = mDocumentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * static method "getInstance()" delivers the only Instance of this Class
     */
    public synchronized static SOAPMessageSender getInstance() {
        if (mInstance == null) {
            mInstance = new SOAPMessageSender();
        }
        return mInstance;
    }

    /**
     * This Method opens a new Connection to the IF-MAP Server
     */
    public void prepareSSRC() throws InitializationException {
        if (mSSRC == null) {
            TrustManager[] tms = IfmapJHelper.getTrustManagers(GeneralConfig.MAPSERVER_TRUSTSTORE_PATH,
                    GeneralConfig.MAPSERVER_TRUSTSTORE_PASSWORD);

            if (GeneralConfig.MAPSERVER_BASICAUTH_ENABLED) {
                mSSRC = IfmapJ.createSSRC(GeneralConfig.MAPSERVER_URL, GeneralConfig.MAPSERVER_BASICAUTH_USER,
                        GeneralConfig.MAPSERVER_BASICAUTH_PASSWORD, tms);
            }
            else {
                KeyManager[] kms = IfmapJHelper.getKeyManagers(GeneralConfig.MAPSERVER_KEYSTORE_PATH,
                        GeneralConfig.MAPSERVER_KEYSTORE_PASSWORD);
                mSSRC = IfmapJ.createSSRC(GeneralConfig.MAPSERVER_URL, kms, tms);
            }
        }
    }

    /**
     * Starts a new session
     */
    public void newSession() throws IfmapErrorResult, IfmapException {
        if (!mSessionActive && mSSRC != null) {
            mSSRC.newSession();
            mSessionActive = true;
        }
    }

    /**
     * Closes a session
     */
    public void endSession() throws IfmapErrorResult, IfmapException {
        if (!mSessionActive || mSSRC == null) {
            return;
        }
        mSSRC.endSession();
        mSessionActive = false;
    }

    /**
     * Purges all published metadata
     */
    public void purgePublisher() throws IfmapErrorResult, IfmapException {
        if (mSSRC != null && mSessionActive)
            mSSRC.purgePublisher();
    }

    /**
     * get the current IF-MAP-Publisher id
     * 
     * @return current publisher-id
     */
    public String getIfMapPublisherId() {
        if (mSSRC != null && mSessionActive) {
            return mSSRC.getPublisherId();
        }
        return null;
    }

    /**
     * get the ARC
     * 
     * @return the ARC from current SSRC
     */
    public ARC getARC() {
        if (mSSRC != null) {
            try {
                return mSSRC.getArc();
            } catch (InitializationException e) {
                IfMapClient.exit("error while initializing ARC");
            }
        }
        return null;
    }

    /**
     * Publishes metadata of detected events
     * 
     * @param events list of read in events from mapping factory
     */
    public void publishEventUpdate(final MappingResult[] resultList) {
        // create publish update and requests objects
        PublishRequest pr = Requests.createPublishReq();
        PublishUpdate pu = null;
        PublishNotify pn = null;
        PublishDelete pd = null;
        Document event = null;

        for (MappingResult mappingResult : resultList) {

            // prepare event-data
            EventMappingResult tmpEvent = (EventMappingResult) mappingResult;

            String name = null;
            if (tmpEvent.getName() != null && tmpEvent.getName().length() > 0) {
                name = tmpEvent.getName();
            }
            else {
                IfMapClient.LOGGER.warning("no name for current event, using 'undefined'");
                tmpEvent.setName("undefined");
            }

            // identity = tmpEvent.getIdentity();

            // create new event from data, other_type_definition and information is currently empty
            event = mMetadataFactory.createEvent(name, tmpEvent.getDiscoveredTime(), getIfMapPublisherId(),
                    Integer.valueOf(tmpEvent.getMagnitude()), Integer.valueOf(tmpEvent.getConfidence()),
                    tmpEvent.getSignificance(), tmpEvent.getEventMessageType(), "", "", tmpEvent.getVulnerabilityUri());

            Identity ident = null;
            if (tmpEvent.getIdentity() != null) {
                ident = Identifiers.createIdentity(IdentityType.aikName, tmpEvent.getIdentity());
            }

            // get values for IP-Identifier from mapping-result
            IpAddress ipAddress = createIpAddress(tmpEvent.getIp(), tmpEvent.getIpType());
            if (ipAddress == null) {
                IfMapClient.LOGGER.warning("error while detecting ip-type from mapping-result, skipping entry");
                continue;
            }

            // create publish update/notify request and add data to it
            if (publishMode == PUBLISH_TYPE_UPDATE) {
                pu = Requests.createPublishUpdate();
                pu.setLifeTime(MetadataLifetime.session);
                pu.setIdentifier1(ipAddress);
                if (tmpEvent.getIdentity() != null) {
                    pu.setIdentifier2(ident);
                }
                pu.addMetadata(event);

                /*
                 * additional publish delete request for nagios states if nagios host state changes
                 * from up to down, also delete the previous state (eg. state is up -> delete down
                 * state). This hole thing is kind of very quick and dirty at the moment...
                 */
                PublishDelete pdNagiosHostState = null;
                PublishDelete pdNagiosServiceState = null;
                String nagiosHostStateMsg = "Detected Host State: ";
                String nagiosServiceStateMsg = "Detected Service State: ";

                if (tmpEvent.getName().startsWith("Detected Host State")) {
                    if (tmpEvent.getName().contains("UP")) {
                        nagiosHostStateMsg += "DOWN";
                    }
                    else if (tmpEvent.getName().contains("DOWN")) {
                        nagiosHostStateMsg += "UP";
                    }

                    pdNagiosHostState = Requests.createPublishDelete();
                    pdNagiosHostState.setIdentifier1(ipAddress);
                    pdNagiosHostState.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
                            IfmapStrings.STD_METADATA_NS_URI);
                    pdNagiosHostState.setFilter("meta:event[name='" + nagiosHostStateMsg + "']");
                    pr.addPublishElement(pdNagiosHostState);
                }
                else if (tmpEvent.getName().startsWith("Detected Service State")) {
                    if (tmpEvent.getName().contains("OK")) {
                        nagiosServiceStateMsg += "CRITICAL";
                    }
                    else if (tmpEvent.getName().contains("CRITICAL")) {
                        nagiosServiceStateMsg += "OK";
                    }

                    pdNagiosServiceState = Requests.createPublishDelete();
                    pdNagiosServiceState.setIdentifier1(ipAddress);
                    pdNagiosServiceState.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
                            IfmapStrings.STD_METADATA_NS_URI);
                    pdNagiosServiceState.setFilter("meta:event[name='" + nagiosServiceStateMsg + "']");
                    pr.addPublishElement(pdNagiosServiceState);
                }
                /*
                 * ...the end of this horrible piece of code ;-)
                 */

                pd = Requests.createPublishDelete();
                pd.setIdentifier1(ipAddress);
                pd.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
                pd.setFilter("meta:event[name='" + name + "']");
                pr.addPublishElement(pd);

                // logger.info("Added new Event to be published -> Element[" + i + "] Name: " + name
                // + "|Type: "
                // + eventtype.toString() + "|IP-Identifier: " + tmpEvent.getIp() + "|IP-Type: "
                // + tmpEvent.getIpType());

                pr.addPublishElement(pu);
            }
            else if (publishMode == PUBLISH_TYPE_NOTIFY) {
                pn = Requests.createPublishNotify();
                pn.setIdentifier1(ipAddress);
                if (tmpEvent.getIdentity() != null)
                    pn.setIdentifier2(ident);
                pn.addMetadata(event);

                pr.addPublishElement(pn);
            }
        }
        publish(pr);
    }

    /**
     * Publishes metadata of detected events as esukom-specific metadata
     * 
     * @param events list of read in events from mapping factory
     */
    public void publishEsukomSpecificSnortEventUpdate(MappingResult[] resultList) {

        // if flag is set, publish all updates in separate publish requests
        boolean useSeperateRequests = true;

        // create publish update and requests objects
        PublishRequest publishRequest = Requests.createPublishReq();

        for (int i = 0; i < resultList.length; i++) {
            if (useSeperateRequests) {
                publishRequest = Requests.createPublishReq();
            }

            // create IDS-Category
            Identifier mIDSCategory = createCategory("ids", getIfMapPublisherId() + ":23");

            // get the event-mapping-result
            EventMappingResult tmpEvent = (EventMappingResult) resultList[i];

            // prepare event-data
            String name = null;
            if (tmpEvent.getName() != null && tmpEvent.getName().length() > 0) {
                name = tmpEvent.getName();
            }
            else {
                name = "undefined";
                tmpEvent.setName(name);
            }

            // get values for IP-Identifier from mapping-result
            IpAddress ipAddress = createIpAddress(tmpEvent.getIp(), tmpEvent.getIpType());
            if (ipAddress == null) {
                continue;
            }

            // create ip-address identifier, just used for testing with irongui
            // IpAddress ipAddressIdent = Identifiers.createIp4("10.10.253.2");

            // create metadata
            Document eventNameFeature = createFeature("ids.EventName", tmpEvent.getDiscoveredTime(), name, ARBIT);
            Document eventTypeFeature = createFeature("ids.EventType", tmpEvent.getDiscoveredTime(), tmpEvent
                    .getEventMessageType().name(), QUALI);
            Document eventConfidenceFeature = createFeature("ids.EventConfidence", tmpEvent.getDiscoveredTime(),
                    tmpEvent.getConfidence(), QUANT);
            Document eventImpactFeature = createFeature("ids.EventImpact", tmpEvent.getDiscoveredTime(),
                    tmpEvent.getMagnitude(), QUANT);
            Document eventThreadFeature = createFeature("ids.EventThread", tmpEvent.getDiscoveredTime(), tmpEvent
                    .getSignificance().name(), QUALI);
            Document eventSourceFeature = createFeature("ids.EventSource", tmpEvent.getDiscoveredTime(),
                    ipAddress.getValue(), ARBIT);

            // addToPublishRequest(publishRequest, ipAddressIdent, mIDSCategory, eventNameFeature,
            // MetadataLifetime.session, publishMode);
            addToPublishRequest(publishRequest, mIDSCategory, null, eventNameFeature, MetadataLifetime.session,
                    publishMode);
            addToPublishRequest(publishRequest, mIDSCategory, null, eventTypeFeature, MetadataLifetime.session,
                    publishMode);
            addToPublishRequest(publishRequest, mIDSCategory, null, eventConfidenceFeature, MetadataLifetime.session,
                    publishMode);
            addToPublishRequest(publishRequest, mIDSCategory, null, eventImpactFeature, MetadataLifetime.session,
                    publishMode);
            addToPublishRequest(publishRequest, mIDSCategory, null, eventThreadFeature, MetadataLifetime.session,
                    publishMode);
            addToPublishRequest(publishRequest, mIDSCategory, null, eventSourceFeature, MetadataLifetime.session,
                    publishMode);

            if (useSeperateRequests) {
                publish(publishRequest);
            }
        }

        if (!useSeperateRequests) {
            publish(publishRequest);
        }
    }

    /**
     * create new subscription request
     * 
     * @param subscriptionName name of the subscription
     * @param ident Identifier to start the search
     */
    public void publishSubscription(String ip, int sequenceNumber) {
        IpAddress myIp = Identifiers.createIp4(ip);
        SubscribeRequest subsc1 = Requests.createSubscribeReq((Requests.createSubscribeUpdate(new Integer(
                sequenceNumber).toString(), null, 10, null, null, null, myIp)));
        try {
            mSSRC.subscribe(subsc1);
        } catch (IfmapErrorResult e) {
            e.printStackTrace();
        } catch (IfmapException e) {
            e.printStackTrace();
        }
    }

    public void publishAlertSubscribtion() {
        Identity myIdentity = Identifiers.createIdentity(IdentityType.other, "alert", null, "32939:category");
        SubscribeRequest subsc1 = Requests.createSubscribeReq((Requests.createSubscribeUpdate("1", null, 10, null,
                null, null, myIdentity)));
        try {
            mSSRC.subscribe(subsc1);

        } catch (IfmapErrorResult e) {
            e.printStackTrace();
        } catch (IfmapException e) {
            e.printStackTrace();
        }
    }

    public de.fhhannover.inform.trust.ifmapj.messages.SearchResult publishSearchRequest(String ip) {
        IpAddress myIp = Identifiers.createIp4(ip);
        SearchRequest sreq1 = Requests.createSearchReq(null, 8, null, null, null, myIp);
        try {
            de.fhhannover.inform.trust.ifmapj.messages.SearchResult myResult = mSSRC.search(sreq1);
            return myResult;
        } catch (IfmapErrorResult e) {
            e.printStackTrace();
        } catch (IfmapException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * public an enforcement report
     * 
     * @param ip ip-address that was enforced
     * @param enforcementAction enforcement-action to be executed
     * @param otherTypeDefintion optional other-type-definition-string
     * @param enforcementReason reason for enforcement-string
     */
    public void publishEnforcementReportUpdate(String ip, EnforcementAction enforcementAction,
            String otherTypeDefinition, String enforcementReason) {
        // check incoming parameters
        if (otherTypeDefinition == null) {
            otherTypeDefinition = "";
        }
        if (enforcementReason == null) {
            enforcementReason = "";
        }

        // create IP-Address-Type
        IpAddress myIp = Identifiers.createIp4(ip);

        // create publish request and metadata-document
        PublishRequest pr = Requests.createPublishReq();
        PublishUpdate pu = null;
        PublishNotify pn = null;
        Document enforcementReport = mMetadataFactory.createEnforcementReport(enforcementAction, otherTypeDefinition,
                enforcementReason);

        // create publish update/notify request and add data to it
        if (enforcementReport != null && myIp != null) {
            if (publishMode == PUBLISH_TYPE_UPDATE) {
                pu = Requests.createPublishUpdate();
                pu.setLifeTime(MetadataLifetime.session);
                pu.setIdentifier1(myIp);
                pu.addMetadata(enforcementReport);
            }
            else if (publishMode == PUBLISH_TYPE_NOTIFY) {
                pn = Requests.createPublishNotify();
                pn.setIdentifier1(myIp);
                pn.addMetadata(enforcementReport);
            }

            // create publish update/notify request and add data to it
            pr.addPublishElement(pu);
            publish(pr);

        }
        else {
        }
    }

    private int arCounter = 0;

    public void publishOpenVpnData(MappingResult[] resultList) {

        // list of clients to be published
        ArrayList<OpenVPNMappingResult> publishClients = new ArrayList<OpenVPNMappingResult>();

        // list of clients to be deleted
        ArrayList<OpenVPNMappingResult> deleteClients = new ArrayList<OpenVPNMappingResult>();

        /*
         * special case: delete all clients. In order to get here, we need some kind of result from
         * the mapping-factory, so we use a result-object that has a special ip (999.999.999.999)
         * that cannot occur in real life. If we detect such a result, we know that the
         * openvpn-status.log is currently empty and we therefore need to delete all clients that
         * have been published before
         */
        boolean deleteAllClients = false;
        if (resultList.length == 1) {
            OpenVPNMappingResult currentResult = (OpenVPNMappingResult) resultList[0];
            if (currentResult.getIspIpAddress().equals("999.999.999.999")) {
                deleteClients = (ArrayList<OpenVPNMappingResult>) mVpnClientList.clone();
                mVpnClientList.clear();
                deleteAllClients = true;
            }
        }

        if (!deleteAllClients) {
            /*
             * check if there are new clients to be published by comparing the passed in
             * result-entries with the list of currently published clients
             */
            for (int i = 0; i < resultList.length; i++) {
                OpenVPNMappingResult currentResult = (OpenVPNMappingResult) resultList[i];
                String currentClientIp = currentResult.getIspIpAddress();
                boolean found = false;
                for (OpenVPNMappingResult currentClient : mVpnClientList) {
                    String currentExistingIp = currentClient.getIspIpAddress();
                    // is current client already published?
                    if (currentExistingIp.equals(currentClientIp)) {
                        found = true;
                        break;
                    }
                }

                // client from result has not been published, so add it to the publish list
                if (!found) {
                    publishClients.add(currentResult);
                }
            }

            /*
             * check if there are clients that have to be deleted. This is done by comparing the
             * list of already published clients with the clients from the result list. If the first
             * list contains clients that are not in the second list, these clients are no longer
             * connected and have to be deleted
             */
            for (OpenVPNMappingResult currentClient : mVpnClientList) {
                String currentExistingClientIp = currentClient.getIspIpAddress();
                boolean found = false;
                for (int i = 0; i < resultList.length; i++) {
                    OpenVPNMappingResult currentResult = (OpenVPNMappingResult) resultList[i];
                    String currentClientIp = currentResult.getIspIpAddress();
                    if (currentClientIp.equals(currentExistingClientIp)) {
                        found = true;
                        break;
                    }
                }

                // already published client is not in current result-list, so add it to delete list
                if (!found) {
                    deleteClients.add((OpenVPNMappingResult) currentClient);
                }
            }
        }

        PublishRequest pr = Requests.createPublishReq();
        PublishDelete pd1 = null;
        PublishDelete pd2 = null;
        PublishDelete pd3 = null;
        PublishUpdate pu = null;
        PublishUpdate pu2 = null;
        PublishUpdate pu3 = null;

        for (OpenVPNMappingResult currentResult : deleteClients) {
            IpAddress ispIp = Identifiers.createIp4(currentResult.getIspIpAddress());
            IpAddress vpnIp = Identifiers.createIp4(currentResult.getVpnIPAddress());
            Identity ident = Identifiers.createIdentity(IdentityType.userName, currentResult.getUsername());
            AccessRequest ar = Identifiers.createAr(getIfMapPublisherId() + ":" + currentResult.getArCounter());

            pd1 = Requests.createPublishDelete(ar, ident);
            pd1.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
            pd2 = Requests.createPublishDelete(ar, ispIp);
            pd2.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
            pd3 = Requests.createPublishDelete(ar, vpnIp);
            pd3.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);

            pr.addPublishElement(pd1);
            pr.addPublishElement(pd2);
            pr.addPublishElement(pd3);

            // remove client to be deleted from currently published client-list
            mVpnClientList.remove(currentResult);
        }

        // publish clients inside publish-list
        for (OpenVPNMappingResult currentResult : publishClients) {
            currentResult.setArCounter(arCounter);
            mVpnClientList.add(currentResult);

            IpAddress ispIp = Identifiers.createIp4(currentResult.getIspIpAddress());
            IpAddress vpnIp = Identifiers.createIp4(currentResult.getVpnIPAddress());
            AccessRequest myAr = Identifiers.createAr(getIfMapPublisherId() + ":" + arCounter++);
            Identity myIdentity = Identifiers.createIdentity(IdentityType.userName, currentResult.getUsername());

            pu = Requests.createPublishUpdate();
            pu.setLifeTime(MetadataLifetime.session);
            pu.setIdentifier1(myIdentity);
            pu.setIdentifier2(myAr);
            pu.addMetadata(mMetadataFactory.createAuthAs());

            pu2 = Requests.createPublishUpdate();
            pu2.setLifeTime(MetadataLifetime.session);
            pu2.setIdentifier1(ispIp);
            pu2.setIdentifier2(myAr);
            pu2.addMetadata(mMetadataFactory.createArIp());

            pu2 = Requests.createPublishUpdate();
            pu2.setLifeTime(MetadataLifetime.session);
            pu2.setIdentifier1(ispIp);
            pu2.setIdentifier2(myAr);
            pu2.addMetadata(mMetadataFactory.createArIp());

            pu3 = Requests.createPublishUpdate();
            pu3.setLifeTime(MetadataLifetime.session);
            pu3.setIdentifier1(vpnIp);
            pu3.setIdentifier2(myAr);
            pu3.addMetadata(mMetadataFactory.createArIp());

            pr.addPublishElement(pu);
            pr.addPublishElement(pu2);
            pr.addPublishElement(pu3);

        }

        // publish to MAP-Server
        if (pr != null && pr.getPublishElements() != null && pr.getPublishElements().size() > 0) {
            publish(pr);
        }

    }

    /**
     * send passed in publish-request to MAP-Server
     * 
     * @param pr publish-request to be send
     */
    private void publish(PublishRequest pr) {
        try {
            mSSRC.publish(pr);

            // set initial-publish-done-flag
            if (!mInitialPublishDone) {
                mInitialPublishDone = true;
            }
        } catch (IfmapErrorResult e) {
            e.printStackTrace();
            IfMapClient.exit("ifmap-error-result while publishing data over ssrc:");
        } catch (IfmapException e) {
            e.printStackTrace();
            IfMapClient.exit("ifmap-error-exception while publishing data over ssrc:");
        }
    }

    /**
     * create a new category
     * 
     * @param name category-name
     * @param admDomain administrative-domain-string
     * 
     * @return category-identifer
     */
    private Identity createCategory(String name, String admDomain) {
        return Identifiers.createIdentity(IdentityType.other, name, admDomain, OTHER_TYPE_DEFINITION);
    }

    /**
     * create new publish-update and add it to passed in publish-request if the data has already
     * been send, append a publish-delete to erase the previously published data
     * 
     * @param request publish-request-object to add updates to
     * @param ident1 first identifier for update-request
     * @param ident2 second identifier for update-request
     * @param metadata metadata to append to identifier(s)
     * @param metadataLifeTime lifetime of metadata
     * @param publishType type of publish request - either update or notify
     */
    public void addToPublishRequest(PublishRequest request, Identifier ident1, Identifier ident2, Document metadata,
            MetadataLifetime metadataLifeTime, byte publishType) {
        if (publishType == PUBLISH_TYPE_UPDATE) {
            // create publish-update
            PublishUpdate publishUpdate = Requests.createPublishUpdate();
            publishUpdate.setIdentifier1(ident1);
            if (ident2 != null) {
                publishUpdate.setIdentifier2(ident2);
            }
            publishUpdate.addMetadata(metadata);
            publishUpdate.setLifeTime(metadataLifeTime);

            // create publish-delete for previous (esukom-specific) metadata
            if (mInitialPublishDone) {
                PublishDelete publishDelete = Requests.createPublishDelete();
                if (metadata.getElementsByTagName("id").item(0) != null) {
                    publishDelete.addNamespaceDeclaration(NAMESPACE_PREFIX, NAMESPACE);
                    publishDelete.setFilter(metadata.getChildNodes().item(0).getPrefix() + ":"
                            + metadata.getChildNodes().item(0).getLocalName() + "[" + "id='"
                            + metadata.getElementsByTagName("id").item(0).getTextContent() + "'" + " and "
                            + "@ifmap-publisher-id=\'" + mSSRC.getPublisherId() + "\']");
                }
                publishDelete.setIdentifier1(ident1);
                if (ident2 != null) {
                    publishDelete.setIdentifier2(ident2);
                }
                // add publish-delete to request
                request.addPublishElement(publishDelete);

            }

            // add publish-update to request
            request.addPublishElement(publishUpdate);
        }
        else if (publishType == PUBLISH_TYPE_NOTIFY) {
            // create publish-notify
            PublishNotify publishNotify = Requests.createPublishNotify();
            publishNotify.setIdentifier1(ident1);
            if (ident2 != null) {
                publishNotify.setIdentifier2(ident2);
            }
            publishNotify.addMetadata(metadata);

            // add publish-notify to request
            request.addPublishElement(publishNotify);
        }
    }

    /**
     * create feature-metadata
     * 
     * @param id content for id-element
     * @param timestamp timestamp-string
     * @param value content for value-element
     * @param contentType type of content
     * 
     * @return feature-metadata-document
     */
    private Document createFeature(String id, String timestamp, String value, String contentType) {
        Document doc = mDocumentBuilder.newDocument();
        Element feature = doc.createElementNS(NAMESPACE, NAMESPACE_PREFIX + ":feature");

        feature.setAttributeNS(null, "ifmap-cardinality", "multiValue");
        feature.setAttribute("ctxp-timestamp", timestamp);

        Element idElement = doc.createElement("id");
        idElement.setTextContent(id);
        feature.appendChild(idElement);

        Element typeElement = doc.createElement("type");
        typeElement.setTextContent(contentType);
        feature.appendChild(typeElement);

        Element valueElement = doc.createElement("value");
        valueElement.setTextContent(value);
        feature.appendChild(valueElement);

        doc.appendChild(feature);
        return doc;
    }

    /**
     * create IpAddress-Object from passed in String
     * 
     * @param address ip-address string
     * @return ip-address
     */
    private IpAddress createIpAddress(String address, String type) {
        // get values for IP-Identifier from mapping-result
        IpAddress ipAddress = null;
        if (type.startsWith("IPv4")) {
            ipAddress = Identifiers.createIp4(address);
        }
        else if (type.startsWith("IPv6")) {
            ipAddress = Identifiers.createIp6(address);
        }
        return ipAddress;
    }
}
