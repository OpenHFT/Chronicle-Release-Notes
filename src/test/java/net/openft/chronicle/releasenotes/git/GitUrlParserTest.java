package net.openft.chronicle.releasenotes.git;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.openft.chronicle.releasenotes.git.GitUrlParser.GitProvider;
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
        var parseResult = GitUrlParser.parseUrl(url);

        assertEquals(GitProvider.GITHUB, parseResult.getGitProvider());
        assertEquals("owner", parseResult.getOwner());
        assertEquals("repository", parseResult.getRepository());
    }

}
