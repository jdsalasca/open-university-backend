package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandAsset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class BrandAssetService {

    private final BrandImageValidator imageValidator;
    private final BrandAssetRepository assetRepository;
    private final AssetStorage assetStorage;

    public BrandAssetService(
            BrandImageValidator imageValidator,
            BrandAssetRepository assetRepository,
            AssetStorage assetStorage
    ) {
        this.imageValidator = imageValidator;
        this.assetRepository = assetRepository;
        this.assetStorage = assetStorage;
    }

    @Transactional
    public StoredAsset store(byte[] content, Actor actor) {
        BrandImageValidator.ValidatedImage image = imageValidator.validate(content);
        String assetId = UUID.randomUUID().toString();
        String storageKey = UUID.randomUUID() + ".asset";
        BrandAsset asset = new BrandAsset(
                assetId,
                storageKey,
                image.mimeType(),
                content.length,
                image.widthPx(),
                image.heightPx(),
                sha256(content)
        );

        try {
            assetStorage.write(storageKey, content);
            removeFileOnTransactionRollback(storageKey);
            assetRepository.insert(asset, actor);
            return StoredAsset.from(asset);
        } catch (IOException exception) {
            cleanupAfterFailure(storageKey, exception);
            throw new BrandAssetStorageException("The image could not be stored.", exception);
        } catch (RuntimeException exception) {
            cleanupAfterFailure(storageKey, exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public AssetContent openPublished(String assetId) {
        if (!isCanonicalUuid(assetId)) {
            throw new BrandAssetNotFoundException();
        }

        BrandAsset asset = assetRepository.findCurrentlyPublished(assetId, Instant.now())
                .orElseThrow(BrandAssetNotFoundException::new);
        try {
            byte[] content = assetStorage.read(asset.storageKey(), asset.sizeBytes());
            if (content.length != asset.sizeBytes() || !sha256(content).equals(asset.sha256())) {
                throw new BrandAssetStorageException("The stored image failed its integrity check.", new IOException("Asset size or checksum mismatch."));
            }
            return new AssetContent(asset.mimeType(), asset.sha256(), content);
        } catch (IOException exception) {
            throw new BrandAssetStorageException("The published image is temporarily unavailable.", exception);
        }
    }

    private void removeFileOnTransactionRollback(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(storageKey);
                }
            }
        });
    }

    private void cleanupAfterFailure(String storageKey, RuntimeException originalException) {
        try {
            assetStorage.delete(storageKey);
        } catch (IOException | RuntimeException cleanupFailure) {
            originalException.addSuppressed(cleanupFailure);
        }
    }

    private void cleanupAfterFailure(String storageKey, IOException originalException) {
        try {
            assetStorage.delete(storageKey);
        } catch (IOException | RuntimeException cleanupFailure) {
            originalException.addSuppressed(cleanupFailure);
        }
    }

    private void deleteQuietly(String storageKey) {
        try {
            assetStorage.delete(storageKey);
        } catch (IOException | RuntimeException ignored) {
            // A background storage reconciliation can report cleanup failures without breaking transaction completion.
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("The Java runtime must provide SHA-256.", exception);
        }
    }

    private static boolean isCanonicalUuid(String value) {
        if (value == null) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public record StoredAsset(String assetId, String mimeType, long sizeBytes, int width, int height) {
        private static StoredAsset from(BrandAsset asset) {
            return new StoredAsset(asset.assetId(), asset.mimeType(), asset.sizeBytes(), asset.widthPx(), asset.heightPx());
        }
    }

    public record AssetContent(String mimeType, String sha256, byte[] bytes) {
    }
}
