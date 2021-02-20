package net.openhft.chronicle.releasenotes.cli.util;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Mislav Milicevic
 */
public final class GitUrlParser {

    private static final GitProvider[] VALID_PROVIDERS = Arrays.copyOfRange(GitProvider.values(), 0, GitProvider
            .values().length - 1);

    /**
     * Parses a provided git {@code url} and returns the parsed
     * results as a {@link ParseResult}.
     *
     * If the provided {@code url} does not belong to a registered
     * {@link GitProvider}, an empty {@link ParseResult} is returned.
     *
     * @param url to parse
     * @return {@link ParseResult}
     */
    public static ParseResult parseUrl(final String url) {
        requireNonNull(url);

        final Optional<GitProvider> optionalGitProvider = Arrays.stream(VALID_PROVIDERS)
            .filter(gitProvider -> url.startsWith(gitProvider.getHttpUrl()) || url.startsWith(gitProvider.getSshUrl()))
            .findAny();

        if (!optionalGitProvider.isPresent()) {
            return ParseResult.EMPTY;
        }

        final GitProvider gitProvider = optionalGitProvider.get();

        final int endIndex = url.endsWith(".git") ? url.length() - ".git".length() : url.length();

        final String identifier = url.startsWith(gitProvider.getHttpUrl())
            ? url.substring(gitProvider.getHttpUrl().length(), endIndex)
            : url.substring(gitProvider.getSshUrl().length() + 1, endIndex);

        final String[] identifierComponents = identifier.split("/");

        final String owner = identifierComponents[0];
        final String repository = identifierComponents[1];

        return new ParseResult(gitProvider, owner, repository);
    }

    /**
     * Holds the information extracted from a parsed git URL.
     *
     * @author Mislav Milicevic
     */
    public static class ParseResult {

        public static final ParseResult EMPTY = new ParseResult(GitProvider.UNKNOWN, null, null);

        private final GitProvider gitProvider;
        private final String owner;
        private final String repository;

        private ParseResult(GitProvider gitProvider, String owner, String repository) {
            this.gitProvider = requireNonNull(gitProvider);
            this.owner = owner;
            this.repository = repository;
        }

        /**
         * Returns the {@link GitProvider}.
         *
         * @return the {@link GitProvider}.
         */
        public GitProvider getGitProvider() {
            return gitProvider;
        }

        /**
         * Returns the repository owner.
         *
         * @return the repository owner.
         */
        public String getOwner() {
            return owner;
        }

        /**
         * Returns the repository name.
         *
         * @return the repository name.
         */
        public String getRepository() {
            return repository;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ParseResult that = (ParseResult) o;
            return gitProvider == that.gitProvider && Objects.equals(owner, that.owner)
                    && Objects.equals(repository, that.repository);
        }

        @Override
        public int hashCode() {
            return Objects.hash(gitProvider, owner, repository);
        }
    }

    /**
     * Represents different git providers.
     *
     * @author Mislav Milicevic
     */
    public enum GitProvider {
        GITHUB {
            @Override
            public String getHttpUrl() {
                return "https://github.com/";
            }

            @Override
            public String getSshUrl() {
                return "git@github.com";
            }
        },

        UNKNOWN {
            @Override
            public String getHttpUrl() {
                return null;
            }

            @Override
            public String getSshUrl() {
                return null;
            }
        };

        /**
         * Returns the HTTP/S URL for this {@link GitProvider}.
         *
         * @return the HTTP/S URL for this {@link GitProvider}.
         */
        public abstract String getHttpUrl();

        /**
         * Returns the SSH URL for this {@link GitProvider}.
         *
         * @return the SSH URL for this {@link GitProvider}.
         */
        public abstract String getSshUrl();
    }
}
