package com.marklogic.entityservices;

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
        if (instance == null) {
            instance = new TestSetup();
            if (instance._client == null) {
                instance._client = DatabaseClientFactory.newClient("localhost", 8000, "admin", "admin", Authentication.DIGEST);
            }
        }
        return instance;
    }
    
    public DatabaseClient getClient() {
        return _client;
    }
    
}
