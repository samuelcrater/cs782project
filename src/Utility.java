import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Utility {
    
    private static Pair<double[][], int[]> data;
    private static int dataOutlierCount, dataLength;
    private static ExtendedIsolationForest eif;
    
    public static Pair<double[][], int[]> loadSatellite(String path) {
        double[][] satellite = new double[6435][36];
        int[] labels = new int[6435];
        try {
            Scanner scan = new Scanner(new File(path));
            int line = 0;
            while (scan.hasNextInt()) {
                for (int i = 0; i < 36; i++) {
                    satellite[line][i] = (scan.nextInt() / 255.0);
                }
                int rawLabel = scan.nextInt();
                if (rawLabel == 2 || rawLabel == 4 || rawLabel == 5) {
                    labels[line] = 1;
                } else {
                    labels[line] = 0;
                }
                line++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        return new Pair<double[][], int[]>(satellite, labels);
    }
    
    public static Pair<double[][], int[]> loadMammography(String path) {
        double[][] mammography = new double[11183][6];
        int[] labels = new int[11183];
        try {
            Scanner scan = new Scanner(new File(path));
            scan.nextLine(); //mammography has a header line
            scan.useDelimiter("[,\\n]");//comma delimited
            int line = 0;
            while (scan.hasNextLine()) {
                for (int i = 0; i < 6; i++) {
                    mammography[line][i] = scan.nextDouble();
                }
                //if (line % 100 == 0) System.out.println(Arrays.toString(mammography[line]));
                int rawLabel = scan.nextInt();
                if (rawLabel == 1) {
                    labels[line] = 1;
                } else {
                    labels[line] = 0;
                }
                line++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        return new Pair<double[][], int[]>(mammography, labels);
    }
    
    public static Pair<double[][], int[]> loadIonosphere(String path) {
        double[][] ionosphere = new double[351][34];
        int[] labels = new int[351];
        try {
            Scanner scan = new Scanner(new File(path));
            scan.useDelimiter("[,\\n]");//comma delimited
            int line = 0;
            while (scan.hasNextInt()) {
                for (int i = 0; i < 34; i++) {
                    ionosphere[line][i] = scan.nextDouble();
                }
                String rawLabel = scan.next();
                if (rawLabel.equals("b")) {
                    labels[line] = 1;
                } else {
                    labels[line] = 0;
                }
                line++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        return new Pair<double[][], int[]>(ionosphere, labels);
    }
    
    public static Pair<double[][], int[]> loadForestCover(String path) {
        double[][] forestCover = new double[286048][10]; //ran through dataset and counted
        int[] labels = new int[286048];
        try {
            Scanner scan = new Scanner(new File(path));
            scan.useDelimiter("[,\\n]");//comma delimited
            int line = 0;
            while (scan.hasNextInt()) {
                double[] currentLine = new double[10];
                
                for (int i = 0; i < 54; i++) {
                    double currentVar = scan.nextInt();
                    if (i < 10) {
                        currentLine[i] = currentVar;
                    }
                }
                //if (line == 20) System.out.println(Arrays.toString(currentLine));
                int label = scan.nextInt();
                if (label == 4 || label == 2) {
                    if (label == 4) {
                        labels[line] = 1;
                    }
                    for (int i = 0; i < 10; i++) {
                        forestCover[line][i] = currentLine[i];
                    }
                    line++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new Pair<double[][], int[]>(forestCover, labels);
    }
    
    public static Pair<double[][], int[]> loadCardio(String path) {
        double[][] cardio = new double[1831][22]; //ran through dataset and counted
        int[] labels = new int[1831];
        try {
            Scanner scan = new Scanner(new File(path));
            scan.useDelimiter("[,\\n]"); //comma delimited
            scan.nextLine();
            scan.nextLine(); //header row and empty row
            int line = 0;
            while (scan.hasNext()) {
                double[] currentLine = new double[22];
                for(int i = 0; i < 6; i++) { //trash at beginning
                    scan.next();
                }
                for (int i = 0; i < 22; i++) {
                    double currentVar = scan.nextDouble();
                    currentLine[i] = currentVar;
                }
                for(int i = 0; i < 11; i++) { //trash at end except final entry
                    scan.next();
                }
                int label = scan.nextInt();
                scan.nextLine();
                if (label == 1 || label == 3) {
                    if (label == 3) {
                        labels[line] = 1;
                    }
                    for (int i = 0; i < 21; i++) {
                        cardio[line][i] = currentLine[i];
                    }
                    line++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new Pair<double[][], int[]>(cardio, labels);
    }
    
    /*
    public static void runEIF_LOF() {
        Pair<double[][], int[]> sat = loadSatellite("sat.all");
        double[][] dataset = sat.l;
        int[] labels = sat.r;
        EIF_LOF eiflof = new EIF_LOF(dataset, labels);
        int[] outliers = eiflof.getOutliers();
        int correctOutliers = 0;
        int allOutliers = 0;
        for (int i = 0; i < outliers.length; i++) {
            if (labels[outliers[i]] == 1) {
                correctOutliers++;
            }
        }
        for (int i = 0; i < dataset.length; i++) {
            if (labels[i] == 1) {
                allOutliers++;
            }
        }
        System.out.println("EIF_LOF: " + outliers.length + " points declared outliers.");
        System.out.println("Found " + correctOutliers + " out of " + allOutliers + " outliers.");
    }
    */

    private static Pair<double[][], int[]> getDataset(String filename) {
        if (filename.equals("sat.all")) {
            return loadSatellite(filename);
        } else if (filename.equals("mammography.csv")) {
            return loadMammography(filename);
        } else if (filename.equals("ionosphere.data")){
            return loadIonosphere(filename);
        } else if (filename.equals("covtype.data")) {
            return loadForestCover(filename);
        } else if (filename.equals("cardio.csv")) {
            return loadCardio(filename);
        }
        throw new RuntimeException("Bad filename passed to getDataset.");
    }
    
    //because im lazy and a bad programmer
    private static Pair<Integer, Integer> runEIF(String filename, double threshold,
            int numberOfOutliers, boolean proximity) {
        //if (data == null) data = loadSatellite(filename);
        if (data == null) data = getDataset(filename);
        double[][] dataset = data.l;
        int[] labels = data.r;
        int truePositives = 0;
        int allOutliers = 0;
        for (int i = 0; i < dataset.length; i++) {
            if (labels[i] == 1) {
                allOutliers++;
            }
        }
        dataLength = data.l.length;
        dataOutlierCount = allOutliers;
        
        if (eif == null) eif = new ExtendedIsolationForest(dataset, 100, 256, proximity);
        List<Integer> iforestOutliers;
        if (numberOfOutliers > 0) {
            iforestOutliers = eif.getOutliers(numberOfOutliers);
        } else {
            iforestOutliers = eif.getOutliers(threshold);
        }
        for (int i = 0; i < iforestOutliers.size(); i++) {
            if (labels[iforestOutliers.get(i)] == 1) {
                truePositives++;
            }
        }
        //System.out.println("EIF: " + iforestOutliers.size() + " points declared outliers.");
        //System.out.println("Found " + truePositives + " out of " + allOutliers + " outliers.");
        //returns true positives and all positives
        return new Pair<Integer, Integer>(truePositives, iforestOutliers.size());
    }
    
    public static double eifAUC(String filename, boolean proximity, boolean write) {
        int numberOfRuns = 100;
        double area = 0;
        
        //run eif with varrying threshold
        List<Pair<Double, Double>> rates = new LinkedList<>();
        for (int i = 0; i < numberOfRuns; i++) {
            Pair<Integer, Integer> data = runEIF(filename, i*(1.0 / numberOfRuns), -1, proximity);
            
            rates.add(new Pair<Double, Double>((data.l / (double)dataOutlierCount),
                    (data.r - data.l) / (dataLength - (double)dataOutlierCount)));
        }
        
        //sort by false positive rate
        for (int i = 0; i < numberOfRuns; i++) {
            int min = i;
            for (int j = i; j < numberOfRuns; j++) {
                if (rates.get(j).r < rates.get(min).r) {
                    min = j;
                }
            }
            rates.add(i, rates.remove(min));
        }
        
        //area is positiveRate[i] * (negativeRate[i+1] - negativeRate[i])
        for(int i = 0; i < numberOfRuns - 1; i++) {
            //Trapezoidal Riemann sum
            area += rates.get(i).l * (rates.get(i + 1).r - rates.get(i).r);
            area += .5 * (rates.get(i+1).l - rates.get(i).l) * (rates.get(i + 1).r - rates.get(i).r);
            //System.out.println(rates.get(i).l + "," + rates.get(i).r);
        }
        area += rates.get(numberOfRuns - 1).l * (1 - rates.get(numberOfRuns - 1).r);
        area += .5 * (1 - rates.get(numberOfRuns - 1).l) * (1 - rates.get(numberOfRuns - 1).r);
        
        if (write) {
            String name = filename + "-auc-" + area;
            writeAUC(name, rates);
        }
        return area;
    }
    
    public static void writeAUC(String filename, List<Pair<Double, Double>> data) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
                String header = "truePositiveRate,falsePositiveRate\n";
                Files.write(Paths.get(filename), header.getBytes(), StandardOpenOption.APPEND);
            }
            
            DecimalFormat df = new DecimalFormat("#.#####");
            String output = "";
            for (Pair<Double, Double> p : data) {
                output += df.format(p.l) + "," + df.format(p.r) + "\n";
            }
            Files.write(Paths.get(filename), output.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Error writing to file.");
        }
    }
    
    public static void reset() {
        eif = null;
        data = null;
    }
    
    /**
     * Hello! If you're reading this it means you probably want to play with my awful
     * code. It started pretty well but this class is a total mess. I might suggest
     * just running main() as is. :) It currently is rigged to calculate the AUC for
     * all five datasets using EIF with the proximity change.
     * There is also a commented out section that gathers some basic statistics from
     * 100 AUC calculations on a given dataset.
     * 
     * To measure the AUC of a ROC graph, call eifAUC(). eifAUC() takes the name of
     * the dataset, a Boolean for whether you want to use EIF with its proximity mode,
     * and a Boolean for whether you want to write the individual plot points to a file.
     * 
     * @param args
     */
    public static void main(String[] args) {
        /*
        double total = 0, min = 1, max = 0;
        for (int i = 0; i < 100; i++) {
            double current = eifAUC("mammography.data", false, i==50?true:false);
            total += current;
            if (current < min) min = current;
            if (current > max) max = current;
            //force remake of eif each run
            eif = null;
        }
        System.out.println("AUC stats from 100 runs of EIF:\n\t Min: " + min + " Max: " + max
                + " Average: " + total/100);
        */
        System.out.println("EIF Cardio AUC: " + eifAUC("cardio.csv", true, false));
        //for performance and evaluation consistency when doing a great number of runs,
        //the class will use the last EIF instance and dataset unless cleared.
        reset(); 
        System.out.println("EIF ForestCover AUC: " + eifAUC("covtype.data", true, false));
        reset(); 
        System.out.println("EIF Ionosphere AUC: " + eifAUC("ionosphere.data", true, false));
        reset(); 
        System.out.println("EIF Mammography AUC: " + eifAUC("mammography.csv", true, false));
        reset(); 
        System.out.println("EIF Satellite AUC: " + eifAUC("sat.all", true, false));
    }
}
