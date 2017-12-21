package com.github.tkocsis.vertx.grahpql;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tkocsis.vertx.graphql.codec.GraphQLBodyCodec;
import com.github.tkocsis.vertx.graphql.datafetcher.AsyncDataFetcher;
import com.github.tkocsis.vertx.graphql.model.GraphQLQueryResult;
import com.github.tkocsis.vertx.graphql.routehandler.GraphQLPostRouteHandler;
import com.github.tkocsis.vertx.graphql.utils.GraphQLQueryBuilder;
import com.github.tkocsis.vertx.graphql.utils.IDLSchemaParser;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;

@RunWith(VertxUnitRunner.class)
public class RouteHandlerFailureWithBodyCodecTest {
	
	Vertx vertx;
	int httpPort;
	HttpServer httpServer;
	
	@Before
	public void setup() throws IOException {
		vertx = Vertx.vertx();
		try (ServerSocket s = new ServerSocket(0)) {
			httpPort = s.getLocalPort();
		}
	}
	
	@After
	public void teardown(TestContext context) {
		httpServer.close(context.asyncAssertSuccess());
		vertx.close(context.asyncAssertSuccess());
	}

	@Test
	public void test_helloWorld(TestContext context) throws Exception {
		Async async = context.async();
		
		// create the router
		Router router = Router.router(vertx);
		
		// setup an async datafetcher
		AsyncDataFetcher<String> helloFieldFetcher = (env, handler) -> {
			vertx.<String> executeBlocking(fut -> {
				// fail
				fut.fail(new RuntimeException("TestFailure"));
			}, handler);
		};
		
		// parse graphql IDL schema from hard coded string
		GraphQLSchema schema = IDLSchemaParser.create(vertx).fromString(
				"schema {" + 
				"    query: HelloWorld" + 
				"}" + 
				"type HelloWorld {" + 
				"    hello: String" + 
				"}", 
				RuntimeWiring.newRuntimeWiring()
						.type("HelloWorld", typeWiring -> typeWiring.dataFetcher("hello", helloFieldFetcher))
						.build());
		
		// prepare a routing context variable
		router.route().handler(routingContext -> {
			routingContext.put("testdata", "testvalue");
			routingContext.next();
		});
		
		router.post("/graphql").handler(BodyHandler.create()); // we need the body
		// create the graphql endpoint
		router.post("/graphql").handler(GraphQLPostRouteHandler.create(schema));
		
		// start the http server and make a call
		Future<HttpServer> server = Future.future();
		vertx.createHttpServer().requestHandler(router::accept).listen(httpPort, server);
		server.compose(httpServer -> {
			this.httpServer = httpServer;
			WebClient webClient= WebClient.create(vertx, new WebClientOptions().setDefaultPort(httpPort));
			
			// create the query request
			Future<HttpResponse<GraphQLQueryResult>> webClientResult = Future.future();
			JsonObject query = GraphQLQueryBuilder.newQuery("query { hello }").build();
			webClient.post("/graphql").as(GraphQLBodyCodec.queryResult()).sendJson(query, webClientResult);
			return webClientResult;
		}).map(response -> {
			// the response
			context.assertEquals(400, response.statusCode());
			context.assertNotNull(response.body());
			
			GraphQLQueryResult body = response.body();
			
			context.assertNull(body.getJson("hello"));
			context.assertNull(body.getData("hello", String.class));
			context.assertEquals(1, body.getErrors().size());
			context.assertEquals("TestFailure", body.getErrors().getJsonObject(0).getJsonObject("exception").getString("message"));
			return 0;
		}).setHandler(res -> {
			if (res.failed()) {
				context.fail(res.cause());
				return;
			}
			async.complete();
		});
	}
}
