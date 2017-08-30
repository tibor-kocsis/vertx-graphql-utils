package com.github.tkocsis.vertx.graphql.utils.impl;

import com.github.tkocsis.vertx.graphql.utils.IDLSchemaParser;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class IDLSchemaParserImpl implements IDLSchemaParser {

	Vertx vertx;
	
	public IDLSchemaParserImpl(Vertx vertx) {
		this.vertx = vertx;
	}
	
	@Override
	public Future<GraphQLSchema> fromFile(String path, RuntimeWiring runtimeWiring) {
		Future<GraphQLSchema> result = Future.future();
		Future<Buffer> fileBuf = Future.future();
		vertx.fileSystem().readFile(path, fileBuf);
		fileBuf.map(buffer -> {
			return fromString(buffer.toString(), runtimeWiring);
		}).setHandler(result);
		return result;
	}
	
	@Override
	public GraphQLSchema fromString(String schemaString, RuntimeWiring runtimeWiring) {
		SchemaParser schemaParser = new SchemaParser();
		TypeDefinitionRegistry typeRegistry = schemaParser.parse(schemaString);
		RuntimeWiring wiring = runtimeWiring;
		SchemaGenerator schemaGenerator = new SchemaGenerator();
		return schemaGenerator.makeExecutableSchema(typeRegistry, wiring);
	}
	
}
