package com.liulishuo.engzo.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Created by rantianhua on 17/7/26.
 * provide some common methods
 */

public class Utility {

    public static String generateRandomString(int len) {
        String alphabet = "0123456789abcdef";
        int n = alphabet.length();

        String result = "";
        Random r = new Random();
        for (int i = 0; i < len; i++) {
            result = result + alphabet.charAt(r.nextInt(n));
        }

        return result;
    }

    public static String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
