package com.github.tkocsis.vertx.graphql.queryexecutor;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface AsyncGraphQLExec {

	public Future<JsonObject> executeQuery(String query, Object context, Map<String, Object> variables);
	
}
