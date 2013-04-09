/* 
 * IFMAPMessagePollingThread.java        0.1.4 12/02/16
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
import de.esukom.decoit.ifmapclient.pollingthreads.PollingThread;

import de.fhhannover.inform.trust.ifmapj.channel.ARC;
import de.fhhannover.inform.trust.ifmapj.messages.PollResult;

import java.util.Properties;

/**
 * Thread for handling polling-requests/responses to/from MAP-Server
 * 
 * @version 0.1.4
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class IFMAPMessagePollingThread extends PollingThread {

    // arc channel for communication
    public ARC aRCChannel = null;

    /**
     * constructor
     * 
     * @param arcChannel
     *            channel to use for communication
     * @param pr
     *            Properties for Polling-Thread
     */
    public IFMAPMessagePollingThread(ARC arcChannel) {
        if (arcChannel != null) {
            IfMapClient.LOGGER.info("initalizing arc-channel for IFMAPMessagePollingThread");
            aRCChannel = arcChannel;
        } else {
            IfMapClient
                    .exit("error while contructing IFMAPMessagePollingThread: passed in arc-channel cannot be null!");
        }

    }

    @Override
    protected void initProperties(Properties props) {
        // nothing here a.t.m.
    }

    @Override
    public void run() {
        while (running) {
            if (!pausing) {
                try {
                    IfMapClient.LOGGER.info("running arc-polling-thread...");

                    // send poll request
                    PollResult pr = aRCChannel.poll();

                    // if there is a poll-result, hold thread and check it
                    if (pr != null) {
                        // pausing = true;
                        IfMapClient.LOGGER.info("incoming poll-result detected...processing");
                        notify(pr);
                    } else {
                        IfMapClient.LOGGER
                                .info("incoming poll-result is null..not doing anything!");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    IfMapClient.exit("error while running SOAPMessagePoller-Thread");
                }
                // Thread.yield();
            }
        }
    }

    @Override
    public void notify(Object o) {
        if (o != null) {
            PollResult result = (PollResult) o;
            if (result != null) {
                // in case of new results, notify observer and pass new entries
                setChanged();
                IfMapClient.LOGGER
                        .info("notifying observer about new result from IFMAPMessaging-Polling-Thread");
                notifyObservers(result);

            }
        } else {
            IfMapClient.LOGGER
                    .warning("retrieved IFMAP-Polling-Thread result is null, not calling observer");
        }
    }
}