package com.github.tkocsis.vertx.graphql.queryexecutor.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.github.tkocsis.graphql.execution.RxExecutionResult;
import com.github.tkocsis.graphql.execution.RxExecutionStrategy;
import com.github.tkocsis.vertx.graphql.queryexecutor.AsyncGraphQLExec;

import graphql.GraphQL;
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

	@Override
	public Future<JsonObject> executeQuery(String query, String operationName, Object context, Map<String, Object> variables) {
		Future<JsonObject> fut = Future.future();
		
		RxExecutionResult rxExecutionResult = (RxExecutionResult) graphQL.execute(query, operationName, context, variables != null ? variables : new HashMap<>());
		Observable.zip(rxExecutionResult.getDataObservable(), rxExecutionResult.getErrorsObservable(), (data, errors) -> {
			Map<String, Object> res = new HashMap<>();
			if (errors.size() > 0) {
				res.put("errors", errors);
			} 
			res.put("data", data);
			return res;
		}).subscribe(e -> {
			fut.complete(new JsonObject(e));
		}, error -> {
			Map<String, Object> res = new HashMap<>();
			res.put("errors", Arrays.asList(error));
			res.put("data", null);
			fut.complete(new JsonObject(res));
		});
		return fut;
	}

}
