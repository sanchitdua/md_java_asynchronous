package partner;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;

public class CreateRecord {

	public static void main(String[] args) throws Exception{
		PartnerConnection p = new PartnerLoginUtil().login("df14@force.com", "testing123"+"DgRyd4WDqUIQOQNTbrMl23PPz");
		
		SObject sobj = new SObject();
		sobj.setType("MyCustomObject__c");
		sobj.addField("name", "Name");
		sobj.addField("City__c", "Delhi");
		sobj.addField("ContactNo__c", "999999999");
		sobj.addField("Email_Id__c", "sdua@astreait.com");
		sobj.addField("Last_Name__c", "dua");
//		sobj.addField("id", "a0090000016LuUdAAK");
		
		SaveResult[] sr = p.create(new SObject[]{sobj});
		
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
