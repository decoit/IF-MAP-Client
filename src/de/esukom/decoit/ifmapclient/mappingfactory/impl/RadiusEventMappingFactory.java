/*
 * RadiusEventMappingFactory.java 0.1.4 12/02/16
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

package de.esukom.decoit.ifmapclient.mappingfactory.impl;

import de.esukom.decoit.ifmapclient.mappingfactory.SimpleEventMappingFactory;
import de.esukom.decoit.ifmapclient.mappingfactory.result.EventMappingResult;
import de.esukom.decoit.ifmapclient.util.Toolbox;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * concrete implementation of abstract mapping-factory for mapping values from polling-threads to
 * event objects that can be send to MAP-Server
 * 
 * @author Marcel Jahnke, DECOIT GmbH
 */
public class RadiusEventMappingFactory extends SimpleEventMappingFactory {

    public RadiusEventMappingFactory(final Properties props, final ArrayList<HashMap<String, String>> data) {
        super(props, data);
    }

    @Override
    protected void createMappingResult(final Properties props, final ArrayList<HashMap<String, String>> data) {
        SimpleDateFormat ifmapTimeStyle = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss'Z'");
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
        SimpleDateFormat dateFormat2 = new SimpleDateFormat(Toolbox.getNowDateAsString("yyyy-MM-dd HH:mm:ss"));
        for (int i = 0; i < data.size(); i++) {
            HashMap<String, String> temp = data.get(i);
            for (int j = 1; j <= temp.size(); j++) {

                if (temp.get(String.valueOf(j)) != null) {
                    Calendar date = null;
                    Matcher dateMatcher = Toolbox.getRegExPattern("regex.date").matcher(temp.get(String.valueOf(j)));
                    Date datum = null;
                    if (dateMatcher.find()) {
                        try {
                            datum = dateFormat.parse(dateMatcher.group());
                            date = Toolbox
                                    .getCalendarFromString(dateFormat2.format(datum), "yyyy-MM-dd HH:mm:ss", null);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        Calendar calender = Toolbox.getCalendarFromString(Toolbox.sClientStartTime,
                                "yyyy-MM-dd HH:mm:ss", null);
                        if (date.compareTo(calender) >= 0) {
                            EventMappingResult event = new EventMappingResult();
                            event.setDiscoveredTime(ifmapTimeStyle.format(datum));
                            Matcher user_pw = Toolbox.getRegExPattern("regex.pwdefault").matcher(
                                    temp.get(String.valueOf(j)));
                            if (user_pw.find()) {
                                event.setIdentity(user_pw.group());
                            }

                            user_pw = Toolbox.getRegExPattern("regex.userdetail").matcher(temp.get(String.valueOf(j)));
                            if (user_pw.find()) {
                                event.setIdentity(user_pw.group().replace("User-Name = ", "").replace(",", ""));
                            }

                            Matcher ipMatcher = Toolbox.getRegExPattern("regex.ip4").matcher(
                                    temp.get(String.valueOf(j)).replace("localhost", "127.0.0.1"));
                            if (ipMatcher.find()) {
                                event.setIp(ipMatcher.group());
                                event.setIpType("IPv4");
                            }
                            Matcher typeMatcher = Toolbox.getRegExPattern("regex.defaultlogin").matcher(
                                    temp.get(String.valueOf(j)));
                            if (typeMatcher.find()) {
                                event.setName(typeMatcher.group());
                            }
                            typeMatcher = Toolbox.getRegExPattern("regex.acctstatus").matcher(
                                    temp.get(String.valueOf(j)));
                            if (typeMatcher.find()) {
                                event.setName(typeMatcher.group());
                            }
                            else {
                                event.setName("Acct");
                            }

                            event.setConfidence(mConfidenceDefault);
                            event.setMagnitude(mMagnitudeDefault);
                            event.setSignificance(mSignificanceDefault);
                            event.setEventMessageType(mEventtypeDefault);

                            super.mapResult.add(event);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < mapResult.size(); i++) {
            System.out.println(mapResult.get(i).showOnConsole());
        }
    }
}
