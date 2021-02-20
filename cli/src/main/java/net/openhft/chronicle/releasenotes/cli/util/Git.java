package net.openhft.chronicle.releasenotes.cli.util;

import static java.util.Objects.requireNonNull;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_REMOTE_SECTION;

import net.openhft.chronicle.releasenotes.cli.util.GitUrlParser.GitProvider;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * @author Mislav Milicevic
 */
public final class Git {

    private static final Pattern REPOSITORY_PATTERN = Pattern.compile("^[a-z0-9-_.]+/[a-z0-9-_.]+$");

    private static String configuredRepository;

    private Git() {
    }

    /**
     * Returns the name of the current git repository in the
     * format {@code owner/repository}.
     *
     * A {@link RuntimeException} is thrown if one of the
     * following conditions is met:
     * <ul>
     *     <li>the current directory is not a git repository
     *     <li>the repository origin does not exist
     *     <li>the git repository provider is not supported
     * </ul>
     *
     * @return the name of the current repository in the
     *         format {@code owner/repository}
     */
    public static String getCurrentRepository() {
        if (configuredRepository != null && !configuredRepository.isEmpty()) {
            return configuredRepository;
        }

        String currentDir = System.getProperty("user.dir");

        try {
            final Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(currentDir + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build();

            final String origin = repository.getConfig().getString(CONFIG_REMOTE_SECTION, "origin", "url");

            if (origin == null) {
                throw new RuntimeException("Origin not found for git repository");
            }

            final GitUrlParser.ParseResult parseResult = GitUrlParser.parseUrl(origin);

            if (parseResult.getGitProvider() == GitProvider.UNKNOWN) {
                throw new RuntimeException("Unsupported git provider (url = " + origin + ")");
            }

            return parseResult.getOwner() + "/" + parseResult.getRepository();
        } catch (IOException e) {
            throw new RuntimeException("Git repository not found in directory '" + currentDir + "'");
        }
    }

    public static void setConfiguredRepository(String repository) {
        requireNonNull(repository);

        if (!REPOSITORY_PATTERN.matcher(repository).find()) {
            throw new RuntimeException("Invalid repository naming format (must be 'owner/repository')");
        }

        configuredRepository = repository;
    }
}
