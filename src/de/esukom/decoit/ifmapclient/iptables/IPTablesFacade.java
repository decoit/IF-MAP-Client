/* 
 * IPTablesFacade.java        0.1.4 12/02/16
 * 
 * DEVELOPED BY DECOIT GMBH WITHIN THE ESUKOM-PROJECT:
 * http://www.decoit.de/
 * http://www.esukom.de/cms/front_content.php?idcat=10&lang=1
 * 
 * DERIVED FROM  THE DHCP-IFMAP-CLIENT-IMPLEMENTATION DEVELOPED BY 
 * FHH/TRUST WITHIN THE IRON-PROJECT:
 * http://trust.inform.fh-hannover.de/joomla/
 * 
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file 
 * distributed with this work for additional information 
 * regarding copyright ownership.  The ASF licenses this file 
 * to you under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance 
 * with the License.  You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations 
 * under the License. 
 */

package de.esukom.decoit.ifmapclient.iptables;

import de.esukom.decoit.ifmapclient.config.GeneralConfig;
import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.messaging.IFMAPMessagingFacade;
import de.esukom.decoit.ifmapclient.messaging.PollingClientListEntry;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import de.fhhannover.inform.trust.ifmapj.identifier.IpAddress;
import de.fhhannover.inform.trust.ifmapj.messages.ResultItem;
import de.fhhannover.inform.trust.ifmapj.metadata.EnforcementAction;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * facade for handling different ip-tables related tasks
 * 
 * @version 0.1.4
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class IPTablesFacade {

    // singleton
    private static IPTablesFacade sInstance = new IPTablesFacade();

    // flag for turning of initial "allow-client"-check
    public boolean mSkipAllowClientCheck = false;

    // helper class for checking polling-results
    private PollResultChecker mResultChecker;

    // list for events that lead to an allowance/enforcement of a particular
    // host will be read out from enforcement.properties

    // +----------------------------+
    // | ENFORCEMENT - RULES - MAPS |
    // +----------------------------+
    private Multimap<String, String> mEnforcementMappingContains = HashMultimap.create();
    private Multimap<String, String> mEnforcementMappingMatches = HashMultimap.create();
    private Multimap<String, Integer> mEnforcementMappingEquals = HashMultimap.create();
    private Multimap<String, Integer> mEnforcementMappingLower = HashMultimap.create();
    private Multimap<String, Integer> mEnforcementMappingGreater = HashMultimap.create();

    // +---------------------+
    // | ALLOW - RULES - MAP |
    // +---------------------+
    private Multimap<String, String> mAllowMappingContains = HashMultimap.create();
    private Multimap<String, String> mAllowMappingMatches = HashMultimap.create();
    private Multimap<String, Integer> mAllowMappingEquals = HashMultimap.create();
    private Multimap<String, Integer> mAllowMappingLower = HashMultimap.create();
    private Multimap<String, Integer> mAllowMappingGreater = HashMultimap.create();

    private IPTablesFacade() {
        mResultChecker = new PollResultChecker();
    }

    /**
     * return instance of IPTablesFacade
     * 
     * @return instance of IPTablesFacade
     */
    public synchronized static IPTablesFacade getInstance() {
        return sInstance;
    }

    /**
     * initialize properties for iptables-facade
     * 
     * @param pr
     *            properties-object
     */
    public void init(Properties pr) {
        // initialize allow-rules
        //initializeProperties(pr, "iptables.allow.mapping", mAllowMappingContains, mAllowMappingMatches, mAllowMappingEquals,
        //        mAllowMappingGreater, mAllowMappingLower, Toolbox.REGEX_IPTABLES_ALLOW_ATTRIBUTE);
        initializeProperties(pr, "iptables.allow.mapping", mAllowMappingContains, mAllowMappingMatches, mAllowMappingEquals,
                mAllowMappingGreater, mAllowMappingLower, Toolbox.getRegExPattern("regex.allowatr"));

        // initialize enforcement-rules
        //initializeProperties(pr, "iptables.enforcement.mapping", mEnforcementMappingContains, mEnforcementMappingMatches,
        //       mEnforcementMappingEquals, mEnforcementMappingGreater, mEnforcementMappingLower,
        //        Toolbox.REGEX_IPTABLES_ENFORCEMENT_ATTRIBUTE);
        initializeProperties(pr, "iptables.enforcement.mapping", mEnforcementMappingContains, mEnforcementMappingMatches,
               mEnforcementMappingEquals, mEnforcementMappingGreater, mEnforcementMappingLower,
                Toolbox.getRegExPattern("regex.enforcementatr"));

        // show initialized rules-lists
        ouputRulesLists();
    }

    /**
     * initialize properties for iptables-facade. that includes initialization of enforcement- and allowing-maps from configuration file
     * (iptables/enforcement.properties)
     * 
     * @param pr
     *            properties-object
     * @param targetProperty
     *            target-property
     * @param containsMap
     *            target map for contains-filter-strings
     * @param matchesMap
     *            target map for matches-filter-strings
     * @param attributesRegEx
     *            regular expression for target attributes to look for
     */
    private void initializeProperties(Properties pr, String targetProperty, Multimap<String, String> containsMap,
            Multimap<String, String> matchesMap, Multimap<String, Integer> equalsMap, Multimap<String, Integer> greaterMap,
            Multimap<String, Integer> lowerMap, Pattern attributesRegEx) {

        // check "skip-allow-rules"-flag
        if (targetProperty.equals("iptables.allow.mapping")) {
            String[] values = pr.getProperty("iptables.allow.mapping").split(",");
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals("skip") || values[i].equals("SKIP")) {
                    // set "dont check for allowance"-flag
                    mSkipAllowClientCheck = true;
                    IfMapClient.LOGGER.config("ip-tables client allow-check has been set to skip...disabling allow-check");
                    return;
                }
            }
        }

        // get filter-rules from enforcement.config
        String[] propertiesStrings = null;
        if (!Toolbox.checkConfig(propertiesStrings = pr.getProperty(targetProperty).split(","))) {
            // no filters found in specified properties
            matchesMap = null;
            containsMap = null;
            equalsMap = null;
            greaterMap = null;
            lowerMap = null;
            IfMapClient.LOGGER.config("could not find any properties for ip-tables-rules...not initializing any rules");
        }

        // get filters from properties and build maps
        else {
            for (int i = 0; i < propertiesStrings.length; i++) {
                if (targetProperty.equals("iptables.allow.mapping")) {
                    // allowance rules
                    initializeAllowanceRules(attributesRegEx, propertiesStrings[i], containsMap, matchesMap);
                } else {
                    // enforcement rules
                    initializeEnforcementRules(attributesRegEx, propertiesStrings[i], equalsMap, greaterMap, lowerMap, containsMap,
                            matchesMap);
                }
            }
        }
    }

    /**
     * initialize ip-tables rules from read in enforcement-configuration
     * 
     * @param attributesRegEx
     *            regular expression for target attribute
     * @param propertiesString
     *            property to search for
     * @param equalsMap
     *            map to be filled with equal-rules
     * @param greaterMap
     *            map to be filled with greater-rules
     * @param lowerMap
     *            map to be filled with lower-rules
     * @param containsMap
     *            map to be filled with contains-rules
     * @param matchesMap
     *            map to be filled with matcher-rules
     */
    private void initializeEnforcementRules(Pattern attributesRegEx, String propertiesString, Multimap<String, Integer> equalsMap,
            Multimap<String, Integer> greaterMap, Multimap<String, Integer> lowerMap, Multimap<String, String> containsMap,
            Multimap<String, String> matchesMap) {

        // get current filter-target-attribute
        String targetAttributeString = null;
        Matcher targetAttributeMatcher = attributesRegEx.matcher(propertiesString);
        if (targetAttributeMatcher.find()) {
            targetAttributeString = targetAttributeMatcher.group();
        } else {
            // no attribute found for current entry...skipping
            IfMapClient.LOGGER.config("could not find target-attribute for rule: " + propertiesString + "...skipping rule");
            return;
        }

        // get current filter string and strip delimiters
        String filterString = null;
        Matcher filterStringMatcher = Toolbox.getRegExPattern("regex.delimiter").matcher(propertiesString);
        if (filterStringMatcher.find()) {
            filterString = filterStringMatcher.group().replace("[", "").replace("]", "");
        } else {
            // no filter-string found for current entry...skipping
            IfMapClient.LOGGER.config("could not find filter for rule: " + propertiesString + "...skipping rule");
            return;
        }

        // check filter-rule
        if ((filterString != null & filterString.length() > 0) & (targetAttributeString != null & targetAttributeString.length() > 0)) {
            // filter-rules for integer values
            int filterValue;
            if (targetAttributeString.equalsIgnoreCase("confidence") || targetAttributeString.equalsIgnoreCase("magnitude")) {
                try {
                    filterValue = new Integer(filterString).intValue();
                } catch (ClassCastException e) {
                    IfMapClient.LOGGER.config("could not cast filter-String " + filterString + " to integer value...skipping rule");
                    return;
                }

                // equals-rule
                if (propertiesString.startsWith(targetAttributeString + "_e") | propertiesString.startsWith(targetAttributeString + "_E")) {
                    equalsMap.put(targetAttributeString, filterValue);
                    IfMapClient.LOGGER.config("setting new rule: " + targetAttributeString + " EQUALS " + filterValue);
                }

                // greater-rule
                else if (propertiesString.startsWith(targetAttributeString + "_g")
                        | propertiesString.startsWith(targetAttributeString + "_G")) {
                    greaterMap.put(targetAttributeString, filterValue);
                    IfMapClient.LOGGER.config("setting new rule: " + targetAttributeString + " GREATER " + filterValue);
                }

                // lower-rule
                else if (propertiesString.startsWith(targetAttributeString + "_l")
                        | propertiesString.startsWith(targetAttributeString + "_L")) {
                    lowerMap.put(targetAttributeString, filterValue);
                    IfMapClient.LOGGER.config("setting new rule: " + targetAttributeString + " LESS " + filterValue);
                }
            }

            // filter-rules for string values
            else if (targetAttributeString.equalsIgnoreCase("significance") || targetAttributeString.equals("type")
                    || targetAttributeString.equals("name") || targetAttributeString.equals("discovererId")
                    || targetAttributeString.equals("ip") || targetAttributeString.equals("message")) {

                // contains-rule
                if (propertiesString.startsWith(targetAttributeString + "_c") | propertiesString.startsWith(targetAttributeString + "_C")) {
                    containsMap.put(targetAttributeString, filterString);
                    IfMapClient.LOGGER.config("setting new rule: " + targetAttributeString + " CONTAINS " + filterString);
                }

                // matches-rule
                else if (propertiesString.startsWith(targetAttributeString + "_m")
                        | propertiesString.startsWith(targetAttributeString + "_M")) {
                    matchesMap.put(targetAttributeString, filterString);
                    IfMapClient.LOGGER.config("setting new rule: " + targetAttributeString + " MATCHES " + filterString);
                }

            }
            // undefined rule
            else {
                IfMapClient.LOGGER.config("could not detectect filter-option for entry: " + targetAttributeString + " MATCHES/CONTAINS "
                        + filterString + "...skipping entry");
            }
        }

        // undefined filter string
        else {
            IfMapClient.LOGGER.config("could not detectect filter-string for entry: " + targetAttributeString + " OPERATION "
                    + filterString + "...skipping entry");
        }
    }

    /**
     * initialize ip-tables rules from read in enforcement-configuration
     * 
     * @param attributesRegEx
     *            regular expression for target attribute
     * @param propertiesString
     *            property to search for
     * @param containsMap
     *            map to be filled with contains-rules
     * @param matchesMap
     *            map to be filled with matcher-rules
     */
    private void initializeAllowanceRules(Pattern attributesRegEx, String propertiesString, Multimap<String, String> containsMap,
            Multimap<String, String> matchesMap) {

        // get current filter-target-attribute
        String targetAttributeString = null;
        Matcher targetAttributeMatcher = attributesRegEx.matcher(propertiesString);
        if (targetAttributeMatcher.find()) {
            targetAttributeString = targetAttributeMatcher.group();

        } else {
            // no attribute found for current entry...skipping
            IfMapClient.LOGGER.config("could not find target-attribute for rule: " + propertiesString + "...skipping rule");
            return;
        }

        // get current filter string and strip delimiters
        String filterString = null;
        Matcher filterStringMatcher = Toolbox.getRegExPattern("regex.delimiter").matcher(propertiesString);
        if (filterStringMatcher.find()) {
            filterString = filterStringMatcher.group().replace("[", "").replace("]", "");
        } else {
            // no filter-string found for current entry...skipping
            IfMapClient.LOGGER.config("could not find filter for rule: " + propertiesString + "...skipping rule");
            return;
        }

        // check filter-rule
        if ((filterString != null & filterString.length() > 0) & (targetAttributeString != null & targetAttributeString.length() > 0)) {
            // contains-rule
            if (propertiesString.startsWith(targetAttributeString + "_c") | propertiesString.startsWith(targetAttributeString + "_C")) {
                containsMap.put(targetAttributeString, filterString);
                IfMapClient.LOGGER.config("setting new rule: " + targetAttributeString + " CONTAINS " + filterString);
            }

            // matches-rule
            else if (propertiesString.startsWith(targetAttributeString + "_m") | propertiesString.startsWith(targetAttributeString + "_M")) {
                matchesMap.put(targetAttributeString, filterString);
                IfMapClient.LOGGER.config("setting new rule: " + targetAttributeString + " MATCHES " + filterString);
            }

            // not-contains-rule
            else if (propertiesString.startsWith(targetAttributeString + "_!c")
                    | propertiesString.startsWith(targetAttributeString + "_!C")) {
                containsMap.put("!" + targetAttributeString, filterString);
                IfMapClient.LOGGER.config("setting new rule: " + targetAttributeString + " NOT CONTAINS " + filterString);
            }

            // not-matches-rule
            else if (propertiesString.startsWith(targetAttributeString + "_!m")
                    | propertiesString.startsWith(targetAttributeString + "_!M")) {
                matchesMap.put("!" + targetAttributeString, filterString);
                IfMapClient.LOGGER.config("setting new rule: " + targetAttributeString + " NOT MATCHES " + filterString);
            }

            // undefined rule
            else {
                IfMapClient.LOGGER.config("could not detectect filter-option for entry: " + targetAttributeString + " MATCHES/CONTAINS "
                        + filterString + "...skipping entry");
            }
        } else {
            IfMapClient.LOGGER.config("could not detectect filter-string for entry: " + targetAttributeString + " MATCHES/CONTAINS "
                    + filterString + "...skipping entry");
        }
    }

    /**
     * check passed in collection of result-items for iptables related metadata that can cause an enforcement or an allowance of specific
     * clients. This Method checks Event-Metadata and is used when the esukom-specific-approach is deactivated.
     * 
     * @param ris
     *            collection of result-items
     * @param pollResultType
     *            type of poll-result (update/search)
     */
    public void checkPollResultForIPTablesEvent(Collection<ResultItem> ris, byte pollResultType) {
        IfMapClient.LOGGER.info("checking poll-result for ip-tables-related metadata...");
        if (pollResultType == PollResultChecker.RESULTCHECK_TYPE_SEARCHRESULT & mSkipAllowClientCheck) {
            IfMapClient.LOGGER.info("skipping of allowance-check has been activated...unblocking all clients that are new!");
            allowClientsFromClientList();
        }

        String ip = null;
        for (ResultItem ri : ris) {
            // get ip-address from identifier
            if (ri.getIdentifier1().toString().contains("ip")) {
                IpAddress ipIdentifier = (IpAddress) ri.getIdentifier1();
                ip = ipIdentifier.getValue();
            }

            // get result meta-data
            Collection<Document> mresult = ri.getMetadata();
            for (Document currentMetaDoc : mresult) {
                NodeList list = currentMetaDoc.getChildNodes();
                if (list != null && list.getLength() > 0) {

                    // update-result-check...may lead to enforcement
                    if (pollResultType == PollResultChecker.RESULTCHECK_TYPE_UPDATERESULT) {
                        // String reason = checkPollUpdateMetadataForEnforcement(list, pollResultType);
                        String reason = checkPollUpdateMetadataForEnforcement(list, pollResultType);
                        if (reason != null) {
                            // enforce/block client
                            if (ip != null && ip.length() > 0) {
                                enforceClient(ip, reason);
                            }
                        }
                    }
                    // search-result-check...may lead to allowance
                    else if (pollResultType == PollResultChecker.RESULTCHECK_TYPE_SEARCHRESULT) {
                        if (checkPollSearchMetadataForAllowance(list, pollResultType)) {
                            if (ip != null && ip.length() > 0) {
                                allowClient(ip);
                            }
                        }
                    }
                    // not so good...
                    else {
                        IfMapClient.LOGGER.warning("unknown poll-result type...skipping result-check!");
                    }
                }
            }
        }
    }

    /**
     * get all necessary data from incoming poll-result
     * 
     * @param pollResult
     *            poll-result passed from ifmapj
     * 
     * @return map containing the single values from a poll-result (currently message and source)
     */
    private HashMap<String, String> collectEnforcementDataFromPollResult(Collection<Document> pollResult) {
        String source = null;
        String message = null;
        boolean isAlertSource = false;
        boolean isAlertMessage = false;

        for (Document currentMetaDoc : pollResult) {
            // get a node of poll result
            NodeList list = currentMetaDoc.getChildNodes();
            if (list != null && list.getLength() > 0) {
                for (int j = 0; j < list.getLength(); j++) {
                    if (list.item(j).getLocalName().equals("feature")) {
                        NodeList child = list.item(j).getChildNodes();
                        // walk through elements of a feature
                        for (int k = 0; k < child.getLength(); k++) {
                            if (child.item(k).getNodeName() != null && child.item(k).getTextContent() != null) {

                                if (child.item(k).getNodeName().equals("id")
                                        && child.item(k).getTextContent().equalsIgnoreCase(("Alert.Source"))) {
                                    isAlertSource = true;
                                }

                                else if (child.item(k).getNodeName().equals("id")
                                        && child.item(k).getTextContent().equalsIgnoreCase(("Alert.Message"))) {
                                    isAlertMessage = true;
                                }

                                if (child.item(k).getNodeName().equals("value")) {
                                    if (isAlertSource) {
                                        source = child.item(k).getTextContent();
                                        isAlertSource = false;
                                    }
                                    if (isAlertMessage) {
                                        message = child.item(k).getTextContent();
                                        isAlertMessage = false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (source != null && message != null) {
            HashMap<String, String> returnMap = new HashMap<String, String>();
            returnMap.put("source", source);
            returnMap.put("message", message);
            return returnMap;
        } else {
            return null;
        }
    }

    /**
     * check passed in collection of result-items for iptables related metadata that can cause an enforcement or an allowance of specific
     * clients. This Method checks Alert-Metadata and is used when the esukom-specific-approach is activated.
     * 
     * @param ris
     *            collection of result-items
     * @param pollResultType
     *            type of poll-result (update/search)
     */
    public void checkPollResultForIPTablesAlert(Collection<ResultItem> ris, byte pollResultType) {
        IfMapClient.LOGGER.info("checking poll-result for ip-tables-related metadata...");
        /*
         * if (pollResultType == PollResultChecker.RESULTCHECK_TYPE_SEARCHRESULT & mSkipAllowClientCheck) {
         * IfMapClient.LOGGER.info("skipping of allowance-check has been activated...unblocking all clients that are new!");
         * allowClientsFromClientList(); }
         */

        // list for ip-addresses and their reported alert-messages
        ArrayList<HashMap<String, String>> res = new ArrayList<HashMap<String, String>>();

        // walk through the poll-result
        for (ResultItem ri : ris) {
            Collection<Document> mresult = ri.getMetadata();

            // collect relevant data from poll result for enforcement check
            if (pollResultType == PollResultChecker.RESULTCHECK_TYPE_UPDATERESULT) {
                HashMap<String, String> collectedData = collectEnforcementDataFromPollResult(mresult);
                if (collectedData != null) {
                    res.add(collectedData);
                }
            }
        }

        // enforcement check
        checkAndExecuteEnforcementByAlertEvent(res);
    }

    /**
     * check poll-result-data for enforcement-event and execute relating iptables-rules if necessary
     * 
     * @param pollResultData
     *            the poll-result
     */
    private void checkAndExecuteEnforcementByAlertEvent(ArrayList<HashMap<String, String>> pollResultData) {
        IfMapClient.LOGGER.info("checkAndExecuteEnforcementByAlertEvent called...");
        for (int i = 0; i < pollResultData.size(); i++) {
            String currentMessage = pollResultData.get(i).get("message");
            IfMapClient.LOGGER.info("current message: " + currentMessage);

            // matches check
            if (mResultChecker.checkPollResultForIPTablesStringRules(mEnforcementMappingMatches, "message", currentMessage,
                    PollResultChecker.RULECHECK_TYPE_MATCHES, PollResultChecker.RESULTCHECK_TYPE_UPDATERESULT)) {
                IfMapClient.LOGGER.info("Der eine hier");
                // perform enforcement
                enforceClient(pollResultData.get(i).get("source"), currentMessage);

            }

            // contains check
            if (mResultChecker.checkPollResultForIPTablesStringRules(mEnforcementMappingContains, "message", currentMessage,
                    PollResultChecker.RULECHECK_TYPE_CONTAINS, PollResultChecker.RESULTCHECK_TYPE_UPDATERESULT)) {
                IfMapClient.LOGGER.info("Der andere Dort");
                // perform enforcement
                enforceClient(pollResultData.get(i).get("source"), currentMessage);
            }

        }
    }

    /**
     * check meta-data inside passed in node-list for entries that leads to an enforcement
     * 
     * @param list
     *            list containing the meta-data to be checked
     * 
     * @return enforcement reason if event detected, otherwise null
     */
    private String checkPollUpdateMetadataForEnforcement(NodeList list, byte resultType) {
        IfMapClient.LOGGER.info("checking poll-update result for enforcement...");

        for (int j = 0; j < list.getLength(); j++) {
            if (list.item(j).getLocalName().equals("event")) {
                // get child nodes
                NodeList child = list.item(j).getChildNodes();
                for (int k = 0; k < child.getLength(); k++) {
                    // IfMapClient.mLogger.info("entry [" + k + "]: "
                    // + child.item(k).getNodeName() + " - "
                    // + child.item(k).getTextContent());
                    if (child.item(k).getNodeName() != null && child.item(k).getTextContent() != null) {
                        // String-Rules-Checking
                        if (child.item(k).getNodeName().equals("name") || child.item(k).getNodeName().equals("significance")
                                || child.item(k).getNodeName().equals("type") || child.item(k).getNodeName().equals("discovered-time")
                                || child.item(k).getNodeName().equals("discoverer-id")

                                || child.item(k).getNodeName().equals("other-type-definition")
                                || child.item(k).getNodeName().equals("information")) {

                            // check for "contains"-filter
                            if (mResultChecker.checkPollResultForIPTablesStringRules(mEnforcementMappingContains, child.item(k)
                                    .getNodeName(), child.item(k).getTextContent(), PollResultChecker.RULECHECK_TYPE_CONTAINS, resultType)) {
                                return "ENFORCEMENT REASON: " + child.item(k).getTextContent() + " detected";
                            }

                            // check for "matches"-filter
                            if (mResultChecker.checkPollResultForIPTablesStringRules(mEnforcementMappingMatches, child.item(k)
                                    .getNodeName(), child.item(k).getTextContent(), PollResultChecker.RULECHECK_TYPE_MATCHES, resultType)) {
                                return "ENFORCEMENT REASON: " + child.item(k).getTextContent() + " detected";
                            }
                        }
                        // Integer-Rules-Checking
                        else if (child.item(k).getNodeName().equals("magnitude") || child.item(k).getNodeName().equals("confidence")) {

                            // check for "equals"-filter
                            if (mResultChecker.checkPollResultForIPTablesIntegerRules(mEnforcementMappingEquals, child.item(k)
                                    .getNodeName(), new Integer(child.item(k).getTextContent()).intValue(),
                                    PollResultChecker.RULECHECK_TYPE_EQUALS, resultType)) {
                                return "ENFORCEMENT REASON: " + child.item(k).getTextContent() + " detected";
                            }

                            // check for "greater"-filter
                            if (mResultChecker.checkPollResultForIPTablesIntegerRules(mEnforcementMappingGreater, child.item(k)
                                    .getNodeName(), new Integer(child.item(k).getTextContent()).intValue(),
                                    PollResultChecker.RULECHECK_TYPE_GREATER, resultType)) {
                                return "ENFORCEMENT REASON: " + child.item(k).getTextContent() + " detected";
                            }

                            // check for "lower"-filter
                            if (mResultChecker.checkPollResultForIPTablesIntegerRules(mEnforcementMappingLower,
                                    child.item(k).getNodeName(), new Integer(child.item(k).getTextContent()).intValue(),
                                    PollResultChecker.RULECHECK_TYPE_LOWER, resultType)) {
                                return "ENFORCEMENT REASON: " + child.item(k).getTextContent() + " detected";
                            }
                        }
                    }

                }
            }
        }
        return null;
    }

    /**
     * check meta-data inside passed in node-list for entries that leads to an "allowance"
     * 
     * @param list
     *            list containing the meta-data to be checked
     * 
     * @return true, if list contains entries that lead to an "allowance"
     */
    public boolean checkPollSearchMetadataForAllowance(NodeList list, byte resultType) {
        IfMapClient.LOGGER.fine("checking poll-search result for allowance...");
        for (int j = 0; j < list.getLength(); j++) {
            IfMapClient.LOGGER.fine("current local name is: " + list.item(j).getLocalName());
            // detect if current metadata document is capability-type
            if (list.item(j).getLocalName().equals("role") || list.item(j).getLocalName().equals("capability")
                    || list.item(j).getLocalName().equals("device-characteristic")
                    || list.item(j).getLocalName().equals("access-request-ip")) {

                // get child nodes
                NodeList child = list.item(j).getChildNodes();
                for (int k = 0; k < child.getLength(); k++) {
                    if (child.item(k).getNodeName() != null && child.item(k).getTextContent() != null) {
                        IfMapClient.LOGGER.fine("entry [" + k + "]: " + child.item(k).getNodeName() + " - "
                                + child.item(k).getTextContent());

                        IfMapClient.LOGGER.fine("----> NOW CHECKING " + child.item(k).getNodeName() + " <----");

                        // check for "contains"-filter
                        if (mResultChecker.checkPollResultForIPTablesStringRules(mAllowMappingContains, child.item(k).getNodeName(), child
                                .item(k).getTextContent(), PollResultChecker.RULECHECK_TYPE_CONTAINS, resultType)) {
                            return true;
                        }

                        // check for "matches"-filter
                        if (mResultChecker.checkPollResultForIPTablesStringRules(mAllowMappingMatches, child.item(k).getNodeName(), child
                                .item(k).getTextContent(), PollResultChecker.RULECHECK_TYPE_MATCHES, resultType)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * check if client with passed in ip-address is already enforced/blocked, if not enforce client from passed in result entry via
     * ip-tables
     * 
     * @param ip
     *            ip-address of the client that should be blocked
     * @param reason
     *            the reason why this ip should be blocked
     */
    private void enforceClient(String ip, String reason) {
        // don't block MAP-Server IP!
        if (!ip.equals(IFMAPMessagingFacade.getInstance().mapServerIP)) {

            // check if client is already in blocked-state (no need to block twice)
            if (IFMAPMessagingFacade.getInstance().pollingClientList.isClientBlocked(ip)) {
                IfMapClient.LOGGER.info("client with ip " + ip + " is already blocked...no need to block it again!");
            }

            // client is not blocked yet (..or is not inside client list at all)
            else {
                IfMapClient.LOGGER.info("client with ip " + ip + " is not blocked...executing ip-tables blocking rule!");

                // block client via iptables (input-table)
                RulesExecutor ipTablesRulesExecutor = RulesExecutor.getInstance();
                IfMapClient.LOGGER.info("executing block rule for input tables for client " + ip);

                // if this call return false it�s mostly due to reason that we try to delete a rule that doesnt exist...
                if (!ipTablesRulesExecutor.executePredefinedRule(Rules.PREDEFINED_RULE_DELETE_APPEND_ALLOW_IP_INPUT, new String[] { ip })) {          
                    // ...so we set the client inside the list to force-block state, what will result in that the client
                    // will not be allowed anymore if he tries to connect
                    
                    // but first, we need to check if the client is inside the client list
                    PollingClientListEntry curEntry = IFMAPMessagingFacade.getInstance().pollingClientList.getEntryByIpString(ip);
                    if (curEntry == null){
                        // create new entry for client inside list
                        IfMapClient.LOGGER.info("client " + ip + "is not in client list...creating new entry");   
                        curEntry = new PollingClientListEntry(ip);
                        IFMAPMessagingFacade.getInstance().pollingClientList.insertEntryIfIPNotExists(curEntry);  
                    }
                    
                    // set client to blocked-state
                    IfMapClient.LOGGER.info("setting client " + ip + " in client list in block state");
                    IFMAPMessagingFacade.getInstance().pollingClientList.getEntryByIpString(ip).setForceBlocked(true);
                    //IFMAPMessagingFacade.getInstance().pollingClientList.insertEntryIfIPNotExists(curEntry);  
                }

                // block client via iptables (input-table)
                if (GeneralConfig.IPTABLES_GATEWAYMODE) {
                    IfMapClient.LOGGER.info("executing block rule for forward tables for client " + ip);
                    if (ipTablesRulesExecutor.executePredefinedRule(Rules.PREDEFINED_RULE_DELETE_APPEND_ALLOW_IP_FORWARD,
                            new String[] { ip })) {
                        // set client state inside client-list to "blocked"
                        IFMAPMessagingFacade.getInstance().pollingClientList.getEntryByIpString(ip).setAllowed(false);
                        IFMAPMessagingFacade.getInstance().pollingClientList.getEntryByIpString(ip).setForceBlocked(true);

                        
                    } else {
                        IfMapClient.LOGGER.info("setting client " + ip + " in client list in block state");
                        IFMAPMessagingFacade.getInstance().pollingClientList.getEntryByIpString(ip).setForceBlocked(true);

                    }
                }
                // build enforcement report and send it toMAP-Server
                IFMAPMessagingFacade.getInstance().sender.publishEnforcementReportUpdate(ip, EnforcementAction.block, reason,
                        reason);

            }
        }
    }

    /**
     * allow/unblock client with passed in ip via iptables
     * 
     * @param ip
     *            ip-address of the client that should be allowed
     */
    private void allowClient(String ip) {
        IFMAPMessagingFacade.getInstance().pollingClientList.allowClient(ip);
    }

    /**
     * execute IP-Tables Starting Rules. This Method will only be used if IP-Tables-Component is activated/enabled inside config.properties
     */
    public void executeIPTableStartRules(String mapServerIp) {
        Process p = null;
        try {
            IfMapClient.LOGGER.config("executing ip-tables-startup-script at: " + GeneralConfig.IPTABLES_STARTSCRIPT);
            p = Runtime.getRuntime().exec("sh " + GeneralConfig.IPTABLES_STARTSCRIPT);
            int returnCode = p.waitFor();
            IfMapClient.LOGGER.config("ip-tables-startup-script return-code: " + returnCode);

            if (returnCode != 0) {
                IfMapClient.exit("error while executing iptables-startup-rules-script...please check the file-path in config.properties");
            }
        } catch (Exception e) {
            IfMapClient.exit("error while executing iptables-startup-rules-script...please check the file-path in config.properties");
        }

        // flush all previous ip-tables-rules
        // RulesExecutor.getInstance().executePredefinedRule(Rules.PREDEFINED_RULE_FLUSH, null);

        // create initial ip-tables policy
        // RulesExecutor.getInstance().executePredefinedRule(Rules.PREDEFINED_RULE_POLICY_DROP, null);

        // create ip-tables rule for logging (ulog)
        // RulesExecutor.getInstance().executePredefinedRule(Rules.PREDEFINED_RULE_INSERT_ULOG, null);

        // allow MAP-Server
        RulesExecutor.getInstance()
                .executePredefinedRule(Rules.PREDEFINED_RULE_INSERT_INPUT_APPEND_ALLOW__IP, new String[] { mapServerIp });
    }

    /**
     * iterate through client-list and unblock all clients that are subscribed but not yet allowed via IP-Tables
     */
    public void allowClientsFromClientList() {
        for (int i = 0; i < IFMAPMessagingFacade.getInstance().pollingClientList.getClientList().size(); i++) {
            // exclude rule for Map-Server cause its already executed at initialization
            if (!IFMAPMessagingFacade.getInstance().pollingClientList.getClientList().get(i).getIPAddress()
                    .equals(IFMAPMessagingFacade.getInstance().mapServerIP)) {
                if (IFMAPMessagingFacade.getInstance().pollingClientList.getClientList().get(i).isSubscribed()
                        & !IFMAPMessagingFacade.getInstance().pollingClientList.getClientList().get(i).isAllowed()
                        & !IFMAPMessagingFacade.getInstance().pollingClientList.getClientList().get(i).isForceBlocked()) {
                    // execute iptables-allow-rule
                    RulesExecutor ipTablesRulesExecutor = RulesExecutor.getInstance();
                    ipTablesRulesExecutor.executePredefinedRule(Rules.PREDEFINED_RULE_INSERT_INPUT_APPEND_ALLOW__IP,
                            new String[] { IFMAPMessagingFacade.getInstance().pollingClientList.getClientList().get(i).getIPAddress() });
                    // set entry-state to allowed
                    IFMAPMessagingFacade.getInstance().pollingClientList.getClientList().get(i).setAllowed(true);
                    IfMapClient.LOGGER.info("Client with IP "
                            + IFMAPMessagingFacade.getInstance().pollingClientList.getClientList().get(i).getIPAddress()
                            + " has been alowed and unblocked!");
                }
            }
        }
    }

    /**
     * show content of rules-lists
     */
    public void ouputRulesLists() {
        // output enforcement rules
        IfMapClient.LOGGER
                .config("-------------------------------------------------------------------------------------------------------");
        IfMapClient.LOGGER.config("completed initialization of ip-rules-lists ... current initialized rules are:");
        IfMapClient.LOGGER.config("");
        IfMapClient.LOGGER.config("ENFORCMENT_MAPPING_CONTAINS: " + mEnforcementMappingContains.toString());
        IfMapClient.LOGGER.config("ENFORCMENT_MAPPING_MATCHES: " + mEnforcementMappingMatches.toString());
        IfMapClient.LOGGER.config("ENFORCMENT_MAPPING_EQUALS: " + mEnforcementMappingEquals.toString());
        IfMapClient.LOGGER.config("ENFORCMENT_MAPPING_GREATER: " + mEnforcementMappingGreater.toString());
        IfMapClient.LOGGER.config("ENFORCMENT_MAPPING_LOWER: " + mEnforcementMappingLower.toString());
        IfMapClient.LOGGER.config("");
        IfMapClient.LOGGER.config("ALLOW_MAPPING_CONTAINS: " + mAllowMappingContains.toString());
        IfMapClient.LOGGER.config("ALLOW_MAPPING_MATCHES: " + mAllowMappingMatches.toString());
        IfMapClient.LOGGER
                .config("-------------------------------------------------------------------------------------------------------");
    }
}
