package com.crypto.utils;

public class StringUtils {

    /**
     * Remove all non-numeric characters from string
     * Example: a12.334tyz.78x would become 12.334.78
     * @param val
     * @return
     */
    public static String removeNonNumericCharacters(String val) {
        return val.replaceAll("[^\\d.]", "");
    }
}
