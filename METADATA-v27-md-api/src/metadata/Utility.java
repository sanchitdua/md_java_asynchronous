package metadata;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.xpath.*;

import org.w3c.dom.Document;
import org.w3c.dom.*;
import org.xml.sax.*;

import com.sforce.soap.metadata.*;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

import java.util.*;
import java.util.zip.*;
import java.io.*;

public class Utility {
	

	/** This method is meant to add the sObjects in "package.xml" or Menifest file to retrive all of their Metadata Information.
	 * @param sObjects - Collection of sObjects to be retrieved.
	 * @throws Exception
	 */
	public static void createMenifest(Set<String> sObjects) throws Exception {
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance(); 
		domFactory.setIgnoringComments(true);
		DocumentBuilder builder = domFactory.newDocumentBuilder(); 
		Document doc = builder.parse(new File("resources/package.xml")); 

		NodeList membersTag = doc.getElementsByTagName("members");
		NodeList typesTag = doc.getElementsByTagName("types");
		int condition = membersTag.getLength();

		for(int j=0; j<membersTag.getLength(); j++ ){
			typesTag.item(0).removeChild(membersTag.item(j));
			if(membersTag.getLength()>=0 && condition!=1) {
				j=0;
				typesTag.item(0).removeChild(membersTag.item(j));
				if(membersTag.getLength() ==1){
					typesTag.item(0).removeChild(membersTag.item(0));
					break;
				}
				continue;
			}
		}


		NodeList nodes = doc.getElementsByTagName("name");

		for(String sObj: sObjects) {
			Text a = doc.createTextNode(sObj);
			Element p = doc.createElement("members");
			p.appendChild(a);
			nodes.item(0).getParentNode().insertBefore(p, nodes.item(0));
		}


		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);

		String xmlOutput = result.getWriter().toString();

		File file = new File("resources/package.xml");

		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(xmlOutput);
		bw.close();
		System.nanoTime();
		System.out.println("Modifications done.");


	} // END public static void createMenifest(Set<String> sObjects)


	public static void retrieveZip(MetadataConnection mConnection, String menifestFile, final Double Api_Version) throws Exception {
		RetrieveRequest retrieveRequest = new RetrieveRequest();
		retrieveRequest.setApiVersion(Api_Version);
		setUnpackaged(retrieveRequest, menifestFile, Api_Version);

		AsyncResult asyncResult = mConnection.retrieve(retrieveRequest);
		asyncResult = waitForRetrieveCompletion(asyncResult, mConnection);
		RetrieveResult result =
				mConnection.checkRetrieveStatus(asyncResult.getId());

		// Print out any warning messages
		StringBuilder stringBuilder = new StringBuilder();
		if (result.getMessages() != null) {
			for (RetrieveMessage rm : result.getMessages()) {
				stringBuilder.append(rm.getFileName() + " - " + rm.getProblem() + "\n");
			}
		}
		if (stringBuilder.length() > 0) {
			System.out.println("Retrieve warnings:\n" + stringBuilder);
		}

		System.out.println("Writing results to zip file");
		File resultsFile = new File("Temp/mypackage.zip");
		FileOutputStream os = new FileOutputStream(resultsFile);

		try {
			os.write(result.getZipFile());
		} finally {
			os.close();
		}
	}


	private static void setUnpackaged(RetrieveRequest request, String menifestFile, Double Api_Version) throws Exception {
		// Edit the path, if necessary, if your package.xml file is located elsewhere
		File unpackedManifest = new File(menifestFile);

		if (!unpackedManifest.exists() || !unpackedManifest.isFile()) {
			throw new Exception("Should provide a valid retrieve manifest " +
					"for unpackaged content. Looking for " +
					unpackedManifest.getAbsolutePath());
		}

		// Note that we use the fully quualified class name because
		// of a collision with the java.lang.Package class
		com.sforce.soap.metadata.Package p = parsePackageManifest(unpackedManifest, Api_Version);
		request.setUnpackaged(p);
	}


	private static AsyncResult waitForRetrieveCompletion(AsyncResult asyncResult, MetadataConnection mConnection) throws Exception {
		int poll = 0;
		long waitTimeMilliSecs = 1000;
		while (!asyncResult.isDone()) {
			Thread.sleep(waitTimeMilliSecs);
			// double the wait time for the next iteration

			waitTimeMilliSecs *= 2;
			if (poll++ > 20) {
				throw new Exception(
						"Request timed out. If this is a large set of metadata components, " +
						"ensure that MAX_NUM_POLL_REQUESTS is sufficient.");
			}

			asyncResult = mConnection.checkStatus(
					new String[]{asyncResult.getId()})[0];
			System.out.println("Status is: " + asyncResult.getState());
		}

		if (asyncResult.getState() != AsyncRequestState.Completed) {
			throw new Exception(asyncResult.getStatusCode() + " msg: " +
					asyncResult.getMessage());
		}
		return asyncResult;
	}

	private static com.sforce.soap.metadata.Package parsePackageManifest(File file, Double Api_Version)
			throws ParserConfigurationException, IOException, SAXException {
		com.sforce.soap.metadata.Package packageManifest = null;
		List<PackageTypeMembers> listPackageTypes = new ArrayList<PackageTypeMembers>();
		DocumentBuilder db =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
		InputStream inputStream = new FileInputStream(file);
		org.w3c.dom.Element d = (org.w3c.dom.Element) db.parse(inputStream).getDocumentElement();
		for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
			if (c instanceof Element) {
				Element ce = (Element) c;
				NodeList nodeList = ce.getElementsByTagName("name"); // CustomObject
				if (nodeList.getLength() == 0) {
					continue;
				}
				String name = nodeList.item(0).getTextContent(); // CustomObject
				NodeList m = ce.getElementsByTagName("members"); // *, Paycheck__c, Employee__c 
				List<String> members = new ArrayList<String>(); // [*, Paycheck__c, Employee__c]
				for (int i = 0; i < m.getLength(); i++) { // length = 3
					Node mm = m.item(i);
					members.add(mm.getTextContent()); // [Paycheck__c, Employee__c]
				}
				System.out.println("members obtained are: "+members);
				PackageTypeMembers packageTypes = new PackageTypeMembers();
				packageTypes.setName(name);
				packageTypes.setMembers(members.toArray(new String[members.size()]));
				listPackageTypes.add(packageTypes);
			}
		}
		packageManifest = new com.sforce.soap.metadata.Package();
		PackageTypeMembers[] packageTypesArray =
				new PackageTypeMembers[listPackageTypes.size()];
		packageManifest.setTypes(listPackageTypes.toArray(packageTypesArray));
		packageManifest.setVersion(Api_Version + "");
		return packageManifest;
	}

	/**
	 * @param zipName - The zip fileName that is extracted in "Temp" Folder
	 * @param sObjects - All the sObjects in concern
	 * @return - returning the sObject "SharingModel" element value
	 */
	public static Map<String, Map<String, String>> parseZip(String zipName, Set<String> sObjects) {
		Map<String, Map<String, String>> returnVal = new HashMap<String, Map<String, String>>();
		try {
			final ZipFile zipFile = new ZipFile(zipName);
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				final ZipEntry zipEntry = entries.nextElement();
				if (!zipEntry.isDirectory()) {
					final String fileName = zipEntry.getName();

					for(String objName: sObjects) {
						if(fileName.endsWith(objName+".object")) {
							InputStream input = zipFile.getInputStream(zipEntry);
							Map<String, String> elementalsMap = new HashMap<String, String>();

							// --> DOM Parsing for getting the sObject's Label Name :D <-- //

							DocumentBuilder db =
									DocumentBuilderFactory.newInstance().newDocumentBuilder();
							org.w3c.dom.Element d = (org.w3c.dom.Element) db.parse(input).getDocumentElement();

							for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
								if (c instanceof Element) {
									Element ce = (Element) c;
									if(ce.getNodeName().equalsIgnoreCase("label") && ce.getParentNode().getNodeName().equalsIgnoreCase("CustomObject")){
										elementalsMap.put("label", ce.getTextContent()); // âœ“
									}

									if(ce.getNodeName().equalsIgnoreCase("sharingModel") && ce.getParentNode().getNodeName().equalsIgnoreCase("CustomObject")) {
										elementalsMap.put("sharingModel", ce.getTextContent());
									}

									if(ce.getNodeName().equalsIgnoreCase("deploymentStatus") && ce.getParentNode().getNodeName().equalsIgnoreCase("CustomObject")) {
										elementalsMap.put("deploymentStatus", ce.getTextContent());
									}
									if(ce.getNodeName().equalsIgnoreCase("pluralLabel") && ce.getParentNode().getNodeName().equalsIgnoreCase("CustomObject")) {
										elementalsMap.put("pluralLabel", ce.getTextContent());
									}
									if(ce.getNodeName().equalsIgnoreCase("enableActivities") && ce.getParentNode().getNodeName().equalsIgnoreCase("CustomObject")) {
										elementalsMap.put("enableActivities", ce.getTextContent());
									}
								}
							}

							// END DOM Parsing
							input = zipFile.getInputStream(zipEntry);

							BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
							try {
								String xml;
								while ((xml = reader.readLine()) != null ) {
									if(!xml.contains("//nameField") && xml.contains("nameField")) {

										boolean isAutonumber = false;

										if((xml = reader.readLine()) != null){
											if(xml.contains("displayFormat")) {
												isAutonumber = true;
												XPathFactory xpathFactory = XPathFactory.newInstance();
												XPath xpath = xpathFactory.newXPath();
												InputSource source = new InputSource(new StringReader(xml));
												Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
												String val = xpath.evaluate("/displayFormat", doc);
												elementalsMap.put("displayFormat", val);
											}
											if(xml.contains("type")) {
												isAutonumber = true;
												XPathFactory xpathFactory = XPathFactory.newInstance();
												XPath xpath = xpathFactory.newXPath();
												InputSource source = new InputSource(new StringReader(xml));
												Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
												String val = xpath.evaluate("/type", doc);
												if(val.equalsIgnoreCase("AutoNumber"))
													isAutonumber = true;
												elementalsMap.put("type", val);
											}
											if(xml.contains("label")) { // âœ“
												XPathFactory xpathFactory = XPathFactory.newInstance();
												XPath xpath = xpathFactory.newXPath();
												InputSource source = new InputSource(new StringReader(xml));
												Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
												String val = xpath.evaluate("/label", doc);
												elementalsMap.put("nameLabel", val);
											}
										}

										if((xml = reader.readLine()) != null){
											if(xml.contains("displayFormat")) {
												isAutonumber = true;
												XPathFactory xpathFactory = XPathFactory.newInstance();
												XPath xpath = xpathFactory.newXPath();
												InputSource source = new InputSource(new StringReader(xml));
												Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
												String val = xpath.evaluate("/displayFormat", doc);
												elementalsMap.put("displayFormat", val);
											}
											if(xml.contains("type")) {
												XPathFactory xpathFactory = XPathFactory.newInstance();
												XPath xpath = xpathFactory.newXPath();
												InputSource source = new InputSource(new StringReader(xml));
												Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
												String val = xpath.evaluate("/type", doc);
												if(val.equalsIgnoreCase("AutoNumber"))
													isAutonumber = true;
												elementalsMap.put("type", val);
											}
											if(xml.contains("label")) {
												XPathFactory xpathFactory = XPathFactory.newInstance();
												XPath xpath = xpathFactory.newXPath();
												InputSource source = new InputSource(new StringReader(xml));
												Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
												String val = xpath.evaluate("/label", doc);
												elementalsMap.put("nameLabel", val);
											}
										}

										if((xml = reader.readLine()) != null){
											if(xml.contains("displayFormat")) {
												isAutonumber = true;
												XPathFactory xpathFactory = XPathFactory.newInstance();
												XPath xpath = xpathFactory.newXPath();
												InputSource source = new InputSource(new StringReader(xml));
												Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
												String val = xpath.evaluate("/displayFormat", doc);
												elementalsMap.put("displayFormat", val);
											}
											if(xml.contains("type")) {
												XPathFactory xpathFactory = XPathFactory.newInstance();
												XPath xpath = xpathFactory.newXPath();
												InputSource source = new InputSource(new StringReader(xml));
												Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
												String val = xpath.evaluate("/type", doc);
												if(val.equalsIgnoreCase("AutoNumber"))
													isAutonumber = true;
												elementalsMap.put("type", val);
											}
											if(xml.contains("label")) {
												XPathFactory xpathFactory = XPathFactory.newInstance();
												XPath xpath = xpathFactory.newXPath();
												InputSource source = new InputSource(new StringReader(xml));
												Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
												String val = xpath.evaluate("/label", doc);
												elementalsMap.put("nameLabel", val);
											}
										}


										if(isAutonumber){
											if((xml = reader.readLine()) != null){
												if(xml.contains("displayFormat")) {
													isAutonumber = true;
													XPathFactory xpathFactory = XPathFactory.newInstance();
													XPath xpath = xpathFactory.newXPath();
													InputSource source = new InputSource(new StringReader(xml));
													Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
													String val = xpath.evaluate("/displayFormat", doc);
													elementalsMap.put("displayFormat", val);
												}
												if(xml.contains("type")) {
													XPathFactory xpathFactory = XPathFactory.newInstance();
													XPath xpath = xpathFactory.newXPath();
													InputSource source = new InputSource(new StringReader(xml));
													Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
													String val = xpath.evaluate("/type", doc);
													if(val.equalsIgnoreCase("AutoNumber"))
														isAutonumber = true;
													elementalsMap.put("type", val);
												}
												if(xml.contains("label")) {
													XPathFactory xpathFactory = XPathFactory.newInstance();
													XPath xpath = xpathFactory.newXPath();
													InputSource source = new InputSource(new StringReader(xml));
													Object doc = xpath.evaluate("/", source, XPathConstants.NODE);
													String val = xpath.evaluate("/label", doc);
													elementalsMap.put("nameLabel", val);
												}
											}
										}
									}
								}
								returnVal.put(objName, elementalsMap);
							} catch( XPathExpressionException xpee ) {
								xpee.printStackTrace();
							}
							catch (IOException e) {
								e.printStackTrace();
							} finally {
								try {
									reader.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					} // END for
				}
			}
			zipFile.close();
		}
		catch(FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			return null;
		}
		catch (final IOException ioe) {
			System.err.println("Unhandled exception:");
			ioe.printStackTrace();
			return null;
		}
		catch(ParserConfigurationException pce){
			pce.printStackTrace();
		}
		catch(SAXException se){
			se.printStackTrace();
		}
		return returnVal;
	} // END private void parseZip()


	public static Map<String, CustomObject> createCustomObjectDetails(Map<String, Map<String, String>> arg){
		Map<String, CustomObject> returnVal = new HashMap<String, CustomObject>();


		for(Map.Entry<String, Map<String, String>> entry:  arg.entrySet()) {
			// Populating the Current Custom Object to be Refreshed after creating the Record Types
			CustomObject customObject = new CustomObject();
			customObject.setFullName(entry.getKey());
			customObject.setLabel(entry.getValue().get("label"));
			customObject.setPluralLabel(entry.getValue().get("pluralLabel"));
			customObject.setDescription("Record Types created");
			if(entry.getValue().get("deploymentStatus").equalsIgnoreCase("Deployed"))
				customObject.setDeploymentStatus(DeploymentStatus.Deployed);
			else 
				customObject.setDeploymentStatus(DeploymentStatus.InDevelopment);
			if(entry.getValue().get("enableActivities").equalsIgnoreCase("true"))
				customObject.setEnableActivities(true);
			else 
				customObject.setEnableActivities(false);
			if(entry.getValue().get("sharingModel").equalsIgnoreCase("ControlledByParent")){
				customObject.setSharingModel(SharingModel.ControlledByParent);
			} else if(entry.getValue().get("sharingModel").equalsIgnoreCase("FullAccess")){
				customObject.setSharingModel(SharingModel.FullAccess);
			} else if(entry.getValue().get("sharingModel").equalsIgnoreCase("Private")){
				customObject.setSharingModel(SharingModel.Private);
			} else if(entry.getValue().get("sharingModel").equalsIgnoreCase("Read")){
				customObject.setSharingModel(SharingModel.Read);
			} else if(entry.getValue().get("sharingModel").equalsIgnoreCase("ReadWrite")){
				customObject.setSharingModel(SharingModel.ReadWrite);
			} else if(entry.getValue().get("sharingModel").equalsIgnoreCase("ReadWriteTransfer")){
				customObject.setSharingModel(SharingModel.ReadWriteTransfer);
			} 

			CustomField nf = new CustomField();
			nf.setLabel(entry.getValue().get("nameLabel"));
			if(entry.getValue().get("type")!=null)
				if(entry.getValue().get("type").equalsIgnoreCase("AutoNumber")){
					nf.setType(com.sforce.soap.metadata.FieldType.AutoNumber);
					nf.setDisplayFormat(entry.getValue().get("displayFormat"));
				} else{
					nf.setType(com.sforce.soap.metadata.FieldType.Text);
				}
			customObject.setNameField(nf);
			returnVal.put(entry.getKey(), customObject);
		} // END for

		return returnVal;
	} // END public static Map<String, CustomObject> createCustomObject(Map<String, Map<String, String>> arg)


	/**
	 * @param objectName
	 * @param recordTypes
	 * @param isDefault
	 */
	public static void createRecordTypes (CustomObject customObject, Set<String> recordTypes, boolean isDefault, MetadataConnection mConnection, PartnerConnection pConnection){

		//		boolean returnVal = false;

		try{
			if(customObject==null)
				return;
			System.out.println("\nFor the sObject: "+customObject.getFullName()+""+" Record Type to be made are: "+recordTypes);
			// Getting the Current User's License using UserInfo class
			// Metadata Query over Profile for getting the Current User's Profile FullName
			ListMetadataQuery lmq = new ListMetadataQuery();
			lmq.setType("Profile");

			double asOfVersion = 27.0;
			FileProperties[] lmr = mConnection.listMetadata(new ListMetadataQuery[] {lmq}, asOfVersion);
			String profileFullName = "";

			if(lmr != null){
				for(FileProperties n: lmr){
					if(pConnection.getUserInfo().getProfileId().equalsIgnoreCase(n.getId())){
						profileFullName = n.getFullName(); // <-- Profile Full Name
						break;
					}
				}
			}

			// Instantiating Metadata type "RecordType" for setting the record Types
			RecordType[] rType = new RecordType[recordTypes.size()];
			// Iterating over the Record Types based on indices using the Collection Type List<String>
			List<String> recordTypesList = new ArrayList<String>();
			recordTypesList.addAll(recordTypes);
			// Instantiating the Metadata type "ProfileRecordTypeVisibility" for setting the Visibility of Record Types over current Profile
			com.sforce.soap.metadata.ProfileRecordTypeVisibility[] prtv = new com.sforce.soap.metadata.ProfileRecordTypeVisibility[recordTypes.size()];
			// Using counter we are making sure the first Record Type should get "Default" value set 
			int counter = 0 ;

			for(int i=0; i<recordTypes.size(); i++){
				// |[â—�â–ªâ–ªâ—�]||[â—�â–ªâ–ªâ—�]||[â—�â–ªâ–ªâ—�]||[â—�â–ªâ–ªâ—�]||[â—�â–ªâ–ªâ—�]||[â—�â–ªâ–ªâ—�]||[â—�â–ªâ–ªâ—�]||[â—�â–ªâ–ªâ—�]||[â—�â–ªâ–ªâ—�]|
				// Instantiating the Metadata type "RecordType" for setting some mandatory fields - (Name, Label, Active)
				rType[i] = new RecordType();

				rType[i].setLabel(recordTypesList.get(i)); // <-- setting the label name of Record Type
				if( recordTypesList.get(i).trim().contains(" ") ) {
					rType[i].setFullName(customObject.getFullName()+"."+recordTypesList.get(i).trim().replaceAll(" ", "_")); // <-- If there are any white spaces in the Record Type name from Configuration (replacing the white spaces with underscores _ )
				} else
					rType[i].setFullName(customObject.getFullName()+"."+recordTypesList.get(i));
				rType[i].setActive(true); // <-- Making the record type Active

				// |-o-| (-o-) |-o-||-o-| (-o-) |-o-||-o-| (-o-) |-o-||-o-| (-o-) |-o-||-o-| (-o-) 
				// Instantiating the Metadata type ProfileRecordTypeVisibility
				prtv[i] = new ProfileRecordTypeVisibility();
				if( recordTypesList.get(i).contains(" ") ) {
					prtv[i].setRecordType(""+customObject.getFullName()+"."+recordTypesList.get(i).trim().replaceAll(" ", "_"));
				} else
					prtv[i].setRecordType(""+customObject.getFullName()+"."+recordTypesList.get(i));

				if(counter==0 && !isDefault){
					prtv[i].setDefault(true); // <-- If it is the first record type then setting it "Default"
				} else
					prtv[i].setDefault(false);
				prtv[i].setVisible(true); // <-- Visibility over Profile
				counter++;
			}
			// é¾´â†€â—¡â†€é¾´é¾´â†€â—¡â†€é¾´é¾´â†€â—¡â†€é¾´é¾´â†€â—¡â†€é¾´é¾´â†€â—¡â†€é¾´é¾´â†€â—¡â†€é¾´é¾´â†€â—¡â†€é¾´é¾´â†€â—¡â†€é¾´
			// Instantiating the Metadata type Profile for UPDATE Metadata Call

			Profile pr = new Profile();
			pr.setRecordTypeVisibilities(prtv); // <-- Assigning the Record Type visiblities from above
			AsyncResult[] asyncResultsupdate = null;
			if(mConnection != null)
				asyncResultsupdate = mConnection.create(rType);   ////////////////// <---- CREATE Metadata Call on "RecordType"

			AsyncResult asyncResult = asyncResultsupdate[0];

			final long ONE_SECOND = 1000;
			final int MAX_NUM_POLL_REQUESTS = 3;
			int poll = 0;
			long waitTimeMilliSecs = ONE_SECOND;

			int countRTs = 0;

			while (!asyncResult.isDone()) {
				Thread.sleep(waitTimeMilliSecs);
				waitTimeMilliSecs *= 2;
				if (poll++ > MAX_NUM_POLL_REQUESTS) {
					throw new Exception("Request timed out. If this is a large set of metadata components, check that the time allowed by MAX_NUM_POLL_REQUESTS is sufficient.");
				}
				asyncResult = mConnection.checkStatus(new String[] {asyncResult.getId()})[0];
				System.out.println("\t\tStatus for Record Types is: " + asyncResult.getState());
			}
			if (asyncResult.getState() != AsyncRequestState.Completed) {        	
				//				System.out.println(asyncResult.getStatusCode() + " msg: " +	asyncResult.getMessage());
				if(asyncResult.getStatusCode() == StatusCode.DUPLICATE_VALUE) {
					System.out.println("ERROR: The Record Type you specified already exists.");
					countRTs++;
				}

			}
			else{
				System.out.println("\tThe Record Types created successfully for the sObject: "+customObject.getFullName());
			}

			if(countRTs==recordTypes.size()){
				System.out.println("No Record Types to update for the Object: "+customObject.getFullName());
				return;
			}

			System.out.println("\n\tUpdating the Current User's profile to the get the Reocord Types Visible ...");

			UpdateMetadata ut = new UpdateMetadata();
			pr.setFullName(""+profileFullName);
			ut.setMetadata(pr);

			// Updating the Profile for setting the RecordTypeVisiblity
			AsyncResult[] ars = mConnection.update(new UpdateMetadata[]  
					{ ut }); /////////////// <------- UPDATE Metadata Call on "Profile"

			AsyncResult asyncResultt = ars[0];
			// set initial wait time to one second in milliseconds
			waitTimeMilliSecs = 1000;
			while (!asyncResultt.isDone()) {
				Thread.sleep(waitTimeMilliSecs);
				// double the wait time for the next iteration
				waitTimeMilliSecs *= 2;
				asyncResultt = mConnection.checkStatus(
						new String[] {asyncResultt.getId()})[0];
				System.out.println("\t\tStatus is: " + asyncResultt.getState());
			}

			if (asyncResultt.getState() != AsyncRequestState.Completed) {
				System.out.println(asyncResultt.getStatusCode() + " msg: " +
						asyncResultt.getMessage());

				//				DUPLICATE_VALUE msg: The label:adlt on record type:MyCustomObject2__c.adlt is not unique



			}
			else{
				System.out.println("\tThe Profile is Updated successfully for the sObject: "+customObject.getFullName());
			}



			UpdateMetadata updateMetadata = new UpdateMetadata();
			updateMetadata.setMetadata(customObject);

			// Refreshing the Custom Object on which the Record type has been created.
			ars = mConnection.update(new UpdateMetadata[] 
					{ updateMetadata }); //////////////// <---------- UPDATE Metadata Call on "CustomObject"

			if(ars==null)
				return;
			asyncResultt = ars[0];
			// set initial wait time to one second in milliseconds
			waitTimeMilliSecs = 1000;
			while (!asyncResultt.isDone()) {
				Thread.sleep(waitTimeMilliSecs);
				// double the wait time for the next iteration
				waitTimeMilliSecs *= 2;
				asyncResultt = mConnection.checkStatus(
						new String[] {asyncResultt.getId()})[0];
				System.out.println("\t\tStatus is: " + asyncResultt.getState());
			}

			if (asyncResultt.getState() != AsyncRequestState.Completed) {
				System.out.println(asyncResultt.getStatusCode() + " msg: " +
						asyncResultt.getMessage());
			}
			else
				System.out.println("\tThe Object "+customObject.getFullName()+" is Refreshed successfully.");

			System.out.println("Record Type Creations performed.");
		}catch(ConnectionException ce){
			ce.printStackTrace();
		}catch(Exception ex){
			ex.printStackTrace();
		}

	} // END createRecordTypes()


	/**
	 * @param mConnection
	 * @param recordTypeTest
	 * @param sObjName
	 * @param secName
	 * @param f
	 * @param isNameAutoNumber
	 * @param pConnection
	 */
	public static void updateLayout(MetadataConnection mConnection, String[] recordTypeTest, String sObjName, String secName, Set<String> f, boolean isNameAutoNumber, PartnerConnection pConnection){
		try{
			if(f!= null){
				boolean nameSet = false;
				boolean hit = false;
				com.sforce.soap.partner.DescribeSObjectResult[] dsrArray = null;

				boolean isNameDone = false;
				boolean isNameAuto = false;
				Set<String> masters = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

				if(pConnection != null)
					dsrArray = pConnection.describeSObjects(new String[] { sObjName }); // lablename, apiname, all of the child components as well / listMetadata ==> Type :: 

				boolean isNameIncluded = false;
				com.sforce.soap.partner.DescribeSObjectResult dsr = null;
				if(dsrArray != null)
					if(dsrArray.length >0)
						dsr = dsrArray[0];
				// Here, we're checking if the Name Standard field is autonumber or not. 
				// If the name field is autonumber then its treated as readonly field which is not required to set on the edit layout while if the name std field is of type Text then its mandatory to set on the layout.
				if(dsr != null)
					for (int i = 0; i < dsr.getFields().length; i++) {
						com.sforce.soap.partner.Field field = dsr.getFields()[i];
						if(!field.getCustom()){
							if(field.getNameField()){
								if(!field.isAutoNumber()){
									nameSet = true;
								}
								if(field.isAutoNumber())
									isNameAuto = true;
							}
						}

					}

				String sectionName = ""+secName;

				// The set coming as argument
				Set<String> fields = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
				fields.addAll(f);

				Set<String> allFieldsAlreadyonLayout = new HashSet<String>();
				com.sforce.soap.partner.DescribeLayoutResult dlResult = null;

				if(recordTypeTest==null)
					dlResult = pConnection.describeLayout(""+sObjName, null);
				else
					dlResult = pConnection.describeLayout(""+sObjName, recordTypeTest);

				Map<String, LayoutSection> sectionNameWithLayoutSection = new LinkedHashMap<String, LayoutSection>();

				List<LayoutSection> lsList = new ArrayList<LayoutSection>();


				// collecting every section's items so that we can compare it with the upcoming section.
				// also the styling should be maintained;

				// i think it would be difficult to maintain the previous 
				// things on the section as it is if we are trying to add some more items in it.

				Map<String, Set<String>> sectionWithItems = new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);

				for(com.sforce.soap.partner.DescribeLayout lay: dlResult.getLayouts()){
					boolean isChecked = false;
					for(com.sforce.soap.partner.DescribeLayoutSection dls: lay.getEditLayoutSections()){
						Set<String> items = new LinkedHashSet<String>();
						String sectionHeading = dls.getHeading();
						if(sectionName.equalsIgnoreCase(sectionHeading)){
							sectionName = sectionHeading;
						}
						List<LayoutItem> leftLayoutItemList = new ArrayList<LayoutItem>();
						List<LayoutItem> rightLayoutItemList = new ArrayList<LayoutItem>();

						for(com.sforce.soap.partner.DescribeLayoutRow dlr: dls.getLayoutRows()){
							com.sforce.soap.partner.DescribeLayoutItem[] dli = dlr.getLayoutItems();

							if(dli[0].getLayoutComponents() != null && dli[0].getLayoutComponents().length >0){
								LayoutItem li = new LayoutItem();

								if(dli[0].getLayoutComponents()[0].getValue() == null){
									li.setEmptySpace(true);
									leftLayoutItemList.add(li);
								}
								// TODO to be checked for this by including the Name
								if(dli[0].getLayoutComponents()[0].getValue() != null ){
									if(!fields.contains(dli[0].getLayoutComponents()[0].getValue()) || dli[0].getLayoutComponents()[0].getValue().equalsIgnoreCase("Name")){

										li.setField(dli[0].getLayoutComponents()[0].getValue());
										if(dli[0].getLayoutComponents()[0].getValue().equalsIgnoreCase("CreatedBy") || dli[0].getLayoutComponents()[0].getValue().equalsIgnoreCase("LastModifiedBy") || dli[0].getLayoutComponents()[0].getValue().equalsIgnoreCase("Owner"))
											li.setBehavior(UiBehavior.Readonly);
										else if(dli[0].getLayoutComponents()[0].getValue().equalsIgnoreCase("Name") && isNameAuto && !nameSet){
											li.setBehavior(UiBehavior.Readonly);
											isNameIncluded = true;
										}
										else if(dli[0].getLayoutComponents()[0].getValue().equalsIgnoreCase("Name") && !isNameAuto && nameSet){
											li.setBehavior(UiBehavior.Required);
											isNameIncluded = true;
										}
										else
											li.setBehavior(UiBehavior.Edit);
										allFieldsAlreadyonLayout.add(dli[0].getLayoutComponents()[0].getValue());

										leftLayoutItemList.add(li);
										items.add(dli[0].getLayoutComponents()[0].getValue());
									}

								}
							}	

							if(dli[1].getLayoutComponents() != null && dli[1].getLayoutComponents().length >0){
								LayoutItem li = new LayoutItem();

								if(dli[1].getLayoutComponents()[0].getValue() == null){
									li.setEmptySpace(true);
									rightLayoutItemList.add(li);
								}

								if(dli[1].getLayoutComponents()[0].getValue() != null){

									if(!fields.contains(dli[1].getLayoutComponents()[0].getValue()) || dli[1].getLayoutComponents()[0].getValue().equalsIgnoreCase("Name")){
										li.setField(dli[1].getLayoutComponents()[0].getValue());
										if(dli[1].getLayoutComponents()!= null){
											if(dli[1].getLayoutComponents() != null){
												if(dli[1].getLayoutComponents()[0]!=null){
													if(dli[1].getLayoutComponents()[0].getValue()!= null){
														if(dli[1].getLayoutComponents()[0].getValue().equalsIgnoreCase("CreatedBy") || dli[1].getLayoutComponents()[0].getValue().equalsIgnoreCase("LastModifiedBy") || dli[1].getLayoutComponents()[0].getValue().equalsIgnoreCase("Owner"))
															li.setBehavior(UiBehavior.Readonly);
														else if(dli[1].getLayoutComponents()[0].getValue().equalsIgnoreCase("Name") && isNameAuto && !nameSet && !isNameIncluded)
															li.setBehavior(UiBehavior.Readonly);
														else if(dli[1].getLayoutComponents()[0].getValue().equalsIgnoreCase("Name") && !isNameAuto && nameSet && !isNameIncluded)
															li.setBehavior(UiBehavior.Required);
														else
															li.setBehavior(UiBehavior.Edit);
													}
												}

											}

										}

										allFieldsAlreadyonLayout.add(dli[1].getLayoutComponents()[0].getValue());
										rightLayoutItemList.add(li);
										items.add(dli[1].getLayoutComponents()[0].getValue());
									}
								}

							}							
						}
						// here put the map information.
						LayoutColumn lColumn1 = new LayoutColumn();
						lColumn1.setLayoutItems(leftLayoutItemList.toArray(new LayoutItem[leftLayoutItemList.size()]));
						LayoutColumn lColumn2 = new LayoutColumn();
						lColumn2.setLayoutItems(rightLayoutItemList.toArray(new LayoutItem[rightLayoutItemList.size()]));
						LayoutSection ls = new LayoutSection();
						ls.setLabel(""+sectionHeading);
						ls.setCustomLabel(true);
						ls.setDetailHeading(true);
						ls.setEditHeading(true); // to enable collapse and expand functionality
						ls.setStyle(LayoutSectionStyle.TwoColumnsLeftToRight);
						ls.setLayoutColumns(new LayoutColumn[]{lColumn1, lColumn2});
						lsList.add(ls);
						sectionNameWithLayoutSection.put(sectionHeading, ls);
						sectionWithItems.put(sectionHeading, items);

						// If the section already exists ==> Evaluation of upcoming fields so that we may compare the duplicacy and the space required according to the number of items (i.e odd or even)
						if(sectionWithItems.containsKey(sectionName) && !isChecked){

							Set<String> existingFields=  sectionWithItems.get(sectionName);
							if(existingFields.size()>0) // To get rid of the duplicacy that can occur while updating the Layout Section
								fields.removeAll(existingFields);

							existingFields.clear();

							isChecked = true;
							hit = true;
							if(!fields.contains("Name")){
								List<LayoutItem> leftLayoutItemsList = new ArrayList<LayoutItem>();
								List<LayoutItem> rightLayoutItemsList = new ArrayList<LayoutItem>();
								leftLayoutItemsList.addAll(leftLayoutItemList);
								rightLayoutItemsList.addAll(rightLayoutItemList);

								List<String> totalList = new ArrayList<String>();
								totalList.addAll(fields);

								int midVal = 0;

								if(totalList.size()%2==0)
									midVal = totalList.size()/2;
								else
									midVal = totalList.size()/2+1;

								List<String> subListOne = totalList.subList(0, midVal);
								List<String> subListTwo = totalList.subList(midVal, totalList.size());


								for(String str: subListOne){
									LayoutItem li = new LayoutItem();
									li.setField(str);
									leftLayoutItemsList.add(li);
								}
								for(String str: subListTwo){
									LayoutItem li = new LayoutItem();
									li.setField(str);
									rightLayoutItemsList.add(li);
								}

								LayoutColumn lColumn12 = new LayoutColumn();
								LayoutColumn lColumn22 = new LayoutColumn();
								lColumn12.setLayoutItems(leftLayoutItemsList.toArray(new LayoutItem[leftLayoutItemsList.size()]));
								lColumn22.setLayoutItems(rightLayoutItemsList.toArray(new LayoutItem[rightLayoutItemsList.size()]));
								leftLayoutItemsList.clear();
								rightLayoutItemsList.clear();
								LayoutSection ls1 = new LayoutSection();
								ls1.setLabel(""+sectionName);
								ls1.setCustomLabel(true);
								ls1.setDetailHeading(true);
								ls1.setEditHeading(true);
								ls1.setStyle(LayoutSectionStyle.TwoColumnsLeftToRight);
								ls1.setLayoutColumns(new LayoutColumn[]{lColumn12, lColumn22});

								sectionNameWithLayoutSection.put(sectionName, ls1);

							}
						}
					}
				} // END for

				/********************<start>*Manipulations*</start>********************************/
				// If the section needs to be created // it will also execute when there are no Layout Items in the existing section.
				if(!sectionWithItems.containsKey(sectionName)){

					if(true){

						List<LayoutItem> leftLayoutItemsList = new ArrayList<LayoutItem>();
						List<LayoutItem> rightLayoutItemsList = new ArrayList<LayoutItem>();
						List<String> newItems = new ArrayList<String>();
						if(fields.contains("Name"))
							fields.remove("Name");

						newItems.addAll(fields); // upcoming
						int midVal = 0;

						if(newItems.size()%2==0)
							midVal = newItems.size()/2;
						else
							midVal = newItems.size()/2+1;

						List<String> subListOne = newItems.subList(0, midVal);
						List<String> subListTwo = newItems.subList(midVal, newItems.size());

						for(String str: subListOne){
							LayoutItem li = new LayoutItem();
							li.setField(str);
							li.setBehavior(UiBehavior.Edit);
							leftLayoutItemsList.add(li);
						}

						for(String str: subListTwo){
							LayoutItem li = new LayoutItem();
							li.setField(str);
							rightLayoutItemsList.add(li);
						}
						LayoutColumn lColumn21 = new LayoutColumn();
						LayoutColumn lColumn22 = new LayoutColumn();
						lColumn21.setLayoutItems(leftLayoutItemsList.toArray(new LayoutItem[leftLayoutItemsList.size()]));
						lColumn22.setLayoutItems(rightLayoutItemsList.toArray(new LayoutItem[rightLayoutItemsList.size()]));
						leftLayoutItemsList.clear();
						rightLayoutItemsList.clear();

						LayoutSection ls1 = new LayoutSection();
						ls1.setLabel(""+sectionName);
						ls1.setCustomLabel(true);

						ls1.setDetailHeading(true);
						ls1.setEditHeading(true);
						ls1.setStyle(LayoutSectionStyle.TwoColumnsLeftToRight);
						ls1.setLayoutColumns(new LayoutColumn[]{lColumn21, lColumn22});
						sectionNameWithLayoutSection.put(sectionName, ls1);
					}

				} // END if(!sectionWithItems.containsKey(sectionName))

				/*********************<end>*Manipulations*</end>******************************/
				//				System.out.println("before setting the layout sectionNameWithLayoutSection.keySet is: "+sectionNameWithLayoutSection.keySet());
				ListMetadataQuery lmq = new ListMetadataQuery();
				lmq.setType("Layout");

				String layoutName = "";
				FileProperties[] lmr = mConnection.listMetadata(new ListMetadataQuery[] {lmq}, 27.0);
				for(FileProperties fp: lmr){
					if(fp.getFullName().split("\\-")[0].equalsIgnoreCase(""+sObjName)){
						layoutName = fp.getFullName();
					}
				}
				Layout lay1 = new Layout();
				lay1.setFullName(""+layoutName);
				lay1.setEmailDefault(true);
				lay1.setLayoutSections(sectionNameWithLayoutSection.values().toArray(new LayoutSection[sectionNameWithLayoutSection.size()]));
				sectionNameWithLayoutSection.clear();

				UpdateMetadata ut = new UpdateMetadata();
				ut.setCurrentName(""+layoutName);
				ut.setMetadata(lay1);
				AsyncResult[] asyncResultsupdate = mConnection.update(new UpdateMetadata[] {ut});  

				AsyncResult asyncResult = asyncResultsupdate[0];
				final long ONE_SECOND = 1000;
				final int MAX_NUM_POLL_REQUESTS = 50;
				int poll = 0;
				long waitTimeMilliSecs = ONE_SECOND;

				while (!asyncResult.isDone()) {
					Thread.sleep(waitTimeMilliSecs);
					waitTimeMilliSecs *= 2;
					if (poll++ > MAX_NUM_POLL_REQUESTS) {
						throw new Exception("Request timed out. If this is a large set of metadata components, check that the time allowed by MAX_NUM_POLL_REQUESTS is sufficient.");
					}
					asyncResult = mConnection.checkStatus(new String[] {asyncResult.getId()})[0];
					System.out.println("Status for Pagelayout is: " + asyncResult.getState());
				}
				if (asyncResult.getState() != AsyncRequestState.Completed) {        	
					System.out.println(asyncResult.getStatusCode() + " msg: " +	asyncResult.getMessage());
					
				}
				else{
					System.out.println("The Page Layout for sObject \'"+""+sObjName+"\' has been Updated.");
					System.out.println("The further information is: "+asyncResult.getId());
				}

				dsrArray = null;
				fields.clear();
				fields = null;
				dlResult = null;
			}

		}catch(Exception ex){
			ex.printStackTrace();
		}
	} // END udpateLayout()
	
	/**
	 * @param mConnection
	 * 					 mConnections is Salesforce MetadataConnection reference
	 * @param recordTypeTest
	 * 					    This is the String array containing the API names of the RecordTypes we want to retrieve the layout for.
	 * @param sObjName
	 * @param secName
	 * @param f
	 * @param isNameAutoNumber
	 * @param pConnection
	 * @throws Exception
	 */
	// Under Review
	public static void createLayout(MetadataConnection mConnection, String[] recordTypeTest, String sObjName, String secName, Set<String> f, boolean isNameAutoNumber, PartnerConnection pConnection) throws Exception{
		String sectionName = ""+secName;

		// The set coming as argument
		Set<String> fields = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		fields.addAll(f);
		Map<String, LayoutSection> sectionNameWithLayoutSection = new LinkedHashMap<String, LayoutSection>();
		/********************<start>*Manipulations*</start>********************************/
		// If the section needs to be created // it will also execute when there are no Layout Items in the existing section.

		List<LayoutItem> leftLayoutItemsList = new ArrayList<LayoutItem>();
		List<LayoutItem> rightLayoutItemsList = new ArrayList<LayoutItem>();
		List<String> newItems = new ArrayList<String>();
		if(fields.contains("Name"))
			fields.remove("Name");

		newItems.addAll(fields); // upcoming
		int midVal = 0;

		if(newItems.size()%2==0)
			midVal = newItems.size()/2;
		else
			midVal = newItems.size()/2+1;

		List<String> subListOne = newItems.subList(0, midVal);
		List<String> subListTwo = newItems.subList(midVal, newItems.size());

		for(String str: subListOne){
			LayoutItem li = new LayoutItem();
			li.setField(str);
			li.setBehavior(UiBehavior.Edit);
			leftLayoutItemsList.add(li);
		}

		for(String str: subListTwo){
			LayoutItem li = new LayoutItem();
			li.setField(str);
			rightLayoutItemsList.add(li);
		}
		LayoutColumn lColumn21 = new LayoutColumn();
		LayoutColumn lColumn22 = new LayoutColumn();
		lColumn21.setLayoutItems(leftLayoutItemsList.toArray(new LayoutItem[leftLayoutItemsList.size()]));
		lColumn22.setLayoutItems(rightLayoutItemsList.toArray(new LayoutItem[rightLayoutItemsList.size()]));
		leftLayoutItemsList.clear();
		rightLayoutItemsList.clear();

		LayoutSection ls1 = new LayoutSection();
		ls1.setLabel(""+sectionName);
		ls1.setCustomLabel(true);

		ls1.setDetailHeading(true);
		ls1.setEditHeading(true);
		ls1.setStyle(LayoutSectionStyle.TwoColumnsLeftToRight);
		ls1.setLayoutColumns(new LayoutColumn[]{lColumn21, lColumn22});
		sectionNameWithLayoutSection.put(sectionName, ls1);



		/*********************<end>*Manipulations*</end>******************************/
		//				System.out.println("before setting the layout sectionNameWithLayoutSection.keySet is: "+sectionNameWithLayoutSection.keySet());
		ListMetadataQuery lmq = new ListMetadataQuery();
		lmq.setType("Layout");

		String layoutName = sObjName+"-Testing";
		Layout lay1 = new Layout();
		lay1.setFullName(""+layoutName);
		lay1.setEmailDefault(true);
		lay1.setLayoutSections(sectionNameWithLayoutSection.values().toArray(new LayoutSection[sectionNameWithLayoutSection.size()]));
		sectionNameWithLayoutSection.clear();

		AsyncResult[] asyncResultsupdate = mConnection.create(new Metadata[] {lay1});  

		AsyncResult asyncResult = asyncResultsupdate[0];
		final long ONE_SECOND = 1000;
		final int MAX_NUM_POLL_REQUESTS = 50;
		int poll = 0;
		long waitTimeMilliSecs = ONE_SECOND;

		while (!asyncResult.isDone()) {
			Thread.sleep(waitTimeMilliSecs);
			waitTimeMilliSecs *= 2;
			if (poll++ > MAX_NUM_POLL_REQUESTS) {
				throw new Exception("Request timed out. If this is a large set of metadata components, check that the time allowed by MAX_NUM_POLL_REQUESTS is sufficient.");
			}
			asyncResult = mConnection.checkStatus(new String[] {asyncResult.getId()})[0];
			System.out.println("Status for Pagelayout is: " + asyncResult.getState());
		}
		if (asyncResult.getState() != AsyncRequestState.Completed) {        	
			System.out.println(asyncResult.getStatusCode() + " msg: " +	asyncResult.getMessage());
		}
		else{
			System.out.println("The Page Layout for sObject \'"+""+sObjName+"\' has been Created.");
		}

		fields.clear();
		fields = null;

	} // END createLayout()

	
	
	
} // END public class Utility
