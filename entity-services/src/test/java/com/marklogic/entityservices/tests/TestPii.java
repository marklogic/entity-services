package com.marklogic.entityservices.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.marklogic.client.io.JacksonHandle;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class TestPii extends EntityServicesTestBase {

    @BeforeClass
    public static void setup() {
        setupClients();
        TestSetup.getInstance().loadEntityTypes("/json-models", "^.*pii.*.json$");
        TestSetup.getInstance().loadEntityTypes("/json-models", "^Order-0.0.4.json$");
    }

    @Test
    public void entityCreatesPolicyELSConfigurations() throws IOException {
        String modelName = "Customer-pii-0.0.4.json";
        String unitName = "/model-units/Customer-piiconfig.json";
        JacksonHandle handle =
            evalOneResult("", "fn:doc('" + modelName + "')=>es:pii-generate()", new JacksonHandle());
        JsonNode elsConfig = handle.get();

        //save(unitName, elsConfig);


        ObjectMapper mapper = new ObjectMapper();
        InputStream is = this.getClass().getResourceAsStream(unitName);
        JsonNode control = mapper.readValue(is, JsonNode.class);

        org.hamcrest.MatcherAssert.assertThat(elsConfig, org.hamcrest.Matchers.equalTo(control));
    }

    @Test
    public void entityCreatesELSNoNS() throws IOException {
        String modelName = "Order-0.0.4.json";
        String unitName = "/model-units/Order-piiconfig.json";
        JacksonHandle handle =
        evalOneResult("", "fn:doc('" + modelName + "')=>es:pii-generate()", new JacksonHandle());
        JsonNode elsConfig = handle.get();

    //save(unitName, elsConfig);


        ObjectMapper mapper = new ObjectMapper();
        InputStream is = this.getClass().getResourceAsStream(unitName);
        JsonNode control = mapper.readValue(is, JsonNode.class);

        org.hamcrest.MatcherAssert.assertThat(elsConfig, org.hamcrest.Matchers.equalTo(control));
    }

}
