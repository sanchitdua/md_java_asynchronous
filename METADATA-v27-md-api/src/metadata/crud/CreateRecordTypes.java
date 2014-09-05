package metadata.crud;


import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CustomField;
import com.sforce.soap.metadata.CustomObject;
import com.sforce.soap.metadata.DeploymentStatus;
import com.sforce.soap.metadata.FieldType;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.SharingModel;

public class CreateRecordTypes {
	
	private MetadataConnection metadataConnection;

	public static void main(String... str) throws Exception{
		CreateRecordTypes cof = new CreateRecordTypes();
		cof.runCreate();
	}
	
	private void runCreate() throws Exception {
		metadata.MetadataLoginUtil mUtil = new metadata.MetadataLoginUtil();
		metadataConnection = mUtil.login("YOUR_USERNAME", "YOUR_PASSWORD"+"YOUR_SECURITY_TOKEN");
        System.out.println("After successfully loggin in ... ");
        // Custom objects and fields must have __c suffix in the full name.
        final String uniqueObjectName = "MyCustomObject__c";
        createCustomObjectSync(uniqueObjectName);
    }
	
	private void createCustomObjectSync(final String uniqueName) throws Exception {
        final String label = "My Custom Object";
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

        AsyncResult[] results = metadataConnection.create(new Metadata[] { co });
        System.out.println("After issuing the create command.");
        final long ONE_SECOND = 1000;
		final int MAX_NUM_POLL_REQUESTS = 25;
		int poll = 0;
		long waitTimeMilliSecs = ONE_SECOND;
        for (AsyncResult ar : results) {
			AsyncResult asyncResult = ar;
			while (!asyncResult.isDone()) {
				Thread.sleep(waitTimeMilliSecs);
				waitTimeMilliSecs *= 2;
				if (poll++ > MAX_NUM_POLL_REQUESTS) {
					throw new Exception(
							"Request timed out. If this is a large set of metadata components, check that the time allowed by MAX_NUM_POLL_REQUESTS is sufficient.");
				}
				asyncResult = metadataConnection
						.checkStatus(new String[] { asyncResult.getId() })[0];
				System.out.println("Status for object creation "+ar.getId()+" is: "
						+ asyncResult.getState());
			}
			if (asyncResult.getState() != AsyncRequestState.Completed) {
				System.out.println(asyncResult.getStatusCode() + " msg: "
						+ asyncResult.getMessage());
			} else {
				System.out
				.println("The object is successfully created.");
			}
		}
    }
	
}