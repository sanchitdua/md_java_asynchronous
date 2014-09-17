package metadata;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class MetadataLoginUtil {

	public MetadataConnection login(String user, String pass, String authEndPoint){
		MetadataConnection mc = null;
		try {
				final LoginResult loginResult = loginToSalesforce(user, pass, authEndPoint);
				mc = createMetadataConnection(loginResult);
		} 
		catch(NullPointerException npe){
			System.out.println(" Warning: Cannot make the Metadata Connection as the daily quota of 5,000 API calls for the SFDC account: "+user+" would have been used. Please wait until some calls become free.");
		}
		
		catch (Exception ce) {
			ce.printStackTrace();
		} 
		return mc;
	}
	
	private static MetadataConnection createMetadataConnection(final LoginResult loginResult) throws ConnectionException {
		final ConnectorConfig config = new ConnectorConfig();
		config.setServiceEndpoint(loginResult.getMetadataServerUrl());
		config.setSessionId(loginResult.getSessionId());
		return new MetadataConnection(config);
	}

	private static LoginResult loginToSalesforce( final String username, final String password, final String loginUrl) throws ConnectionException {
		final ConnectorConfig config = new ConnectorConfig();
		config.setAuthEndpoint(loginUrl);
		config.setServiceEndpoint(loginUrl);
		config.setManualLogin(true);
		return (new EnterpriseConnection(config)).login(username, password);
	}
}