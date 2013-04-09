/*
 * SocketPollingThread.java 0.2 13/02/07
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

package de.esukom.decoit.ifmapclient.pollingthreads.socket;

import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.pollingthreads.PollingThread;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * abstract base class for polling messages over a socket
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public abstract class SocketPollingThread extends PollingThread {

    // port
    protected int mPort;

    // socket
    protected ServerSocket mProviderSocket;

    // socket specific fields
    protected Socket mConnection = null;
    protected BufferedReader mBufferReader = null;
    protected DataInputStream mIn = null;

    @Override
    protected void initProperties(final Properties props) {
        // initialize socket
        mPort = new Integer(props.getProperty("server.port", "0")).intValue();
        IfMapClient.LOGGER.config("initalizing socket on port: " + mPort);
        try {
            mProviderSocket = new ServerSocket(mPort);
        } catch (IOException e) {
            IfMapClient.exit("I/O error while creating server socket");
        }
    }

    @Override
    public void run() {
        while (running) {
            if (!pausing) {
                try {
                    // wait for connection
                    mConnection = mProviderSocket.accept();

                    // I/O
                    mIn = new DataInputStream(mConnection.getInputStream());
                    mBufferReader = new BufferedReader(new InputStreamReader(mIn));

                    // read incoming line from server
                    String inputLine = "";
                    try {
                        inputLine = mBufferReader.readLine();
                        if (!Toolbox.isNullOrEmpty(inputLine)) {
                            IfMapClient.LOGGER.info("socket polling-thread received message: " + inputLine + " from "
                                    + mConnection.getInetAddress().getHostName());
                            notify(parseLine(inputLine));
                        }
                        else {
                            break;
                        }
                    } catch (IOException e) {
                        IfMapClient.exit("I/O error while reading from socket");
                    }
                } catch (IOException ioException) {
                    IfMapClient.exit("error while building in/output-streams");
                } finally {
                    try {
                        mIn.close();
                    } catch (IOException ioException) {
                        IfMapClient.exit("error while closing streams and socket");
                    }
                }
            }
        }
    }

    @Override
    public void notify(final Object o) {
        if (o != null) {
            ArrayList<HashMap<String, String>> resultList = new ArrayList<HashMap<String, String>>();
            resultList.add((HashMap<String, String>) o);
            // in case of new entries, notify observer and pass the new entries
            if (resultList.size() > 0) {
                // notify the observer and pass new entries
                setChanged();
                notifyObservers(resultList);
            }
            else {
                IfMapClient.LOGGER.info("no updates received over socket, not calling observer");
            }
        }
        else {
            IfMapClient.LOGGER.warning("retrieved message from socket is empty, not calling observer");
        }
    }

    /**
     * parse a single line
     * 
     * @param line the line that needs to be parsed
     * 
     * @return 2d-list containing the different nagios macros
     */
    protected abstract HashMap<String, String> parseLine(final String line);
}