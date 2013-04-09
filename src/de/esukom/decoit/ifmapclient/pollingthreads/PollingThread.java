/*
 * PollingThread.java 0.2 13/02/13
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

package de.esukom.decoit.ifmapclient.pollingthreads;

import java.util.Observable;
import java.util.Properties;

/**
 * Abstract Base-Class for PollingThread-Classes
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public abstract class PollingThread extends Observable implements Runnable {

    // thread sleeping time between two polls
    public int sleepTime = 0;

    // flag indicating if polling thread is currently running
    public boolean running = false;

    // flag indicating if polling thread is currently pausing
    public boolean pausing = false;

    /**
     * abstract method for initializing properties before the thread is executed
     * 
     * @param props properties-object containing values for initialization
     */
    protected abstract void initProperties(final Properties props);

    /**
     * abstract method for notifying observers about occurred updates
     * 
     * @param Object read in data as object
     */
    protected abstract void notify(final Object o);
}
