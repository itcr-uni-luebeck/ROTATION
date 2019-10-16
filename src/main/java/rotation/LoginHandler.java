package rotation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import de.samply.auth.client.ClientUtil;
import de.samply.auth.client.InvalidKeyException;
import de.samply.auth.client.InvalidTokenException;
import de.samply.auth.client.jwt.JWTAccessToken;
import de.samply.auth.rest.Scope;
import de.samply.auth.utils.OAuth2ClientConfig;
import de.samply.common.config.OAuth2Client;
import spark.Request;
import org.apache.commons.configuration.CompositeConfiguration;

/**
 * @author Sebastian Germer
 *
 */
public class LoginHandler {

	private String link;
	private OAuth2Client config;
	private CustomUser user = new CustomUser();
	private CustomAuthClient client;
	private CompositeConfiguration compConfig = Configs.get();

	public LoginHandler() {
		config = new OAuth2Client();
		config.setHostPublicKey(compConfig.getString("auth.publickey"));
		config.setClientId(compConfig.getString("auth.clientid"));
		config.setClientSecret(compConfig.getString("auth.clientsecret"));
		config.setHost(compConfig.getString("auth.hosturl"));
	}

	public void login(Request req) {
		try {
			this.link = OAuth2ClientConfig.getRedirectUrl(config, "http", "localhost", req.port(), "", "/validateLogin",
					Scope.OPENID);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public String getLink() {
		return this.link;
	}

	public CustomUser getUser() {
		return user;
	}

	public boolean validate(String randomCode) {
		client = new CustomAuthClient(config, randomCode, ClientUtil.getClient());
		try {
			user.basicLogin(client);
			return user.getLoginValid();
		} catch (InvalidTokenException | InvalidKeyException e) {
			System.err.println("User isnt logged in");
			return false;
		}
	}

	public String logout(Request req) throws IOException {
		client = null;
		return user.basicLogout(config, req);
	}

	public boolean isLoggedIn() {
		if (client != null) {
			try {
				JWTAccessToken token = client.getAccessToken();
				client.getIDToken();
				return token.isValid();
			} catch (InvalidKeyException | InvalidTokenException e) {
				return false;
			}
		}
		return false;
	}
}
