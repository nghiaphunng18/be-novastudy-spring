package com.novastudy.utils;

import java.util.HashSet;
import java.util.Set;

public class PublicEndpoints {
    private static final Set<String> PUBLIC_ENDPOINTS = new HashSet<>();

    static {
        PUBLIC_ENDPOINTS.add("/");
        PUBLIC_ENDPOINTS.add("/auth/register");
        PUBLIC_ENDPOINTS.add("/auth/login");
        PUBLIC_ENDPOINTS.add("/auth/refresh-token");
    }

    public static boolean isPublic(String path) {
        return PUBLIC_ENDPOINTS.contains(path);
    }

    public static String[] getPublicEndpoints() {
        return PUBLIC_ENDPOINTS.toArray(new String[0]);
    }
}
