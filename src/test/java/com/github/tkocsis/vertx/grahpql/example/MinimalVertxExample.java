package com.github.tkocsis.vertx.grahpql.example;

import org.junit.Ignore;

import com.github.tkocsis.vertx.graphql.datafetcher.AsyncDataFetcher;
import com.github.tkocsis.vertx.graphql.routehandler.GraphQLPostRouteHandler;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

@Ignore
public class MinimalVertxExample {

	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		// create the router
		Router router = Router.router(vertx);
		
		// setup an async datafetcher
		AsyncDataFetcher<String> helloFieldFetcher = (env, handler) -> {
			vertx.<String> executeBlocking(fut -> {
				fut.complete("world");
			}, handler);
		};
		
		// setup graphql helloworld schema
		GraphQLObjectType query = GraphQLObjectType.newObject()
		        .name("query")
		        .field(GraphQLFieldDefinition.newFieldDefinition()
		                .name("hello")
		                .type(Scalars.GraphQLString)
		                .dataFetcher(helloFieldFetcher))
		        .build(); 
		
		GraphQLSchema schema = GraphQLSchema.newSchema()
				.query(query)
				.build();
		
		router.route("/graphql").handler(BodyHandler.create()); // we need the body
		// create the graphql endpoint
		router.post("/graphql").handler(GraphQLPostRouteHandler.create(schema));
		
		// start the http server and make a call
		vertx.createHttpServer().requestHandler(router::accept).listen(8080);
	}
	
}
