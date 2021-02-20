package net.openhft.chronicle.releasenotes.cli.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.openhft.chronicle.releasenotes.cli.util.GitUrlParser.GitProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class GitUrlParserTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "https://github.com/owner/repository",
        "https://github.com/owner/repository.git",
        "git@github.com:owner/repository",
        "git@github.com:owner/repository.git",
    })
    void parseUrl(String url) {
        final GitUrlParser.ParseResult parseResult = GitUrlParser.parseUrl(url);

        assertEquals(GitProvider.GITHUB, parseResult.getGitProvider());
        assertEquals("owner", parseResult.getOwner());
        assertEquals("repository", parseResult.getRepository());
    }

}
