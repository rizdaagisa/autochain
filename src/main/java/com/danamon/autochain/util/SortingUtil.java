package com.danamon.autochain.util;

public class SortingUtil {
    public static  <T> String sortByValidation(Class<T> clazz, String sortBy, String defaultSortBy) {
        try {
            return clazz.getDeclaredField(sortBy).getName();
        } catch (NoSuchFieldException e) {
            return defaultSortBy;
        }
    }
}
