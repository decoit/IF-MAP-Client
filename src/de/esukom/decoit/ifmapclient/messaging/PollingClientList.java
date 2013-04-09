/* 
 * PollingClientList.java        0.1.4 12/02/16
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

package de.esukom.decoit.ifmapclient.messaging;

import de.esukom.decoit.ifmapclient.iptables.Rules;
import de.esukom.decoit.ifmapclient.iptables.RulesExecutor;
import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.mappingfactory.result.EventMappingResult;
import de.esukom.decoit.ifmapclient.mappingfactory.result.MappingResult;

import java.util.ArrayList;

/**
 * class containing a list of all current polling-clients and some methods that can be performed on that list (and its entries)
 * 
 * @version 0.1.4
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class PollingClientList {

    // list holding all poll-clients
    private ArrayList<PollingClientListEntry> mClientList = null;

    /**
     * constructor
     */
    public PollingClientList() {
        mClientList = new ArrayList<PollingClientListEntry>();
    }

    /**
     * get the complete client-list
     * 
     * @return the clientList
     */
    public ArrayList<PollingClientListEntry> getClientList() {
        return mClientList;
    }

    /**
     * check passed in result-list for new clients to be subscribed and add them to client list
     * 
     * @param resultList
     * 
     * @return true, if at least one new client has been added to client-list
     */
    public boolean addNewClientsToClientList(MappingResult[] resultList) {
        boolean entryInserted = false;
        for (int i = 0; i < resultList.length; i++) {
            EventMappingResult currentEventResult = (EventMappingResult) resultList[i];
            String currentIp = currentEventResult.getIp();

            // create new client-list-entry
            PollingClientListEntry newEntry = new PollingClientListEntry(currentIp);

            // check if current IP is already in client list if not, insert it
            if (insertEntryIfIPNotExists(newEntry)) {
                IfMapClient.LOGGER.info("new client has been added to client list: " + currentIp);
                entryInserted = true;
            } else {
                IfMapClient.LOGGER.info("not added client " + currentIp + " to client list because it already exists! ");
            }
        }

        return entryInserted;
    }

    /**
     * allow client with the passed in ip-address (if its not in "force-block"-state)
     * 
     * @param ip
     *            ip-address of client to be allowed
     */
    public void allowClient(String ip) {
        for (int i = 0; i < getClientList().size(); i++) {
            // exclude rule for Map-Server cause its already executed
            // at initialization
            if (!getClientList().get(i).getIPAddress().equals(IFMAPMessagingFacade.getInstance().mapServerIP)) {

                if (getClientList().get(i).isSubscribed() & getClientList().get(i).getIPAddress().equals(ip)
                        & !getClientList().get(i).isAllowed() & !getClientList().get(i).isForceBlocked()) {

                    // execute iptables-allow-rule
                    RulesExecutor ipTablesRulesExecutor = RulesExecutor.getInstance();
                    ipTablesRulesExecutor.executePredefinedRule(Rules.PREDEFINED_RULE_INSERT_INPUT_APPEND_ALLOW__IP,
                            new String[] { getClientList().get(i).getIPAddress() });
                    // set entry-state to allowed
                    getClientList().get(i).setAllowed(true);
                    IfMapClient.LOGGER.info("Client with IP " + getClientList().get(i).getIPAddress() + " has been alowed and unblocked!");
                }
            }
        }
    }

    /**
     * insert new entry into client-list, if there is no other entry in client-list with the same ip
     * 
     * @param entryToInsert
     *            entry to be inserted
     * 
     * @return true, if entry was inserted
     */
    public boolean insertEntryIfIPNotExists(PollingClientListEntry entryToInsert) {
        boolean alreadyExists = false;
        if (mClientList != null) {
            for (int i = 0; i < mClientList.size(); i++) {
                PollingClientListEntry currentEntry = (PollingClientListEntry) mClientList.get(i);
                if (currentEntry.getIPAddress().equals(entryToInsert.getIPAddress())) {
                    alreadyExists = true;
                }
            }
        }

        if (!alreadyExists) {
            mClientList.add(entryToInsert);
            return true;
        }

        return false;
    }

    /**
     * get single entry from client-list containing the passed in ip-address
     * 
     * @param ip
     *            ip of entry that should be returned
     * 
     * @return entry containing passed in ip-address
     */
    public PollingClientListEntry getEntryByIpString(String ip) {
        if (mClientList != null) {
            for (int i = 0; i < mClientList.size(); i++) {
                if (mClientList.get(i).getIPAddress().equals(ip)) {
                    return mClientList.get(i);
                }
            }
        }
        return null;
    }

    /**
     * get all entries from client-list that are subscribed
     * 
     * @return list of subscribed entries
     */
    public ArrayList<PollingClientListEntry> getAllSubscribedEntrys() {
        ArrayList<PollingClientListEntry> newList = new ArrayList<PollingClientListEntry>();
        if (mClientList != null) {
            for (int i = 0; i < mClientList.size(); i++) {
                if (mClientList.get(i).isSubscribed()) {
                    newList.add(mClientList.get(i));
                }
            }
        }

        if (newList.size() > 0) {
            return newList;
        }

        return null;
    }

    /**
     * get all entries from client-list that are currently blocked
     * 
     * @return list of entries that are blocked
     */
    private ArrayList<PollingClientListEntry> getAllBlockedEntrys() {
        ArrayList<PollingClientListEntry> newList = new ArrayList<PollingClientListEntry>();
        if (mClientList != null) {
            for (int i = 0; i < mClientList.size(); i++) {
                if (mClientList.get(i).isForceBlocked()) {
                    newList.add(mClientList.get(i));
                }
            }
        }

        if (newList.size() > 0) {
            return newList;
        }

        return null;
    }

    /**
     * check if a client with the passed in ip exists inside client-list and determine if it its currently in "blocked"-state
     * 
     * @param ipAddress
     *            ip-address of client to be checked
     * 
     * @return true, if client exists in client-list an is set to blocked-state
     */
    public boolean isClientBlocked(String ipAddress) {
        ArrayList<PollingClientListEntry> blockedClients = getAllBlockedEntrys();
        if (blockedClients != null) {
            for (int i = 0; i < blockedClients.size(); i++) {
                if (blockedClients.get(i).getIPAddress().equals(ipAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * check if a client with the passed in ip exists inside client-list and determine if it its currently in "blocked"- or "allowed"-state
     * 
     * @param ipAddress
     *            ip-address of client to be checked
     * 
     * @return true, if client exists in client-list an is set to blocked- or allowed-state
     */
    public boolean isClientBlockedOrAllowed(String ipAddress) {
        if (mClientList != null) {
            for (int i = 0; i < mClientList.size(); i++) {
                if (mClientList.get(i).getIPAddress().equals(ipAddress) && (mClientList.get(i).isAllowed() || mClientList.get(i).isForceBlocked())){
                    return true;
                }
            }
        }

        return false;
    }
}