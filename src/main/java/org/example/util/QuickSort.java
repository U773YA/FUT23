package org.example.util;

import java.util.ArrayList;
import java.util.List;

public class QuickSort {

    public static List<Integer> quickSort(List<Integer> input) {
        if (input.size() <= 1) {
            return input;
        }
        int pivot = input.get(0);
        List<Integer> less = new ArrayList<>();
        List<Integer> greater = new ArrayList<>();
        List<Integer> equal = new ArrayList<>();
        for (Integer num : input) {
            if (num < pivot) {
                less.add(num);
            } else if (num > pivot) {
                greater.add(num);
            } else {
                equal.add(num);
            }
        }
        List<Integer> sortedLess = quickSort(less);
        List<Integer> sortedGreater = quickSort(greater);
        sortedLess.addAll(equal);
        sortedLess.addAll(sortedGreater);
        return sortedLess;
    }
}
