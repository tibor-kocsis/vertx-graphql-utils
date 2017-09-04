package com.github.tkocsis.vertx.grahpql;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.tkocsis.vertx.graphql.datafetcher.AsyncDataFetcher;
import com.github.tkocsis.vertx.graphql.routehandler.GraphQLPostRouteHandler;
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
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.BodyHandler;

@RunWith(VertxUnitRunner.class)
public class RouteHandlerComplexQueryTest {
	
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
			
			/**
			 * Perform a query with embedded parameter
			 */
			Future<HttpResponse<JsonObject>> webClientResult = Future.future();
			JsonObject query = new JsonObject()
					.put("query", "query { echo(p: \"myvalue\") }")
					.put("variables", new JsonObject());
			webClient.post("/graphql").as(BodyCodec.jsonObject()).sendJson(query, webClientResult);
			return webClientResult;
		}).compose(response -> {
			// the response
			context.assertEquals(200, response.statusCode());
			context.assertNotNull(response.body());
			context.assertEquals("myvalue", response.body().getJsonObject("data").getString("echo"));
			
			/**
			 * Perform a query with variables json
			 */
			Future<HttpResponse<JsonObject>> webClientResult = Future.future();
			JsonObject query = new JsonObject()
					.put("query", "query ($param: String) { echo(p: $param) }")
					.put("variables", new JsonObject().put("param", "myvalue"));
			webClient.post("/graphql").as(BodyCodec.jsonObject()).sendJson(query, webClientResult);
			return webClientResult;
		}).compose(response -> {
			context.assertEquals(200, response.statusCode());
			context.assertNotNull(response.body());
			context.assertEquals("myvalue", response.body().getJsonObject("data").getString("echo"));
			
			/**
			 * Perform a query with variables json
			 */
			Future<HttpResponse<JsonObject>> webClientResult = Future.future();
			JsonObject query = new JsonObject()
					.put("query", "mutation ($hero: HeroInput) { insertHero(hero: $hero) { id, name, age } }")
					.put("variables", new JsonObject().put("hero", new JsonObject()
							.put("name", "testName")
							.put("age", 30)));
			webClient.post("/graphql").as(BodyCodec.jsonObject()).sendJson(query, webClientResult);
			return webClientResult;
		}).map(response -> {
			// the response
			context.assertEquals(200, response.statusCode());
			context.assertNotNull(response.body());
			Hero newHero = response.body().getJsonObject("data").getJsonObject("insertHero").mapTo(Hero.class);
			context.assertEquals(10, newHero.id);
			context.assertEquals(30, newHero.age);
			context.assertEquals("testName", newHero.name);
			return 0;
		}).setHandler(res -> {
			if (res.failed()) {
				context.fail(res.cause());
				return;
			}
			async.complete();
		});
	}
	
	private Future<GraphQLSchema> getSchema() {
		return IDLSchemaParser.create(vertx).fromFile("complex_query_testschema.graphqls",
				RuntimeWiring.newRuntimeWiring()
						.type("RootQueries", typeWiring -> typeWiring.dataFetcher("echo", helloFetcher()))
						.type("RootMutations", typeWiring -> typeWiring.dataFetcher("insertHero", heroMutation()))
						.build());
	}
	
	private AsyncDataFetcher<String> helloFetcher() {
		return (env, handler) -> {
			vertx.<String> executeBlocking(fut -> {
				fut.complete(env.getArgument("p"));
			}, handler);
		};
	}
	
	private AsyncDataFetcher<Hero> heroMutation() {
		return (env, handler) -> {
			vertx.<Hero> executeBlocking(fut -> {
				@SuppressWarnings("unchecked")
				JsonObject heroJson = new JsonObject((Map<String, Object>) env.getArgument("hero"));
				Hero hero = heroJson.mapTo(Hero.class);
				hero.id = 10;
				fut.complete(hero);
			}, handler);
		};
	}
	
}
