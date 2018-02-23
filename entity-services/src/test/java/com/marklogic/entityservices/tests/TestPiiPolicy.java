package com.marklogic.entityservices.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.io.JacksonHandle;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPiiPolicy extends EntityServicesTestBase {

    private static String modelName = "Customer-Pii-0.0.4.json";
    @BeforeClass
    public static void setup() {
        setupClients();
        TestSetup.getInstance().loadEntityTypes("/pii-units", modelName);
    }

    @Test
    public void entityCreatesPolicyELSConfigurations() {
        JacksonHandle handle =
            evalOneResult("","fn:doc('"+modelName+"')=>es:pii-artifacts-generate()", new JacksonHandle());
        JsonNode elsConfig = handle.get();


    }

}
