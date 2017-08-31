package com.github.tkocsis.vertx.graphql.queryexecutor.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.github.tkocsis.graphql.execution.RxExecutionResult;
import com.github.tkocsis.graphql.execution.RxExecutionStrategy;
import com.github.tkocsis.vertx.graphql.queryexecutor.AsyncExecutionException;
import com.github.tkocsis.vertx.graphql.queryexecutor.AsyncGraphQLExec;

import graphql.ExceptionWhileDataFetching;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.schema.GraphQLSchema;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import rx.Observable;

public class AsyncGraphQLExecImpl implements AsyncGraphQLExec {
	
	GraphQL graphQL;
	
	public AsyncGraphQLExecImpl(GraphQLSchema schema) {
		this.graphQL = GraphQL.newGraphQL(schema)
				.queryExecutionStrategy(new RxExecutionStrategy())
				.mutationExecutionStrategy(new RxExecutionStrategy())
				.build();;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Future<JsonObject> executeQuery(String query, String operationName, Object context, Map<String, Object> variables) {
		Future<JsonObject> fut = Future.future();
		
		RxExecutionResult rxExecutionResult = (RxExecutionResult) graphQL.execute(query, operationName, context, variables != null ? variables : new HashMap<>());
		Observable.zip(rxExecutionResult.getDataObservable(), rxExecutionResult.getErrorsObservable(), (data, errors) -> {
			if (errors.size() > 0) {
				fut.fail(new AsyncExecutionException("Exception during execution", errors));
			} else {
				fut.complete(new JsonObject((Map<String, Object>) data));
			}
			return 0;
		}).subscribe(e -> {
		}, error -> {
			try {
				fut.fail(new AsyncExecutionException("Exception during execution", 
						Arrays.asList((error instanceof GraphQLError) ? (GraphQLError) error : (GraphQLError) new ExceptionWhileDataFetching(error))));
			} catch (Exception e) {
				fut.fail(e);
			}
		});
		return fut;
	}

}
