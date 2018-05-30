package com.github.tkocsis.vertx.graphql.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GraphQLQueryResult {
	
	public static class CannotCastException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public CannotCastException(String message) {
			super(message);
		}
	}

	private final JsonObject data;
	private final JsonArray errors;
	
	public GraphQLQueryResult(JsonObject data, JsonArray errors) {
		this.data = data;
		this.errors = errors;
	}

	public JsonObject getJson(String queryField) {
		if (data == null) {
			return null;
		}
		return (JsonObject) data.getValue(queryField);
	}
	
	public <T> T getData(String queryField, Class<T> mapTo) {
		if (data == null || data.getValue(queryField) == null) {
			return null;
		}
		Object value = data.getValue(queryField);
		if (mapTo.isAssignableFrom(value.getClass())) {
			return mapTo.cast(value);
		}
		if (value instanceof JsonObject) {
			return ((JsonObject) value).mapTo(mapTo);
		}
		throw new CannotCastException("Can't map " + value.getClass() + " to " + mapTo.getName());
	}
	
	public JsonObject getData() {
		return data;
	}
	
	public JsonArray getErrors() {
		if (errors == null) {
			return new JsonArray();
		}
		return errors;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("data=");
		if (data != null) {
			result.append(data.encodePrettily()).append("; ");
		} else {
			result.append("null; ");
		}
		result.append("errors=");
		if (errors != null) {
			result.append(errors.encodePrettily()).append(";");
		} else {
			result.append("null");
		}
		return result.toString();
	}
}
