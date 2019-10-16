package rotation;

import java.io.IOException;

import de.samply.auth.client.AuthClient;
import de.samply.auth.client.InvalidKeyException;
import de.samply.auth.client.InvalidTokenException;
import de.samply.auth.rest.LocationDTO;
import de.samply.auth.user.UserAuthentication;
import de.samply.auth.utils.OAuth2ClientConfig;
import de.samply.common.config.OAuth2Client;
import spark.Request;

/**
 * @author Sebastian Germer
 *
 */
public class CustomUser extends UserAuthentication {

	private static final long serialVersionUID = 1L;
	private LocationDTO location;

	private void reset() {
		setAccessToken(null);
		setIdToken(null);
		setRefreshToken(null);
		setLoginValid(false);
		setUserIdentity("");
		setRealName("");
		setUsername("");
		location = null;
	}

	@Override
	public boolean basicLogin(AuthClient client) throws InvalidTokenException, InvalidKeyException {
		if (super.basicLogin(client)) {
			location = this.getIdToken().getLocations().isEmpty() ? null : this.getIdToken().getLocations().get(0);
			return true;
		} else
			return false;
	}

	public String basicLogout(OAuth2Client config, Request req) throws IOException {
		reset();
		return OAuth2ClientConfig.getLogoutUrl(config, "http", "localhost", req.port(), "", "/");
	}

	public LocationDTO getLocation() {
		return location;
	}

	public void setLocation(LocationDTO location) {
		this.location = location;
	}

}