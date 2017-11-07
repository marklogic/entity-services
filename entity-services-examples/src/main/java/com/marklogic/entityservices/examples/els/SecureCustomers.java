package com.marklogic.entityservices.examples.els;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.RequestConstants;
import com.marklogic.client.extensions.ResourceManager;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.util.RequestParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class SecureCustomers extends ResourceManager {

    private static Logger logger = LoggerFactory.getLogger(SecureCustomers.class);
    private static String NAME = "els-pii";
    private DatabaseClient client;


    public SecureCustomers(DatabaseClient client) {
        super();
        this.client = client;
        client.init(NAME, this);
    }

    public String unSecureElements() {
        try {
            logger.info("Removing any existing element-level-security configuration.");
            RequestParameters params = new RequestParameters();
            StringHandle result = getServices().delete(params, new StringHandle());
            logger.info("Done unprotecting elements.");
            return result.get();
        } catch (FailedRequestException e) {
            logger.info("Removing element level security failed -- it was not configured -- proceeding...");
            return "move along...";
        }
    }

    public String secureElements() {
        logger.info("Securing elements based on Customer model and policy");
        RequestParameters params = new RequestParameters();
        StringHandle result = new StringHandle();
        getServices().get(params, new StringHandle());
        logger.info("Done securing elements.");
        return result.get();
    }

}
