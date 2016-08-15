package com.ibm.liberty.starter.it.api.v1;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EndpointInputValidationTest {
    
    @Test
    public void testServiceFinderEndpoint() throws Exception {
        String endpoint = "/start/api/v1/services";
        Response response = callEndpoint(endpoint);
        int status = response.getStatus();
        assertTrue("Response incorrect, response status was " + status, status == 200);
    }

    @Test
    public void testTechFinderInvalidTechType() throws Exception {
        String endpoint = "/start/api/v1/tech/ABC123";
        Response response = callEndpoint(endpoint);
        int status = response.getStatus();
        assertTrue("Response incorrect, response status was " + status, status == Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testTechSelectorInvalidTechType() throws Exception {
        String endpoint = "/start/api/v1/data?tech=ABC123&deploy=local";
        Response response = callEndpoint(endpoint);
        int status = response.getStatus();
        assertTrue("Response incorrect, response status was " + status, status == Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testTechSelectorValidName() throws Exception {
        String endpoint = "/start/api/v1/data?tech=test&name=testName&deploy=local";
        Response response = callEndpoint(endpoint);
        int status = response.getStatus();
        assertTrue("Response incorrect, response status was " + status, status == 200);
    }

    @Test
    public void testTechSelectorInvalidName() throws Exception {
        String endpoint = "/start/api/v1/data?tech=test&name=in/valid&deploy=local";
        Response response = callEndpoint(endpoint);
        int status = response.getStatus();
        assertTrue("Response incorrect, response status was " + status, status == Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testRepoInvalidTech() throws Exception {
        String url = "/start/api/v1/repo/net/wasdev/wlp/starters/abc123/provided-pom/0.0.1/provided-pom-0.0.1.pom";
        Response response = callEndpoint(url);
        int status = response.getStatus();
        assertTrue("Response incorrect, response status was " + status, status == Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testRepoInvalidPath() throws Exception {
        String url = "/start/api/v1/repo/net/wasdev/wlp/starters/test/&=";
        Response response = callEndpoint(url);
        int status = response.getStatus();
        assertTrue("Response incorrect, response status was " + status, status == Response.Status.BAD_REQUEST.getStatusCode());
    }

    private Response callEndpoint(String endpoint) throws Exception {
        Client client = ClientBuilder.newClient();
        String port = System.getProperty("liberty.test.port");
        String url = "http://localhost:" + port + endpoint;
        System.out.println("Testing " + url);
        Response response = client.target(url).request().get();
        return response;
    }

    @Test
    public void testStarterWorkspaceEndpoint() throws Exception {
        String endpoint = "/start/api/v1/workspace";
        Response response = callEndpoint(endpoint);
        int status = response.getStatus();
        assertTrue("Response incorrect, response status was " + status, status == 200);
        String workspaceId = response.readEntity(String.class);
        assertNotNull("Returned workspace ID was not a valid UUID : " + workspaceId, UUID.fromString(workspaceId));
    }
    
    @Test
    public void testUploadInvalidNoTech() throws Exception {
        int response = invokeUploadEndpoint("","sampleFileNoTech.txt");
        assertTrue("Response incorrect, response was " + response, response == Response.Status.BAD_REQUEST.getStatusCode());
    }
    
    @Test
    public void testUploadInvalidNoWorkspace() throws Exception {
        int response = invokeUploadEndpoint("tech=test", "sampleFileNoWorkspace.txt");
        assertTrue("Response incorrect, response was " + response, response == Response.Status.BAD_REQUEST.getStatusCode());
    }
    
    /**
     * **IMPORTANT** - Please ensure that this test always runs and passes as micro-services such as Swagger rely 
     * on the functionalities being tested by this test. 
     * 
     * Never ignore/skip this test. If it fails then investigate and fix the core so that the test passes. 
     */
    @Test
    public void testUploadProcessPackage() throws Exception {
    	String serverOutputDir = System.getProperty("liberty.server.output.dir");
    	String uuid = UUID.randomUUID().toString();
    	
    	// Upload a file
        int responseCode = invokeUploadEndpoint("tech=test&workspace=" + uuid, "sampleUpload.txt");
        assertTrue("Response 1 was incorrect, response was " + responseCode, responseCode == Response.Status.OK.getStatusCode());
        String uploadedFilePath = serverOutputDir + "/workarea/appAccelerator/" + uuid + "/test/sampleUpload.txt";
        File uploadedFile = new File(uploadedFilePath);
        assertTrue("File was not uploaded : path=" + uploadedFilePath, uploadedFile.exists() && uploadedFile.isFile());
        System.out.println("uploadedFilePath=" + uploadedFilePath);
        
        // Upload second file (without cleaning up)
        responseCode = invokeUploadEndpoint("tech=test&workspace=" + uuid, "sampleUpload2.txt");
        assertTrue("Response 2 was incorrect, response was " + responseCode, responseCode == Response.Status.OK.getStatusCode());
        assertTrue("Previously uploaded file was not found : path=" + uploadedFilePath, uploadedFile.exists() && uploadedFile.isFile());
        String uploadedFilePath2 = serverOutputDir + "/workarea/appAccelerator/" + uuid + "/test/sampleUpload2.txt";
        File uploadedFile2 = new File(uploadedFilePath2);
        assertTrue("Second file was not uploaded : path=" + uploadedFilePath2, uploadedFile2.exists() && uploadedFile2.isFile());
        
        // Upload third file (after cleaning up existing files) and process the file
        responseCode = invokeUploadEndpoint("tech=test&workspace=" + uuid + "&cleanup=true&process=true", "sampleUpload3.txt");
        assertTrue("Response 3 was incorrect, response was " + responseCode, responseCode == Response.Status.OK.getStatusCode());
        assertTrue("Previously uploaded files were not cleaned-up : path=" + uploadedFilePath + " : " + uploadedFilePath2, !uploadedFile.exists() && !uploadedFile2.exists() );
        String uploadedFilePath3 = serverOutputDir + "/workarea/appAccelerator/" + uuid + "/test/sampleUpload3.txt";
        File uploadedFile3 = new File(uploadedFilePath3);
        //check if the file was 'processed' (should have been renamed)
        String processedFilePath = serverOutputDir + "/workarea/appAccelerator/" + uuid + "/test/sampleUpload3.txt_renamed";
        File processedFile = new File(processedFilePath);
        assertTrue("Processed file was not found : path=" + processedFile, processedFile.exists() && processedFile.isFile());
        assertTrue("Original third file should not exist : path=" + uploadedFilePath3, !uploadedFile3.exists());
        
        // Invoke the v1/data endpoint to ensure that the packaged files are contained within the zip and the features to 
        // install specified by the 'test' micro-service are present within myProject-wlpcfg/pom.xml
        Client client = ClientBuilder.newClient();
        String port = System.getProperty("liberty.test.port");
        String url = "http://localhost:" + port + "/start/api/v1/data?tech=test&name=Test&deploy=local&techoptions=test:testoption1&workspace=" + uuid;
        System.out.println("Testing " + url);
        Response response = client.target(url).request("application/zip").get();
        int responseStatus = response.getStatus();
        assertTrue("Incorrect response code. Response status is: " + responseStatus, responseStatus == 200);
        // Read the response into an InputStream
        InputStream entityInputStream = response.readEntity(InputStream.class);
        // Create a new ZipInputStream from the response InputStream
        ZipInputStream zipIn = new ZipInputStream(entityInputStream);
        ZipEntry inputEntry = null;
        boolean packagedFileExists = false;
        boolean foundFeaturesToInstall = false;
        while ((inputEntry = zipIn.getNextEntry()) != null) {
            String entryName = inputEntry.getName();
            if ("myProject-application/sampleUpload3.txt_renamed".equals(entryName)) {
            	packagedFileExists = true;
            } else if ("myProject-wlpcfg/pom.xml".equals(entryName)) {
            	DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = domFactory.newDocumentBuilder();
                Document doc = db.parse(zipIn);
                Node assemblyInstallDirectory = doc.getElementsByTagName("assemblyInstallDirectory").item(0);
                assertNotNull("assemblyInstallDirectory node was not found within myProject-wlpcfg/pom.xml", assemblyInstallDirectory);
                Node configuration = assemblyInstallDirectory.getParentNode();
	            Node features =  getNode(configuration, "features");
	            assertNotNull("features node was not found within myProject-wlpcfg/pom.xml", features);
	            assertTrue("Install feature was not found : servlet-3.0", hasNode(features, "feature", "servlet-3.0"));
	            assertTrue("Install feature was not found : apiDiscovery-1.0", hasNode(features, "feature", "apiDiscovery-1.0"));
	            assertTrue("acceptLicense node with ${accept.license} property was not found", hasNode(features, "acceptLicense", "${accept.license}"));
	            
	            Node plugins = configuration.getParentNode().getParentNode();
	            Node artifactIdNode = getNode(plugins, "plugin", "artifactId", "maven-enforcer-plugin");
	            assertNotNull("maven-enforcer-plugin was not found", artifactIdNode);
	            Node enforcerPlugin = artifactIdNode.getParentNode();
	            Node executions = getNode(enforcerPlugin, "executions");
	            assertNotNull("executions node was not found within maven-enforcer-plugin", executions);
	            Node enforcePropertyNode =  getNode(executions, "execution", "id", "enforce-property");
	            assertNotNull("enforce-property id was not found within maven-enforcer-plugin", enforcePropertyNode);
	            Node execution = enforcePropertyNode.getParentNode();
	            Node configurationNode = getNode(execution, "configuration");
	            assertNotNull("configuration node was not found within maven-enforcer-plugin", configurationNode);
	            Node rules = getNode(configurationNode, "rules");
	            assertNotNull("rules node was not found within maven-enforcer-plugin", rules);
	            Node acceptLicenseProperty = getNode(rules, "requireProperty", "property", "accept.license");
	            assertNotNull("requireProperty with accept.license property was not found within maven-enforcer-plugin", acceptLicenseProperty);
	            foundFeaturesToInstall = true;
	            break;
            }
            zipIn.closeEntry();
        }
        zipIn.close();
        
        assertTrue("Packaged file doesn't exist at myProject-application/sampleUpload3.txt_renamed in the zip file", packagedFileExists);
        assertTrue("Features to install were not found in myProject-wlpcfg/pom.xml from the zip file", foundFeaturesToInstall);
        
    }
    
    private Node getNode(Node parentNode, String name){
    	if(parentNode == null || name == null){
    		return null;
    	}
    	if (parentNode.getNodeType() == Node.ELEMENT_NODE && parentNode.hasChildNodes()) {
    		NodeList children = parentNode.getChildNodes();
        	for(int i=0; i < children.getLength(); i++){
            	Node child = children.item(i);
            	if(child != null && name.equals(child.getNodeName())){
            		return child;
            	}
            }
    	}
    	return null;
    }
    
    private boolean hasNode(Node parentNode, String nodeName, String nodeValue){
    	if(parentNode == null || nodeName == null || nodeValue == null){
    		return false;
    	}
    	if (parentNode.getNodeType() == Node.ELEMENT_NODE && parentNode.hasChildNodes()) {
    		NodeList children = parentNode.getChildNodes();
        	for(int i=0; i < children.getLength(); i++){
            	Node child = children.item(i);
            	if(child != null && nodeName.equals(child.getNodeName()) && nodeValue.equals(child.getTextContent())){
            		return true;
            	}
            }
    	}
    	return false;
    }
    
    /**
     * Determine if the parent node contains a child node and a grand child node with matching name and value
     * @param parentNode - the parent node
     * @param childNodeName - name of child node to match
     * @param grandChildNodeName - name of grand child node to match 
     * @param grandChildNodeValue - value of grand child node to match
     * @return boolean value to indicate whether the parent node contains matching child node and a grand child node
     */
    private boolean hasNode(Node parentNode, String childNodeName, String grandChildNodeName, String grandChildNodeValue){
    	if(parentNode == null || childNodeName == null || grandChildNodeName == null || grandChildNodeValue == null){
    		return false;
    	}
    	
    	if (parentNode.getNodeType() == Node.ELEMENT_NODE && parentNode.hasChildNodes()) {
    		NodeList children = parentNode.getChildNodes();
        	for(int i=0; i < children.getLength(); i++){
            	Node child = children.item(i);
            	if(child != null && childNodeName.equals(child.getNodeName())){
            		if (child.getNodeType() == Node.ELEMENT_NODE && child.hasChildNodes()) {
            			NodeList grandChildren = child.getChildNodes();
                    	for(int j=0; j < grandChildren.getLength(); j++){
                    		Node grandChild = grandChildren.item(j);
                    		if(grandChild != null && grandChildNodeName.equals(grandChild.getNodeName()) && grandChildNodeValue.equals(grandChild.getTextContent())){
                    			return true;
                    		}
                    	}
            		}
            	}
            }
    	}
    	return false;
    }
    
    /**
     * Get the matching grand child node
     * @param parentNode - the parent node
     * @param childNodeName - name of child node to match
     * @param grandChildNodeName - name of grand child node to match 
     * @param grandChildNodeValue - value of grand child node to match
     * @return the grand child node if a match was found, null otherwise
     */
    private Node getNode(Node parentNode, String childNodeName, String grandChildNodeName, String grandChildNodeValue){
    	if(parentNode == null || childNodeName == null || grandChildNodeName == null || grandChildNodeValue == null){
    		return null;
    	}
    	
    	if (parentNode.getNodeType() == Node.ELEMENT_NODE && parentNode.hasChildNodes()) {
    		NodeList children = parentNode.getChildNodes();
        	for(int i=0; i < children.getLength(); i++){
            	Node child = children.item(i);
            	if(child != null && childNodeName.equals(child.getNodeName())){
            		if (child.getNodeType() == Node.ELEMENT_NODE && child.hasChildNodes()) {
            			NodeList grandChildren = child.getChildNodes();
                    	for(int j=0; j < grandChildren.getLength(); j++){
                    		Node grandChild = grandChildren.item(j);
                    		if(grandChild != null && grandChildNodeName.equals(grandChild.getNodeName()) && grandChildNodeValue.equals(grandChild.getTextContent())){
                    			return grandChild;
                    		}
                    	}
            		}
            	}
            }
    	}
    	return null;    	
    }
    
    /**
     * Get the matching child node
     * @param parentNode - the parent node
     * @param name - name of child node to match 
     * @param value - value of child node to match
     * @return the child node if a match was found, null otherwise
     */
    private Node getNode(Node parentNode, String name, String value){
    	if(parentNode == null || name == null || value == null){
    		return null;
    	}
    	if (parentNode.getNodeType() == Node.ELEMENT_NODE && parentNode.hasChildNodes()) {
    		NodeList children = parentNode.getChildNodes();
        	for(int i=0; i < children.getLength(); i++){
            	Node child = children.item(i);
            	if(child != null && name.equals(child.getNodeName()) && value.equals(child.getTextContent())){
            		return child;
            	}
            }
    	}
    	
    	return null;
    }
    
    private int invokeUploadEndpoint(String params, String fileName) throws Exception {
        String port = System.getProperty("liberty.test.port");
        String path = "http://localhost:" + port + "/start/api/v1/upload" + ((params != null && !params.trim().isEmpty()) ? ("?" + params) : "");
        System.out.println("Testing " + path);
        
        String boundary = "----WebKitFormBoundarybcoFJqLu81T8NPk8";
        URL url = new URL(path);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.setUseCaches(false);
        httpConnection.setDoInput(true);
        httpConnection.setDoOutput(true);        
        
        httpConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        OutputStream outputStream = httpConnection.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream), true);
        
        final String NEW_LINE = "\r\n";
        writer.append("--" + boundary).append(NEW_LINE);
        writer.append("Content-Disposition: form-data; name=\"fileFormData\"; filename=\"" + fileName + "\"").append(NEW_LINE);
        writer.append("Content-Type: application/octet-stream").append(NEW_LINE).append(NEW_LINE);
        writer.append("Content of the file line 1").append(NEW_LINE).append("Content of file line 2").append(NEW_LINE);
        writer.append(NEW_LINE).flush();
        writer.append("--" + boundary + "--").append(NEW_LINE);
        writer.close();
        
        httpConnection.disconnect();
        return httpConnection.getResponseCode();
    }

}
