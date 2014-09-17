package partner;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;

import connection.ConnectionProvider;

public class CreateRecord {
	public PartnerConnection pConnection = null;
	public boolean isTest=false;
	
	public void RecCreate() throws Exception{
		pConnection = ConnectionProvider.getPartnerConnection();
		SObject sobj = new SObject();
		sobj.setType("MyCustomObject__c");
		sobj.addField("name", "Name");
		sobj.addField("City__c", "Delhi");
		sobj.addField("ContactNo__c", "999999999");
		sobj.addField("Email_Id__c", "sdua@astreait.com");
		sobj.addField("Last_Name__c", "dua");
		SaveResult[] sr= null;
		if(!isTest)
			sr = pConnection.create(new SObject[]{sobj});
		if(sr==null){
			System.out.println("Error: the SaveResult is null.");
			return;
		}		
		for(SaveResult s: sr) {
			if(s.getSuccess())
				System.out.println("The record is successfully created with id: "+s.getId());
			else
				System.out.println("There got some error creating a record. Error: "+s.getErrors()[0].getMessage());
		}
	}
}
