package metadata.crud;
import java.util.HashSet;
import java.util.Set;
import metadata.Utility;


import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;

import connection.ConnectionProvider;

public class CreateOrUpdateLayouts {
	
	public MetadataConnection mConnection;
	public PartnerConnection pConnection;
	public boolean isTest=false;

	public void updateLayoutItems() throws Exception {
		// Fields to update on Layout
		Set<String> fields = new HashSet<String>();
		fields.add("City__c");
		fields.add("ContactNo__c");
		fields.add("Email_Id__c");
		fields.add("Last_Name__c");
		mConnection = ConnectionProvider.getMetadataConnection();
		pConnection = ConnectionProvider.getPartnerConnection();
		
		Utility.updateLayout(mConnection, null, "MyCustomObject__c", "MY Section", fields, true, pConnection, isTest);
		
	} // END public void updateLayoutItems()
}