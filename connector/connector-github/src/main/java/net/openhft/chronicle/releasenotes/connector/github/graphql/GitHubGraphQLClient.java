package net.openhft.chronicle.releasenotes.connector.github.graphql;

import static java.util.Objects.requireNonNull;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloCall.Callback;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import net.openhft.chronicle.releasenotes.connector.github.graphql.GetTagsQuery.AsCommit;
import net.openhft.chronicle.releasenotes.connector.github.graphql.GetTagsQuery.AsCommit1;
import net.openhft.chronicle.releasenotes.connector.github.graphql.GetTagsQuery.AsTag;
import net.openhft.chronicle.releasenotes.connector.github.graphql.GetTagsQuery.Data;
import net.openhft.chronicle.releasenotes.connector.github.graphql.GetTagsQuery.Node;
import net.openhft.chronicle.releasenotes.connector.github.graphql.GetTagsQuery.Refs;
import net.openhft.chronicle.releasenotes.connector.github.graphql.GetTagsQuery.Repository;
import net.openhft.chronicle.releasenotes.connector.github.graphql.GetTagsQuery.Target;
import net.openhft.chronicle.releasenotes.connector.github.graphql.model.Tag;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GitHubGraphQLClient {

    private static final String GITHUB_GRAPHQL_URL = "https://api.github.com/graphql";

    private final ApolloClient apolloClient;

    public GitHubGraphQLClient(String token) {
        requireNonNull(token);

        this.apolloClient = ApolloClient.builder()
            .serverUrl(GITHUB_GRAPHQL_URL)
            .okHttpClient(new OkHttpClient.Builder()
                .addInterceptor(chain -> chain.proceed(
                    chain.request().newBuilder().addHeader("Authorization", "Bearer " + token).build()
                ))
                .build()
            )
        .build();
    }

    public List<Tag> getTags(String owner, String repository) {
        final Optional<Data> optionalData =  callSync(apolloClient.query(new GetTagsQuery(owner, repository, Input.absent()))).getData();

        if (!optionalData.isPresent()) {
            throw new RuntimeException("Failed to fetch tag data for repository '" + owner + "/" + repository + "'");
        }

        final Data data = optionalData.get();

        if (!data.getRepository().isPresent()) {
            throw new RuntimeException("Failed to find repository '" + owner + "/" + repository + "'");
        }

        final Repository repo = data.getRepository().get();

        if (!repo.getRefs().isPresent()) {
            throw new RuntimeException("Failed to find tag refs for repository '" + owner + "/" + repository + "'");
        }

        final Refs refs = repo.getRefs().get();

        if (!refs.getEdges().isPresent()) {
            throw new RuntimeException("Failed to find tag ref edges for repository '" + owner  + "/" + repository + "'");
        }

        final List<Node> nodes = refs.getEdges().get().stream()
            .filter(edge -> edge.getNode().isPresent())
            .map(edge -> edge.getNode().get())
            .collect(Collectors.toList());

        final List<Tag> tags = new ArrayList<>();

        for (final Node node : nodes) {
            if (!node.getTarget().isPresent()) {
                continue;
            }

            final Target nodeTarget = node.getTarget().get();

            if (!(nodeTarget instanceof AsCommit) && !(nodeTarget instanceof AsTag)) {
                continue;
            }

            String tagName = null;
            String commitSHA1 = null;

            if (nodeTarget instanceof AsCommit) {
                final AsCommit commitTarget = (AsCommit) nodeTarget;

                if (!(commitTarget.getOid() instanceof String)) {
                    continue;
                }

                tagName = node.getName();
                commitSHA1 = (String) commitTarget.getOid();
            }

            if (nodeTarget instanceof AsTag) {
                final AsTag tagTarget = (AsTag) nodeTarget;

                if (!(tagTarget.getTarget() instanceof AsCommit1)) {
                    continue;
                }

                final AsCommit1 commitTarget = (AsCommit1) tagTarget.getTarget();

                if (!(commitTarget.getOid() instanceof String)) {
                    continue;
                }

                tagName = node.getName();
                commitSHA1 = (String) commitTarget.getOid();

                final Tag tag = new Tag(tagName, commitSHA1);

                tags.add(tag);
            }

            final Tag tag = new Tag(tagName, commitSHA1);

            tags.add(tag);
        }

        return tags;
    }

    private <T> Response<T> callSync(final ApolloCall<T> call) {
        final CompletableFuture<Response<T>> completableFuture = new CompletableFuture<>();

        completableFuture.whenComplete((tResponse, throwable) -> {
            if (completableFuture.isCancelled()) {
                completableFuture.cancel(true);
            }
        });

        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NotNull Response<T> response) {
                completableFuture.complete(response);
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                completableFuture.completeExceptionally(e);
            }
        });

        return completableFuture.join();
    }
}
