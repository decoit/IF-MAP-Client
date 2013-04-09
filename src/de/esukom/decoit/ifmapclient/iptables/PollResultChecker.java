/*
 * PollResultChecker.java 0.1.4 12/02/16
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

package de.esukom.decoit.ifmapclient.iptables;

import de.esukom.decoit.ifmapclient.main.IfMapClient;

import com.google.common.collect.Multimap;

/**
 * Object for Cheking IF-MAP-Poll/Search-Results for Enforcement-Strings as defined in
 * /config/iptables/enforcement.properties
 * 
 * @version 0.1.4
 * @author Dennis Dunekacke, DECOIT GmbH
 */
public class PollResultChecker {

    // rules for checking filter-strings types
    public static final byte RULECHECK_TYPE_CONTAINS = 0;
    public static final byte RULECHECK_TYPE_MATCHES = 1;
    public static final byte RULECHECK_TYPE_EQUALS = 2;
    public static final byte RULECHECK_TYPE_GREATER = 3;
    public static final byte RULECHECK_TYPE_LOWER = 4;

    // values for poll-result types to be checked
    public static final byte RESULTCHECK_TYPE_SEARCHRESULT = 5;
    public static final byte RESULTCHECK_TYPE_UPDATERESULT = 6;

    /**
     * constructor
     */
    public PollResultChecker() {
        // empty constructor
    }

    /**
     * check if passed in entry from poll-result-meta-data matches an "allowance" or an enforcement
     * filter-strings from enforcement.properties
     * 
     * @param filterStrings map of rules to check against
     * @param resultPropertyValue the property of the entry to check
     * @param resultFilterValue value to compare to entry-content
     * @param checkType type of check to perform (contains/matches)
     * 
     * @return true, if passed in result-filter-value is contained or matched with entry inside
     *         filter-string--map
     */
    public boolean checkPollResultForIPTablesStringRules(Multimap<String, String> filterStrings,
            String resultPropertyValue, String resultFilterValue, byte checkType, byte resultType) {

        IfMapClient.LOGGER.fine("checking poll result for string rules...");
        IfMapClient.LOGGER.fine("resultPropertyValue: " + resultPropertyValue);
        IfMapClient.LOGGER.fine("resultFilterValue: " + resultFilterValue);
        IfMapClient.LOGGER.fine("filterStrings: " + filterStrings.toString());

        // contains-match
        if (checkType == RULECHECK_TYPE_CONTAINS) {
            if (resultType == RESULTCHECK_TYPE_SEARCHRESULT) {
                if (filterStrings.containsKey("role_" + resultPropertyValue)) {
                    return checkStringAttributeContainsValue(filterStrings, "role_" + resultPropertyValue,
                            resultFilterValue);
                }
                else if (filterStrings.containsKey("!role_" + resultPropertyValue)) {
                    return checkStringAttributeContainsValue(filterStrings, "!role_" + resultPropertyValue,
                            resultFilterValue);
                }
                else if (filterStrings.containsKey("capability_" + resultPropertyValue)) {
                    return checkStringAttributeContainsValue(filterStrings, "capability_" + resultPropertyValue,
                            resultFilterValue);
                }
                else if (filterStrings.containsKey("!capability_" + resultPropertyValue)) {
                    return checkStringAttributeContainsValue(filterStrings, "!capability_" + resultPropertyValue,
                            resultFilterValue);
                }
                else if (filterStrings.containsKey("device-characteristic_" + resultPropertyValue)) {
                    return checkStringAttributeContainsValue(filterStrings, "device-characteristic_"
                            + resultPropertyValue, resultFilterValue);
                }
                else if (filterStrings.containsKey("!device-characteristic_" + resultPropertyValue)) {
                    return checkStringAttributeContainsValue(filterStrings, "!device-characteristic_"
                            + resultPropertyValue, resultFilterValue);
                }
                else if (filterStrings.containsKey("access-request-ip_" + resultPropertyValue)) {
                    return checkStringAttributeContainsValue(filterStrings, "access-request-ip_" + resultPropertyValue,
                            resultFilterValue);
                }
                else if (filterStrings.containsKey("!access-request-ip_" + resultPropertyValue)) {
                    return checkStringAttributeContainsValue(filterStrings,
                            "!access-request-ip_" + resultPropertyValue, resultFilterValue);
                }

            }
            else {
                if (filterStrings.containsKey(resultPropertyValue)) {
                    return checkStringAttributeContainsValue(filterStrings, resultPropertyValue, resultFilterValue);
                }
            }

        }

        // match-match ;-)
        else /* if (checkType == RULECHECK_TYPE_MATCHES) */{
            if (resultType == RESULTCHECK_TYPE_SEARCHRESULT) {
                if (filterStrings.containsKey("role_" + resultPropertyValue)) {
                    return checkStringAttributeMatchesValue(filterStrings, "role_" + resultPropertyValue,
                            resultFilterValue);
                }
                else if (filterStrings.containsKey("!role_" + resultPropertyValue)) {
                    return checkStringAttributeMatchesValue(filterStrings, "!role_" + resultPropertyValue,
                            resultFilterValue);
                }
                else if (filterStrings.containsKey("capability_" + resultPropertyValue)) {
                    return checkStringAttributeMatchesValue(filterStrings, "capability_" + resultPropertyValue,
                            resultFilterValue);
                }
                else if (filterStrings.containsKey("!capability_" + resultPropertyValue)) {
                    return checkStringAttributeMatchesValue(filterStrings, "!capability_" + resultPropertyValue,
                            resultFilterValue);
                }
                else if (filterStrings.containsKey("device-characteristic_" + resultPropertyValue)) {
                    return checkStringAttributeMatchesValue(filterStrings, "device-characteristic_"
                            + resultPropertyValue, resultFilterValue);
                }
                else if (filterStrings.containsKey("!device-characteristic_" + resultPropertyValue)) {
                    return checkStringAttributeMatchesValue(filterStrings, "!device-characteristic_"
                            + resultPropertyValue, resultFilterValue);
                }
                else if (filterStrings.containsKey("access-request-ip_" + resultPropertyValue)) {
                    return checkStringAttributeMatchesValue(filterStrings, "access-request-ip_" + resultPropertyValue,
                            resultFilterValue);
                }
                else if (filterStrings.containsKey("!access-request-ip_" + resultPropertyValue)) {
                    return checkStringAttributeMatchesValue(filterStrings, "!access-request-ip_" + resultPropertyValue,
                            resultFilterValue);
                }
            }
            else {
                return checkStringAttributeMatchesValue(filterStrings, resultPropertyValue, resultFilterValue);
            }
        }

        return false;
    }

    /**
     * check if passed in filter-string-map contains the passed in property and if the related value
     * contains the passed in filter-value
     * 
     * @param filterStrings map containing filter-string
     * @param resultPropertyValue property to search for
     * @param resultFilterValue value of property that must be contained
     * 
     * @return true, if passed in map contains the property and the value of that property contains
     *         the passed in filter-value
     */
    public boolean checkStringAttributeContainsValue(Multimap<String, String> filterStrings,
            String resultPropertyValue, String resultFilterValue) {

        // get entry for attribute
        Object[] resultFilter = filterStrings.get(resultPropertyValue).toArray();

        for (int i = 0; i < resultFilter.length; i++) {
            String currentFilter = (String) resultFilter[i];
            if (resultPropertyValue.startsWith("!")) {
                if (!resultFilterValue.contains(currentFilter)) {
                    return true;
                }
            }
            else {
                if (resultFilterValue.contains(currentFilter)) {
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * check if passed in filter-string-map contains the passed in property and if the related value
     * matches the passed in filter-value
     * 
     * @param filterStrings map containing filter-string
     * @param resultPropertyValue property to search for
     * @param resultFilterValue value of property that must be contained
     * 
     * @return true, if passed in map contains the property and the value of that property matches
     *         the passed in filter-value
     */
    public boolean checkStringAttributeMatchesValue(Multimap<String, String> filterStrings, String resultPropertyValue,
            String resultFilterValue) {
        // get entry for attribute
        Object[] resultFilter = filterStrings.get(resultPropertyValue).toArray();

        for (int i = 0; i < resultFilter.length; i++) {
            String currentFilter = (String) resultFilter[i];
            if (resultPropertyValue.startsWith("!")) {
                if (!resultFilterValue.matches(currentFilter)) {
                    return true;
                }
            }
            else {
                if (resultFilterValue.matches(currentFilter)) {
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * check if passed in entry from poll-result-meta-data matches an "allowance" or an enforcement
     * filter-integer from enforcement.properties
     * 
     * @param filterStrings map of rules to check against
     * @param resultPropertyValue the property of the entry to check
     * @param resultFilterValue value to compare to entry-content
     * @param checkType type of check to perform (contains/matches)
     * 
     * @return true, if passed in result-filter-value is contained or matched with entry inside
     *         filter-string--map
     */
    public boolean checkPollResultForIPTablesIntegerRules(Multimap<String, Integer> filterInteger,
            String resultPropertyValue, int resultFilterValue, byte checkType, byte resultType) {

        // equals-match
        if (checkType == RULECHECK_TYPE_EQUALS) {
            if (filterInteger.containsKey(resultPropertyValue)) {
                // get entry for attribute
                Object[] resultFilter = filterInteger.get(resultPropertyValue).toArray();
                for (int i = 0; i < resultFilter.length; i++) {
                    int currentFilter = new Integer((Integer) resultFilter[0]).intValue();
                    if (resultFilterValue == currentFilter) {
                        return true;
                    }
                }
            }
        }

        // greater-match
        else if (checkType == RULECHECK_TYPE_GREATER) {
            if (filterInteger.containsKey(resultPropertyValue)) {
                // get entry for attribute
                Object[] resultFilter = filterInteger.get(resultPropertyValue).toArray();
                for (int i = 0; i < resultFilter.length; i++) {
                    int currentFilter = new Integer((Integer) resultFilter[0]).intValue();
                    if (resultFilterValue > currentFilter) {
                        return true;
                    }
                }
            }
        }

        // lower-match ;-)
        else /* if (checkType == RULECHECK_TYPE_MATCHES) */{
            if (filterInteger.containsKey(resultPropertyValue)) {
                // get entry for attribute
                Object[] resultFilter = filterInteger.get(resultPropertyValue).toArray();
                for (int i = 0; i < resultFilter.length; i++) {
                    int currentFilter = new Integer((Integer) resultFilter[0]).intValue();
                    if (resultFilterValue < currentFilter) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
