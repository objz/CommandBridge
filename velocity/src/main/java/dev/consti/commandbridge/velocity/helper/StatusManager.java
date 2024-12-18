package dev.consti.commandbridge.velocity.helper;

import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.foundationlib.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatusManager {
    private final Logger logger;
    private final Map<String, String> statusMap = new HashMap<>();
    private final Set<String> connectedClients = Runtime.getInstance().getServer().getConnectedClients();

    public StatusManager(Logger logger) {
        this.logger = logger;
    }

    public void addClientToStatus(String clientId, String status) {
        statusMap.put(clientId, status);
        logger.debug("Updated statusMap: {}", statusMap);
    }

    public void clearStatusMap() {
        statusMap.clear();
    }

    public String checkForFailures() {
        logger.debug("Checking for failures...");

        List<String> missingClients = connectedClients.stream()
                .filter(client -> !statusMap.containsKey(client))
                .toList();

        if (!missingClients.isEmpty()) {
            String missingClientString = String.join(", ", missingClients);
            logger.warn("Waiting for responses from clients: {}", missingClientString);
            return "Missing responses from: " + missingClientString;
        }

        String failedClients = statusMap.entrySet().stream()
                .filter(entry -> !"success".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);

        if (failedClients != null) {
            logger.error("Failure detected on clients: {}", failedClients);
            return "Failure detected on: " + failedClients;
        }

        logger.debug("No failures detected. All clients are operational");
        return null;
    }
}
