package rotation;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Consumer;

import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.Document;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.ConceptMap.TargetElementComponent;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;

/**
 * SuggestionHandler
 * 
 * @author Sebastian Germer, Hannes Ulrich
 */
public class SuggestionHandler {

	private ConceptMap cm;
	private int group;
	private int elem;
	private MongoDatabase database;

	/**
	 * Constructor for the SuggestionHandler
	 * 
	 * @param cm
	 *            Current ConceptMap
	 * @param group
	 *            Current group
	 * @param elem
	 *            Current element
	 * @param database
	 *            The database with the existing rules
	 */
	public SuggestionHandler(ConceptMap cm, int group, int elem, MongoDatabase database) {
		this.cm = cm;
		this.group = group;
		this.elem = elem;
		this.database = database;

	}

	/**
	 * Method to find the "closest" rule.
	 * 
	 * @return A "similar" rule (if there is one) or an empty string.
	 */
	public String find() {
		// Case 1: Urns match
		String sourceUrn = cm.getGroup().get(group).getElement().get(elem).getCode();
		ArrayList<String> targetUrns = new ArrayList<>();
		for (TargetElementComponent t : cm.getGroup().get(group).getElement().get(elem).getTarget()) {
			targetUrns.add(t.getCode());
		}
		FindIterable<Document> selection = database.getCollection("Rules").find(eq("Rule.source.context", sourceUrn));// .forEach(printBlock);
		
		for (String t : targetUrns) {
			selection.filter(eq("Rule.target.context", t));
		}
		
		BsonDocument rules = selection.first() != null ? (BsonDocument.parse(selection.first().toJson())) : null;
		if (rules != null) {
			return rules.getDocument("Rule").getArray("target").get(0).asDocument().getArray("parameter").get(0)
					.asDocument().get("valueString").asString().getValue();
		}
		// Case 2: Descriptions are similar
		else {
			ArrayList<String> sourceDescriptions = new ArrayList<>();
			sourceDescriptions.add(cm.getGroup().get(group).getElement().get(elem).getDisplay());
			ArrayList<String> targetDescriptions = new ArrayList<>();
			for (TargetElementComponent t : cm.getGroup().get(group).getElement().get(elem).getTarget()) {
				targetDescriptions.add(t.getDisplay());
			}
			MinHash minHash = new MinHash();
			ArrayList<ArrayList<Integer>> hashes = minHash.calcHash(sourceDescriptions, targetDescriptions);
			ArrayList<Integer> hashesSource = hashes.get(0);
			ArrayList<Integer> hashesTarget = hashes.get(1);
			ArrayList<BsonDocument> results = new ArrayList<BsonDocument>();
			Consumer<Document> action = doc -> {
				BsonDocument bdoc = BsonDocument.parse(doc.toJson());
				ArrayList<Integer> docHashesSource = new ArrayList<>();
				(bdoc.getArray("HashSource").getValues()).forEach(entry -> {
					int entryInt = entry.asInt32().getValue();
					docHashesSource.add(entryInt);
				});
				ArrayList<Integer> docHashesTarget = new ArrayList<>();
				(bdoc.getArray("HashTarget").getValues()).forEach(entry -> {
					int entryInt = entry.asInt32().getValue();
					docHashesTarget.add(entryInt);
				});

				double jaccard1 = minHash.calcJaccardSim(docHashesSource, hashesSource);
				double jaccard2 = minHash.calcJaccardSim(docHashesTarget, hashesTarget);
				double sim = (jaccard1 + jaccard2) / 2.0;

				if (sim > 0.5) {
					bdoc.append("sim", new BsonDouble(sim));
					results.add(bdoc);
				}
			};

			FindIterable<Document> HashSelection = database.getCollection("Rules").find();
			HashSelection.forEach(action);
			if (!results.isEmpty()) {
				Collections.sort(results, new Comparator<BsonDocument>() {
					@Override
					public int compare(BsonDocument first, BsonDocument second) {
						return Double.compare(first.getDouble("sim").getValue(), second.getDouble("sim").getValue());
					}
				});
				return results.get(0).getDocument("Rule").getArray("target").get(0).asDocument().getArray("parameter")
						.get(0).asDocument().get("valueString").asString().getValue();

			} else {
				return "nosuggestion";
			}
		}
	}
}