package rotation;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.r4.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.r4.model.ConceptMap.TargetElementComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Provenance.ProvenanceAgentComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.model.StructureMap.StructureMapGroupComponent;
import org.hl7.fhir.r4.model.StructureMap.StructureMapGroupInputComponent;
import org.hl7.fhir.r4.model.StructureMap.StructureMapGroupRuleComponent;
import org.hl7.fhir.r4.model.StructureMap.StructureMapGroupRuleSourceComponent;
import org.hl7.fhir.r4.model.StructureMap.StructureMapGroupRuleTargetComponent;
import org.hl7.fhir.r4.model.StructureMap.StructureMapGroupRuleTargetParameterComponent;
import org.hl7.fhir.r4.model.StructureMap.StructureMapInputMode;
import org.hl7.fhir.r4.model.StructureMap.StructureMapTransform;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import spark.Request;
import spark.Response;

/**
 * Class to generate a StructureMap from {@link ConceptMap} and single
 * transformations
 * 
 * @author Sebastian Germer, Hannes Ulrich
 */
public class MappingHandler {
	/**
	 * A {@link HashMap} containing the single rules
	 */
	private HashMap<String, String> mappingMap;
	private StructureMap sMap;
	private ConceptMap updatedCMap;
	private Provenance prov;
	private Practitioner user;
	private Bundle resourceBundle;

	/**
	 * Constructor
	 * 
	 * @param mappingString
	 *            The mappings which have been created
	 * @param cm
	 *            The underlying {@link ConceptMap} as String
	 * @param lHandler
	 * @param req
	 */
	public MappingHandler(String mappingString, ConceptMap cm, LoginHandler lHandler, Request req) {
		Gson gson = new Gson();
		Type mappingType = new TypeToken<HashMap<String, String>>() {}.getType();
		HashMap<String, String> mapping = gson.fromJson(mappingString, mappingType);
		setMappingMap(mapping);
		generateStructureMap(cm);
		generateProvenance(lHandler, req);
		generateBundle();
	}

	private void generateProvenance(LoginHandler lHandler, Request req) {
		Provenance prov = new Provenance();
		ProvenanceAgentComponent agent = new ProvenanceAgentComponent();
		prov.addAgent(agent);
		prov.addTarget(new Reference("StructureMap/#structureMapId"));
		prov.setRecorded(new Date());
		Practitioner user = new Practitioner();
		user.setId("#pracId");
		user.addName(new HumanName().setText(lHandler.getUser().getRealName()));
		user.addTelecom(
				new ContactPoint().setValue(lHandler.getUser().getUsername()).setSystem(ContactPointSystem.EMAIL));
		setUser(user);
		agent.setWho(new Reference("Practitioner/#pracId"));

		if (req.session().attribute("prevProvenance") != null) {
			String provId = req.session().attribute("prevProvenance");
			Extension ext = new Extension();
			ext.setUrl("http://hl7.org/fhir/StructureDefinition/event-eventHistory");
			ext.setValue(new StringType(provId));
			prov.addExtension(ext);
		}
		setProv(prov);
	}

	private void generateBundle() {
		Bundle bundle = new Bundle();
		BundleEntryComponent entry1 = new BundleEntryComponent();
		entry1.setResource(getsMap());
		BundleEntryRequestComponent requestType = new BundleEntryRequestComponent();
		requestType.setMethod(HTTPVerb.POST);
		entry1.setRequest(requestType);
		bundle.addEntry(entry1);

		BundleEntryComponent entry2 = new BundleEntryComponent();
		entry2.setResource(getUser());
		entry2.setRequest(requestType);
		bundle.addEntry(entry2);

		BundleEntryComponent entry3 = new BundleEntryComponent();
		entry3.setResource(getProv());
		entry3.setRequest(requestType);
		bundle.addEntry(entry3);
		setResourceBundle(bundle);

	}

	/**
	 * Getter for the MappingMap
	 * 
	 * @return the MappingMap
	 */
	public HashMap<String, String> getMappingMap() {
		return mappingMap;
	}

	/**
	 * Setter for the MappingMap
	 * 
	 * @param mappingMap
	 */
	public void setMappingMap(HashMap<String, String> mappingMap) {
		this.mappingMap = mappingMap;
	}

	/**
	 * Function to generate the {@link StructureMap} from the given data
	 * 
	 * @param cm
	 *            the underlying ConceptMap as String
	 */
	public void generateStructureMap(ConceptMap cMap) {
		StructureMap sMap = new StructureMap();
		int groupNumber = 0;
		sMap.setName("Mapping from " + cMap.getSourceUriType().asStringValue() + " to "
				+ cMap.getTargetUriType().asStringValue());
		sMap.setId("#structureMapId");
		sMap.setUrl("StructureMap/#structureMapId");

		sMap.setStatus(PublicationStatus.ACTIVE);

		for (ConceptMapGroupComponent group : cMap.getGroup()) {
			int elementNumber = 0;
			StructureMapGroupComponent sMapGroup = new StructureMapGroupComponent();
			sMapGroup.getInputFirstRep().setMode(StructureMapInputMode.SOURCE);
			sMapGroup.getInputFirstRep().setName("Source");
			sMapGroup.addInput(
					new StructureMapGroupInputComponent().setMode(StructureMapInputMode.TARGET).setName("Target"));
			ArrayList<SourceElementComponent> elementsToRemove = new ArrayList<SourceElementComponent>();
			for (SourceElementComponent element : group.getElement()) {
				System.out.println(this.mappingMap.get("Mapping(" + groupNumber + "," + elementNumber + ")"));
				// if element was not dismissed, create new rule
				if (!this.mappingMap.get("Mapping(" + groupNumber + "," + elementNumber + ")").equals("DISMISSED")) {
					StructureMapGroupRuleComponent rule = new StructureMapGroupRuleComponent();
					String sourceUri = element.getCode();
					String sourceName = element.getDisplay();
					StructureMapGroupRuleSourceComponent sourceComponent = new StructureMapGroupRuleSourceComponent();
					sourceComponent.setContext(sourceUri);
					sourceComponent.setElement(sourceName);

					rule.addSource(sourceComponent);
					for (TargetElementComponent target : element.getTarget()) {
						StructureMapGroupRuleTargetComponent targetComponent = new StructureMapGroupRuleTargetComponent();
						targetComponent.setContext(target.getCode());
						targetComponent.setElement(target.getDisplay());
						targetComponent.setTransform(StructureMapTransform.CAST);
						StructureMapGroupRuleTargetParameterComponent parameterComponent = new StructureMapGroupRuleTargetParameterComponent();

						parameterComponent.setValue(new StringType(this.mappingMap
								.get("Mapping(" + groupNumber + "," + elementNumber + ")").replaceAll("\"", "'")));
						targetComponent.addParameter(parameterComponent);
						rule.addTarget(targetComponent);
					}
					sMapGroup.addRule(rule);
				}
				// else delete element from ConceptMap
				else {
					elementsToRemove.add(element);
				}

				elementNumber++;

			}
			sMap.addGroup(sMapGroup);
			group.getElement().removeAll(elementsToRemove);

			groupNumber++;
		}

		setsMap(sMap);
		setUpdatedCMap(cMap);

	}

	/**
	 * Function to convert the {@link StructureMap} object to a {@link String}
	 * 
	 * @return The parsed {@link String}
	 */
	public String SMapToString() {
		FhirContext context = FhirContext.forR4();
		IParser parser = context.newJsonParser();
		return parser.encodeResourceToString(getsMap());
	}

	/**
	 * Function to convert the {@link Bundle} object to a {@link String}
	 * 
	 * @return The parsed {@link String}
	 */
	public String BundleToString() {
		FhirContext context = FhirContext.forR4();
		IParser parser = context.newJsonParser();
		return parser.encodeResourceToString(getResourceBundle());
	}

	/**
	 * Function to convert the {@link ConceptMap} object to a {@link String}
	 * 
	 * @return The parsed {@link String}
	 */
	public String CMapToString() {
		FhirContext context = FhirContext.forR4();
		IParser parser = context.newJsonParser();
		return parser.encodeResourceToString(getUpdatedCMap());
	}

	/**
	 * Setter for the {@link StructureMap}
	 * 
	 * @param sMap
	 */
	public void setsMap(StructureMap sMap) {
		this.sMap = sMap;
	}

	/**
	 * Getter for the {@link StructureMap}
	 * 
	 * @return The {@link StructureMap}
	 */
	public StructureMap getsMap() {
		return sMap;
	}

	/**
	 * Getter for the updated {@link ConceptMap}
	 * 
	 * @return The {@link ConceptMap}
	 */
	public ConceptMap getUpdatedCMap() {
		return updatedCMap;
	}

	/**
	 * Setter for the updated {@link ConceptMap}
	 * 
	 * @param updatedCMap
	 */
	public void setUpdatedCMap(ConceptMap updatedCMap) {
		this.updatedCMap = updatedCMap;
	}

	/**
	 * Function to write the content of the {@link Bundle} in the Server
	 * Response
	 * 
	 * @param res
	 *            The {@link Response} to write in
	 * @return The written {@link} HttpServletResponse}
	 */
	public HttpServletResponse createLocalFile(Response res) {

		byte[] outputBytes = this.BundleToString().getBytes();
		try {
			ServletOutputStream outputStream = res.raw().getOutputStream();
			outputStream.write(outputBytes);
			outputStream.flush();
			outputStream.close();
			return res.raw();
		} catch (IOException e) {
			res.status(500);
			res.body("Fehler in Dateigenerierung!");
			return res.raw();
		}

	}

	public Provenance getProv() {
		return prov;
	}

	private void setProv(Provenance prov) {
		this.prov = prov;
	}

	public Practitioner getUser() {
		return user;
	}

	private void setUser(Practitioner user) {
		this.user = user;
	}

	public Bundle getResourceBundle() {
		return resourceBundle;
	}

	private void setResourceBundle(Bundle resourceBundle) {
		this.resourceBundle = resourceBundle;
	}

}