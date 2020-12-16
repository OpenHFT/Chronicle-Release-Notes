package net.openft.chronicle.releasenotes.exception.git;

public final class GitRepositoryNotFoundException extends RuntimeException {

    public GitRepositoryNotFoundException(String dir) {
        super("Git repository not found in directory '" + dir + "'");
    }
}
