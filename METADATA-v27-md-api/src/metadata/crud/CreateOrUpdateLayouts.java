package metadata.crud;


import java.util.HashSet;
import java.util.Set;

import metadata.MetadataLoginUtil;
import metadata.Utility;
import partner.PartnerLoginUtil;

import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;

public class CreateOrUpdateLayouts {
	
	private MetadataConnection metadataConnection;

	public static void main(String... str) throws Exception{
		CreateOrUpdateLayouts cof = new CreateOrUpdateLayouts();
		cof.updateLayoutItems();
	}
	public void updateLayoutItems() throws Exception {
		// Fields to update on Layout
		Set<String> fields = new HashSet<String>();
		fields.add("City__c");
		fields.add("ContactNo__c");
		fields.add("Email_Id__c");
		fields.add("Last_Name__c");
		MetadataConnection mConnection = new MetadataLoginUtil().login("df14@force.com", "testing123"+"DgRyd4WDqUIQOQNTbrMl23PPz");
		PartnerConnection pConnection = new PartnerLoginUtil().login("df14@force.com", "testing123"+"DgRyd4WDqUIQOQNTbrMl23PPz");
		
		Utility.updateLayout(mConnection, null, "MyCustomObject__c", "MY Section", fields, true, pConnection);
		
	} // END public void updateLayoutItems()
}