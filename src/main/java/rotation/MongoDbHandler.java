package rotation;

import java.util.ArrayList;

import com.mongodb.BasicDBList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInvalidOperationException;
import org.bson.Document;
import org.hl7.fhir.utilities.xml.SchematronWriter.Rule;

/**
 * Class to upload Elements to MongoDB instance
 * 
 * @author Sebastian Germer
 */
public class MongoDbHandler {
	private MappingHandler map;
	private MongoDatabase db;

	/**
	 * Constructor
	 * 
	 * @param map
	 *            The {@link MappingHandler} whose elements are to be uploaded
	 * @param db
	 *            The {@link MongoDatabase} in which the elements should be
	 *            stored
	 */
	public MongoDbHandler(MappingHandler map, MongoDatabase db) {
		this.map = map;
		this.db = db;
	}

	/**
	 * Function to upload the generated {@link StructureMap} to a collection
	 * called "StructureMaps". Also uploads every {@link Rule} to a seperate
	 * collection called "Rules".
	 * 
	 */
	public void uploadStructureMap() {
		MongoCollection<Document> collection = db.getCollection("StructureMaps");
		BsonDocument bson = BsonDocument.parse(map.sMapToString());
		Document doc = new Document("StructureMap", bson);
		collection.insertOne(doc);
		collection = db.getCollection("Rules");
		BsonArray group = bson.getArray("group");
		for (int i = 0; i < group.size(); i++) {
			BsonDocument curGroup = group.get(i).asDocument();
			try {
				BsonArray rules = curGroup.getArray("rule");
				for (int j = 0; j < rules.size(); j++) {
					BsonDocument curRule = rules.get(j).asDocument();
					Document ruleDoc = new Document("Rule", curRule);
					BsonArray sources = curRule.getArray("source");
					ArrayList<String> sourceDescriptions = new ArrayList<>();
					for (int k = 0; k < sources.size(); k++) {
						BsonDocument curSource = sources.get(k).asDocument();
						sourceDescriptions.add(curSource.get("element").asString().getValue());
					}
					ArrayList<String> targetDescriptions = new ArrayList<>();
					BsonArray targets = curRule.getArray("target");
					for (int k = 0; k < targets.size(); k++) {
						BsonDocument curTarget = targets.get(k).asDocument();
						targetDescriptions.add(curTarget.get("element").asString().getValue());
					}
					ArrayList<ArrayList<Integer>> hashes = new MinHash().calcHash(sourceDescriptions,
							targetDescriptions);
					BasicDBList dbhashSource = new BasicDBList();
					dbhashSource.addAll(hashes.get(0));
					BasicDBList dbhashTarget = new BasicDBList();
					dbhashTarget.addAll(hashes.get(1));
					ruleDoc.append("HashSource", dbhashSource);
					ruleDoc.append("HashTarget", dbhashTarget);

					collection.insertOne(ruleDoc);

				}
			} catch (BsonInvalidOperationException e) {
				System.out.println("No rules in this group!");
			}
		}
	}

	/**
	 * Function to upload the updated {@link ConceptMap} to a collection called
	 * "ConceptMaps".
	 */
	public void uploadConceptMap() {
		MongoCollection<Document> collection = db.getCollection("ConceptMaps");
		BsonDocument bson = BsonDocument.parse(map.cMapToString());
		Document doc = new Document("ConceptMap", bson);
		collection.insertOne(doc);
	}

}