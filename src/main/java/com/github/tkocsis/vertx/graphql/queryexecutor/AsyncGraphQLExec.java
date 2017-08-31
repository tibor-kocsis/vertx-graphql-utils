package com.github.tkocsis.vertx.graphql.queryexecutor;

import java.util.Map;

import com.github.tkocsis.vertx.graphql.queryexecutor.impl.AsyncGraphQLExecImpl;

import graphql.schema.GraphQLSchema;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface AsyncGraphQLExec {
	
	public static AsyncGraphQLExec create(GraphQLSchema schema) {
		return new AsyncGraphQLExecImpl(schema);
	}

	public Future<JsonObject> executeQuery(String query, String operationName, Object context, Map<String, Object> variables);

}
