package com.github.tkocsis.vertx.graphql.datafetcher;

import java.util.concurrent.CompletableFuture;

import com.github.tkocsis.vertx.graphql.utils.AsyncResCF;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface AsyncDataFetcher<T> extends DataFetcher<CompletableFuture<T>> {

	default public CompletableFuture<T> get(DataFetchingEnvironment environment) {
		AsyncResCF<T> asyncResCF = new AsyncResCF<>();
		getAsync(environment, asyncResCF);
		return asyncResCF;
	}

	public void getAsync(DataFetchingEnvironment environment, Handler<AsyncResult<T>> handler);
}
