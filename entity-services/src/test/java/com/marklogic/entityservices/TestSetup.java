/*
 * Copyright 2016 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.entityservices;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;


public class TestSetup extends EntityServicesTestBase {
    
    private static TestSetup instance = null;
    private DatabaseClient _client;
    private DatabaseClient _modulesClient, _schemasClient;

    protected TestSetup() {
        // No instantiation allowed.
    }

    public synchronized static TestSetup getInstance() {
    	Properties prop = new Properties();
    	InputStream input = null;

    	try {

    	    input = prop.getClass().getResourceAsStream("/gradle.properties");

    	    // load a properties file
    	    prop.load(input);

    	} catch (IOException ex) {
    	    ex.printStackTrace();
    	    throw new RuntimeException(ex);
    	}
    	
    	String host = prop.getProperty("mlHost");
    	String username = prop.getProperty("mlUsername");
    	String password = prop.getProperty("mlPassword");
    	String port = prop.getProperty("mlRestPort");
    	String modulesDatabase = prop.getProperty("mlModulesDatabaseName");
    	String schemasDatabase = prop.getProperty("mlSchemasDatabaseName");

    	
    	if (instance == null) {
            instance = new TestSetup();
            if (instance._client == null) {
                instance._client = DatabaseClientFactory.newClient(host, Integer.parseInt(port), username, password, Authentication.DIGEST);
            }
            if (instance._modulesClient == null) {
            	instance._modulesClient = DatabaseClientFactory.newClient(host, Integer.parseInt(port), modulesDatabase,  username, password, Authentication.DIGEST );
            }
            if (instance._schemasClient == null) {
            	instance._schemasClient = DatabaseClientFactory.newClient(host, Integer.parseInt(port), schemasDatabase,  username, password, Authentication.DIGEST );
            }
        }
        return instance;
    }
    
    public DatabaseClient getClient() {
        return _client;
    }
    
    public DatabaseClient getModulesClient() {
    	return _modulesClient;
    }
    
    public DatabaseClient getSchemasClient() {
    	return _schemasClient;
    }
    
}
