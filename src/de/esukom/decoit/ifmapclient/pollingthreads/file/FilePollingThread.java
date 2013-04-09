/*
 * FilePollingThread.java 0.2 13/02/07
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

package de.esukom.decoit.ifmapclient.pollingthreads.file;

import de.esukom.decoit.ifmapclient.main.IfMapClient;
import de.esukom.decoit.ifmapclient.pollingthreads.PollingThread;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * abstract base class for reading and polling files
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public abstract class FilePollingThread extends PollingThread {

    // perform file-exists-check before running the polling thread
    protected boolean performPreLogFileExistsCheck = false;
    protected boolean logFound = false;

    // properties from config-file
    protected String filePath;

    // log-file to watch/poll
    protected File file;

    // flag for indicating that reading of file is necessary, even if there was no update
    protected boolean isFirstStart = false;

    // position of last read-in entry in last cycle
    protected int lastEntryLineNumber = 0;

    // time stamp of last file modification
    protected long lastModified;

    // log-rotate
    protected boolean useLogRotate;
    protected String logRotatePattern;

    public boolean isLogRotateActive() {
        return this.useLogRotate;
    }

    @Override
    protected void initProperties(final Properties props) {
        // flag indicating first start
        isFirstStart = true;

        // get file-path from configuration
        filePath = props.getProperty("filepath", null);
        if (Toolbox.isNullOrEmpty(filePath)) {
            IfMapClient.exit("file path null or empty");
        }

        // if log-rotate is activated, build log-file-name
        useLogRotate = Boolean.valueOf(props.getProperty("logrotate", "false"));
        if (useLogRotate) {
            logRotatePattern = props.getProperty("rotatepattern", "");
            filePath = filePath.replace("$", Toolbox.getNowDateAsString(logRotatePattern));
        }

        // open file if precheck-flag from configuration is set
        performPreLogFileExistsCheck = Boolean.valueOf(props.getProperty("precheck", "true"));
        if (performPreLogFileExistsCheck) {
            IfMapClient.LOGGER.config("opening file at path: [" + filePath + "]");
            if ((new File(filePath)).exists()) {
                file = new File(filePath);
                performPreLogFileExistsCheck = false;
            }
            else {
                IfMapClient.exit("log-file at [" + filePath + "] could not be found");
            }

            lastModified = file.lastModified();
        }
    }

    @Override
    public void run() {
        while (running) {
            if (!pausing) {
                try {
                    if (!performPreLogFileExistsCheck && !logFound) {
                        if ((new File(filePath)).exists()) {
                            file = new File(filePath);
                            lastModified = file.lastModified();
                            logFound = true;
                        }
                        else {
                            Thread.sleep(sleepTime);
                        }
                    }

                    if (logFound) {
                        long actualLastModified = file.lastModified();
                        // check if file changes have occurred since last cycle
                        IfMapClient.LOGGER.info("checking file  [" + filePath + "] for updates");
                        if (isFirstStart | lastModified != actualLastModified) {
                            lastModified = actualLastModified;
                            IfMapClient.LOGGER.info("file has been updated, looking for new entries");
                            notify(readFile());
                        }
                        else {
                            IfMapClient.LOGGER.info("file has not been updated");
                        }
                        Thread.sleep(sleepTime);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    IfMapClient.exit("error while checking file [" + filePath + "] for updates");
                }
            }
        }
    }

    @Override
    public void notify(final Object o) {
        if (o != null) {
            ArrayList<HashMap<String, String>> resultList = (ArrayList<HashMap<String, String>>) o;
            if (resultList.size() > 0) {
                pausing = true;
                setChanged();
                notifyObservers(resultList);
            }
            else {
                IfMapClient.LOGGER.info("no new entries found in file [" + filePath + "]");
            }
        }
        else {
            IfMapClient.LOGGER.info("file [" + filePath + "] is null, not calling observer");
        }
    }

    /**
     * read entries from file and save them in String-HashMap
     * 
     * 
     * @return
     */
    protected abstract ArrayList<HashMap<String, String>> readFile();
}