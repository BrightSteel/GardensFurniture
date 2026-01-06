package com.gardensmc.gardensfurniture.util;

import java.util.HashMap;
import java.util.Map;

public class MapUtil {

    public static <K, V> Map<K, V> merge(Map<K, V> map1, Map<K, V> map2) {
        var map = new HashMap<>(map1);
        map.putAll(map2);
        return map;
    }
}
