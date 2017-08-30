package com.github.tkocsis.vertx.graphql.datafetcher;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;

public interface AsyncDataFetcher<T> extends DataFetcher<ObservableFuture<T>> {

	default public ObservableFuture<T> get(DataFetchingEnvironment environment) {
		ObservableFuture<T> result = RxHelper.observableFuture();
		getAsync(environment, result.toHandler());
		return result;
	}

	public void getAsync(DataFetchingEnvironment environment, Handler<AsyncResult<T>> handler);
}
