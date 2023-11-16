package com.github.williamli0707.webpanda.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class test {
    public static void main(String[] args) {
        RunestoneAPI api = new RunestoneAPI();
        System.out.println(api.requestHistory("lhs_5200016", "lhs_test_list2"));
        HashMap<String, double[]> h = api.getAllScores("lhs_6_1");
        for(String i: h.keySet()) System.out.printf("%.5f, ", h.get(i)[0]);
        System.out.println();
        System.out.println();
        for(String i: h.keySet()) System.out.printf("%.5f, ", h.get(i)[1]);
        System.out.println();


        StringBuilder list = new StringBuilder("lhs_6_1");
//        for(int i = 2;i <= 2;i++) list.append(" lhs_6_").append(i);
        String[] probs = list.toString().split("\\s+");
        HashMap<String, ArrayList<Double>> result = api.getMultipleProblems(probs);
        HashMap<String, double[]> res = new HashMap<>();
        for(String i: result.keySet()) {
            System.out.println("Student " + i + " (" + api.getNames().get(i) + "): ");
            ArrayList<Double> curr = result.get(i);
            if(curr.size() == 0) {
                System.out.println("Skipped - no data");
                continue;
            }
            Collections.sort(curr);
            double sum = 0, max = 0, med = curr.size() % 2 == 0 ? (curr.get(curr.size() / 2) + curr.get(curr.size() / 2 - 1)) / 2 : curr.get(curr.size() / 2);
            for(double j: curr) {
                sum += j;
                max = Math.max(max, j);
            }
            sum /= curr.size();
            System.out.printf("Average: %.5f\n", sum);
            System.out.printf("Median: %.5f\n", med);
            System.out.printf("Max: %.5f\n", max);
            res.put(i, new double[]{sum, med, max});
        }
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        for(String i: res.keySet()) System.out.printf("%.5f, ", res.get(i)[0]);
        System.out.println();
        for(String i: res.keySet()) System.out.printf("%.5f, ", res.get(i)[1]);
        System.out.println();
        for(String i: res.keySet()) System.out.printf("%.5f, ", res.get(i)[2]);
        System.out.println();

    }
}
