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
package com.marklogic.entityservices.e2e;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.extensions.ResourceManager;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.util.RequestParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    //private String projectDir;

    private Properties props;

    public CodeGenerator() throws IOException {
        super();
        props = new Properties();
        props.load(this.getClass().getClassLoader().getResourceAsStream("application.properties"));
        Path currentRelativePath = Paths.get("");
        String projectDir = currentRelativePath.toAbsolutePath().toString();
        if (!projectDir.endsWith("e2e")) projectDir += "/entity-services-e2e";
        codeGenDir = projectDir + "/gen";
        modelsDir = projectDir + "/data/models";

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
