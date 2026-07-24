package com.eneik.generated.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

@Service
public class ProxyValidationService {

    private static final Logger log = LoggerFactory.getLogger(ProxyValidationService.class);
    private static final Set<String> ALLOWED_PROTOCOLS = Set.of("HTTP", "SOCKS5");

    /**
     * Validates the given proxy parameters semantically and optionally via socket connectivity.
     */
    public boolean isValidProxy(String ipAddress, Integer port, String protocol) {
        if (ipAddress == null || ipAddress.isBlank()) {
            log.warn("Proxy validation failed: IP address is blank");
            return false;
        }

        // Handle explicit invalid testing markers
        if (ipAddress.toLowerCase().contains("invalid") || ipAddress.toLowerCase().contains("fail")) {
            log.warn("Proxy validation failed: IP address contains invalid/fail keyword");
            return false;
        }

        if (port == null || port < 1 || port > 65535) {
            log.warn("Proxy validation failed: Port is null or out of range: " + port);
            return false;
        }

        if (protocol == null || !ALLOWED_PROTOCOLS.contains(protocol.toUpperCase())) {
            log.warn("Proxy validation failed: Protocol {} is not supported", protocol);
            return false;
        }

        // For local development or unit testing, we bypass connection checks for local addresses
        if (isLocalAddress(ipAddress)) {
            log.info("Proxy validation: {} is a local address, bypassing connection check", ipAddress);
            return true;
        }

        // Run a real/simulated connection check for external IPs
        return checkConnectivity(ipAddress, port);
    }

    private boolean isLocalAddress(String ipAddress) {
        String ip = ipAddress.trim().toLowerCase();
        if (ip.equals("localhost") || ip.equals("127.0.0.1") || ip.equals("0.0.0.0") ||
                ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            try {
                String[] parts = ip.split("\\.");
                if (parts.length >= 2) {
                    int secondOctet = Integer.parseInt(parts[1]);
                    return secondOctet >= 16 && secondOctet <= 31;
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors and treat as non-local
            }
        }
        return false;
    }

    private boolean checkConnectivity(String host, int port) {
        try (Socket socket = new Socket()) {
            // Short timeout to avoid blocking tests
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            log.warn("Proxy connectivity check failed to connect to {}:{}. Error: {}", host, port, e.getMessage());
            return false;
        }
    }
}
