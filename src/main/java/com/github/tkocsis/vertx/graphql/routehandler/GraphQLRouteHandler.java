package com.github.tkocsis.vertx.graphql.routehandler;

import java.util.HashMap;
import java.util.Map;

import com.github.tkocsis.vertx.graphql.queryexecutor.impl.AsyncGraphQLExecImpl;

import graphql.schema.GraphQLSchema;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class GraphQLRouteHandler {

	public static Handler<RoutingContext> create(GraphQLSchema schema) {
		AsyncGraphQLExecImpl asyncGraphQLExec = new AsyncGraphQLExecImpl(schema);
		
		// get the body and parse
		return rountingContext -> {
			JsonObject bodyJson = rountingContext.getBodyAsJson();
			String query = (String) bodyJson.getString("query");
			String bodyVariables = bodyJson.getString("variables");
			Map<String, Object> variables;
			if (bodyVariables == null || bodyVariables.isEmpty()) {
				variables = new HashMap<>();
			} else {
				variables = new JsonObject(bodyVariables).getMap();
			}
			
			// execute the graphql query
			asyncGraphQLExec.executeQuery(query, rountingContext, variables).setHandler(queryResult -> {
				if (queryResult.succeeded()) {
					JsonObject json = queryResult.result();
					rountingContext.response().end(json.encodePrettily());
				} else {
					rountingContext.response().setStatusCode(400).end(new JsonObject().put("errors", "Internal error").encode());
				}
			});
		};
	}
}
