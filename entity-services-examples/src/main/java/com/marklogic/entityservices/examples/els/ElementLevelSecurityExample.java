package com.marklogic.entityservices.examples.els;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.datamovement.WriteBatcher;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.entityservices.examples.ExamplesBase;
import io.codearte.jfairy.Fairy;
import io.codearte.jfairy.producer.person.Person;

import java.io.IOException;
import java.nio.file.Paths;

public class ElementLevelSecurityExample extends ExamplesBase {

    protected String getExampleName() {
        return "example-els";
    }
    protected ObjectMapper mapper = new ObjectMapper();

    private static String PROPERTYFILENAME = "secured-app.properties";
    public ElementLevelSecurityExample() {
        super(PROPERTYFILENAME);
    }

    public void generateOrders(int n) throws IOException {

        Fairy fairy = Fairy.create();

        WriteBatcher batcher = super.newBatcher()
            .withTransform(new ServerTransform("ingest-customer"));

        moveMgr.startJob(batcher);

        batcher.flushAndWait();

        /* Load the model */
        importJSON(Paths.get(projectDir + "/Customer-pii-0.0.1.json"), "http://marklogic.com/entity-services/models");

        importRDF(Paths.get( projectDir + "/pii-policy.ttl"), "http://marklogic.com/entity-services/eaxmple-ontology");

        for (int i=0; i < n; i++) {
            Person p = fairy.person();
            Customer customer = new Customer();
            customer.setId("" + i);
            customer.setName(p.getFirstName());
            customer.setEmail(p.getEmail());
            customer.setSsn(p.getNationalIdentityCardNumber());
            JsonNode asNode = mapper.convertValue(customer, ObjectNode.class);
            batcher.add("/customer/" + i + ".json", new JacksonHandle().with(asNode));
        }

        batcher.flushAndWait();
        //moveMgr.stopJob(batcher);
    }

    public static void main(String[] args) throws IOException {
        ElementLevelSecurityExample ingester = new ElementLevelSecurityExample();

        DatabaseClient extensionClient = ingester.getClient("mlExtensionAdminUsername", "mlExtensionAdminPassword");
        SecureCustomers c = new SecureCustomers(extensionClient);

        Queryer queryer = new Queryer(ingester.client);

        c.unSecureElements();

        ingester.generateOrders(100);

        System.out.println("Some customer data has been created, but not secured yet");
        System.out.println("Now running query with unprivileged user over unsecured data.");
        queryer.query();

        String secureMessage = c.secureElements();
        System.out.println("Secure extension returned with message: " + secureMessage);

        System.out.println("Now running query with unprivileged user -- data should be missing.");
        queryer.query();

        System.out.println("Now to run the same query with a privileged user");

        queryer = new Queryer(ingester.getClient("mlPrivilegedUsername", "mlPrivilegedPassword"));

        queryer.query();

    }
}
