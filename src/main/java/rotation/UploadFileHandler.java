package rotation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Provenance;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import spark.Request;
import static spark.Spark.halt;

/**
 * Class to process uploaded {@link ConceptMap}
 * 
 * @author Sebastian Germer
 */
public class UploadFileHandler {
	private ConceptMap conceptMap;

	public UploadFileHandler(Request req) throws IOException, ServletException, DataFormatException {
		FhirContext context = FhirContext.forR4();
		IParser parser = context.newJsonParser();
		String result;
		if (req.contentType().contains("multipart/form-data")) {
			req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
			if (req.raw().getPart("File").getContentType().contains("json")) {
				InputStream is = req.raw().getPart("File").getInputStream();
				result = new BufferedReader(new InputStreamReader(is, "utf-8")).lines().parallel()
						.collect(Collectors.joining("\n"));
			} else {
				halt(405, "Wrong data format, please use JSON!");
				result = "";
			}

		} else {
			result = req.body();
		}
		try {
			this.conceptMap = ((ConceptMap) parser.parseResource(result));
			accessProvenance(req);
		} catch (DataFormatException e) {
			throw e;
		}

	}

	private void accessProvenance(Request req) throws IOException {
		int lastIndex = conceptMap.getUrl().indexOf("ConceptMap");
		if (lastIndex == -1)
			return;
		String baseUrl = conceptMap.getUrl().substring(0, lastIndex);
		FhirContext context = FhirContext.forR4();
		IGenericClient client = context.newRestfulGenericClient(baseUrl);
		Bundle bundle = client.search().forResource(Provenance.class).where(Provenance.TARGET.hasId(conceptMap.getId()))
				.returnBundle(Bundle.class).execute();
		if (bundle.isEmpty())
			return;
		req.session().attribute("prevProvenance", ((Provenance) bundle.getEntryFirstRep().getResource()).getId());

	}

	/**
	 * Function to build a String consisting only of relevant elements of the
	 * ConceptMap
	 * 
	 * @return The compromised String
	 */
	public String parseToString() {
		String doc = buildLine("Name", conceptMap.getName()) + buildLine("Publisher", conceptMap.getPublisher())
				+ "<br>";
		for (ConceptMap.ConceptMapGroupComponent i : conceptMap.getGroup()) {
			doc += buildLine("GroupId", i.getId());
			doc += buildLine("<b>Source</b>", i.getSource());
			doc += buildLine("<b>Target</b>", i.getTarget()) + "<br>";
			for (ConceptMap.SourceElementComponent j : i.getElement()) {
				doc += buildLine("SourceName", j.getDisplay());
				for (ConceptMap.TargetElementComponent k : j.getTarget()) {
					doc += buildLine("TargetName", k.getDisplay());
					doc += buildLine("Equivalence", k.getEquivalence().getDisplay());

				}
				doc += "<br>";

			}

		}
		return doc;
	}

	/**
	 * Convert the {@link ConceptMap} to a corresponding String
	 * 
	 * @return The corresponding String
	 */
	public String jsonAsString() {
		FhirContext context = FhirContext.forR4();
		IParser parser = context.newJsonParser();
		return parser.encodeResourceToString(conceptMap);
	}

	/**
	 * Function to build a {@link String} consisting of a type and the
	 * corresponding value
	 * 
	 * @param type
	 *            The type of the element
	 * @param input
	 *            The value of the element
	 * @return The assembled String
	 */
	private static String buildLine(String type, String input) {
		return input != null ? type + ": " + input + "<br>\r\n" : "";
	}

	/**
	 * Getter for the ConceptMap
	 * 
	 * @return The {@link ConceptMap}
	 */
	public ConceptMap getConceptMap() {
		return conceptMap;
	}

}
