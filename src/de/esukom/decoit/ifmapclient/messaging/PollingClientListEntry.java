/* 
 * PollingClientListEntry.java        0.1.4 12/02/16
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

import de.esukom.decoit.ifmapclient.main.IfMapClient;

/**
 * class representing a single polling-client-list entry
 * 
 * @version 0.1.4
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class PollingClientListEntry {

    private String mIPAddress;
    private int mSubscriptionId;
    private boolean mIsAllowed;
    private boolean mIsSubscribed;

    // if this flag is set, this client will stay blocked forever
    // ...until i expand the concept for clients in client-list...
    private boolean mForceBlocked;

    /**
     * constructor
     * 
     * @param ip
     *            ip-address
     */
    public PollingClientListEntry(String ip) {
        mIPAddress = ip;
        mIsAllowed = false;
        mIsSubscribed = false;
    }

    /**
     * @return the iPAddress
     */
    public String getIPAddress() {
        return mIPAddress;
    }

    /**
     * @param iPAddress
     *            the iPAddress to set
     */
    public void setIPAddress(String iPAddress) {
        mIPAddress = iPAddress;
    }

    /**
     * @return the isAllowed
     */
    public boolean isAllowed() {
        return mIsAllowed;
    }

    /**
     * @param isAllowed
     *            the isAllowed to set
     */
    public void setAllowed(boolean isAllowed) {
        this.mIsAllowed = isAllowed;
    }

    /**
     * @return the isSubscribed
     */
    public boolean isSubscribed() {
        return mIsSubscribed;
    }

    /**
     * @param isSubscribed
     *            the isSubscribed to set
     */
    public void setSubscribed(boolean isSubscribed) {
        this.mIsSubscribed = isSubscribed;
    }

    /**
     * @return the subscriptionName
     */
    public int getSubscriptionName() {
        return mSubscriptionId;
    }

    /**
     * @param subscriptionName
     *            the subscriptionName to set
     */
    public void setSubscriptionName(int subscriptionId) {
        this.mSubscriptionId = subscriptionId;
    }

    /**
     * @return the forceBlocked
     */
    public boolean isForceBlocked() {
        return mForceBlocked;
    }

    /**
     * @param forceBlocked
     *            the forceBlocked to set
     */
    public void setForceBlocked(boolean forceBlocked) {
        this.mForceBlocked = forceBlocked;
    }
}