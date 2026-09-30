package co.edu.uptc.universiry.branding.application;

import java.io.IOException;

public interface AssetStorage {

    void write(String storageKey, byte[] content) throws IOException;

    byte[] read(String storageKey, long maxBytes) throws IOException;

    void delete(String storageKey) throws IOException;
}
