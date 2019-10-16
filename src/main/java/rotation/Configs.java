package rotation;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

import ca.uhn.fhir.context.FhirContext;

/**
 * @author Hannes Ulrich
 */
public class Configs {

	public static Configs instance = null;
	public static FhirContext context = null;
	public static CompositeConfiguration config = null;

	private Configs() {

		context = FhirContext.forR4();
		config = new CompositeConfiguration();
		config.addConfiguration(new SystemConfiguration());

		try {
			config.addConfiguration(new PropertiesConfiguration("application.properties"));
		} catch (ConfigurationException e) {
			System.err.println("application.properties was not found!");
			System.exit(0);
		}
	}

	public static CompositeConfiguration get() {
		if (instance == null) {
			instance = new Configs();
		}
		return config;
	}
	
	public static FhirContext getFHIRContext() {
		if (instance == null) {
			instance = new Configs();
		}
		return context;
	}
}
