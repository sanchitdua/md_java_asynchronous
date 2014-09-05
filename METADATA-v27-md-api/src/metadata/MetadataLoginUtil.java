package metadata;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class MetadataLoginUtil {

	public MetadataConnection login(String user, String pass){
		
		MetadataConnection mc = null;
		String authEndPoint = "";
			authEndPoint = "https://login.salesforce.com/services/Soap/c/27.0/";
		
		try {
				final LoginResult loginResult = loginToSalesforce(user, pass, authEndPoint);
				mc = createMetadataConnection(loginResult);
		} 
		catch(NullPointerException npe){
			System.out.println(" Warning: Cannot make the Metadata Connection as the daily quota of 5,000 API calls for the SFDC account: "+user+" would have been used. Please wait until some calls become free.");
		}
		
		catch (ConnectionException ce) {
			ce.printStackTrace();
			final long ONE_SECOND = 1000;
			final int MAX_NUM_POLL_REQUESTS = 20;
			int poll = 0;
			long waitTimeMilliSecs = ONE_SECOND;
			boolean isDone = false;
			
			int counter = 1;
			
			while(!isDone){
				counter++;
				System.out.println("Attempting to connect "+counter+" times using Metadata Connection...");
				try{
					Thread.sleep(waitTimeMilliSecs);
					waitTimeMilliSecs *= 2;
					
					if (poll++ > MAX_NUM_POLL_REQUESTS) {
						throw new Exception("Request timed out. Check that the time allowed by MAX_NUM_POLL_REQUESTS is sufficient.");
					}
					
					
					final LoginResult loginResult = loginToSalesforce(user, pass, authEndPoint);
					mc = createMetadataConnection(loginResult);
					isDone = true;
					
				}catch(ConnectionException connEx){
					connEx.printStackTrace();
					if(!connEx.getMessage().startsWith("Failed to send request to "))
						isDone = true;
				}
				catch(InterruptedException ie){
					ie.printStackTrace();
				}
				catch(Exception ex){
					ex.printStackTrace();
				}
			}
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