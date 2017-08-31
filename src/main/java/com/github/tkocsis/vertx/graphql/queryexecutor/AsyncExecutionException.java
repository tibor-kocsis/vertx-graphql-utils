package com.github.tkocsis.vertx.graphql.queryexecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import graphql.GraphQLError;

public class AsyncExecutionException extends Exception {
	private static final long serialVersionUID = 1L;

	List<GraphQLError> errors = new ArrayList<>();
	
	public AsyncExecutionException(String message, List<GraphQLError> errors) {
		super(message);
		this.errors = errors;
	}
	
	public List<GraphQLError> getErrors() {
		return errors;
	}
	
	public Stream<GraphQLError> errorStream() {
		return errors.stream();
	}
	
	@Override
	public String toString() {
		return super.toString() + " " + errors;
	}
}
