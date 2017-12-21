package com.github.tkocsis.vertx.grahpql;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class GraphQLBodyCodecTest {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Hero {
		public int id;
		public String name;
		public int age;
		
		public Hero() {}
		
		public Hero(int id, String name, int age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}
	}
	
	Vertx vertx;
	int httpPort;
	HttpServer httpServer;
	WebClient webClient;
	
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
		
		// create the graphql endpoint
		getSchema().compose(schema -> {
			router.post("/graphql").handler(BodyHandler.create()); // we need the body
			router.post("/graphql").handler(GraphQLPostRouteHandler.create(schema));
			
			// start the http server and make a call
			Future<HttpServer> server = Future.future();
			vertx.createHttpServer().requestHandler(router::accept).listen(httpPort, server);
			return server;
		}).compose(httpServer -> {
			this.httpServer = httpServer;
			webClient= WebClient.create(vertx, new WebClientOptions().setDefaultPort(httpPort));
			
			Future<HttpResponse<GraphQLQueryResult>> webClientResult = Future.future();
			JsonObject query = GraphQLQueryBuilder
					.newQuery("query ($id: ID!) { hero(id: $id) { id name age } }")
					.var("id", 10)
					.build();
			
			webClient.post("/graphql").as(GraphQLBodyCodec.queryResult()).sendJson(query, webClientResult);
			return webClientResult;
		}).setHandler(res -> {
			if (res.failed()) {
				context.fail(res.cause());
				return;
			}
			context.assertEquals(200, res.result().statusCode());
			context.assertNotNull(res.result().body());
			
			GraphQLQueryResult queryResult = res.result().body();
			Hero hero = queryResult.getData("hero", Hero.class);
			context.assertEquals(10, hero.id);
			context.assertEquals(20, hero.age);
			context.assertEquals("Hero Name", hero.name);
			async.complete();
		});
	}
	
	private Future<GraphQLSchema> getSchema() {
		return IDLSchemaParser.create(vertx).fromFile("complex_query_testschema.graphqls",
				RuntimeWiring.newRuntimeWiring()
						.type("RootQueries", typeWiring -> typeWiring.dataFetcher("hero", heroFetcher()))
						.build());
	}
	
	private AsyncDataFetcher<Hero> heroFetcher() {
		return (env, handler) -> {
			vertx.<Hero> executeBlocking(fut -> {
				Hero hero = new Hero(Integer.parseInt(env.getArgument("id")), "Hero Name", 20);
				fut.complete(hero);
			}, handler);
		};
	}
	
}
