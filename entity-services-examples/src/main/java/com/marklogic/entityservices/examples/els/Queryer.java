package com.marklogic.entityservices.examples.els;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.SearchResults;
import com.marklogic.client.query.StringQueryDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class Queryer {

    private static Logger logger = LoggerFactory.getLogger(SecureCustomers.class);
    private DatabaseClient client;
    private ObjectMapper mapper = new ObjectMapper();

    public Queryer(DatabaseClient client) {
        this.client = client;
    }

    public void query() throws IOException {

        System.out.println("Fetching as String:");
        JSONDocumentManager mgr = client.newJSONDocumentManager();
        StringHandle s = mgr.read("/customer/12.json", new StringHandle());
        System.out.println(s.get());


        System.out.println("Fetching as Java Object (hydrating):");
        JacksonHandle c= mgr.read("/customer/12.json", new JacksonHandle());
        Customer customer = mapper.convertValue(c.get().get("envelope").get("instance").get("Customer"), Customer.class);

        String customerString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customer);
        System.out.println(customerString);

        System.out.println("Fetching as part of Search");
        QueryManager qmgr = client.newQueryManager();
        qmgr.setPageLength(3);
        StringQueryDefinition qdef = qmgr.newStringDefinition("pii");
        qdef.setCriteria("12");
        JacksonHandle results = qmgr.search(qdef, new JacksonHandle());

        String resultsString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results.get().get("results"));
        System.out.println(resultsString);

    }

}
