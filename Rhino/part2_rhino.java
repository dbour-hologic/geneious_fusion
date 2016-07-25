import java.util.regex.*;

/**
*************
*  PART II
*************
*/

/**
* Compares the .ab1 files that are > 100 bp with consensus sequences made. If consensus is missing
* and > 100 bp, return it and BLAST the files.
*/

public static List<AnnotatedPluginDocument> performOperation(List<AnnotatedPluginDocument> documents, Options options,
													  ProgressListener progressListener) throws DocumentOperationException 

{

	// Gets the folder the files are in.
	GeneiousService service = ServiceUtilities.getSelectedService();
	DatabaseService databaseService = (DatabaseService) service;

	// Grab all files in the document viewer
	List<AnnotatedPluginDocument> documentsInFolder = databaseService.retrieve("");

	// Contains all the individual sequences used to make consesus sequences
	List<AnnotatedPluginDocument> alignmentSequences = getAlignmentDocuments(documentsInFolder);

	// Contains all of the individual sequences that are post-trim & > 100 bp
	List<AnnotatedPluginDocument> postTrimSeq = getThresholdRaw(documentsInFolder);

	// A list of all sequences with > 100 bp that didn't make consensus , "probable-positive"
	List<AnnotatedPluginDocument> completedList = comparisons(postTrimSeq, alignmentSequences);


	return completedList;
}


/**
*
* Searches the entire document viewer for consensus sequences and returns the 
* individual sequences used to make it.
*/
public static List<AnnotatedPluginDocument> getAlignmentDocuments(List<AnnotatedPluginDocument> query)
{
	
	// Contains all individual sequences used to make consensus
	List<AnnotatedPluginDocument> individualSequences = new ArrayList<AnnotatedPluginDocument>();

	for (AnnotatedPluginDocument docs : query)
	{
	// Get the type of sequence, we're interested in the alignment class documents
	String queryResult = docs.getDocumentClass().toString();
	if (queryResult.contains("DefaultAlignmentDocument"))
		{

			try 
			{
			PluginDocument innerDoc = docs.getDocument();
			DefaultAlignmentDocument alignmentDoc = (DefaultAlignmentDocument) innerDoc;

			// Grabs all sequences used to make this specific consensus document
			List<AnnotatedPluginDocument> referenceDocs = alignmentDoc.getReferencedDocuments(); 
			for (AnnotatedPluginDocument refDocs : referenceDocs)
				{
				individualSequences.add(refDocs);
				}
			}

			catch (DocumentOperationException e)
			{
			e.printStackTrace();
			}
		
		}
	}
	return individualSequences;
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
* Searches the entire document viewer for individual sequences that are > 100 bp post-trim
* returns the list 
*/
public static List<AnnotatedPluginDocument> getThresholdRaw(List<AnnotatedPluginDocument> query)
{

	// Contains all individual sequences with > 100 bp post trim
	List<AnnotatedPluginDocument> abSequences = new ArrayList<AnnotatedPluginDocument>();

	for (AnnotatedPluginDocument docs : query)
	{
	// Get the type of sequence, we're interested in the individual .ab1 files
	String queryResult = docs.getDocumentClass().toString();

	// Checks if both nucleotide sequence and is a post-trim sequence
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


/**
*
* Compares the sequences used to make the consensus with all sequences with > 100 bp
* Returns the sequences that are NOT in the consensus list.
*/
public static List<AnnotatedPluginDocument> comparisons(List<AnnotatedPluginDocument> query,
															List<AnnotatedPluginDocument> search)
{

	// QUERY - All documents with > 100 bp && post-trim sequence
	// SEARCH - All documents that made consensus

	List<AnnotatedPluginDocument> filteredList = new ArrayList<AnnotatedPluginDocument>();

	// Create a map with (String Name) : (Seq Object) from QUERY
	Map<String, AnnotatedPluginDocument> map1 = new HashMap<String, AnnotatedPluginDocument>();

	for (AnnotatedPluginDocument docs : query)
	{
	String nameOf = docs.getName();
	map1.put(nameOf, docs);
	}

	// Removes all consensus sequences, leaving only the ones with > 100 bp and no consensus
	for (AnnotatedPluginDocument docsRemove : search)
	{
	String getDocName = docsRemove.getName();
	map1.remove(getDocName);
	}

	// Push back remaining from the MAP 
	for (Map.Entry<String, AnnotatedPluginDocument> entry : map1.entrySet())
	{
	filteredList.add(entry.getValue());
	}

	System.out.println("Getting names that didn't make consensus...");
	for (AnnotatedPluginDocument getSome : filteredList) {
		System.out.println(getSome.getName());
	}
return filteredList;
}