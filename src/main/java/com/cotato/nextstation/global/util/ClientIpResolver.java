package com.cotato.nextstation.global.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "X-Real-IP"
    };

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}