import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExtendedIsolationForest {
    //sort not above/below, but near/far. near dist 3 std dev or similar
    //could generate two hyperplanes. same angle. from random point in range,
    //shift up/down by std dev of current points. or by like 1/4 the range
    //sort into points within this volume and outside of this volume
    /*
     * avg path length:c(n): 2H(n-1) - (2(n-1)/n)
     * for n being the number of samples of the whole dataset X
     * for H(i) being ln(i) + .5772156649
     * 
     * anomaly score of element x: s(x,n) = 2^-(E(h(x))/c(n))
     * for h(x) being the path length to an element
     * */
    
    private double[][] dataset;
    private int[] labels;
    private int[] candidates;
    private IsolationTree[] trees;
    private double[] anomalyScores;
    
    private int treeCount;
    private int heightLimit;
    private int sampleSize;
    private double avgPathLength;
    private boolean splitOnProximity;
    
    public ExtendedIsolationForest(double[][] dataset, int treeCount, int sampleSize,
            boolean splitOnProximity) {
        this.dataset = dataset;
        this.treeCount = treeCount;
        this.sampleSize = sampleSize; //authors suggest 128 or 256
        this.trees = new IsolationTree[treeCount];
        this.heightLimit = (int) Math.ceil(Math.log10(sampleSize)/Math.log10(2));
        this.anomalyScores = new double[dataset.length];
        //avg path length: c(n) = 2H(n-1) - (2(n-1)/n)
        //for n being the number of samples of the whole dataset X
        //for H(i) being ln(i) + .5772156649
        this.avgPathLength = getAvgPathLength(sampleSize);
        this.splitOnProximity = splitOnProximity;
    }
    
    public static double getAvgPathLength(int sampleSize) {
        if (sampleSize < 2) return 0;
        return 2 * (Math.log(sampleSize - 1) + .5772156649)
        - ((2 * (sampleSize - 1)) / sampleSize);
    }
    
    //returns the indices of all points with an outlier score higher than the threshold
    public List<Integer> getOutliers(double threshold) {
        if (trees[0] == null) {
            generateTrees();
            calculateAnomalyScores();
        }
        List<Integer> outliers = new ArrayList<>();
        for (int i = 0; i < dataset.length; i++) {
            if (anomalyScores[i] > threshold) {
                outliers.add(i);
            }
        }
        return outliers;
    }
    
    //returns the indices of the top N outliers
    public List<Integer> getOutliers(int n) {
        generateTrees();
        calculateAnomalyScores();
        List<Integer> outliers = new ArrayList<>(n);
        
        //sort a copy of the anomaly scores and mirror the sort on a list of indices
        double[] anomalyCopy = new double[anomalyScores.length];
        for (int i = 0; i < dataset.length; i++) {
            anomalyCopy[i] = anomalyScores[i];
        }
        int[] indices = new int[dataset.length];
        for (int i = 0; i < dataset.length; i++) {
            indices[i] = i;
        }
        
        //simple selection sort
        for (int i = 0; i < anomalyCopy.length; i++) {
            double maxElement = anomalyCopy[i];
            int maxElementIndex = i;
            for (int j = i; j < anomalyCopy.length; j++) {
                if (anomalyCopy[j] > maxElement) {
                    maxElement = anomalyCopy[j];
                    maxElementIndex = j;
                }
            }
            double tempAnomaly = anomalyCopy[i];
            int tempIndex = indices[i];
            anomalyCopy[i] = anomalyCopy[maxElementIndex];
            anomalyCopy[maxElementIndex] = tempAnomaly;
            indices[i] = indices[maxElementIndex];
            indices[maxElementIndex] = tempIndex;
        }
        System.out.println("EIF effective threshold: " + anomalyCopy[n-1]);
        for (int i = 0; i < n; i++) {
            outliers.add(indices[i]);
        }
        
        return outliers;
    }
    
    private void generateTrees() {
        //selection without replacement, as per paper
        if (sampleSize * treeCount < dataset.length) {
            List<Integer> indices = new ArrayList<>(dataset.length);
            for (int i = 0; i < dataset.length; i++) {
                indices.add(i);
            }
            Collections.shuffle(indices);
            for(int i = 0; i < treeCount; i++) {
                List<Integer> sample = new ArrayList<>(sampleSize);
                for (int j = 0; j < sampleSize; j++) {
                    sample.add(indices.get(i * sampleSize + j));
                }
                //trees[i] = new IsolationTree(dataset, new ArrayList<Integer>(sample), heightLimit);
                trees[i] = new IsolationTree(dataset, sample, heightLimit, splitOnProximity);
            }
        } else {
            //selection with replacement if dataset isnt big enough
            for(int i = 0; i < treeCount; i++) {
                Set<Integer> sampleSet = new HashSet<>(sampleSize);
                while (sampleSet.size() < sampleSize) {
                    sampleSet.add((int)(Math.random() * dataset.length));
                }
                List<Integer> sample = new ArrayList<>(sampleSet);
                trees[i] = new IsolationTree(dataset, sample, heightLimit, splitOnProximity);
            }
        }
    }
    
    //runThroughTrees //float[]
    private void calculateAnomalyScores() {
        double avgScore = 0;
        for (int i = 0; i < dataset.length; i++) {
            double pathLengthSum = 0;
            for (int j = 0; j < trees.length; j++) {
                pathLengthSum += trees[j].getPathLength(dataset[i]);
            }
            //if (i % 100 == 0)
            //    System.out.print("Point depth: " + trees[0].getPathLength(dataset[i]));
            //anomaly score of element x: s(x,n) = 2^-(E(h(x))/c(n))
            //for h(x) being the path length to an element
            anomalyScores[i] = Math.pow(2, -((pathLengthSum / treeCount) / avgPathLength));
            avgScore += anomalyScores[i];
            //if (i % 100 == 0)
            //    System.out.println(" Score: " + anomalyScores[i]);
        }
        //System.out.println("EIF average anomaly score: " + avgScore / dataset.length);
    }
    
    
}
