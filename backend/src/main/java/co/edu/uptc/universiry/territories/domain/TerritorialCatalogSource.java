package co.edu.uptc.universiry.territories.domain;

import java.net.URI;
import java.time.LocalDate;

public record TerritorialCatalogSource(
        String publisher,
        String datasetName,
        String datasetVersion,
        LocalDate snapshotRetrievedAt,
        String serviceUrl,
        String documentationUrl
) {

    public TerritorialCatalogSource {
        if (publisher == null || publisher.isBlank()
                || datasetName == null || datasetName.isBlank()
                || datasetVersion == null || datasetVersion.isBlank()
                || snapshotRetrievedAt == null
                || !isHttpsUrl(serviceUrl)
                || !isHttpsUrl(documentationUrl)) {
            throw new IllegalArgumentException("Territorial catalog source metadata is incomplete or unsafe.");
        }
    }

    private static boolean isHttpsUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
