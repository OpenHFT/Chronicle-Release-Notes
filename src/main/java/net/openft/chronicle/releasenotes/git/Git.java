package net.openft.chronicle.releasenotes.git;

import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_REMOTE_SECTION;

import net.openft.chronicle.releasenotes.exception.git.GitOriginNotFoundException;
import net.openft.chronicle.releasenotes.exception.git.GitRepositoryNotFoundException;
import net.openft.chronicle.releasenotes.exception.git.UnsupportedGitProviderException;
import net.openft.chronicle.releasenotes.git.GitUrlParser.GitProvider;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;

public final class Git {

    private Git() {
    }

    public static String getCurrentRepository() {
        var currentDir = System.getProperty("user.dir");
        try {
            var repository = new FileRepositoryBuilder()
                .setGitDir(new File(currentDir + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build();

            var origin = repository.getConfig().getString(CONFIG_REMOTE_SECTION, "origin", "url");

            if (origin == null) {
                throw new GitOriginNotFoundException();
            }

            var parseResult = GitUrlParser.parseUrl(origin);

            if (parseResult.getGitProvider() == GitProvider.UNKNOWN) {
                throw new UnsupportedGitProviderException(origin);
            }

            return parseResult.getOwner() + "/" + parseResult.getRepository();
        } catch (IOException e) {
            throw new GitRepositoryNotFoundException(currentDir);
        }
    }
}
