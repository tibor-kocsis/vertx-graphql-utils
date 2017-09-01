package com.github.tkocsis.vertx.graphql.queryexecutor.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.github.tkocsis.vertx.graphql.queryexecutor.AsyncExecutionException;
import com.github.tkocsis.vertx.graphql.queryexecutor.AsyncGraphQLExec;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.schema.GraphQLSchema;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class AsyncGraphQLExecImpl implements AsyncGraphQLExec {
	
	GraphQL graphQL;
	
	public AsyncGraphQLExecImpl(GraphQLSchema schema) {
		this.graphQL = GraphQL.newGraphQL(schema).build();
	}

	@Override
	public Future<JsonObject> executeQuery(String query, String operationName, Object context, Map<String, Object> variables) {
		Future<JsonObject> fut = Future.future();
		try {
			ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(query)
					.operationName(operationName)
					.context(context)
					.variables(variables)
					.build();

			CompletableFuture<ExecutionResult> asyncResult = graphQL.executeAsync(executionInput);
			asyncResult.handle((executionResult, exception) -> {
				try {
					Map<String, Object> data = executionResult.getData();
					List<GraphQLError> errors = executionResult.getErrors();
					if (errors.size() > 0) {
						fut.fail(new AsyncExecutionException("Exception during execution", errors));
					} else {
						fut.complete(new JsonObject(data));
					}
				} catch (Exception e) {
					fut.fail(e);
				}
				return 0;
			});
		} catch (Exception e) {
			fut.fail(e);
		}
		return fut;
	}

}
