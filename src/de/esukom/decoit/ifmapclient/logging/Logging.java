/*
 * Logging.java 0.1.4 12/02/16
 * 
 * Copyright (C) 2010 Fachhochschule Hannover Ricklinger Stadtweg 118, 30459 Hannover, Germany
 * 
 * Licensed under the Apache License, Version 2.0 (the License); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an AS IS BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package de.esukom.decoit.ifmapclient.logging;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Provide a method to a Root Logger. Initialization is provided here.
 * 
 * @version 0.1.4
 * @author awelzel
 */
public class Logging {

    public static Logger getTheLogger() {
        Logger log = Logger.getLogger("de.esukom.decoit.ifmapclient.logging");

        // Laden der Konfiguration aus einer Datei
        LogManager manager = LogManager.getLogManager();
        try {
            manager.readConfiguration(new FileInputStream("config/logging.properties"));
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return log;
    }
}