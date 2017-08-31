package com.github.tkocsis.vertx.grahpql;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tkocsis.vertx.graphql.datafetcher.AsyncDataFetcher;
import com.github.tkocsis.vertx.graphql.queryexecutor.AsyncGraphQLExec;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class AsyncGraphQLExecTest {

	Vertx vertx;

	@Before
	public void setup() throws IOException {
		vertx = Vertx.vertx();
	}

	@After
	public void teardown(TestContext context) {
		vertx.close(context.asyncAssertSuccess());
	}

	@Test
	public void test_helloWorld(TestContext context) {
		Async async = context.async();
		
		GraphQLObjectType query = GraphQLObjectType.newObject()
		        .name("query")
		        .field(GraphQLFieldDefinition.newFieldDefinition()
		                .name("hello")
		                .type(Scalars.GraphQLString)
		                .dataFetcher(environment -> {
		        			return "world";
		        		}))
		        .build(); 
		
		GraphQLSchema schema = GraphQLSchema.newSchema()
				.query(query)
				.build();
		
		AsyncGraphQLExec asyncGraphQL = AsyncGraphQLExec.create(schema);
		Future<JsonObject> queryResult = asyncGraphQL.executeQuery("query { hello }", null, null, null);
		queryResult.setHandler(res -> {
			JsonObject json = res.result();
			context.assertEquals(new JsonObject().put("data", new JsonObject().put("hello", "world")), json);
			async.complete();
		});
	}
	
	@Test
	public void test_failureSimpleDataFetcher(TestContext context) {
		Async async = context.async();
		
		GraphQLObjectType query = GraphQLObjectType.newObject()
		        .name("query")
		        .field(GraphQLFieldDefinition.newFieldDefinition()
		                .name("hello")
		                .type(Scalars.GraphQLString)
		                .dataFetcher(environment -> {
		        			throw new RuntimeException("TestFailure");
		        		}))
		        .build(); 
		
		GraphQLSchema schema = GraphQLSchema.newSchema()
				.query(query)
				.build();
		
		AsyncGraphQLExec asyncGraphQL = AsyncGraphQLExec.create(schema);
		Future<JsonObject> queryResult = asyncGraphQL.executeQuery("query { hello }", null, null, null);
		queryResult.setHandler(res -> {
			JsonObject json = res.result();
			//System.out.println(json.encodePrettily());
			
			context.assertEquals(new JsonObject().put("hello", (String) null), json.getJsonObject("data"));
			context.assertTrue(json.containsKey("errors"));
			context.assertTrue(json.getJsonArray("errors").size() > 0);
			async.complete();
		});
	}

	@Test
	public void test_failureAsyncDataFetcher(TestContext context) {
		Async async = context.async();
		
		// complete the execution with a failedFuture
		AsyncDataFetcher<String> asyncDataFetcher = (env, handler) -> {
			handler.handle(Future.failedFuture(new RuntimeException("TestFailure")));
		};
		
		GraphQLObjectType query = GraphQLObjectType.newObject()
		        .name("query")
		        .field(GraphQLFieldDefinition.newFieldDefinition()
		                .name("hello")
		                .type(Scalars.GraphQLString)
		                .dataFetcher(asyncDataFetcher))
		        .build(); 
		
		GraphQLSchema schema = GraphQLSchema.newSchema()
				.query(query)
				.build();
		
		AsyncGraphQLExec asyncGraphQL = AsyncGraphQLExec.create(schema);
		Future<JsonObject> queryResult = asyncGraphQL.executeQuery("query { hello }", null, null, null);
		queryResult.setHandler(res -> {
			context.assertTrue(res.failed());
			context.assertEquals(RuntimeException.class, res.cause().getClass());
			context.assertEquals("TestFailure", res.cause().getMessage());
			async.complete();
		});
	}
	
}
