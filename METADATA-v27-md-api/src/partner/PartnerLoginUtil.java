package partner;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class PartnerLoginUtil {

	public PartnerConnection login(String user, String pass){
		
		PartnerConnection p = null;
		String authEndPoint = "";
			authEndPoint = "https://login.salesforce.com/services/Soap/u/27.0/";
		
		try {
				p = loginToSalesforce(user, pass, authEndPoint);
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
					p = loginToSalesforce(user, pass, authEndPoint);
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
		
		return p;
	}
	
	private static PartnerConnection loginToSalesforce( final String username, final String password, final String loginUrl) throws ConnectionException {
		final ConnectorConfig config = new ConnectorConfig();
		config.setAuthEndpoint(loginUrl);
		config.setUsername(username);
		config.setPassword(password);
		return new PartnerConnection(config);
	}
}