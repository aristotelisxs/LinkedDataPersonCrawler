import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class PersonCrawler {

	private final String sparqlEndpointURL =  "http://dbpedia.org/sparql";

	/**
	 * Maps a resource URI with a given integer number (index) to minimise retrieval times and 
	 * insertion for the termIndecesMapping data structure.
	 */
	private HashMap<String, Integer> uriIndexMapping = new HashMap<String, Integer>();

	/**
	 * Stores keywords and the indexes of resource URIs to which it appears.
	 */
	private HashMap<String, HashSet<Integer>> termIndecesMapping = new HashMap<String, HashSet<Integer>>();

	/**
	 * Tracks how many people have been currently recorded for a given country. 
	 */
	private int peopleVisited = 0;

	/**
	 * Used to remove stop words from given text input using the removeStopWords function. 
	 */
	private StopWords stopWords = new StopWords();

	/**
	 * Specifies how many people associated with a given country should be extracted. 
	 */
	private int peopleRequired;

	/**
	 * Maximum duration for which an inverse index file will not require an update. 
	 */
	private long MAX_DURATION = MILLISECONDS.convert(1, DAYS);

	/**
	 * Defines if the search has been filtered using a country or if it is a generic search. 
	 */
	private boolean fromCountry = true;

	/**
	 * Prevent recursion to the same resources when searching to branch out from a seed URI 
	 * by logging visits to them in this HashSet. 
	 */
	private HashSet<String> alreadyVisitedURIs = new HashSet<String>();

	/**
	 * Any valid words in the search query? 
	 */
	private List<String> validWords = new ArrayList<String>();

	/**
	 * Maximum number of people allowed to crawl 
	 */
	private int PERSON_UPPER_LIMIT = 300;

	//	Recursion limits.
	private int recursions = 0;
	private int RECURSION_LIMIT = 15;

	/**
	 * Single constructor for the current crawler object that will define the depth and filtering of the search.
	 * @param requiredPeople The number of people the search should try to find accumulatively.
	 * @param fromCountry Has the crawling been filtered with a specific country.
	 */
	public PersonCrawler(int requiredPeople, boolean fromCountry){

		super();

		if(this.PERSON_UPPER_LIMIT > requiredPeople){ this.peopleRequired = requiredPeople; }
		else{ this.peopleRequired = PERSON_UPPER_LIMIT; }

		this.fromCountry = fromCountry;

		File properties = new File("properties.xml");

		if(properties.exists()){ XMLDataExctraction(properties.getPath()); }
		else{ this.buildDefaultXMLFile(properties); }

	}

	/**
	 * If the properties XML file does not exist, create a new one for future use.
	 * @param properties The File object that we will be using
	 */
	public void buildDefaultXMLFile(File properties) {
		try {

			PrintWriter writer = new PrintWriter(new FileWriter(properties, false));

			writer.print("<LinkedDataPersonCrawler>\n" + 
					"<recursion_limit>" + this.RECURSION_LIMIT + "</recursion_limit>\n" + 
					"<max_duration>" + this.MAX_DURATION + "</max_duration>\n" + 
					"</LinkedDataPersonCrawler>");

			writer.close();

		} catch (IOException e) {
			System.out.println("Something went wrong with the XML file creation.");
		}


	}

	/**
	 * Import a given XML file into a DOM object so that we can browse through its contents
	 * using the tag notations.
	 * @param filePath
	 */
	public void XMLDataExctraction(String filePath) {
		try {

			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse (new File(filePath));

			// Normalise text representation
			doc.getDocumentElement().normalize();

			// We know beforehand that data we need are located in between <tr>
			org.w3c.dom.NodeList listOfElements = doc.getElementsByTagName("recursion_limit");
			if(listOfElements.getLength() > 0)
				this.RECURSION_LIMIT = Integer.parseInt(listOfElements.item(0).getTextContent());

			listOfElements = doc.getElementsByTagName("max_duration");
			if(listOfElements.getLength() > 0)
				// Convert days to milliseconds.
				this.MAX_DURATION = Integer.parseInt(listOfElements.item(0).getTextContent())*86400000;

			listOfElements = doc.getElementsByTagName("max_people_to_find");
			if(listOfElements.getLength() > 0)
				// Convert days to milliseconds.
				this.PERSON_UPPER_LIMIT = Integer.parseInt(listOfElements.item(0).getTextContent());

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Execute the given query and return the results.
	 * @param query The SPARQL query to be executed.
	 * @return Results set.
	 */
	private ResultSet queryResults(String query){

		try{

			QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpointURL, query);
			ResultSet resultSet = qexec.execSelect();

			return resultSet;
		} catch (Exception ex) {
			return null;
		} 
	}

	/**
	 * Used for generic searches (no country filter)
	 * @param keyWords The number of keywords to be matched to the universal, generic index file of this methodology.
	 * @return The resource URIs to which the keyword has been found.
	 */
	public String[] crawlIntoLinkedDataCloud(String[] keyWords, boolean firstRun, boolean precomputeSuccess){

		try {

			int previousEntries = this.uriIndexMapping.size();

			if(!this.fromCountry)
				this.peopleRequired = peopleRequired/keyWords.length;

			File f = new File("Generic_index.tsv");

			if(!precomputeSuccess){

				if(!f.exists()){ f.createNewFile(); }
				else if(firstRun){ this.retrieveFromIndex(f); }
			}

			String[] refinedKeyWords = this.correctKeywordsAmbiguity(keyWords);

			if(validWords.size() == 0){

				return null;

			} else {

				System.out.println("Appending to generic index file.");

				for(String uri : validWords) {

					long startTime = System.nanoTime();

					resourceCrawl(uri);

					Double duration = (Double)((System.nanoTime() - startTime)/1000000000.0);

					try {

						this.appendToResultsFile(duration, recursions, peopleVisited, uri.substring(uri.lastIndexOf("/") + 1, 
								uri.length()), this.uriIndexMapping.size() - previousEntries, peopleRequired);

						previousEntries = this.uriIndexMapping.size();

					} catch (IOException e) {

						System.out.println("An error occurred while appending to the file.");

					}

					// Reset global variables for next iteration.
					recursions = 0;
					this.peopleVisited = 0;

					this.writeToIndex(f.getPath());

				}

				System.out.println("Update complete.");
				// Reset valid words container
				this.validWords = new ArrayList<String>();

				return this.findMatchingURIs(refinedKeyWords);
			}

		} catch (IOException e) {

			e.printStackTrace();

		}

		return null;

	}

	/**
	 * Used for country filtered searching
	 * @param country The name of the country to use as seed URI
	 * @param keyWords The keywords to be matched in the created index files.
	 * @return The resource URIs to which the keywords appear all in.
	 */
	public String[] crawlIntoCountry(String country, String[] keyWords){
		long startTime = System.nanoTime();
		int previousEntries = this.uriIndexMapping.size();

		String lastUpdate = "";
		File f = new File(country + "_index.tsv");

		if(f.exists()){
			System.out.println("Index file for " + country + " exists.");

			try {

				BufferedReader br = new BufferedReader(new InputStreamReader(
						new FileInputStream(f), "UTF8"));

				// Read in the first line that indicated when the index file has been last updated.
				lastUpdate = br.readLine();

				br.close();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if(this.recrawlingNeeded(lastUpdate)){
				System.out.println("Recrawling necessary. Updating..");

				String countryURI = checkIfCountryURIExists(country);

				if(countryURI.equals("")){

					countryURI = this.getRedirectedURI(country);

					if(countryURI.equals("")){
						System.out.println("Country URI does not exist. Exiting..");

						return null;
					}
				} 

				System.out.println("Country URI exists. Resuming update..");
				resourceCrawl(countryURI);

				Double duration = (Double)((System.nanoTime() - startTime)/1000000000.0);

				try {
					this.appendToResultsFile(duration, recursions, peopleVisited, country, this.uriIndexMapping.size() - previousEntries, peopleRequired);
					previousEntries = this.uriIndexMapping.size();
				} catch (IOException e) {
					System.out.println("An error occurred while appending to the file.");
				}

				// Reset global variables.
				recursions = 0;
				this.peopleVisited = 0;

				this.writeToIndex(country + "_index.tsv");

				System.out.println("Update complete.");

			} 

			/*
			 *  If the object exists because another query for the same country was initiated, prevent it from
			 *  retrieving from index as the required data should already have been assigned to the appropriate
			 *  data structures.
			 */
			else if (this.uriIndexMapping.size() == 0) {
				this.retrieveFromIndex(f);
			}

		} else if(!f.exists()) {
			System.out.println("Creating index file for " + country + "..");

			String countryURI = checkIfCountryURIExists(country);

			if(countryURI.equals("")){

				countryURI = this.getRedirectedURI(country);

				if(countryURI.equals("")){
					System.out.println("Country URI does not exist. Exiting..");

					return null;
				}

			}

			resourceCrawl(countryURI);

			// Reset global variables.
			Double duration = (Double)((System.nanoTime() - startTime)/1000000000.0);

			try {
				this.appendToResultsFile(duration, recursions, peopleVisited, country, this.uriIndexMapping.size() - previousEntries, this.peopleRequired);
				previousEntries = this.uriIndexMapping.size();
			} catch (IOException e) {
				System.out.println("An error occurred while appending to the file.");
			}

			recursions = 0;
			this.peopleVisited = 0;

			this.writeToIndex(country + "_index.tsv");
			System.out.println("File successfully created.");

		}

		String[] refinedKeyWords = this.correctKeywordsAmbiguity(keyWords);

		/* 
		 * At this point, our data structures are populated with the required inverse index information.
		 * Now we need to report in the graphical interface about our results.
		 */
		return this.findMatchingURIs(refinedKeyWords);

	}


	/**
	 * This method is an attempt to match a keyword to a resource URI in the LOD Cloud. This method is a simple attempt
	 * to correct misspelled keywords using the redirections that DBpedia offers. If this does not succeed for some element,
	 * the equivalent one in the search query is used instead.
	 * @param keyWords The keywords in the search query
	 * @return The results of the attempt to replace keywords with their valid counterparts.
	 */
	public String[] correctKeywordsAmbiguity(String[] keyWords){

		String[] refinedKeyWords = new String[keyWords.length];

		for(int i=0; i<keyWords.length; i++){

			keyWords[i] = keyWords[i].trim();

			String resourceURI = this.checkIfURIExists(keyWords[i]);

			if(resourceURI != null){

				this.validWords.add(resourceURI);
				refinedKeyWords[i] = resourceURI.substring(resourceURI.lastIndexOf("/") + 1,
						resourceURI.length()).toLowerCase();
				
				/*
				 *  In case the word has been matched with a composite one, this will be reflected by an
				 *  underscore character followed by the next word. Delete those before continuing because
				 *  the index file only contains single keywords.
				 */
				if(refinedKeyWords[i].contains("_")){
					refinedKeyWords[i] = refinedKeyWords[i].substring(0, refinedKeyWords[i].lastIndexOf("_"));
					refinedKeyWords[i] = refinedKeyWords[i].replace("_", "");
				}

			} else {

				refinedKeyWords[i] = keyWords[i];

			}
			
		}

		return refinedKeyWords;
	}

	/**
	 * Appends to the results file the metrics recorded for a crawling process. This process will be repeated for each
	 * keyword (for generic searching) and for every country (for filtered searching) that is being searched for.
	 * @param duration Time needed for crawling
	 * @param recursions Branch outs
	 * @param peopleFound Number of people found relating to keyword
	 * @param keyWord The term used for crawling the linked data cloud
	 * @param addedIndexes Number of new URIs added
	 * @param peopleRequired People the crawling required (will change for generic searching were equally distributed people are asked to be found
	 * by each respective keyword.
	 * @throws IOException
	 */
	public void appendToResultsFile(Double duration, int recursions, int peopleFound, String keyWord, int addedIndexes, int peopleRequired) throws IOException{

		File resultsFile = new File("analytics.csv");
		boolean existedBefore = true;

		if(!resultsFile.exists()){
			existedBefore = false;
			resultsFile.createNewFile();
		}

		try {
			PrintWriter writer = new PrintWriter(new FileWriter(resultsFile, true));

			if(!existedBefore)
				writer.println("Date, Crawling_Duration, Branching_Times, People_Found, Keyword, Added_URIs, People_Required, From_Country");

			int countryBin = 0;
			if(this.fromCountry)
				countryBin = 1;

			writer.println(this.getCurrentDateTime() + "," + duration + "," + recursions + "," + peopleFound + "," +
					keyWord + "," + addedIndexes + "," + peopleRequired + "," + countryBin);

			writer.close();

		} catch (Exception ex) {
			System.out.println("An error occurred while appending to the file.");
		}
	}

	/**
	 * The following method will match resource URIs to which all keywords supplied by the user appear.
	 * The extracted ones are to be displayed to a reporting frame.
	 * @param keyWords the keywords set to find intersection of resources to which they appear.
	 * @return A string array containing the resources that consist of the intersection of keywords to which they appear.
	 */
	public String[] findMatchingURIs(String[] keyWords){

		HashSet<Integer> uriIndecesList = new HashSet<Integer>();
		List<String> uriStringList = new ArrayList<String>();

		/*
		 * Given that words are already mapped to the document numbers we have created,
		 * retrieve for each word the whole list 
		 */
		for(int i=0; i<keyWords.length; i++){

			keyWords[i] = keyWords[i].trim();

			// If you cannot find the given word, ignore it.
			if(this.termIndecesMapping.containsKey(keyWords[i])){
				
				if(i==0){

					uriIndecesList.addAll(this.termIndecesMapping.get(keyWords[i].toLowerCase()));

				} else {

					/*
					 * The documentation for the retainAll function suggests that
					 * only elements that appear in the previously added values and the new
					 * ones will be retained. 
					 */
					uriIndecesList.retainAll(this.termIndecesMapping.get(keyWords[i].toLowerCase()));

				}				
			}

		}

		/*
		 * No resources have been found for any of the keywords. Return null and report back to the user that no results
		 * were found.
		 */
		if(uriIndecesList.size() > 0){

			for(int uriIndex : uriIndecesList){

				// Only one element will be contained by the returned set so quickly retrieve it by using the iterator().next() functions
				uriStringList.add(this.getKeysByValue(this.uriIndexMapping, uriIndex).iterator().next());

			}

			return Arrays.copyOf(uriStringList.toArray(), uriStringList.toArray().length, String[].class);

		} else {

			return null;

		}
	}

	public void printURIs(String keyWord){

		System.out.println("Printing URIS for: " + keyWord);
		Iterator<Integer> iter = this.termIndecesMapping.get(keyWord).iterator();

		while(iter.hasNext()){
			System.out.println(this.getKeysByValue(this.uriIndexMapping, iter.next()));
		}

	}

	/**
	 * This method is responsible of performing shallow searching of people in the linked data cloud 
	 * starting from the resource URI given as a parameter. This function is called recursively to prevent
	 * termination of the algorithm before the desired number of people are found.
	 * @param currentResource The resource URI that will be used first to perform the search. Given the
	 * context of this application, it will most probably be a country's URI.
	 */
	public void resourceCrawl(String currentResource) {

		try {
			// Stop if there are enough people.
			if(this.peopleVisited < this.peopleRequired) {

				/*
				 *  Check if the currently viewed resource is a valid URI first by checking if it starts with
				 *  an "http://dbpedia.org/resource/" prefix.
				 */
				String queryURI = "SELECT DISTINCT ?resourceURI WHERE {"
						+ "{<" + currentResource + "> ?property ?resourceURI FILTER ( strstarts(str(?resourceURI), \"http://dbpedia.org/resource/\") )}"
						+ "UNION"
						+ "{?resourceURI ?property <" + currentResource + "> FILTER ( strstarts(str(?resourceURI), \"http://dbpedia.org/resource/\") )}"
						+ "}";

				/*
				 * Then check if the currently viewed resource is a Person.
				 */
				String personQuery = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
						+ "SELECT DISTINCT ?resourceURI WHERE {"
						+ "{<" + currentResource + "> ?a ?resourceURI}"
						+ "UNION"
						+ "{?resourceURI ?a <" + currentResource + ">}"
						+ "{?resourceURI a foaf:Person}"
						+ "}";

				ResultSet personResults = this.queryResults(personQuery);

				// Run both queries one after the other.
				if(personResults != null){
					while(personResults.hasNext() && this.peopleVisited < this.peopleRequired){

						QuerySolution solution = personResults.nextSolution();

						String person = solution.get("?resourceURI").toString();
						this.peopleVisited++;

						String textToParse = this.getAbstract("<" + person + ">");
						textToParse += " " + this.getComment("<" + person + ">");
						textToParse += " " + this.getDescription("<" + person + ">");

						if(textToParse != null){

							System.out.println("Logging data for: " + person);
							this.parseTextToTokens(textToParse, "<" + person + ">");

						}

					}
				}

				ResultSet resultsURI = this.queryResults(queryURI);

				if(resultsURI != null){
					while(resultsURI.hasNext() && this.peopleVisited < this.peopleRequired) {
						QuerySolution solution = this.getRandomSolution(resultsURI, 5.0);
						String resourceURI = solution.get("?resourceURI").toString();

						System.out.println("Recursion starting in: " + resourceURI);

						// Recursively add new instances of the current method to visit linked URIs.
						if(!this.alreadyVisitedURIs.contains(resourceURI)){

							// Limit recursions:
							if( recursions == this.RECURSION_LIMIT ){ return; }
							else{ recursions ++; }

							this.alreadyVisitedURIs.add(resourceURI);
							this.resourceCrawl(resourceURI);

						}
					}
				} 
			}
			
		} catch (StackOverflowError soe) {
			/*
			 *  Ungracefully let the exception be thrown and then iteratively return from all stacks as this will
			 *  be thrown in all consecutively running recursions.
			 */
			return;
		}
	}

	/**
	 * This method is responsible for cleaning text inputs from stop words and
	 * assigning each valid word token it contains in the global data structures for
	 * inverse index file creation purposes.
	 * @param textToParse Text input to parse, originating from the dbpedia-owl:abstract or
	 * rdfs:comment (which have the most information out of all the alternatives)
	 * @param resourceURI The resource URI to which the text input was extracted from.
	 */
	public void parseTextToTokens(String textToParse, String resourceURI){
		if(!this.uriIndexMapping.containsKey(resourceURI)){
			uriIndexMapping.put(resourceURI, uriIndexMapping.size());
		}

		// Convert to lower case to avoid wrong outputs.
		textToParse = textToParse.toLowerCase();
		// Remove stop words using the function given in the SCC413 course.
		textToParse = this.stopWords.removeStopWords(textToParse);

		String[] splittedText = textToParse.split(" ");

		for(String token : splittedText){

			if(!token.equals("")){ //|| !this.isInteger(token, 10)){


				if(!this.termIndecesMapping.containsKey(token)){
					HashSet<Integer> termURIs = new HashSet<Integer>();

					termURIs.add(this.uriIndexMapping.get(resourceURI));

					// Map a word from the text to the document numbers it appears.
					this.termIndecesMapping.put(token, termURIs);
				} else {
					// Retrieve the indexes of the URIs to which the word appears 
					HashSet<Integer> termURIs = this.termIndecesMapping.get(token);

					termURIs.add(this.uriIndexMapping.get(resourceURI));

					// Add the new document numbers to which the word appears.
					this.termIndecesMapping.put(token, termURIs);
				}

			}
		}
	}

	/**
	 * UNUSED
	 * Iterate through a given string and check if all the characters that it contains
	 * are integers to return true. If any of these is not, return false. This is based on a
	 * radix (the base of the number system to check if the integer belongs to - we chose 10
	 * obviously here). We don't know if this is a necessity in our application though as we naturally want
	 * to accept some numbers (e.g. years or dates).
	 * @param token The word token that may prove to be a number.
	 * @param radix Base 10 numbering system but left as a parameter for generalisation
	 * @return True if it is a number, false if not.
	 */
	public boolean isInteger(String token, int radix) {
		if(token.isEmpty()) 
			return false;

		for(int i = 0; i < token.length(); i++) {

			if(i == 0 && token.charAt(i) == '-') {

				if(token.length() == 1) 
					return false;
				else
					continue;

			}

			if(Character.digit(token.charAt(i),radix) < 0) 
				return false;
		}

		// All characters have passed the tests as digits. Return true.
		return true;
	}

	/**
	 * For already created inverse index files for a certain country, retrieve the words and in which documents
	 * they appear to be queried later on based on user keyword inputs.
	 * @param f The name of the file from which to retrieve words and the documents they appear in.
	 */
	public void retrieveFromIndex(File f){
		try {
			String line = "";
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), "UTF8"));

			// Ignore the first line
			br.readLine();
			while((line = br.readLine()) != null){
				String[] toks = line.split("\t");

				HashSet<Integer> URIValues = new HashSet<Integer>();

				for(int i = 1; i < toks.length; i++){

					toks[i] = toks[i].replace("[", "");
					toks[i] = toks[i].replace("]", "");

					if(!uriIndexMapping.containsKey(toks[i])){

						this.uriIndexMapping.put(toks[i], uriIndexMapping.size());

						System.out.println("Added: " + uriIndexMapping.get(toks[i]) + ", " + toks[i]);
					}

					URIValues.add(this.uriIndexMapping.get(toks[i]));
				}

				this.termIndecesMapping.put(toks[0].toLowerCase(), URIValues);
			}

			br.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Create a new inverse-index file for the given country
	 * @param countryIndexFile The name of the inverse-index file to be created
	 */
	public void writeToIndex(String countryIndexFile){

		try {
			PrintWriter writer = new PrintWriter(new FileWriter(countryIndexFile, false));

			Iterator<String> termIndexIter = this.termIndecesMapping.keySet().iterator();

			// Delete all previous content of the file by simply typing an empty string.
			writer.print("");
			// Write the current date on the first line of the file.
			writer.println(this.getCurrentDateTime());

			while(termIndexIter.hasNext()){
				String toWrite = "";
				String uriVal = "";
				String nextKey = termIndexIter.next();
				HashSet<Integer> values = termIndecesMapping.get(nextKey.toLowerCase());

				Iterator<Integer> valuesIter = values.iterator();

				while(valuesIter.hasNext()){
					uriVal = "";
					uriVal += this.getKeysByValue(this.uriIndexMapping, valuesIter.next()).toString();
					toWrite += uriVal + "\t";
				}

				toWrite = toWrite.substring(0, toWrite.lastIndexOf("\t")).trim();
				toWrite = nextKey + "\t" + toWrite;

				writer.println(toWrite);
			}

			writer.close();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	/**
	 * Check if re-crawling to the linked data cloud will be required for the selected country
	 * @param lastUpdate Date time in dd/MM/yy HH:mm:ss since last update of the index file.
	 * @return If re-crawling is needed return true, else false. 
	 */
	public boolean recrawlingNeeded(String lastUpdate){

		try {
			SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
			Date lastUpdateDate;

			lastUpdateDate = parser.parse(lastUpdate);

			Date now = Calendar.getInstance().getTime();

			long duration = now.getTime() - lastUpdateDate.getTime();

			if (duration >= this.MAX_DURATION) {

				System.out.println("Time passed since last update is not within threshold: " + duration + 
						". Last update in: " + lastUpdateDate.toString());
				return true;

			} else {

				System.out.println("Time passed since last update is within threshold: " + lastUpdateDate.toString());
				return false;

			}

		} catch (ParseException e) {

			System.out.println("Failed to parse date. Re-crawling will not commence.");
			return false;

		}

	}

	/**
	 * This method will make sure that the country string we have in our combo box
	 * has a valid resource URI. We make this check because we don't want to waste time
	 * in manually checking valid string for all countries listed in the combo box selections.
	 * @param country The name of the country to check for existence.
	 * @return The country URI. If not found, return empty string.
	 */
	public String checkIfCountryURIExists(String country){

		String countryCheckSPARQL = "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n" +
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
				"SELECT ?c WHERE { " +
				"?c rdf:type dbpedia-owl:Place;" +
				"foaf:name \"" + country + "\"@en ." +
				"FILTER NOT EXISTS { ?c dbpedia-owl:dissolutionYear ?y }" +
				"}";

		ResultSet results = this.queryResults(countryCheckSPARQL);

		if(results != null && results.hasNext()){
			QuerySolution solution = results.nextSolution();

			System.out.println("URI resource found: " + solution.get("?c").toString());

			return solution.get("?c").toString();
		} else {

			System.out.println("URI resource not found");
			return "";

		}

	}

	/**
	 * A generic method to check if a given resource exists by checking if for that URI any triples are
	 * associated with.
	 * @param keyWord The keyword to create the resource URI with.
	 * @return The resulting resource or empty string if it has not been found.
	 */
	public String checkIfURIExists(String keyWord){

		keyWord = Character.toString(keyWord.charAt(0)).toUpperCase() + keyWord.substring(1, keyWord.length()).toLowerCase();
		String uriFromKeyword = this.getRedirectedURI(keyWord);

		if(uriFromKeyword.equals(""))
			uriFromKeyword = "http://dbpedia.org/resource/" + keyWord;

		String uriQuery = "SELECT ?property ?hasValue ?isValueOf\n" +
				"WHERE {\n" +
				"{<" + uriFromKeyword + "> ?property ?hasValue }\n" +
				"UNION\n" +
				"{ ?isValueOf ?property <" + uriFromKeyword + "> }\n" +
				"}";

		//System.out.println(uriQuery);
		ResultSet results = this.queryResults(uriQuery);

		if(results != null && results.hasNext()){ return uriFromKeyword; } 
		else { return null; }
	}

	/**
	 * UNUSED:
	 * Get the names of the people by querying the resource for its foaf:name property.
	 * @param PersonURI A person's resource URI.
	 * @return The name of the person to which the resource URI corresponds.
	 */
	public String getName(String PersonURI){
		QueryExecution qexec = null;

		// Query dynamically adjusts to accommodate to the new URI linked to the previous one.
		String query = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
				"SELECT ?name \n" +
				"WHERE \n" +
				"{ " + PersonURI +" foaf:name ?name \n" +
				"}";

		String sparqlEndpointURL =  "http://dbpedia.org/sparql";

		qexec = QueryExecutionFactory.sparqlService(sparqlEndpointURL,query);
		ResultSet results = qexec.execSelect() ;

		for ( ; results.hasNext() ; )
		{
			QuerySolution soln = results.nextSolution();
			if(soln.get("?name") != null){
				String name = soln.get("?name").toString();

				// The first result is enough. Return.
				return name.substring(0, name.indexOf("@"));
			}
		}
		// If nothing has been found, return with null.
		return null;
	}

	/**
	 * To limit recursion in resource URIs that may contain more people than other resource URIs,
	 * we access a random solution from one of the available ones when we search to branch out from 
	 * a source URI (a country) and expand to URIs that it relates to.
	 * @param results The result set to get a random solution from
	 * @param percentChance The chance % to stop at a given solution
	 * @return The solution to be used in the next recursion.
	 */
	public QuerySolution getRandomSolution(ResultSet results, Double percentChance){

		if(results.hasNext()){
			Double limit = 100/percentChance;
			int lim = limit.intValue();

			Random randomGenerator = new Random();
			QuerySolution solution = null;

			while(results.hasNext()){
				int randomInt = randomGenerator.nextInt(lim);			    
				if(randomInt == 1){

					return results.nextSolution();

				} else {

					solution = results.nextSolution();

				}
			}

			return solution;

		} else {

			return null;

		}

	}

	/**
	 * Returns the current date time in the standard format of dd/MM/yy HH:mm:ss
	 * @return The current date time.
	 */
	public String getCurrentDateTime(){
		DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		Date dateobj = new Date();
		return df.format(dateobj);
	}

	/**
	 * A method using generic notations to extract from a map structure
	 * a key set based on the value those correspond to (could be just one)
	 * @param map The data structure to extract the key(s) from
	 * @param val The value to which the key(s) correspond
	 * @return A key set (could be null).
	 */
	public <T, E> Set<T> getKeysByValue(Map<T, E> map, E val) {

		return map.entrySet()
				.stream()
				.filter(entry -> entry.getValue().equals(val))
				.map(entry -> entry.getKey())
				.collect(Collectors.toSet());

	}

	/**
	 * Get the detailed description of the resource by accessing the dbpedia-owl:abstract resource.
	 * @param PersonURI the current person's extracted resource URI
	 * @return Text description of the dbpedia-owl:abstract resource.
	 */
	public String getAbstract(String PersonURI){

		// Query dynamically adjusts to accommodate to the new URI linked to the previous one.
		String query = "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\n" +
				"SELECT ?abstract \n" +
				"WHERE \n" +
				"{ " + PersonURI +" dbpedia-owl:abstract ?abstract " +
				"FILTER (langMatches(lang(?abstract),\"en\"))" +		
				"}";

		ResultSet results = this.queryResults(query);

		if(results != null){
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();

				if(soln.get("?abstract") != null){

					return soln.get("?abstract").toString();

				}
			}
		}

		System.out.println("Could not find \"dbpedia-owl:abstract\" details for " + PersonURI);
		// If nothing has been found, return with null.
		return null;
	}

	/**
	 * Get the detailed description of the resource by accessing the rdfs:comment resource.
	 * @param PersonURI the current person's extracted resource URI
	 * @return Text description of the rdfs:comment resource.
	 */
	public String getComment(String PersonURI){

		String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
				"SELECT ?comment \n" +
				"WHERE \n" +
				"{ " + PersonURI +" rdfs:comment ?comment " +
				"FILTER (langMatches(lang(?comment),\"en\"))" +		
				"}";

		ResultSet results = this.queryResults(query);

		if(results != null){
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();

				if(soln.get("?comment") != null){

					return soln.get("?comment").toString();

				}
			}
		}

		System.out.println("Could not find \"rdfs:comment\" details for " + PersonURI);
		// If nothing has been found, return with null.
		return null;

	}

	/**
	 * Get the short description of the resource by accessing the rdfs:comment resource.
	 * @param PersonURI the current person's extracted resource URI
	 * @return Text description of the rdfs:comment resource.
	 */
	public String getDescription(String PersonURI){
		String query = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
				"SELECT ?descr \n" +
				"WHERE \n" +
				"{ " + PersonURI +" dc:description ?descr " +
				"FILTER (langMatches(lang(?descr),\"en\"))" +		
				"}";

		ResultSet results = this.queryResults(query);

		if(results != null){
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();

				if(soln.get("?descr") != null){

					return soln.get("?descr").toString();

				}
			}
		}

		System.out.println("Could not find \"dc:description\" details for " + PersonURI);
		// If nothing has been found, return with null.
		return null;

	}

	/**
	 * If we are going to use keywords as source URIs, we need to check for each one if dbpedia redirects a resource
	 * formed based on that keyword to another URI. This method takes care of mapping that resource to the redirected to one.
	 * This is done because the resource formed by the original keyword may not produce enough linked URIs to navigate through
	 * using the adopted recursion strategy.
	 * @param keyWord The keyword to use
	 * @return The redirected to URI or the URI formed by that keyword (if no redirections were found).
	 */
	public String getRedirectedURI(String keyWord){

		String redirectionQuery = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" + 
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
				"PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
				"SELECT ?uri WHERE {\n" +
				"{\n" +
				"?uri rdfs:label \"" + keyWord + "\"@en ;\n" +
				"a owl:Thing .\n" +        
				"}\n" +
				"UNION\n" +
				"{\n" +
				"?altName rdfs:label \"" + keyWord + "\"@en ;\n" +
				"dbo:wikiPageRedirects ?uri .\n" +
				"}\n}";

		ResultSet results = this.queryResults(redirectionQuery);

		if(results != null && results.hasNext()){
			QuerySolution sol = results.nextSolution();
			return sol.get("?uri").toString();

		} else {
			return "";
		}

	}

	/**
	 * This method will infer from past knowledge the first results of a live crawling process and report them in a separate reporting screen
	 * that will later be replaced by one with the complete results. As is implied, this method should only be called for live crawling searches.
	 * @param keyWords The keywords used in the query search.
	 * @param firstRun Is this the first time this crawler object is being used by the GUI?
	 * @return The matching URIs
	 */
	public String[] prepopulateResults(String[] keyWords, boolean firstRun){

		File f = new File("Generic_index.tsv");

		try{

			if(!f.exists()){ f.createNewFile(); }
			else if(firstRun){ this.retrieveFromIndex(f); }

		} catch (IOException ex) {

			return null;

		}

		String[] refinedKeyWords = this.correctKeywordsAmbiguity(keyWords);

		if(validWords.size() == 0){

			return null;

		} else {

			System.out.println("Output of pre-computed results.");
			// Reset valid words container
			validWords = new ArrayList<String>();

			return this.findMatchingURIs(refinedKeyWords);
		}

	}

	/**
	 * Set number of people required by the search
	 * @param people Number of people
	 */
	public void setPeopleRequired(int people){

		this.peopleRequired = people;

	}

}
