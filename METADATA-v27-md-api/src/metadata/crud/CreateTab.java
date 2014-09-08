package metadata.crud;

import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CustomTab;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;

public class CreateTab {

	public static void main(String[] args) throws Exception{
		MetadataConnection metadataConnection = null;
		metadata.MetadataLoginUtil mUtil = new metadata.MetadataLoginUtil();
		metadataConnection = mUtil.login("df14@force.com", "testing123"+"DgRyd4WDqUIQOQNTbrMl23PPz");

		// TODO Auto-generated method stub
		CustomTab myTab = new CustomTab();
		myTab.setFullName("MyCustomObject__c");
		myTab.setMotif("Custom70: Handsaw");
		myTab.setCustomObject(true);
		AsyncResult[] arsTab =
				metadataConnection.create(new Metadata[] {myTab});
	}

}
