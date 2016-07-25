// The following script was written to export Geneious documents after the workflow has completed. 
// Written by David Bour, 03/07/2016 for FUSION validation.

/**
*************
*  PART III
*************
*/


import java.io.*;
import java.text.*;
import java.util.regex.*;
import javax.swing.JOptionPane;


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


public static List<AnnotatedPluginDocument> performOperation(List<AnnotatedPluginDocument> documents, Options options,
													  ProgressListener progressListener) throws DocumentOperationException, IOException 
{
	
	// The following is used for naming purposes.
	
	Calendar theCurrentTime = new GregorianCalendar();
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");	
	String currentTimeString = sdf.format(theCurrentTime.getTime());

	
	// This is where Geneious API goes to work.
	
	// Gets the current working folder/directory on the left hand side of GUI.
	GeneiousService service = ServiceUtilities.getSelectedService();
	
	if (service instanceof DatabaseService) 
	
		{
			
		// Sets up for query.
		DatabaseService databaseService = (DatabaseService) service;
		
		// Get all documents in the document table and store into a list obj. 
		List<AnnotatedPluginDocument> documentInFolder = databaseService.retrieve("");
					
		if (documentInFolder.size() == 0) throw new DocumentOperationException("No documents in folder");

		try {
		
		// Convert the list into an array.
		AnnotatedPluginDocument[] documentsToExport = documentInFolder.toArray(new AnnotatedPluginDocument[documentInFolder.size()]);
		
		// API that does the exporting, the directory should be changed as needed.
		
		// RegEx to parse Plate number out
		String regPattern = ".*-(P\\d+)-\\D.*";
		String plate = "PlateUnknown";
		
		// Search through the document list for a file with "Plate##" in the name or else default to "PlateUnknown"
		for (int count = 0; count < documentsToExport.length; count++)
		{
			String getDocName = documentsToExport[count].getName();
			Pattern p = Pattern.compile(regPattern);
			Matcher m = p.matcher(getDocName);
			if (m.matches())
			{
				plate = m.group(1); 
				break;
			} 
		}
		
		// Create ArrayList to parse separate out different file-types
		ArrayList<AnnotatedPluginDocument> reportArray = new ArrayList<AnnotatedPluginDocument>();
		ArrayList<AnnotatedPluginDocument> conArray = new ArrayList<AnnotatedPluginDocument>();
		ArrayList<AnnotatedPluginDocument> seqArray = new ArrayList<AnnotatedPluginDocument>();
		ArrayList<AnnotatedPluginDocument> blastArray = new ArrayList<AnnotatedPluginDocument>();

		ArrayList<AnnotatedPluginDocument> probableArray = new ArrayList<AnnotatedPluginDocument>();
		
		// Go through documents after the analysis is done and sort into their respective arrays
		for (int x = 0; x < documentsToExport.length; x++)
		{
			String queryResult = documentsToExport[x].getDocumentClass().toString();
			
			if (queryResult.contains("DefaultNucleotideGraphSequence")) 
			{
				
				System.out.println(documentsToExport[x]);
				
				if (isOriginal(documentsToExport[x].getName()) != true) {
				
					seqArray.add(documentsToExport[x]);
				}
				
			} 
			else if (queryResult.contains("DefaultAlignmentDocument")) 
			{
				conArray.add(documentsToExport[x]);
			} 
			else if (queryResult.contains("NucleotideBlastSummaryDocument"))
			{

				String nameOfDoc = documentsToExport[x].getFieldValue("query").toString();
				if (!nameOfDoc.contains("ReadsConsensus"))
				{
				probableArray.add(documentsToExport[x]);
				}
				else
				{
				blastArray.add(documentsToExport[x]);
				}
			} 
			else 
			{
				reportArray.add(documentsToExport[x]);
			}
			
		}
		
		// Convert the ArrayList to Array for the exporter method
		AnnotatedPluginDocument[] report = new AnnotatedPluginDocument[reportArray.size()];
		AnnotatedPluginDocument[] consensus = new AnnotatedPluginDocument[conArray.size()];
		AnnotatedPluginDocument[] blast = new AnnotatedPluginDocument[blastArray.size()];
		AnnotatedPluginDocument[] rawseq =  new AnnotatedPluginDocument[seqArray.size()];

		AnnotatedPluginDocument[] prob = new AnnotatedPluginDocument[probableArray.size()];
		
		report = reportArray.toArray(report);
		consensus = conArray.toArray(consensus);
		blast = blastArray.toArray(blast);
		rawseq = seqArray.toArray(rawseq);

		prob = probableArray.toArray(prob);

		String fileName = "FUSION_RUN-" + plate + ".csv";
		
		

		// Citrix Testing
		
		PluginUtilities.exportDocuments(new File("T:////Geneious/Fusion/Fusion QAS/geneious_output/" + currentTimeString + "-rawseq-" + fileName), rawseq);

			try 
			{
				PluginUtilities.exportDocuments(new File("T:////Geneious/Fusion/Fusion QAS/geneious_output/" + currentTimeString + "-probableBLAST-" + fileName), prob);
			} 
			catch (UnsupportedOperationException pe)
			{
				File probFile = new File("T:////Geneious/Fusion/Fusion QAS/geneious_output/" + currentTimeString + "-probableBLAST-" + fileName);
				probFile.createNewFile();
				JOptionPane.showMessageDialog(null, "No probable positives found. \n Click 'OK' to continue.", "NOTICE", JOptionPane.WARNING_MESSAGE);
			} 

			try
			{
				PluginUtilities.exportDocuments(new File("T:////Geneious/Fusion/Fusion QAS/geneious_output/" + currentTimeString + "-consensus-" + fileName), consensus);
			}
			catch (UnsupportedOperationException ce)
			{
				File conFile = new File("T:////Geneious/Fusion/Fusion QAS/geneious_output/" + currentTimeString + "-consensus-" + fileName);
				conFile.createNewFile();
				JOptionPane.showMessageDialog(null, "No consensus sequences were made. \n Click 'OK' to continue.", "NOTICE", JOptionPane.WARNING_MESSAGE);
			}

			try
			{
				PluginUtilities.exportDocuments(new File("T:////Geneious/Fusion/Fusion QAS/geneious_output/" + currentTimeString + "-blast-" + fileName), blast);
			}
			catch (UnsupportedOperationException be)
			{
				File blastFile = new File("T:////Geneious/Fusion/Fusion QAS/geneious_output/" + currentTimeString + "-blast-" + fileName);
				blastFile.createNewFile();
				JOptionPane.showMessageDialog(null, "No BLAST results were found. \n Click 'OK' to continue.", "NOTICE", JOptionPane.WARNING_MESSAGE);
				
			}

			try
			{
				PluginUtilities.exportDocuments(new File("T:////Geneious/Fusion/Fusion QAS/geneious_output/" + currentTimeString + "-summary-" + fileName), report);
			}
			catch (UnsupportedOperationException summ)
			{
				File sumFile = new File("T:////Geneious/Fusion/Fusion QAS/geneious_output/" + currentTimeString + "-summary-" + fileName);
				sumFile.createNewFile();
				summ.printStackTrace();
				
			}

		
		} catch (IOException e) {
			throw new DocumentOperationException(e);
		} 
			
		}
		
	return Collections.emptyList();
}