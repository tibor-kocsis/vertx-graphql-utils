package com.github.tkocsis.vertx.graphql.utils;

import com.github.tkocsis.vertx.graphql.utils.impl.IDLSchemaParserImpl;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public interface IDLSchemaParser {

	public static IDLSchemaParser create(Vertx vertx) {
		return new IDLSchemaParserImpl(vertx);
	}
	
	public Future<GraphQLSchema> fromFile(String path, RuntimeWiring runtimeWiring);
	
	public GraphQLSchema fromString(String schemaString, RuntimeWiring runtimeWiring);
}
