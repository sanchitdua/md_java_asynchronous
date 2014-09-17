package partner;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
public class PartnerLoginUtil {

	public PartnerConnection login(String user, String pass, String authEndPoint){
		
		PartnerConnection p = null;
		try {
				p = loginToSalesforce(user, pass, authEndPoint);
		} 
		catch(NullPointerException npe){
			System.out.println(" Warning: Cannot make the Metadata Connection as the daily quota of 5,000 API calls for the SFDC account: "+user+" would have been used. Please wait until some calls become free.");
		}
		catch (Exception ce) {
			ce.printStackTrace();
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