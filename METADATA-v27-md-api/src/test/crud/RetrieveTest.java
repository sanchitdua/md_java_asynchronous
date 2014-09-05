package test.crud;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import metadata.Utility;
import metadata.crud.retrieve.RetrieveCallForSharingModel;

import org.apache.commons.io.FileUtils;

import partner.PartnerLoginUtil;

import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CustomField;
import com.sforce.soap.metadata.CustomObject;
import com.sforce.soap.metadata.FieldType;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.UpdateMetadata;
import com.sforce.soap.partner.PartnerConnection;

public class RetrieveTest {

	public static void main(String[] args) throws Exception{
		Set<String> objs = new HashSet<String>();
		objs.add("MyCustomObject__c");
		objs.add("MyCustomObject2__c");
		RetrieveCallForSharingModel.retrieveSharingInfo();
		metadata.MetadataLoginUtil mUtil = new metadata.MetadataLoginUtil();
		MetadataConnection mConnection = mUtil.login("YOUR_USERNAME", "YOUR_PASSWORD"+"YOUR_SECURITY_TOKEN");
		PartnerConnection p = new PartnerLoginUtil().login("YOUR_USERNAME", "YOUR_PASSWORD"+"YOUR_SECURITY_TOKEN");
		System.out
		.println("Retrieving the zipped archive of sObjects ...");
		File f = new File("Temp/mypackage.zip");
		if(f.exists()) {
			FileUtils.deleteQuietly(f);
		}
		if((new File("Temp")).mkdir() || true){
			if(! new File("Temp/mypackage.zip").exists() ) {
				if( new File("Temp/mypackage.zip").createNewFile() ){
					System.out
					.println("File created successfully, making the Retrieve() call ...");
					Utility.retrieveZip(mConnection, "resources/package.xml", 29.0); // <---- retrieve() Metadata Call															
				}
			}
		}
		Thread.sleep(5000);
		System.out
		.println("Retrieving the Sharing Model Information ...");
		Map<String, Map<String, String>> tempMap = new HashMap<String, Map<String,String>>();
		tempMap = Utility.parseZip("Temp/mypackage.zip", objs); // <---- parsing the zip file "sObject" contents 
		Thread.sleep(3000);
		System.out.println("TempMap is: "+tempMap);
		Map<String, CustomObject> sObjectWithSharingModel = Utility.createCustomObjectDetails(tempMap); // <---- Finally obtaining the details in sObjectName -> CustomObject (Metadata-api Class Name)
		//System.out
		//		.println("LatchProvider.executeFirst(), objNameWithRecordTypes: "+objNameWithRecordTypes);
		List<UpdateMetadata> udpateList = new ArrayList<UpdateMetadata>();
		// Let's add some custom fields to this object.
		for(String obj: objs) {
			CustomObject cobj = sObjectWithSharingModel.get(obj);
//			cobj.setLabel("MyLabel "+cobj.getLabel());
			CustomField[] cfArray= new CustomField[1];
			CustomField cf = new CustomField();
			cf.setType(FieldType.AutoNumber);
			cf.setDisplayFormat("A-{000}");
			cf.setStartingNumber(0);
//			cf.setLength(255);
			cf.setLabel(cobj.getLabel());
			cf.setFullName(obj);
			cfArray[0] = cf;
			cobj.setNameField(cf);
			UpdateMetadata um = new UpdateMetadata();
			um.setMetadata(cobj);
			udpateList.add(um);
		}
//		members.toArray(new String[members.size()])
		AsyncResult[] ur = mConnection.update(udpateList.toArray(new UpdateMetadata[udpateList.size()]));
		
		System.out.println("After issuing the update command.");
        final long ONE_SECOND = 1000;
		final int MAX_NUM_POLL_REQUESTS = 25;
		int poll = 0;
		long waitTimeMilliSecs = ONE_SECOND;
        for (AsyncResult ar : ur) {
			AsyncResult asyncResult = ar;
			while (!asyncResult.isDone()) {
				Thread.sleep(waitTimeMilliSecs);
				waitTimeMilliSecs *= 2;
				if (poll++ > MAX_NUM_POLL_REQUESTS) {
					throw new Exception(
							"Request timed out. If this is a large set of metadata components, check that the time allowed by MAX_NUM_POLL_REQUESTS is sufficient.");
				}
				asyncResult = mConnection
						.checkStatus(new String[] { asyncResult.getId() })[0];
				System.out.println("Status for object creation "+ar.getId()+" is: "
						+ asyncResult.getState());
			}
			if (asyncResult.getState() != AsyncRequestState.Completed) {
				System.out.println(asyncResult.getStatusCode() + " msg: "
						+ asyncResult.getMessage());
			} else {
				System.out
				.println("The object is successfully updated.");
			}
		}
		
        // This map is going to hold the FullName (i.e SobjectName and API name) of the RecordType with Lable name of the RecordType to be created
        Map<String, String> recordTypeMap = new HashMap<String, String>();
        recordTypeMap.put("MyCustomObject__c.super", "Super");
        recordTypeMap.put("MyCustomObject__c.Employee", "ADLT");
        recordTypeMap.put("MyCustomObject2__c.adlt", "ADLT");
        
        System.out.println("Creating Record Types ...");

        for(String obj: objs) {
			CustomObject cobj =sObjectWithSharingModel.get(obj);
			
			Set<String> recordTypes = new HashSet<String>();
			
			for(Map.Entry<String, String> entry: recordTypeMap.entrySet()) {
				if(entry.getKey().split("\\.")[0].equalsIgnoreCase(cobj.getFullName())) {
					recordTypes.add(entry.getKey().split("\\.")[1]);
				}
			}
			
			if(!recordTypes.isEmpty())
				Utility.createRecordTypes(cobj, recordTypes, false, mConnection, p);
		}
        
        
	} // END public static void main(String[] args) throws Exception

} // END public class RetrieveTest {
