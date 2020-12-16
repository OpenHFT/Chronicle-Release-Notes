package net.openft.chronicle.releasenotes.exception.git;

public final class UnsupportedGitProviderException extends RuntimeException {

    public UnsupportedGitProviderException(String url) {
        super("Unsupported git provider (url = " + url + ")");
    }
}
