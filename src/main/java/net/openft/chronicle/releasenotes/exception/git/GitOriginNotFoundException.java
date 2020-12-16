package net.openft.chronicle.releasenotes.exception.git;

public final class GitOriginNotFoundException extends RuntimeException {

    public GitOriginNotFoundException() {
        super("Origin not found for git repository");
    }
}
