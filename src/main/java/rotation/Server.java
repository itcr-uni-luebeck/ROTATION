package rotation;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;
import static spark.Spark.before;
import static spark.Spark.halt;
import spark.ModelAndView;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.ConceptMap;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;


/**
 * Server class of the application, contains all paths
 * 
 * @author Sebastian Germer, Hannes Ulrich
 */
public class Server {

	public static void main(String[] args) throws InterruptedException, IOException {
		// Location of the frontend files
		staticFileLocation("/rotation");

		// Connection to MongoDB
		String uri = Configs.get().getString("mongo.url");
		MongoClient mongoClient = MongoClients.create(uri);
		MongoDatabase database = mongoClient.getDatabase(Configs.get().getString("mongo.database"));
		LoginHandler lHandler = new LoginHandler();
		Supplier<HashMap<String, String>> hm = () -> {
			HashMap<String, String> hashm = new HashMap<>();
			hashm.put("Name", lHandler.getUser().getRealName());
			return hashm;
		};

		before("/upload", (req, res) -> {
			if (!lHandler.isLoggedIn()) {
				lHandler.login(req);
				res.redirect(lHandler.getLink());
				return;
			}
		});

		before("/protected/*", (req, res) -> {
			if (!lHandler.isLoggedIn()) {
				lHandler.login(req);
				res.redirect(lHandler.getLink());
				return;
			}
		});
		get("/", (req, res) -> {
			res.redirect("/upload");
			return "";
		});

		get("/upload", (req, res) -> {
			res.cookie("Username", lHandler.getUser().getUsername());
			return new ThymeleafTemplateEngine().render(new ModelAndView(hm.get(), "../WEB-INF/upload"));
		});
		
		get("/page", (req, res) -> new ModelAndView(new HashMap<String, String>(), "../WEB-INF/page"),
				new ThymeleafTemplateEngine());
		
		get("/validateLogin", (req, res) -> {
			String randomCode = req.queryParams("code");
			if (lHandler.validate(randomCode)) {
				res.redirect("/upload");
				return "";
			} else {
				halt("No access");
				return "";
			}

		});

		// Path which is requested after the ConceptMap has been submitted
		post("/parseFile", (req, res) -> {
			res.type("text/html");

			UploadFileHandler fileHandler = new UploadFileHandler(req);
			req.session().attribute("ConceptMap", fileHandler);
			return fileHandler.parseToString();

		});
		// Path which returns the currently used ConceptMap
		get("/loadJson", (req, res) -> {
			res.type("application/json");
			UploadFileHandler fileHandler = req.session().attribute("ConceptMap");
			return fileHandler.jsonAsString();
		});

		// Path which is requested after the single transformations have been
		// made, returns the new generated StructureMap
		post("/parseToStructureMap", (req, res) -> {
			MappingHandler mapHandler = new MappingHandler(req.body(),
					((UploadFileHandler) req.session().attribute("ConceptMap")).getConceptMap(), lHandler, req);
			req.session().attribute("UpdatedConceptMap", mapHandler.CMapToString());
			req.session().attribute("StructureMap", mapHandler);
			res.type("application/json");
			return mapHandler.BundleToString();

		});

		// Returns the StructureMap as json file to be downloaded
		get("/saveLocal", (req, res) -> {
			MappingHandler map = req.session().attribute("StructureMap");
			// collection.insertOne(doc);
			res.header("Content-Disposition", "attachment; filename=Bundle.json");
			res.type("application/json");
			return map.createLocalFile(res);
		});

		// Request to upload the created data to the MongoDB
		get("/uploadToDb", (req, res) -> {
			MappingHandler map = req.session().attribute("StructureMap");
			MongoDbHandler dbHandler = new MongoDbHandler(map, database);
			dbHandler.uploadStructureMap();
			dbHandler.uploadConceptMap();
			return "";
		});

		get("/getSuggestion", (req, res) -> {
			int group = Integer.parseInt(req.queryParams("group"));
			int elem = Integer.parseInt(req.queryParams("elem"));
			ConceptMap cm = ((UploadFileHandler) req.session().attribute("ConceptMap")).getConceptMap();
			SuggestionHandler sh = new SuggestionHandler(cm, group, elem, database);
			return sh.find();

		});

		get("/logout", (req, res) -> {
			if (lHandler.isLoggedIn()) {
				try {
					res.cookie("Username", null);
					res.redirect(lHandler.logout(req));

				} catch (IOException e) {
					e.printStackTrace();
					res.redirect("/");
				}
			}
			return "";
		});
	}
}
