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
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.marker.AbstractReadHandle;


public class EntityServicesTestBase {

	protected static Logger logger = LoggerFactory.getLogger(EntityServicesTestBase.class);
	protected static DatabaseClient client, modulesClient, schemasClient;
	protected static Set<String> entityTypes = new HashSet<String>();
	protected static Set<String> sourceFilesUris = new HashSet<String>();
	protected static Collection<File> files;
	protected static DocumentBuilder builder;

	@SuppressWarnings("unchecked")
	@BeforeClass
	public static void setupClass() throws IOException, ParserConfigurationException {
	    TestSetup testSetup = TestSetup.getInstance();
	    client = testSetup.getClient();
	    modulesClient = testSetup.getModulesClient();
	    schemasClient = testSetup.getSchemasClient();
	    JSONDocumentManager docMgr = client.newJSONDocumentManager();
	    DocumentWriteSet writeSet = docMgr.newWriteSet();
	    DocumentWriteSet metadataWriteSet = docMgr.newWriteSet();
	    
	    tempBootstrapTemplates();
	    
		URL jsonFilesUrl = client.getClass().getResource("/json-entity-types");
		URL xmlFilesUrl = client.getClass().getResource("/xml-entity-types");
		URL sourcesFilesUrl = client.getClass().getResource("/source-documents");
		
		files = FileUtils.listFiles(new File(jsonFilesUrl.getPath()), 
	            FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
	    Collection<File> xmlFiles = FileUtils.listFiles(new File(xmlFilesUrl.getPath()), 
	            FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
	    files.addAll(xmlFiles);
	    
	    Collection<File> sourceFiles = FileUtils.listFiles(new File(sourcesFilesUrl.getPath()),
	            FileFilterUtils.trueFileFilter(), FileFilterUtils.trueFileFilter());
	    
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		builder = factory.newDocumentBuilder();
		
	    for (File f : files) {
	    	if (f.getName().startsWith(".")) { continue; };
	    	if (! ( f.getName().endsWith(".json") || f.getName().endsWith(".xml"))) { continue; };
	    	
	    	// uncomment for quick iteration on TDE.
	    	// if (!f.getName().startsWith("Person-0.0.2")) {continue; };
	    	//.if (!f.getName().equals("schema-complete-entity-type.json")) {continue; };
	    	//if (!f.getName().startsWith("refs")) {continue; };
	    	logger.info("Loading " + f.getName());
	    	//docMgr.write(f.getPath(), new FileHandle(f));
	        writeSet.add(f.getName(), new FileHandle(f));
	       
	        
	        DocumentMetadataHandle metadata = new DocumentMetadataHandle();
	        metadata.getCollections().add("http://marklogic.com/entity-services/entity-types");
	        metadata.setFormat(Format.XML);
	        metadataWriteSet.add(f.getName(), metadata);
	       
	        entityTypes.add(f.getName());
	    }
	    docMgr.write(writeSet);
	    docMgr.write(metadataWriteSet);

	    for (File f : sourceFiles) {
	    	if (f.getName().startsWith(".")) { continue; };
	    	if (! ( f.getName().endsWith(".json") || f.getName().endsWith(".xml"))) { continue; };
	    	
	    	logger.info("Loading " + f.getName());
	    	//docMgr.write(f.getPath(), new FileHandle(f));
	        writeSet.add(f.getName(), new FileHandle(f));
	        
	        sourceFilesUris.add(f.getName());
	    }
	    docMgr.write(writeSet);

	}

	/*
	 * this function goes away once templates move to config directory.
	 */
	private static void tempBootstrapTemplates() {
		String importString = "import module namespace esi = 'http://marklogic.com/entity-services-impl' at '/MarkLogic/entity-services/entity-services-impl.xqy';\n";
		String functionCall = "esi:templates-bootstrap()";
	    ServerEvaluationCall call = 
	            schemasClient.newServerEval().xquery(importString + functionCall);
	   
	}

	//@AfterClass
	public static void teardownClass() {
		JSONDocumentManager docMgr = client.newJSONDocumentManager();
	    for (File f : files) {
	    	logger.info("Removing " + f.getName());
		    docMgr.delete(f.getName());
	    }
	}

	public EvalResultIterator eval(String functionCall) throws TestEvalException {
	    
	    String entityServicesImport = 
	            "import module namespace es = 'http://marklogic.com/entity-services' at '/MarkLogic/entity-services/entity-services.xqy';\n" +
	    		"import module namespace esi = 'http://marklogic.com/entity-services-impl' at '/MarkLogic/entity-services/entity-services-impl.xqy';\n";
	
	    ServerEvaluationCall call = 
	            client.newServerEval().xquery(entityServicesImport + functionCall);
	    EvalResultIterator results = null;
	    try {
	    	results = call.eval();
	    } catch (FailedRequestException e) {
	    	throw new TestEvalException(e);
	    }
	    return results;
	}

	protected <T extends AbstractReadHandle> T evalOneResult(String functionCall, T handle) throws TestEvalException {
		EvalResultIterator results =  eval(functionCall);
		EvalResult result = null;
		if (results.hasNext()) {
			result = results.next();
			return result.get(handle);
		} else {
			return null;
		}
	}
   
	protected void debugOutput(Document xmldoc) throws TransformerException {
		debugOutput(xmldoc, System.out);
	}
	
	protected void debugOutput(Document xmldoc, OutputStream os) throws TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(xmldoc), new StreamResult(os));
	 }
}

