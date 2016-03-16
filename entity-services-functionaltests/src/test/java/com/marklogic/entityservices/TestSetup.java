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

    protected TestSetup() {
        // No instantiation allowed.
    }

    public synchronized static TestSetup getInstance() {
    	Properties prop = new Properties();
    	InputStream input = null;

    	try {

    	    input = new FileInputStream("../gradle.properties");

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
    	
    	if (instance == null) {
            instance = new TestSetup();
            if (instance._client == null) {
                instance._client = DatabaseClientFactory.newClient(host, Integer.parseInt(port), "admin", "admin", Authentication.DIGEST);
            }
        }
        return instance;
    }
    
    public DatabaseClient getClient() {
        return _client;
    }
    
}
