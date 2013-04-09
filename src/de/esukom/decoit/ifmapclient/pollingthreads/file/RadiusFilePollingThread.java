   /*            
    * RadiusFilePollingThread.java        0.1.4 12/02/16
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

package de.esukom.decoit.ifmapclient.pollingthreads.file;

import de.esukom.decoit.ifmapclient.logging.Logging;
import de.esukom.decoit.ifmapclient.main.IfMapClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;




/**
 * Thread for polling the radiusd-File for new radius-Events
 * 
 * @version 0.1.4
 * @author Marcel Jahnke, DECOIT GmbH
 */
public class RadiusFilePollingThread extends FilePollingThread {
    private static boolean sIsFirstStart = true;
    private static boolean sIsFirstStartDetail = true;

    private int mLastEntryLineNumber = 0;
    private int mLastEntryLineNumberDetail = 0;
    private Logger mLogger;
    private String mRadiusLogPath;
    private String mRadiusLogDetailPath;

    private File mFile;
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMdd");

    // indicates which entry was readed recently
    private long mLastReadedEntry;

    // time stamp of last file modification, is used to determine changes
    // of the file
    private long mLastModified;
    private long mLastModifiedDetail;
    // save the readed line numbers of detailed files
    private HashMap<String, String> mLastEntryHashMap = new HashMap<String, String>();

    public RadiusFilePollingThread(Properties pr) {
        mLogger = Logging.getTheLogger();

        initProperties(pr);

        mLogger.info("opening radiusd-log file at path: " + mRadiusLogPath);

        if (new File(mRadiusLogPath).exists()) {
            mFile = new File(mRadiusLogPath);
            mLastModified = mFile.lastModified();
        } else {
            mLogger.warning("radiusd-log-file at: " + mRadiusLogPath + " doesnt exist!");
        }
        if (new File(mRadiusLogDetailPath).exists()) {
            mLastModifiedDetail = getLatestModifiedDate();
        }
    }

    @Override
    protected void initProperties(Properties props) {
        Date date = new Date();
        sIsFirstStart = true;
        mRadiusLogPath = props.getProperty("radius.log.path", null);
        if (mRadiusLogPath == null || mRadiusLogPath.length() == 0) {
            IfMapClient
                    .exit("error while initializing RADIUSFilePolling Thread: property RADIUS_LOG_PATH cannot be null or empty!");
        }
        mRadiusLogPath += mDateFormat.format(date) + ".log";

        mRadiusLogDetailPath = props.getProperty("radius.log.accpath", null);
        if (mRadiusLogDetailPath == null || mRadiusLogDetailPath.length() == 0) {
            IfMapClient
                    .exit("error while initializing RADIUSFilePolling Thread: property RADIUS_LOG_PATH cannot be null or empty!");
        }
    }

    /**
     * read and pars the logfile
     * 
     * @return the result
     */
    protected ArrayList<HashMap<String, String>> readFile() {
        // number of read lines in current cycle, used to determine
        // current position inside file (in terms of already read lines )
        int currentCycleLineNumber = 0;
        int newEntryNumber = 1;

        ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> tempEventData = new HashMap<String, String>();

        BufferedReader logReader = null;
        String line = null;

        try {
            logReader = new BufferedReader(new FileReader(mFile), 1);

            while ((line = logReader.readLine()) != null) {
                if (sIsFirstStart) {
                    mLastEntryLineNumber++;
                }
                currentCycleLineNumber++;
                if (sIsFirstStart) {
                    System.out.println(currentCycleLineNumber + " " + line);
                    tempEventData.put(String.valueOf(newEntryNumber), line);
                    newEntryNumber++;
                } else if (currentCycleLineNumber > mLastEntryLineNumber) {
                    System.out.println(currentCycleLineNumber + " " + line);
                    tempEventData.put(String.valueOf(newEntryNumber), line);
                    newEntryNumber++;
                }
                if (sIsFirstStart) {
                    // first cycle finished
                    sIsFirstStart = false;
                }
            }
            mLastEntryLineNumber = currentCycleLineNumber;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        // reads the detail log files of radius
        ArrayList<String> newestFiles = getLatestModified();
        for (int i = 0; i < newestFiles.size(); i++) {
            if (!mLastEntryHashMap.containsKey(newestFiles.get(i))) {
                mLastEntryHashMap.put(newestFiles.get(i), "0");
            }
        }
        int currentCycleLineNumberDetail = 0;
        // System.out.println(newestFiles.size());
        for (int i = 0; i < newestFiles.size(); i++) {
            currentCycleLineNumberDetail = 0;
            if (mLastEntryHashMap.get(newestFiles.get(i)).equalsIgnoreCase("0")) {
                mLastEntryLineNumberDetail = 0;
            } else {
                mLastEntryLineNumberDetail = Integer.valueOf(mLastEntryHashMap.get(newestFiles.get(i)));
            }
            try {
                logReader = new BufferedReader(new FileReader(newestFiles.get(i)), 1);

                String[] resultDetail = parseDetialInSingleLine(logReader);
                for (int j = 0; j < resultDetail.length; j++) {
                    if (sIsFirstStartDetail) {
                        mLastEntryLineNumberDetail++;
                    }
                    currentCycleLineNumberDetail++;
                    if (sIsFirstStartDetail) {
                        tempEventData.put(String.valueOf(newEntryNumber), resultDetail[j]);
                        System.out.println(currentCycleLineNumberDetail + " " + resultDetail[j]);
                        newEntryNumber++;

                    } else if (currentCycleLineNumberDetail > mLastEntryLineNumberDetail) {
                        System.out.println(currentCycleLineNumberDetail + " " + resultDetail[j]);
                        tempEventData.put(String.valueOf(newEntryNumber), resultDetail[j]);
                        newEntryNumber++;
                    }
                    if (sIsFirstStartDetail) {
                        // first cycle finished
                        sIsFirstStartDetail = false;
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            mLastEntryHashMap.remove(newestFiles.get(i));
            mLastEntryHashMap.put(newestFiles.get(i), String.valueOf(currentCycleLineNumberDetail));
            mLastModifiedDetail = getLatestModifiedDate();
        }
        result.add(tempEventData);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void notify(Object o) {
        // hold Thread
        if (o != null) {
            ArrayList<HashMap<String, String>> resultList = (ArrayList<HashMap<String, String>>) o;
            if (resultList.size() > 0) {
                pausing = true;
                setChanged();
                notifyObservers(resultList);
            } else {
                mLogger.info("no new entries in radius-logfile, not calling observer");
            }
        } else {
            mLogger.warning("retrieved radius-logfile result is null, not calling observer");
        }

    }

    private boolean notLogged = true;

    @Override
    public void run() {
        long actualLastModified;
        while (running) {
            if (!pausing & mFile != null) {
                try {
                    actualLastModified = mFile.lastModified();

                    // check if file changes have occurred since last cycle
                    mLogger.info("checking radius-logfile for updates");
                    if (sIsFirstStart | mLastModified != actualLastModified) {
                        mLastModified = actualLastModified;
                        mLogger.info("reading new entries found inside radius-logfile");
                        notify(readFile());
                        notLogged = false;
                    } else {
                        mLogger.info("no updates in radius-logfile");
                        notLogged = true;
                    }
                    if (mLastModifiedDetail != getLatestModifiedDate() && notLogged) {
                        mLogger.info("reading new entries found inside radius-logfile");
                        notify(readFile());
                    } else {
                        mLogger.info("no updates in radius-detail-logfile");
                    }
                    Thread.sleep(sleepTime);
                } catch (Exception e) {
                    e.printStackTrace();
                    IfMapClient.exit("error while polling log file!");
                }
            }
        }
    }

    public String[] parseDetialInSingleLine(BufferedReader msg) throws IOException {
        String line = null;
        String temp = "";

        ArrayList<String> tempArray = new ArrayList<String>();
        int bla = 1;
        boolean userFound = false;
        while ((line = msg.readLine()) != null) {
            if (!line.equalsIgnoreCase("")) {
                if (line.contains("User-Name") & (!userFound)) {
                    temp += line + ",";
                    userFound = true;
                } else {
                    temp += line;
                }
            } else if (temp != null) {
                tempArray.add(temp);
                temp = "";
                bla++;
                userFound = false;
            }
        }
        String result[] = new String[tempArray.size()];
        for (int i = 0; i < tempArray.size(); i++) {
            result[i] = tempArray.get(i);
        }

        return result;
    }

    private ArrayList<String> getLatestModified() {
        ArrayList<String> ipFolderList = getRaddactContentFolders();
        ArrayList<String> newestFilesPath = new ArrayList<String>();
        for (int i = 0; i < ipFolderList.size(); i++) {
            System.out.println("Search in " + ipFolderList.get(i));
            File ipFolder = new File(ipFolderList.get(i));
            File[] content = ipFolder.listFiles();
            ArrayList<Long> dates = new ArrayList<Long>();
            for (int j = 0; j < content.length; j++) {
                if (content[j].getName().startsWith("detail-")) {
                    dates.add(content[j].lastModified());
                }
            }
            java.util.Collections.sort(dates);
            for (int j = 0; j < content.length; j++) {
                if (content[j].getName().startsWith("detail-")) {
                    if (content[j].lastModified() == dates.get(dates.size() - 1)) {
                        newestFilesPath.add(content[j].getAbsolutePath());
                    }
                }
            }
        }
        return newestFilesPath;
    }

    private long getLatestModifiedDate() {
        ArrayList<String> ipFolderList = getRaddactContentFolders();
        long newestFileDate = 0;
        ArrayList<Long> dates = new ArrayList<Long>();
        for (int i = 0; i < ipFolderList.size(); i++) {
            File ipFolder = new File(ipFolderList.get(i));
            File[] content = ipFolder.listFiles();
            for (int j = 0; j < content.length; j++) {
                if (content[j].getName().startsWith("detail-")) {
                    dates.add(content[j].lastModified());
                }
            }
        }
        java.util.Collections.sort(dates);
        newestFileDate = dates.get(dates.size() - 1);
        return newestFileDate;
    }

    private ArrayList<String> getRaddactContentFolders() {
        ArrayList<String> ipFolderList = new ArrayList<String>();
        File radacctFolder = new File(mRadiusLogDetailPath);

        if (radacctFolder.exists()) {
            File[] ipFolders = radacctFolder.listFiles();
            for (int i = 0; i < ipFolders.length; i++) {
                ipFolderList.add(ipFolders[i].getAbsolutePath());
            }
        }
        return ipFolderList;
    }

}
