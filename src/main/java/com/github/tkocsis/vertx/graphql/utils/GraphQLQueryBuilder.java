package com.github.tkocsis.vertx.graphql.utils;

import io.vertx.core.json.JsonObject;

public class GraphQLQueryBuilder {
	
	public static Builder newQuery(String queryString) {
		return new Builder(queryString);
	}
	
	public static class Builder {
		JsonObject jsonObject = new JsonObject();
		JsonObject variables = new JsonObject();
		
		private Builder(String queryString) {
			jsonObject.put("query", queryString);
		}
		
		public Builder var(String variableName, Object value) {
			variables.put(variableName, value);
			return this;
		}
		
		public JsonObject build() {
			jsonObject.put("variables", variables);
			return jsonObject;
		}
	}

}
