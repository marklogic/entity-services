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
package com.marklogic.entityservices.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.FileHandle;


public class TestSetup {
    
	
    private static TestSetup instance = null;
    protected static Logger logger = LoggerFactory.getLogger(EntityServicesTestBase.class);
	private DatabaseClient _client;
    private DatabaseClient _modulesClient, _schemasClient;
    protected static Collection<File> testCaseFiles;
	protected static DocumentBuilder builder;
    private JSONDocumentManager docMgr;

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
            instance.docMgr = instance._client.newJSONDocumentManager();
        }
        
        
        //instance.loadEntityTypes();
        //instance.loadExtraFiles();
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
    
    

	@SuppressWarnings("unchecked")
	Collection<File> getTestResources(String dirName) {
		URL filesUrl = _client.getClass().getResource(dirName);

		Collection<File> files = new ArrayList<>();

        try {
			Files.walkFileTree(Paths.get(filesUrl.getPath()), new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					files.add(file.toFile());
					return FileVisitResult.CONTINUE;
				}
			});
        } catch (IOException e) {
            throw new TestEvalException(e);
        }

		return files;
	}

    Collection<String> getTestResourceNames(String dirName) {
        Collection<String> strings = new ArrayList<String>();
        getTestResources(dirName).forEach(n -> strings.add(n.getName()));
        return strings;
    }
	
    public HashSet<String> loadEntityTypes(String dirName, String filePattern) {
		
	    DocumentWriteSet writeSet = docMgr.newWriteSet();

        HashSet<String> filesLoaded = new HashSet<String>();
		// testCaseFiles.addAll(getTestResources("/xml-models"));

	    for (File f : getTestResources(dirName)) {
	    	if (f.getName().startsWith(".")) { continue; };
            if (! f.getName().matches(filePattern)) { continue; };

	    	// uncomment for quick iteration on TDE.
	    	// if (!f.getName().startsWith("Person-0.0.2")) {continue; };
	    	//if (!f.getName().equals("OrderDetails-0.0.3.json")) {continue; };
	    	//if (!f.getName().startsWith("refs")) {continue; };
	    	logger.info("Loading " + f.getName());
	    	//docMgr.write(f.getPath(), new FileHandle(f));
	    	DocumentMetadataHandle metadata = new DocumentMetadataHandle();
	        metadata.getCollections().addAll(
	        		"http://marklogic.com/entity-services/models",
	        		f.getName());
	        
	    	writeSet.add(f.getName(), metadata, new FileHandle(f));
	       
	        filesLoaded.add(f.getName());
	    }
	    docMgr.write(writeSet);
        return filesLoaded;
	}

	public HashSet<String> loadExtraFiles(String dirName, String filePattern) {
		HashSet<String> sourceFileUris = new HashSet<String>();
	    
		JSONDocumentManager docMgr = _client.newJSONDocumentManager();
	    DocumentWriteSet writeSet = docMgr.newWriteSet();
	    
	    //Collection<File> sourceFiles = getTestResources("/source-documents");
	    
	    //Collection<File> testDocuments = getTestResources("/test-instances");
        Collection<File> testDocuments = getTestResources(dirName);

	    Collection<File> extraDocuments = new ArrayList<File>();
	    extraDocuments.addAll(testDocuments);

	    for (File f : extraDocuments) {
	    	if (f.getName().startsWith(".")) { continue; };
	    	if (! ( f.getName().endsWith(".json") || f.getName().endsWith(".xml"))) { continue; };
	    	
	    	logger.info("Loading " + f.getName());
	    	writeSet.add(f.getName(), new FileHandle(f));
	        sourceFileUris.add(f.getName());
	    }
	    docMgr.write(writeSet);
		return sourceFileUris;
	}
	
	public void teardownClass() {
		JSONDocumentManager docMgr = _client.newJSONDocumentManager();
	    Collection<String> cleanupDocuments = new ArrayList<String>();
	    cleanupDocuments.addAll(getTestResourceNames("/source-documents"));
        cleanupDocuments.addAll(getTestResourceNames("/test-instances"));
        cleanupDocuments.addAll(getTestResourceNames("/json-models"));
        cleanupDocuments.addAll(getTestResourceNames("/xml-models"));

        docMgr.delete(cleanupDocuments.toArray(new String[] { }));

	}
}
