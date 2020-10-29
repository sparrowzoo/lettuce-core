package io.lettuce.core.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DistinctCharUtils {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("康师傅方便面");
        list.add("方便面康师傅");
        list.add("方便面康师傅2");
        list.add("北京天安门");
        list.add("天安门北京");




        Map<Integer, String> distinctMap = new TreeMap<>();
        for (String key : list) {
            distinctMap.put(getKey(key), key);
        }
        System.out.println(distinctMap);
    }

    private static Integer getKey(String key) {
        int result = 0;
        for (char c : key.toCharArray()) {
            result += c;
        }
        return result;
    }
}
