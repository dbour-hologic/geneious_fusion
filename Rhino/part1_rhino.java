import java.util.regex.*;

/*
***************
* PART I
***************
*/

/**
* 
* 
* 
* Looks for sequences that passed trim AND has a partner that also passed
* the trimming criteria. If one of the pair is missing, exclude both
* in the workflow until reaching the custom script to output them as probable 
* positives.
*
* Rationale:
* This is done so the workflow does not crash for not being able to
* find a second pair when combining pairs by name for De Novo Assembly
* because one partner was removed by trim filtering. 
*/


/**
*
* Main Entry Point
*
* The workflow automatically calls upon this static method.
*
* @param documents documents that have been received from the previous workflow. (not used)
* @param options Geneious GUI options. (not used)
* @param progressListener tracking changes during the workflow. (not used)
* @return the sequences that made a pair
*/
public static List<AnnotatedPluginDocument> performOperation(List<AnnotatedPluginDocument> documents, Options options,
														ProgressListener progressListener) throws DocumentOperationException
{

	// Gets the folder the files are in
	GeneiousService service = ServiceUtilities.getSelectedService();
	DatabaseService databaseService = (DatabaseService) service;

	// Grab all files in the document viewer
	List<AnnotatedPluginDocument> documentsInFolder = databaseService.retrieve("");

	// Change all names
	changeAllNames(documentsInFolder);

	//Contains all of the individual sequences that are post-trim & > 100 bp
	List<AnnotatedPluginDocument> postTrimSeq = getThresholdRaw(documentsInFolder);

	//Contains all of the sequences with pairs & > 100 bp.
	List<AnnotatedPluginDocument> results = checkForPairs(postTrimSeq);


	return results;

}

/** Used for naming the duplicate (".ab1 2") to its original file name
* @param seqDupeName name of the .ab1 file
* @return renamed to the original .ab1 file, removing the " 2"
*/
public static String renameDupe(String seqDupeName) {

	String renameIt = seqDupeName;
	String regPattern = "(.*\\.ab1)(\\s\\d)";
	Pattern p = Pattern.compile(regPattern);
	Matcher m = p.matcher(renameIt);
	
	if (m.matches()) {
		
		if (m.group(2).matches(" 2")) {
			return m.group(1);	
		}
	}
	return "";
}


/**
*
* Converts the post-trimmed names to the original names.
* Adds a suffix " original" to the original raw file names.
*
* This was done to preserve the names for the LIMS system when 
* Geneious exports the data.
*/

public static void changeAllNames(List<AnnotatedPluginDocument> docsInFolder)
{

	for (AnnotatedPluginDocument docs : docsInFolder)
	{

		String docName;

		if (checkPostTrim(docs.getName()))
		{
			docName = renameDupe(docs.getName());
			docs.setName(docName);
		}
		else
		{

			docName = docs.getName();
			docs.setName(docName + " original");
		}
	}


}



/**
*
* Checking for Pairs
*
* Using a 'selection sort' style search method, look for
* sequences that have matching IDs and return the ones that do.
*
* @param query list containing all sequences that passed the trim filter
* @return a list of all sequences that had matching IDs
*/
public static List<AnnotatedPluginDocument> checkForPairs(List<AnnotatedPluginDocument> query)
{

	// Contains all sequences that had a pair
	List<AnnotatedPluginDocument> pairSequences = new ArrayList<AnnotatedPluginDocument>();

	String queryName, queryNameSuffix, searchName, searchNameSuffix;
	AnnotatedPluginDocument queryRaw, searchRaw;

	for (int x = 0; x < query.size(); x++)
	{

		queryRaw = query.get(x);						// Referencing the .ab1 file
		queryName = queryRaw.getName();					// Get the human readable name of the .ab1 file
		queryNameSuffix = getSuffix(queryName);			// Get only the unique ID to the right of the underscore (ex. HSV2-PCR-A-1_UNIQUEID)

		for (int y = x+1; y < query.size(); y++)
		{

			searchRaw = query.get(y);					// Referencing the .ab1 file
			searchName = searchRaw.getName();			// Get the human readable name of the .ab1 file
			searchNameSuffix = getSuffix(searchName);	// Get only the unique ID to the right of the underscore (ex. HSV2-PCR-A-1_UNIQUE_ID)


			// Searches for sequences with matching ID pairs
			if (queryNameSuffix.equalsIgnoreCase(searchNameSuffix))
			{

				pairSequences.add(queryRaw);			
				pairSequences.add(searchRaw);
			}
		}

	}

	return pairSequences;


}

/**
*
* Get the sequence ID/UNIQUE ID name
* We will call this unique ID suffix which means to the right of the underscore in
* a sample name.
*
* @param seqName sequence name to parse unique ID from
* @return the parsed unique ID
*/
public static String getSuffix(String seqName)
{

	String currentName, suffixName;
	currentName = seqName;
	suffixName = currentName.substring(currentName.indexOf("_")+1, currentName.length()-1);

	return suffixName;
}


/**
*
* Use regex to determine if the sequence is of type post-trim.
*
* When the workflow saves after post-trim, it will append a whitespace 2 
* to the end of the original name. We will use this as a way to get
* the post-trim sequence using reg-ex.
*
* @param seqFileName the sequence file name to parse
* @return true or false, depending on if it found a whitespace 2 identifier
*/
public static boolean checkPostTrim(String seqFileName)
{
	String regPattern = "(.*\\.ab1)(\\s\\d)";
	Pattern p = Pattern.compile(regPattern);
	Matcher m = p.matcher(seqFileName);

	if (m.matches())
	{
		if (m.group(2).matches(" 2"))
		{
		return true;
		}
	}
	return false;
}


/**
*
* Use regex to determine if the sequence is of type original.
*
* The original raw files are flagged with the "original" suffix.
* This is used to differentiate between the post-trimmed photos
* which were renamed to the original names.
*/
public static boolean isOriginal(String sequenceName)
{
	String regexPattern = "(.*\\.ab1)(\\s\\w+)";
	Pattern pat = Pattern.compile(regexPattern);
	Matcher matching = pat.matcher(sequenceName);

	if (matching.matches())
	{
		if (matching.group(2).matches(" original"))
		{
			return true;
		}
	}
	return false;

}


/**
*
* Searches the entire document viewer for individual sequences that are > 100 bp
* post-trim and returns the list.
*
* @param query a list of all the sequence uploaded
* @return a filtered list with only sequences that have a length of > 99
*/

public static List<AnnotatedPluginDocument> getThresholdRaw(List<AnnotatedPluginDocument> query)
{

	// Contains all individual sequences with > 100 bp post-trim
	List<AnnotatedPluginDocument> abSequences = new ArrayList<AnnotatedPluginDocument>();

	for (AnnotatedPluginDocument docs : query)
	{
		// Get the type of sequence; we're interested in the individual .ab1 files
		String queryResult = docs.getDocumentClass().toString();

		// Checks if both nucleotide seuqnece and is a post-trim sequence
		if (queryResult.contains("DefaultNucleotideGraphSequence") &&
			isOriginal(docs.getName()) != true)
			{
			String num = docs.getFieldValue(DocumentField.POST_TRIM_LENGTH).toString();
			int seqLength = Integer.parseInt(num);
			if (seqLength > 99)
				{
				abSequences.add(docs);
				}
			}
	}

	return abSequences;

}