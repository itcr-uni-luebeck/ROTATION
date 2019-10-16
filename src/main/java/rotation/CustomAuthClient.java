package rotation;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.ws.rs.client.Client;

import org.slf4j.LoggerFactory;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;

import de.samply.auth.client.AuthClient;
import de.samply.common.config.OAuth2Client;

/**
 * @author Sebastian Germer
 *
 */
public class CustomAuthClient extends AuthClient {

	private static final String[] SUPPORTED_ALGORITHMS = new String[] { "RSA", "EC" };
	private static final Logger logger = LoggerFactory.getLogger(AuthClient.class);

	public CustomAuthClient(OAuth2Client config, String code, Client client) {

		super(config.getHost(), loadKey(config.getHostPublicKey().getBytes()), config.getClientId(),
				config.getClientSecret(), code, client);
	}

	private static PublicKey loadKey(byte[] encryptedKey) {
		try {
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decodeBase64(encryptedKey));

			for (String algorithm : SUPPORTED_ALGORITHMS) {
				try {
					KeyFactory kf = KeyFactory.getInstance(algorithm);
					return kf.generatePublic(keySpec);
				} catch (InvalidKeySpecException e) {
					/**
					 * Ignore, because we just try the next algorithm!
					 */
				}
			}
		} catch (NoSuchAlgorithmException e) {
			logger.error("Exception: ", e);
		}

		throw new UnsupportedOperationException("Unknown Key Format");
	}

}