# TextpressoAnnotations
**Requirements to run**: Textpresso annotation files and the original text documents. See below for details on acquiring annotation files.

**To run**: Place the textpresso annotation files in the TextpressoOutput directory. Create a directory for the original text files called "rawtext" and place the files inside of it. Running the program will output a .tsv file in the GO directory for each annotation file. The .tsv files will contain a list of GO annotations (Starting index of Textpresso annotation term, Ending index of Textpresso annotation term, GO:ID, Textpresso annotation term, and ontology reference term).

## Textpresso Annotation Files
Download and install [Textpresso 2.5](http://www.textpresso.org/textpresso2.5.html). This requires running an Apache server with Perl enabled. To run Textpresso on newer versions of Perl (after v5.8.8), remove all occurrances of the "defined" keyword for arrays and hashes (@ or %). If default settings were used during installation, place the text documents to be annotated in /usr/local/textpresso/myliterature/Data/includes/body. Then to annotate the files, run the MarkupFromScratch.com script (default location: /usr/local/textpresso/myliterature/Procedures/wrappers). The annotation files will then be located in /usr/local/textpresso/myliterature/Data/annotations/body/semantic (default location).
