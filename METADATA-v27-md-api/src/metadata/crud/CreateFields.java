package metadata.crud;
import java.util.*;
import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CustomField;
import com.sforce.soap.metadata.CustomObject;
import com.sforce.soap.metadata.DeploymentStatus;
import com.sforce.soap.metadata.FieldType;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.Picklist;
import com.sforce.soap.metadata.PicklistValue;
import com.sforce.soap.metadata.SharingModel;
import connection.ConnectionProvider;

public class CreateFields {
	public MetadataConnection metadataConnection;
	public boolean isTest=false;
	
	public void runCreate() throws Exception {
		metadataConnection = ConnectionProvider.getMetadataConnection();
		System.out.println("After successfully loggin in ... ");
        // Custom objects and fields must have __c suffix in the full name.
        final String uniqueObjectName = "MyCustomObject__c";
        createCustomObjectSync(uniqueObjectName);
    } // END private void runCreate() throws Exception
	
	private void createCustomObjectSync(final String uniqueName) throws Exception {
        final String label = "My Custom Object 1";
        CustomObject co = new CustomObject();
        co.setFullName(uniqueName);
        co.setDeploymentStatus(DeploymentStatus.Deployed);
        co.setDescription("Created by Sanchit");
        co.setEnableActivities(true);
        co.setLabel(label);
        co.setPluralLabel(label + "s");
        co.setSharingModel(SharingModel.ReadWrite);
        // The name field appears in page layouts, related lists, and elsewhere.
        CustomField nf = new CustomField();
        nf.setType(FieldType.Text);
        nf.setDescription("The custom object identifier on page layouts, related lists etc");
        nf.setLabel(label);
        nf.setFullName(uniqueName);
        co.setNameField(nf);

        Set<String> fields = new HashSet<String>();
		fields.add("City__c");
		fields.add("ContactNo__c");
		fields.add("Email_Id__c");
		fields.add("Last_Name__c");
		
		List<CustomField> cfList = new ArrayList<CustomField>();
		
		for(String field: fields) {
			CustomField cf = new CustomField();
			cf.setType(FieldType.Text);
			cf.setLength(255);
			cf.setFullName("MyCustomObject__c."+field);
			cf.setLabel(field.replaceAll("__c", ""));
			cfList.add(cf);
		}
		
		Picklist myPickList = new Picklist();
		PicklistValue unsubmitted = new PicklistValue();
		unsubmitted.setFullName("Unsubmitted");
		PicklistValue submitted = new PicklistValue();
		submitted.setFullName("Submitted");
		PicklistValue approved = new PicklistValue();
		approved.setFullName("Approved");
		PicklistValue rejected = new PicklistValue();
		rejected.setFullName("Rejected");
		myPickList.setPicklistValues(new PicklistValue[] {
				unsubmitted, submitted, approved, rejected}
				);

		CustomField myPickListField = new CustomField();
		myPickListField.setFullName(
				"MyCustomObject.PickListField__c"
				);
		myPickListField.setLabel("Expense Report Status");
		myPickListField.setType(FieldType.Picklist);
		myPickListField.setPicklist(myPickList);
		cfList.add(myPickListField);
		final long ONE_SECOND = 1000;
		final int MAX_NUM_POLL_REQUESTS = 25;
		int poll = 0;
		long waitTimeMilliSecs = ONE_SECOND;
		AsyncResult[] results =null;
		if(!isTest){
	        results = metadataConnection.create(cfList.toArray(new Metadata[cfList.size()]));
			System.out.println("After issuing the create command.");
	        for (AsyncResult ar : results) {
				AsyncResult asyncResult = ar;
				while (!asyncResult.isDone()) {
					Thread.sleep(waitTimeMilliSecs);
					waitTimeMilliSecs *= 2;
					if (poll++ > MAX_NUM_POLL_REQUESTS) {
						throw new Exception("Request timed out. If this is a large set of metadata components, check that the time allowed by MAX_NUM_POLL_REQUESTS is sufficient.");
					}
					asyncResult = metadataConnection.checkStatus(new String[] { asyncResult.getId() })[0];
					System.out.println("Status for object creation "+ar.getId()+" is: "+ asyncResult.getState());
				}
				if (asyncResult.getState() != AsyncRequestState.Completed) {
					System.out.println(asyncResult.getStatusCode() + " msg: "+ asyncResult.getMessage());
				} else {
					System.out.println("The fields is successfully created.");
				}
			}
		}
    } // END private void createCustomObjectSync(final String uniqueName) throws Exception
}