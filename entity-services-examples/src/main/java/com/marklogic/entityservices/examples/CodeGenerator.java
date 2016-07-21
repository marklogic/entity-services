package com.marklogic.entityservices.examples;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.extensions.ResourceManager;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.util.RequestParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * Class to call an extension that generates code for
 * these examples.
 */
public class CodeGenerator extends ResourceManager {

    private static Logger logger = LoggerFactory.getLogger(CodeGenerator.class);
    private static String NAME = "generate-artifacts";
    private String codeGenDir;
    private String modelsDir;
    private DatabaseClient client;

    private Properties props;

    public CodeGenerator() throws IOException {
        super();
        props = new Properties();
        props.load(this.getClass().getClassLoader().getResourceAsStream("application.properties"));
        codeGenDir = props.getProperty("projectDir") + "/gen";
        modelsDir = props.getProperty("projectDir") + "/data/models";

        client = DatabaseClientFactory.newClient(props.getProperty("mlHost"),
                Integer.parseInt(props.getProperty("mlRestPort")), new DatabaseClientFactory.DigestAuthContext(
                        props.getProperty("mlAdminUsername"), props.getProperty("mlAdminPassword")));

        client.init(NAME, this);
    }

    public void generate() {
        logger.info("Generating fresh artifacts for examples");
        RequestParameters parameters = new RequestParameters();
        parameters.add("codegen-dir", codeGenDir);
        parameters.add("models-dir", modelsDir);

        StringHandle input = new StringHandle().with("{}").withFormat(Format.JSON);

        StringHandle output = getServices().post(parameters, input, new StringHandle());
        logger.info("Done.  Generator returned:");
        logger.info(output.get());
    }

    public static void main(String[] args) throws IOException {
        CodeGenerator generator = new CodeGenerator();
        generator.generate();
    }
}
