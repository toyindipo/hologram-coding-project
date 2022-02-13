package com.hologramsciences;

import java.util.*;

public class Algorithms {
    /**
     *
     *  Compute the cartesian product of a list of lists of any type T
     *  the result is a list of lists of type T, where each element comes
     *  each successive element of the each list.
     *
     *  https://en.wikipedia.org/wiki/Cartesian_product
     *
     *  For this problem order matters.
     *
     *  Example:
     *
     *   listOfLists = Arrays.asList(
     *                         Arrays.asList("A", "B"),
     *                         Arrays.asList("K", "L")
     *                 )
     *
     *   returns:
     *
     *   Arrays.asList(
     *         Arrays.asList("A", "K"),
     *         Arrays.asList("A", "L"),
     *         Arrays.asList("B", "K"),
     *         Arrays.asList("B", "L")
     *   )
     *
     *
     *
     */
    public static final <T> List<List<T>> cartesianProductForLists(final List<List<T>> listOfLists) {
        List<List<T>> product = new ArrayList<>();
        for (int i = 0; i < listOfLists.get(0).size(); i++) {
            Map<Integer, Integer> mapper = new HashMap<>();
            for (int j = 1; j < listOfLists.size(); j++) mapper.put(j, 0);
            mapper.put(0, i);

            boolean end = false;

            while (!end) {
                int marker = listOfLists.size() - 1;
                boolean moveUp = true;
                pushToList(listOfLists, mapper, product);
                while (marker > 0 && moveUp) {
                    int count = mapper.get(marker);
                    if (count + 1 < listOfLists.get(marker).size()) {
                        mapper.put(marker, count + 1);
                        moveUp = false;
                    } else {
                        mapper.put(marker, 0);
                        marker -= 1;
                    }
                }
                if (marker <= 0) end = true;
            }
        }

        return product;
    }

    private static <T> void pushToList(final List<List<T>> inputList, Map<Integer, Integer> mapper, List<List<T>> product) {
        List<T> toAdd = new ArrayList<>();
        for (int i = 0; i < inputList.size(); i++) {
            toAdd.add(inputList.get(i).get(mapper.get(i)));
        }
        product.add(toAdd);
    }

    /**
     *
     *  In the United States there are six coins:
     *  1¢ 5¢ 10¢ 25¢ 50¢ 100¢
     *  Assuming you have an unlimited supply of each coin,
     *  implement a method which returns the number of distinct ways to make totalCents
     */
    public static final long countNumWaysMakeChange(final int totalCents) {
        int[] coins = {1, 5, 10, 25, 50, 100};
        long[] changes = new long[totalCents + 1];
        changes[0] = 1;
        for (int i = 0; i < coins.length; i++) {
            for (int j = coins[i]; j <= totalCents; j++) {
                changes[j] += changes[j - coins[i]];
            }
        }
        return changes[totalCents];
    }
}
