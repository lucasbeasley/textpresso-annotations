import java.io.*;
import java.util.*;


/**
 * Lucas Beasley
 * 08/01/17
 * Retrieving GO annotations from a Textpresso annotation file.
 */

class Main{
    public static class Annotation {
        private String term = "";           //term in paper
        private String id = "";             //GO:ID for ontology term
        private String ref = "";            //ontology term
        private String startIndex = "";     //term's starting index in paper
        private String endIndex = "";       //term's ending index in paper
        private int annoCount = 0;          //used to check if part of the same Textpresso annotation
        private int secWordNum = 0;         //used to check if referring to the same word in paper

        //getters/setters
        public void setTerm(String term){ this.term = term; }
        public void setID(String id){ this.id = id; }
        private void setRef(String ref){ this.ref = ref; }
        private void setStartIndex(String start){ this.startIndex = start; }
        private void setEndIndex(String end){ this.endIndex = end; }
        private void setAnnoCount(int ac){ this.annoCount = ac; }
        private void setSecWordNum(int secWordNum){ this.secWordNum = secWordNum; }
        public String getTerm(){ return this.term; }
        public String getID(){ return this.id; }
        private String getRef(){ return this.ref; }
        private String getStartIndex(){ return this.startIndex; }
        private String getEndIndex(){ return this.endIndex; }
        private int getAnnoCount(){ return this.annoCount; }
        private int getSecWordNum(){ return this.secWordNum; }
    }


    /***
     * pullAnnos pulls individual GO annotations from a Textpresso annotation output file. Each annotation will contain
     * the term, the word count assigned by Textpresso (in a section), the annotation count, the ontology reference term,
     * and the GO ID associated with the ontology reference term.
     * @param directory - directory containing Textpresso output
     * @return list containing GO annotations
     */
    private static Map<String, List<Annotation>> pullAnnos(File directory){
        String line, filename, goID, ref, term;
        int dotIndex, count = 0, secwordnum;
        List<Annotation> annos;
        Annotation tempAnno;
        Map<String, List<Annotation>> GOannos = new HashMap<>();

        for (File inputFile : directory.listFiles()){
            //get file name for key
            filename = inputFile.getName();
            annos = new ArrayList<>();
            try{
                Scanner scan = new Scanner(inputFile);
                while(scan.hasNextLine()){
                    line = scan.nextLine();

                    //check if beginning of annotation
                    if(line.equals("## BOA ##")){
                        line = scan.nextLine();
                        //grab term from annotation (2nd line)
                        term = line;
                        count++;
                        //get to annotations
                        secwordnum = Integer.parseInt(scan.nextLine());
                        line = scan.nextLine();
                        //pull GO annotations from entry
                        while (!line.equals("## EOA ##")){
                            if (line.contains("source='GO-v1.934'")){
                                tempAnno = new Annotation();
                                tempAnno.setTerm(term);
                                tempAnno.setAnnoCount(count);
                                tempAnno.setSecWordNum(secwordnum);
                                //get ID and ref term
                                dotIndex = line.indexOf('.');
                                goID = "GO:" + line.substring(dotIndex-7, dotIndex);
                                ref = line.substring(dotIndex+1, line.indexOf(".2", dotIndex));
                                //clean up ref term
                                ref = ref.replaceAll("_", " ");
                                //set for annotation
                                tempAnno.setID(goID);
                                tempAnno.setRef(ref);
                                //add annotation to list of annotations
                                annos.add(tempAnno);
                            }
                            line = scan.nextLine();
                        }
                    }
                }
            }
            catch (FileNotFoundException ex){
                System.out.println("Error: File not found.");
            }
            //add list of annotations and the originating file title to a map
            GOannos.put(filename, annos);
        }
        return GOannos;
    }


    /***
     * getIndexes retrieves the starting and ending indexes of annotated terms within individual papers.
     * @param annos - map of filenames and a list of their annotations
     * @param directory - directory of the raw text data
     * @return map of filenames and a list of their annotations (filled in indexes)
     */
    private static Map<String, List<Annotation>> getIndexes(Map<String, List<Annotation>> annos, File directory){
        List<Annotation> tempList;
        String text, annoterm, filen, lastannoterm, termbeforelastannoterm;
        int lastindex, termlength, tempstartindex, tempendindex,
                lastcount, annocount, secwordnum, lastsecwordnum;

        for(File inputFile : directory.listFiles()){
            filen = inputFile.toString();
            filen = filen.substring(filen.length()-12, filen.length());
            if(annos.containsKey(filen)){
                //pull in list of annotations
                tempList = annos.get(filen);
                //reset values
                termbeforelastannoterm = "";
                lastannoterm = "";
                tempstartindex = 0;
                tempendindex = 0;
                lastcount = 0;
                lastsecwordnum = -2;
                try{
                    //load text file
                    Scanner scan = new Scanner(inputFile);
                    StringBuilder stringBuilder = new StringBuilder(scan.nextLine());
                    while(scan.hasNextLine()){
                        stringBuilder.append(scan.nextLine());
                    }
                    text = stringBuilder.toString();
                    //search for indexes and assign to annotations
                    for(Annotation temp : tempList){
                        annoterm = temp.getTerm();
                        annocount = temp.getAnnoCount();
                        secwordnum = temp.getSecWordNum();

                        //***possible revision needed*** Textpresso outputs terms that do not exist literally in document
                        //(outputs other terms similar/referring to document terms)
                        switch(annoterm){
                            case "poly _ORB_ A":
                                annoterm = "poly";
                                break;
                            case "ATP-binding cassette _ORB_ ABC":
                                annoterm = "ATP-binding cassette";
                                break;
                            case "A-specific":
                                annoterm = "Aβ-specific";
                                break;
                        }

                        //check if part of the same Textpresso annotation
                        if(annoterm.equals(lastannoterm) && annocount == lastcount){
                            temp.setStartIndex(Integer.toString(tempstartindex));
                            temp.setEndIndex(Integer.toString(tempendindex));
                        }
                        //check if current term contains the last term
                        else if(annoterm.contains(lastannoterm) && secwordnum == lastsecwordnum){
                            temp.setStartIndex(Integer.toString(tempstartindex));

                            termlength = annoterm.length();

                            tempendindex = tempstartindex+termlength;
                            temp.setEndIndex(Integer.toString(tempendindex));

                            termbeforelastannoterm = lastannoterm;
                            lastannoterm = annoterm;
                        }
                        //check if current term was part of the last two terms
                        else if((lastannoterm.contains(annoterm) || termbeforelastannoterm.contains(annoterm)) && (secwordnum-1 == lastsecwordnum ||
                                secwordnum-2 == lastsecwordnum || secwordnum-3 == lastsecwordnum)){
                            tempstartindex = text.indexOf(annoterm, tempstartindex);
                            temp.setStartIndex(Integer.toString(tempstartindex));

                            termlength = annoterm.length();

                            temp.setEndIndex(Integer.toString(tempstartindex+termlength));

                            termbeforelastannoterm = lastannoterm;
                            lastannoterm = annoterm;
                            lastsecwordnum = secwordnum;
                        }
                        //new term
                        else {
                            lastindex = tempendindex;
                            termlength = annoterm.length();

                            tempstartindex = text.indexOf(annoterm, lastindex);
                            temp.setStartIndex(Integer.toString(tempstartindex));

                            tempendindex = tempstartindex+termlength;
                            temp.setEndIndex(Integer.toString(tempendindex));

                            termbeforelastannoterm = lastannoterm;
                            lastannoterm = annoterm;
                            lastcount = annocount;
                            lastsecwordnum = secwordnum;
                        }
                    }
                    annos.replace(filen, tempList);
                }
                catch (FileNotFoundException ex){
                    System.out.println("Error: File not found.");
                }
            }
        }
        return annos;
    }

    /***
     * writeOut writes a tab-separated file for each paper. The file will contain the GO annotations tagged by Textpresso.
     * @param annoMap - map of full annotation data
     */
    private static void writeOut(Map<String, List<Annotation>> annoMap, File directory){
        String filename;
        for(String key : annoMap.keySet()){
            //setup file
            filename = directory + "/" + key.substring(0, key.length()-4) + ".tsv";
            File filen = new File(filename);

            //pull in annotations
            List<Annotation> annos = annoMap.get(key);

            //create and write to file
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filen, false)))) {
                writer.println("StartIndex\tEndIndex\tGO:ID\tTerm\tOntologyTerm");
                for (Annotation a : annos){
                    writer.println(a.getStartIndex() + "\t" + a.getEndIndex() + "\t" + a.getID()
                            + "\t" + a.getTerm() + "\t" + a.getRef());
                }
            } catch (FileNotFoundException ex) {
                System.out.println("Error: File not found; could not append.");
                System.out.println("File path: " + filen);
            } catch (UnsupportedEncodingException ex) {
                System.out.println("Error: Unsupported encoding; could not append.");
                System.out.println("File path: " + filen);
            } catch (IOException ex) {
                System.out.println("Error: IO Exception; could not append.");
                System.out.println("File path: " + filen);
            }
        }
    }

    public static void main(String[] args) {
        //name of directory that contains Textpresso output (annotations)
        File directory = new File("Output/TextpressoOutput");

        //map containing filenames and a list of the annotations within them
        Map<String, List<Annotation>> GOannotations = pullAnnos(directory);

        //name of directory that contains the raw text data that was ran through Textpresso (.txt)
        directory = new File("rawtext");

        //map containing filenames and a list of the annotations within them and their indexes
        GOannotations = getIndexes(GOannotations, directory);

        //name of directory for .tsv output
        directory = new File("Output/GO");

        //print out tsv for each file
        writeOut(GOannotations, directory);
    }
}
