package rotation;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

/**
 * @author Hannes Ulrich
 */
public class Configs {

	public static Configs instance = null;
	public static CompositeConfiguration config = null;

	private Configs() {

		config = new CompositeConfiguration();
		config.addConfiguration(new SystemConfiguration());

		try {
			config.addConfiguration(new PropertiesConfiguration("application.properties"));
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public static CompositeConfiguration get() {
		if (instance == null) {
			instance = new Configs();
		}
		return config;
	}
}
