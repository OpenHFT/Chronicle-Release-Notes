package net.openft.chronicle.releasenotes.git;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Objects;

public final class GitUrlParser {

    private static final GitProvider[] VALID_PROVIDERS = Arrays.copyOfRange(GitProvider.values(), 0, GitProvider.values().length - 1);

    public static ParseResult parseUrl(final String url) {
        requireNonNull(url);

        var optionalGitProvider = Arrays.stream(VALID_PROVIDERS)
            .filter(gitProvider -> url.startsWith(gitProvider.getHttpUrl()) || url.startsWith(gitProvider.getSshUrl()))
            .findAny();

        if (optionalGitProvider.isEmpty()) {
            return ParseResult.EMPTY;
        }

        var gitProvider = optionalGitProvider.get();

        final int endIndex = url.endsWith(".git") ? url.length() - ".git".length() : url.length();

        var identifier = url.startsWith(gitProvider.getHttpUrl())
            ? url.substring(gitProvider.getHttpUrl().length(), endIndex)
            : url.substring(gitProvider.getSshUrl().length() + 1, endIndex);

        var identifierComponents = identifier.split("/");

        var owner = identifierComponents[0];
        var repository = identifierComponents[1];

        return new ParseResult(gitProvider, owner, repository);
    }

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

        public GitProvider getGitProvider() {
            return gitProvider;
        }

        public String getOwner() {
            return owner;
        }

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

    /*
    * In case other providers need to be supported in the future
    * */
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

        public abstract String getHttpUrl();

        public abstract String getSshUrl();
    }
}
