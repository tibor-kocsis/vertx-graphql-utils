package com.github.tkocsis.vertx.graphql.routehandler;

import java.util.HashMap;
import java.util.Map;

import com.github.tkocsis.vertx.graphql.queryexecutor.AsyncGraphQLExec;
import com.github.tkocsis.vertx.graphql.routehandler.internal.ExceptionConvert;

import graphql.schema.GraphQLSchema;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class GraphQLPostRouteHandler {

	public static Handler<RoutingContext> create(GraphQLSchema schema) {
		AsyncGraphQLExec asyncGraphQLExec = AsyncGraphQLExec.create(schema);
		
		// get the body and parse
		return rountingContext -> {
			JsonObject bodyJson = rountingContext.getBodyAsJson();
			String query = bodyJson.getString("query");
			String bodyVariables = bodyJson.getString("variables");
			String operationName = bodyJson.getString("operationName");
			Map<String, Object> variables;
			if (bodyVariables == null || bodyVariables.isEmpty()) {
				variables = new HashMap<>();
			} else {
				variables = new JsonObject(bodyVariables).getMap();
			}
			
			// execute the graphql query
			asyncGraphQLExec.executeQuery(query, operationName, rountingContext, variables).setHandler(queryResult -> {
				if (queryResult.succeeded()) {
					JsonObject json = queryResult.result();
					rountingContext.response().end(json.encodePrettily());
				} else {
					JsonObject errorResult = new JsonObject()
							.put("data", new JsonObject())
							.put("errors", queryResult.cause() != null ? ExceptionConvert.toJsonArray(queryResult.cause()) : new JsonArray().add("Internal error"));
					rountingContext.response().end(errorResult.encodePrettily());
				}
			});
		};
	}
}
