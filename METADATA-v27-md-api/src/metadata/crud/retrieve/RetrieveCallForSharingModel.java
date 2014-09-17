package metadata.crud.retrieve;

import java.util.*;

import metadata.Utility;

public class RetrieveCallForSharingModel {
	public static void retrieveSharingInfo() throws Exception{
		Set<String> objs = new HashSet<String>();
		objs.add("MyCustomObject__c");
		objs.add("MyCustomObject2__c");
		Utility.createMenifest(objs); // Employee and Organization have record types
	}
}
