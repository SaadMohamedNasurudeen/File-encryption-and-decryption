package crypto;

/**
 * Functional callback interface for reporting progress during cryptographic operations.
 * Allows decoupling UI updates from cryptographic logic.
 */
@FunctionalInterface
public interface ProgressListener {
    /**
     * Invoked periodically as bytes are processed.
     *
     * @param bytesProcessed Number of bytes processed so far
     * @param totalBytes     Total expected number of bytes (-1 if unknown)
     */
    void onProgress(long bytesProcessed, long totalBytes);
}
