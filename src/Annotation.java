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
        public void setTerm(String term){
            this.term = term;
        }
        public void setID(String id){
            this.id = id;
        }
        public void setRef(String ref){
            this.ref = ref;
        }
        public void setStartIndex(String start){ this.startIndex = start; }
        public void setEndIndex(String end){ this.endIndex = end; }
        public void setAnnoCount(int ac){
            this.annoCount = ac;
        }
        public void setSecWordNum(int secWordNum){ this.secWordNum = secWordNum; }
        public String getTerm(){
            return this.term;
        }
        public String getID(){
            return this.id;
        }
        public String getRef(){
            return this.ref;
        }
        public String getStartIndex(){ return this.startIndex; }
        public String getEndIndex(){ return this.endIndex; }
        public int getAnnoCount(){ return this.annoCount; }
        public int getSecWordNum(){ return this.secWordNum; }
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
        String text, annoterm, filen, lastannoterm;
        int lastindex, termlength, tempstartindex, tempendindex,
                lastcount, annocount, secwordnum, lastsecwordnum;
        boolean flag = false; //***remove*** trouble-shooting
        List<String> badfiles = new ArrayList<>(); //***remove*** trouble-shooting

        for(File inputFile : directory.listFiles()){
            filen = inputFile.toString();
            filen = filen.substring(filen.length()-12, filen.length());
            if(annos.containsKey(filen)){
                //pull in list of annotations
                tempList = annos.get(filen);
                //reset values
                text = "";
                lastannoterm = "";
                tempstartindex = 0;
                tempendindex = 0;
                lastcount = 0;
                lastsecwordnum = -2;
                try{
                    //load text file
                    Scanner scan = new Scanner(inputFile);
                    while(scan.hasNextLine()){
                        text += scan.nextLine() + " ";
                    }
                    //search for indexes and assign to annotations
                    for(Annotation temp : tempList){
                        annoterm = temp.getTerm();
                        annocount = temp.getAnnoCount();
                        secwordnum = temp.getSecWordNum();

                        //***possible revision needed*** Textpresso outputs terms that do not exist literally in document
                        //(outputs references)
                        if(annoterm.equals("poly _ORB_ A")){
                            annoterm = "poly";
                        }
                        else if(annoterm.equals("ATP-binding cassette _ORB_ ABC")){
                            annoterm = "ATP-binding cassette";
                        }

                        //***remove*** trouble-shooting
                        if(filen.equals("15005800.txt") && annoterm.equals("antibodies") && tempstartindex >= 48000){
                            boolean bool = true;
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
                            tempendindex = text.indexOf(" ", tempstartindex+termlength);
                            temp.setEndIndex(Integer.toString(tempendindex));

                            lastannoterm = annoterm;
                        }
                        //check if current term was part of the last term
                        else if(lastannoterm.contains(annoterm) && (secwordnum-1 == lastsecwordnum ||
                        secwordnum-2 == lastsecwordnum)){
                            tempstartindex = text.indexOf(annoterm, tempstartindex);
                            //***remove*** trouble-shooting
                            if(tempstartindex == -1){
                                flag = true;
                            }
                            temp.setStartIndex(Integer.toString(tempstartindex));

                            temp.setEndIndex(Integer.toString(tempendindex));

                            lastannoterm = annoterm;
                            lastsecwordnum = secwordnum;
                        }
                        //new term
                        else {
                            lastindex = tempendindex;
                            termlength = annoterm.length();

                            tempstartindex = text.indexOf(annoterm, lastindex);
                            //***remove*** trouble-shooting
                            if(tempstartindex == -1){
                                flag = true;
                            }
                            temp.setStartIndex(Integer.toString(tempstartindex));

                            tempendindex = text.indexOf(" ", tempstartindex+termlength);
                            temp.setEndIndex(Integer.toString(tempendindex));

                            lastannoterm = annoterm;
                            lastcount = annocount;
                            lastsecwordnum = secwordnum;
                        }
                        //***remove*** trouble-shooting
                        if(flag){
                            flag = false;
                            if(!badfiles.contains(inputFile.toString())){
                                badfiles.add(inputFile.toString());
                            }
                        }
                    }
                    annos.replace(filen, tempList);
                }
                catch (FileNotFoundException ex){
                    System.out.println("Error: File not found.");
                }
            }
        }
        //***remove*** trouble-shooting
        for(String file: badfiles){
            System.out.println(file);
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
        File directory = new File("TextpressoOutput");

        //map containing filenames and a list of the annotations within them
        Map<String, List<Annotation>> GOannotations = pullAnnos(directory);

        //name of directory that contains the raw text data that was ran through Textpresso (.txt)
        directory = new File("rawtext");

        //map containing filenames and a list of the annotations within them and their indexes
        GOannotations = getIndexes(GOannotations, directory);

        //name of directory for .tsv output
        directory = new File("GO");

        //print out tsv for each file
        writeOut(GOannotations, directory);

        //***ISSUE*** Some starting indexes are set to -1
    }
}
