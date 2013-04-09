/*
 * Toolbox.java 0.2 13/02/07
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

package de.esukom.decoit.ifmapclient.util;

import de.esukom.decoit.ifmapclient.logging.Logging;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * class containing some helper-functions that do not fit anywhere else
 * 
 * @version 0.2
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class Toolbox {

    // the logger
    public static Logger sLogger = Logging.getTheLogger();

    // application start time
    public static final String sClientStartTime = getNowDateAsString("yyyy-MM-dd HH:mm:ss");

    // ********************************************************
    // * REGULAR EXPRESSIONS
    // ********************************************************

    // HashMap containing all regular expressions from config
    public static HashMap<String, Pattern> regExMap = new HashMap<String, Pattern>();

    /**
     * build regular expressions hashmap from regex.properties file
     * 
     * @param pr regex-properties
     */
    public static void loadAndPrepareRegExFromFile(final Properties pr) {
        Enumeration<Object> em = pr.keys();
        while (em.hasMoreElements()) {
            String str = (String) em.nextElement();
            if (!isNullOrEmpty(str) && !isNullOrEmpty(pr.get(str).toString()) && str.startsWith("regex")) {
                regExMap.put(str, Pattern.compile(pr.get(str).toString()));
            }
        }
    }

    /**
     * get regular expressions pattern from regex.hashmap
     * 
     * @param key key for the value to return
     * 
     * @return Pattern for passed in key
     */
    public static Pattern getRegExPattern(String key) {
        if (regExMap == null)
            return null;
        return regExMap.get(key);
    }

    // ********************************************************
    // * CONFIG-RELATED
    // ********************************************************

    /**
     * get string property value from Properties-Object
     * 
     * @param property property-name
     * @param conf properties-object
     * @param checkIfFileExists check the resulting string exists as a file. This is meant to be a
     *            helper function in case the passed in property is a file(path)
     * 
     * @return property as string
     */
    public static String getStringProperty(final String property, final Properties conf, final boolean checkIfFileExists) {
        String propertyValue = conf.getProperty(property, null);
        if (isNullOrEmpty(propertyValue)) {
            sLogger.warning("required property <" + property + "> is empty");
            return null;
        }
        else {
            if (checkIfFileExists) {
                if ((new File(propertyValue)).exists()) {
                    sLogger.info("value for property <" + property + "> is: " + propertyValue);
                    return propertyValue;
                }
                else {
                    sLogger.warning("file for property  <" + property + "> at path " + propertyValue + " doesnt exists");
                    return null;
                }
            }
            else {
                sLogger.info("value for property <" + property + "> is: " + propertyValue);
                return propertyValue;
            }
        }
    }

    /**
     * get string property value from Properties-Object, use passed in default value if no property
     * is found
     * 
     * @param property property-name
     * @param conf properties-object
     * @param default default-value that should be returned if property does not exists
     * 
     * @return property as string
     */
    public static String getStringPropertyWithDefault(final String property, final Properties conf,
            final String defaultValue) {
        String propertyValue = conf.getProperty(property, null);
        if (isNullOrEmpty(propertyValue)) {
            sLogger.warning("required property <" + property + "> not found...using default (" + defaultValue + ")");
            return defaultValue;
        }
        else {
            sLogger.info("value for property <" + property + "> is: " + propertyValue);
            return propertyValue;
        }
    }

    /**
     * get int property value from Properties-Object
     * 
     * @param property property-name
     * @param defaultValue default value in case without zero flag is set
     * @param conf properties-object
     * @param withoutZero in case the property is 0, return the passed in default value
     * 
     * @return property as int
     */
    public static int getIntPropertyWithDefault(final String property, final int defaultValue, final Properties conf,
            final boolean withoutZero) {
        int returnValue;
        String propertyValue = conf.getProperty(property, null);
        if (isNullOrEmpty(propertyValue)) {
            sLogger.warning("<" + property + "> is empty or null...using default value (" + defaultValue + ")");
            return defaultValue;
        }
        if (withoutZero) {
            if (propertyValue.equals("0")) {
                sLogger.warning("<" + property + "> cannot be zero...using default value (" + defaultValue + ")");
                return defaultValue;
            }
        }
        try {
            returnValue = Integer.parseInt(propertyValue);
        } catch (NumberFormatException e) {
            sLogger.warning("<" + property + "> must be an integer...using default value (" + defaultValue + ")");
            return defaultValue;
        }

        sLogger.info("value for <" + property + "> is: " + returnValue);

        return returnValue;
    }

    /**
     * get boolean property value from Properties-Object
     * 
     * @param property property-name
     * @param defaultValue default value in case the property is null/0
     * @param conf properties-object
     * 
     * @return property as boolean
     */
    public static boolean getBoolPropertyWithDefault(final String property, final boolean defaultValue,
            final Properties conf) {
        boolean returnValue;
        String propertyValue = conf.getProperty(property, null);
        if (isNullOrEmpty(propertyValue)) {
            sLogger.warning("<" + property + "> is empty or null...using default value (" + defaultValue + ")");
            return defaultValue;
        }

        returnValue = Boolean.parseBoolean(propertyValue);
        sLogger.info("value for property <" + property + "> is: " + returnValue);

        return returnValue;
    }

    /**
     * check if array really contains values, if not set it to null. this is necessary due to
     * problems with reading the properties from config-files
     * 
     * @param propArray array to check
     * 
     * @return boolean indicating whether array is null
     */
    public static boolean checkConfig(final String[] propArray) {
        if (propArray != null && propArray.length > 0) {
            if (propArray[0].length() == 0) {
                return false;
            }
        }
        return true;
    }

    // ********************************************************
    // * DATE-RELATED
    // ********************************************************

    /**
     * get current date-time as string. format of the returned string is determined by the passed in
     * format-string
     * 
     * @param dateFormat format-string
     * 
     * @return current date as string
     */
    public static String getNowDateAsString(final String dateFormat) {
        Calendar currentDate = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        return formatter.format(currentDate.getTime());
    }

    /**
     * get current date-time as date-object. format is determined by the passed in format-string
     * 
     * @param dateFormat
     * 
     * @return date-object containing current date-time
     */
    public static Date getNowDate(final String dateFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        Date date = null;
        try {
            date = sdf.parse(getNowDateAsString(dateFormat));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * get calendar-object from passed in string using the passed in date format string
     * 
     * @param dateString date of calendar
     * @param dateFormat format of the passed in date
     * 
     * @return calendar-object set to passed in date
     */
    public static Calendar getCalendarFromString(final String dateString, final String dateFormat, final Locale loc) {
        DateFormat formatter;
        Date date = null;
        if (loc == null){
            formatter = new SimpleDateFormat(dateFormat);
        }
        else{
            formatter = new SimpleDateFormat(dateFormat,loc);
        }       
        try {
            date = (Date) formatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        return cal;
    }

    /**
     * convert the passed in timestamp-string to string in IFMAP-compatible format
     * 
     * @param dateString timestamp-string to be converted
     * @param dateSplitter splitter separating date values (e.g YYYY/MM/DD)
     * @param dateTimeSplitter splitter separating the date from the time-values
     * 
     * @return timestamp-string as specified by IFMAP
     */
    public static String convertTimestampToIfMapFormat(final String dateString, final String dateSplitter,
            final String dateTimeSplitter) {
        String[] timestamp = dateString.split(dateTimeSplitter);

        // YYYY/MM/DD => [0]Year [1] Month [2] Day
        String[] date = timestamp[0].split(dateSplitter);

        // build new timestamp-string
        return date[0] + "-" + date[1] + "-" + date[2] + "T" + timestamp[1] + "Z";
    }

    /**
     * get HashMap containing mapping of alphanumeric month values (e.g. Feb) to integer values
     * 
     * @return HashMap
     */
    public static HashMap<String, String> MonthValues = new HashMap<String, String>();
    public static HashMap<String, String> getAplhaNumericMonthMap() {
        if (MonthValues.isEmpty()) {
            MonthValues.put("Jan", "01");
            MonthValues.put("Feb", "02");
            MonthValues.put("Mar", "03");
            MonthValues.put("Apr", "04");
            MonthValues.put("May", "05");
            MonthValues.put("Jun", "06");
            MonthValues.put("Jul", "07");
            MonthValues.put("Aug", "08");
            MonthValues.put("Sep", "09");
            MonthValues.put("Oct", "10");
            MonthValues.put("Nov", "11");
            MonthValues.put("Dec", "12");
        }
        return MonthValues;
    }

    // ********************************************************
    // * MISC
    // ********************************************************

    /**
     * check if passed in string is null or empty
     * 
     * @param test string to be tested
     * 
     * @return true if string is null or empty
     */
    public static boolean isNullOrEmpty(final String test) {
        if (test == null)
            return true;
        if (test.trim().isEmpty())
            return true;
        return false;
    }

    /**
     * convert passed in ip6-address-string to IFMAP-compatible ip6-address-string. this means
     * deleting all leading zeros from every address-part
     * 
     * @param String adr address-string to convert
     * 
     * @return IFMAP-compatible ip6-address-string
     */
    public static String convertIP6AddressToIFMAPIP6AddressPattern(final String adr) {
        String[] singleEntrys = adr.split(":");
        String convertedAddress = new String();

        for (int i = 0; i < singleEntrys.length; i++) {

            // delete "leading" zeros
            singleEntrys[i] = singleEntrys[i].replaceFirst("0*", "");

            // is string is empty after deleting leading zeros, add a zero-char
            if (singleEntrys[i].length() < 1) {
                singleEntrys[i] = "0";
            }

            // add ":" to address, leave out last entry
            if (i != singleEntrys.length - 1) {
                singleEntrys[i] += ":";
            }

            // add entry to converted-address-string
            convertedAddress += singleEntrys[i];
        }

        return convertedAddress;
    }
}