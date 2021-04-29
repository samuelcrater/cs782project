import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

public class IsolationTree {

    private Node root;
    private double[][] dataset;
    private int heightLimit;
    private boolean splitOnProximity;
    
    public IsolationTree(double[][] dataset, List<Integer> subset, int heightLimit,
            boolean splitOnProximity) {
        this.dataset = dataset;
        this.heightLimit = heightLimit;
        this.splitOnProximity = splitOnProximity;
        root = new Node(dataset, subset, 0);
    }
    
    //not strictly path length; depth + number of elements in final node
    public double getPathLength(double[] x) {
        double depth = 0;
        Node current = root;
        
        while (current.left != null) { //children are either both null or both instantiated
            if (splitOnProximity) {
                if (current.isNearHyperplane(x)) {
                    current = current.left;
                } else {
                    current = current.right;
                }
            } else {
                if (current.isAboveHyperplane(x)) {
                    current = current.left;
                } else {
                    current = current.right;
                }
            }
            depth++;
        }
        
        return depth + ExtendedIsolationForest.getAvgPathLength(current.size);
    }
    
    private class Node {
        private Node left, right;
        private double[] hyperplaneCoefficients;
        private double[] hyperplaneOffset;
        private int depth, size;
        private double[][] dataset;
        private double nearDistance;
        //CHANGES:
        //Node minimum size changed from 2
        //Node element sort code changed
        //getPathLength condition changed from isAboveHyperplane
        //EIF max tree height changed
        public Node(double[][] dataset, List<Integer> subset, int depth) {
            this.depth = depth;
            this.dataset = dataset;
            this.size = subset.size();
            //condition for being a leaf node
            if (subset.size() < 2 || depth >= heightLimit) {
                return;
            }
            
            //generate hyperplane
            double[][] range = getRange(subset);
            NormalDistribution n = new NormalDistribution(0, 1);
            hyperplaneCoefficients = new double[dataset[0].length];
            hyperplaneOffset = new double[dataset[0].length];
            for (int i = 0; i < dataset[0].length; i++) {
                hyperplaneCoefficients[i] = n.sample();
                if (range[0][i] != range[1][i]) {
                    UniformRealDistribution u = new UniformRealDistribution(range[0][i], range[1][i]);
                    hyperplaneOffset[i] = u.sample();
                } else {
                    //System.out.println("Equivalent values when at size " + subset.size());
                }
            }
            
            //sort data into left and right children
            List<Integer> leftElements = new ArrayList<>();
            List<Integer> rightElements = new ArrayList<>();
            //begin change code
            if (splitOnProximity) {
                List<Pair<Integer, Double>> distances = new ArrayList<>();
                for (Integer i : subset) {
                    distances.add(new Pair<Integer, Double>(i, getDistToHyperplane(dataset[i])));
                }
                Collections.sort(distances, new Comparator<Pair<Integer, Double>>() {
                    @Override
                    public int compare(Pair<Integer, Double> arg0, Pair<Integer, Double> arg1) {
                        if (arg0.r - arg1.r > 0) {
                            return 1;
                        } else if (arg0.r - arg1.r < 0) {
                            return -1;
                        } else {
                            return 0;
                        }
                        //return  > 0 ? 1 : -1;
                    }
                });
                double portionConsideredClose = .1; //hyperparameter
                this.nearDistance = distances.get((int)(distances.size() * portionConsideredClose)).r;
                for(int i = 0; i < distances.size(); i++) {
                    if (i < portionConsideredClose * distances.size()) {
                        leftElements.add(distances.get(i).l);
                    } else {
                        rightElements.add(distances.get(i).l);
                    }
                }
                //end change code
            } else {
                for (Integer i : subset) {
                    //for (int j = 0; j < dataset[0].length; j++) { //why tf did i do this
                        if (isAboveHyperplane(dataset[i])) {
                            leftElements.add(i);
                        } else {
                            rightElements.add(i);
                        }
                    //}
                }
            }
            left = new Node(dataset, leftElements, depth + 1);
            right = new Node(dataset, rightElements, depth + 1);
        }
        
        public boolean isAboveHyperplane(double[] x) {
            //condition: (x - p) * n > 0
            //x is the input point. p is a point on the plane. n is the normal vector of the plane
            double result = 0;
            for (int i = 0; i < dataset[0].length; i++) {
                result += (x[i] - hyperplaneOffset[i]) * hyperplaneCoefficients[i];
            }
            return result > 0 ? true : false;
        }
        
        public double getDistToHyperplane(double[] x) {
            double numerator = 0;
            for (int i = 0; i < dataset[0].length; i++) {
                //calculates d
                numerator += hyperplaneOffset[i] * hyperplaneCoefficients[i];
                //ax + by + ...
                numerator += x[i] * hyperplaneCoefficients[i];
            }
            numerator = Math.abs(numerator);
            double denominator = 0;
            for (int i = 0; i < dataset[0].length; i++) {
                //calculates d
                denominator += Math.pow(hyperplaneCoefficients[i], 2);
            }
            denominator = Math.sqrt(denominator);
            
            /*
            double result = 0;
            for (int i = 0; i < dataset[0].length; i++) {
                result += (x[i] - hyperplaneOffset[i]) * hyperplaneCoefficients[i];
            }
            return Math.abs(result);
            */
            return numerator / denominator;
        }
        
        public boolean isNearHyperplane(double[] x) {
            return getDistToHyperplane(x) < nearDistance;
        }
        
        private double[][] getRange(List<Integer> subset) {
            //min and max of subset of dataset for all dimensions
            double[][] range = new double[2][dataset[0].length];
            
            //set initial values to the first element in subset
            for (int i = 0; i < dataset[0].length; i++) {
                range[0][i] = dataset[subset.get(0)][i];
                range[1][i] = dataset[subset.get(0)][i];
            }
            
            //walk through subset, remember min and max of each dimension
            for (int i = 1; i < subset.size(); i++) {
                for (int j = 0; j < dataset[0].length; j++) {
                    if (dataset[subset.get(i)][j] < range[0][j]) {
                        range[0][j] = dataset[subset.get(i)][j];
                    }
                    if (dataset[subset.get(i)][j] > range[1][j]) {
                        range[1][j] = dataset[subset.get(i)][j];
                    }
                }
            }
            
            return range;
        }
    }
}
