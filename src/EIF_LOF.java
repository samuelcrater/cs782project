import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class EIF_LOF {
    private double[][] dataset;
    private int[] labels;
    //array of indices of candidate points
    private int[] candidates;
    private double outlierThreshold;
    private ExtendedIsolationForest eif;
    private List<List<Neighbor>> neighborsList;
    private double[] lof;
    private int numberOfOutliers;
    
    public EIF_LOF(double[][] dataset, int[] labels) {
        this.dataset = dataset;
        this.labels = labels;
        eif = new ExtendedIsolationForest(dataset, 40, 128, false);
        neighborsList = new ArrayList<>(dataset.length);
        for (int i = 0; i < dataset.length; i++) {
            neighborsList.add(null);
        }
    }
    
    public int[] getOutliers() {
        calculateOutlierThreshold();
        //get outlier scores for all points from eif. we will then take the top threshold% 
        //eif returns descending list of outliers
        List<Integer> output = eif.getOutliers(dataset.length);
        System.out.println(output.size() + " outliers found from EIF.");
        numberOfOutliers = (int) (dataset.length * outlierThreshold);
        candidates = new int[numberOfOutliers];
        for (int i = 0; i < numberOfOutliers; i++) {
            candidates[i] = output.get(i);
        }
        lof = new double[candidates.length];
        System.out.println("Outlier threshold: " + outlierThreshold
                + ". Dataset size * threshold= " + numberOfOutliers + " " + candidates.length);
        return LOF(15);
    }
    
    //evaluates lof only on the candidate points
    private int[] LOF(int k) {
        //takes in list of candidate points, list of whole dataset,
        //runs LOF on candidates
        //top X scoring points reported as outliers
        //automatically determine number of outliers based on threshold and data size?
        
        for (int i = 0; i < candidates.length; i++) {
            List<Neighbor> neighbors = getNeighbors(candidates[i]);
            double avgReachDens = 0;
            for (int j = 0; j < k; j++) {
                avgReachDens += getReachabilityDensity(neighbors.get(j).index, k);
            }
            lof[i] = (avgReachDens / k) / getReachabilityDensity(candidates[i], k);
        }
        
        //sort candidates by lof score
        //we want to sort lof and apply the equivalent transformation to candidates
        //then pull their indices
        List<Neighbor> outlierList = new ArrayList<>(candidates.length);
        for (int i = 0; i < candidates.length; i++) {
            outlierList.add(new Neighbor(lof[i], candidates[i]));
        }
        Collections.sort(outlierList);
        //TODO this should not be numberOfOutliers
        int m = 2036; 
        int[] outliers = new int[m];
        for (int i = outliers.length - 1; i > outliers.length - m; i--) {
            outliers[outliers.length - 1 - i] = outlierList.get(i).index;
        }
        
        return outliers;
    }
    
    //takes the index of the point in the dataset, NOT in the candidate set
    private List<Neighbor> getNeighbors(int index) {
        if (neighborsList.get(index) != null)
            return neighborsList.get(index);
        
        List<Neighbor> neighbors = new LinkedList<>();
        for (int i = 0; i < dataset.length; i++) {
            //dont count yourself
            if (index == i) continue;
            double distance = 0;
            for (int j = 0; j < dataset[0].length; j++) {
                distance += Math.pow(dataset[index][j] - dataset[i][j], 2);
            }
            distance = Math.sqrt(distance);
            neighbors.add(new Neighbor(distance, i));
        }
        
        Collections.sort(neighbors);
        neighborsList.set(index, neighbors);
        return neighbors;
    }
    
    private double getReachabilityDensity(int index, int k) {
        List<Neighbor> neighbors = getNeighbors(index);
        double reachDens = 0;
        for (int i = 0; i < k; i++) {
            double ithNeighborKDist = getNeighbors(neighbors.get(i).index).get(k).dist;
            reachDens += Math.max(neighbors.get(i).dist, ithNeighborKDist);
        }
        reachDens = 1 / (reachDens / k);
        return reachDens;
    }
    
    private void calculateOutlierThreshold() {
        //calculate average of each dimension
        double[] dimensionAverages = new double[dataset[0].length];
        for (int i = 0; i < dataset.length; i++) {
            for (int j = 0; j < dataset[0].length; j++) {
                dimensionAverages[j] += dataset[i][j]; 
            }
        }
        for (int i = 0; i < dataset[0].length; i++) {
            dimensionAverages[i] /= dataset.length; 
        }
        
        //calculate dispersion in each dimension
        //dispersion coefficient: sqrt(sum(xi - xMean)^2 / n) / xMean
        //sum the squares of the differences
        double[] dimensionDispersions = new double[dataset[0].length];
        for (int i = 0; i < dataset.length; i++) {
            for (int j = 0; j < dataset[0].length; j++) {
                dimensionDispersions[j] += Math.pow(dataset[i][j] - dimensionAverages[j], 2);
            }
        }
        //divide by n, square root it, divide by mean
        for (int i = 0; i < dataset[0].length; i++) {
            dimensionDispersions[i] = Math.sqrt(dimensionDispersions[i] / dataset.length) / dimensionAverages[i];
        }
        
        
        //threshold. it is not clear how to calculate it
        //take average of M highest dispersion values, adjust by alpha
        //(alpha * topM_Df) / m
        double[] dispersionCopy = dimensionDispersions.clone();
        Arrays.sort(dispersionCopy);
        //hyperparameter <= dimensionality. unclear how to set
        int m = 15;
        //adjustment hyperparameter. close to 1 probably. unclear to how set
        double alpha = 2;
        //probably should be a sum
        double dispersionSum = 0;
        for (int i = dispersionCopy.length - 1; i >= dispersionCopy.length - m; i--) {
            dispersionSum += dispersionCopy[i];
        }
        outlierThreshold = (alpha * dispersionSum) / m;
        //System.out.println("Outlier threshold: " + outlierThreshold);
    }
    
    //lof, auc
    //take top 75% of lof scores?
    
    
    private static class Neighbor implements Comparable<Neighbor> {
        double dist;
        int index;
        public Neighbor(double dist, int index) {
            this.dist = dist;
            this.index = index;
        }

        @Override
        public int compareTo(Neighbor n) {
            if (n.dist > dist) {
                return -1;
            } else if (n.dist < dist) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
