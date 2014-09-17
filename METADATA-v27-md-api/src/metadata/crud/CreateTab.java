package metadata.crud;
import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CustomTab;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;

import connection.ConnectionProvider;

public class CreateTab {
	public MetadataConnection metadataConnection;
	public boolean isTest= false;

	public void creatingTab() throws Exception{
		metadataConnection = ConnectionProvider.getMetadataConnection();
		// TODO Auto-generated method stub
		CustomTab myTab = new CustomTab();
		myTab.setFullName("MyCustomObject__c");
		myTab.setDescription("A Tab for MyCustomObject__c");
		myTab.setMotif("Custom70: Handsaw");
		myTab.setCustomObject(true);
		final long ONE_SECOND = 1000;
		final int MAX_NUM_POLL_REQUESTS = 25;
		int poll = 0;
		long waitTimeMilliSecs = ONE_SECOND;
		AsyncResult[] arsTab =null;
		if(!isTest){
			arsTab = metadataConnection.create(new Metadata[] {myTab}); // creating Tab
			System.out.println("After issuing the create command.");
			for (AsyncResult ar : arsTab) {
				AsyncResult asyncResult = ar;
				while (!asyncResult.isDone()) {
					Thread.sleep(waitTimeMilliSecs);
					waitTimeMilliSecs *= 2;
					if (poll++ > MAX_NUM_POLL_REQUESTS) {
						throw new Exception("Request timed out. If this is a large set of metadata components, check that the time allowed by MAX_NUM_POLL_REQUESTS is sufficient.");
					}
					asyncResult = metadataConnection.checkStatus(new String[] { asyncResult.getId() })[0];
					System.out.println("Status for Tab creation "+ar.getId()+" is: "+ asyncResult.getState());
				}
				if (asyncResult.getState() != AsyncRequestState.Completed) {
					System.out.println(asyncResult.getStatusCode() + " msg: "+ asyncResult.getMessage());
				} else {
					System.out.println("The Tab is successfully created.");
				}
			}
		}//END if(!isTest)
	}//END public void creatingTab() throws Exception
}