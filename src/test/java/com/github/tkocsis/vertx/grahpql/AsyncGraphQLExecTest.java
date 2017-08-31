package com.github.tkocsis.vertx.grahpql;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tkocsis.vertx.graphql.datafetcher.AsyncDataFetcher;
import com.github.tkocsis.vertx.graphql.queryexecutor.AsyncExecutionException;
import com.github.tkocsis.vertx.graphql.queryexecutor.AsyncGraphQLExec;

import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
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
			context.assertEquals(new JsonObject().put("hello", "world"), json);
			async.complete();
		});
	}
	
	@Test
	public void test_helloWorldAsync(TestContext context) {
		Async async = context.async();
		
		AsyncDataFetcher<String> asyncDataFetcher = (env, handler) -> {
			vertx.<String> executeBlocking(fut -> {
				fut.complete("world");
			}, handler);
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
			JsonObject json = res.result();
			context.assertEquals(new JsonObject().put("hello", "world"), json);
			async.complete();
		});
	}
	
	@Test
	public void test_helloWorldCollection(TestContext context) {
		Async async = context.async();
		
		GraphQLObjectType query = GraphQLObjectType.newObject()
		        .name("query")
		        .field(GraphQLFieldDefinition.newFieldDefinition()
		                .name("helloList")
		                .type(new GraphQLList(Scalars.GraphQLString))
		                .dataFetcher((env) -> {
		        			return Arrays.asList("a", "b", "c");
		        		}))
		        .build(); 
		
		GraphQLSchema schema = GraphQLSchema.newSchema()
				.query(query)
				.build();
		
		AsyncGraphQLExec asyncGraphQL = AsyncGraphQLExec.create(schema);
		Future<JsonObject> queryResult = asyncGraphQL.executeQuery("query { helloList }", null, null, null);
		queryResult.setHandler(res -> {
			JsonObject json = res.result();
			JsonArray jsonArray = json.getJsonArray("helloList");
			JsonArray expected = new JsonArray(Arrays.asList("a", "b", "c"));
			context.assertEquals(expected, jsonArray);
			async.complete();
		});
	}
	
	@Test
	public void test_helloWorldCollectionAsync(TestContext context) {
		Async async = context.async();
		
		AsyncDataFetcher<List<String>> asyncDataFetcher = (env, handler) -> {
			vertx.<List<String>> executeBlocking(fut -> {
				fut.complete(Arrays.asList("a", "b", "c"));
			}, handler);
		};
		
		GraphQLObjectType query = GraphQLObjectType.newObject()
		        .name("query")
		        .field(GraphQLFieldDefinition.newFieldDefinition()
		                .name("helloList")
		                .type(new GraphQLList(Scalars.GraphQLString))
		                .dataFetcher(asyncDataFetcher))
		        .build(); 
		
		GraphQLSchema schema = GraphQLSchema.newSchema()
				.query(query)
				.build();
		
		AsyncGraphQLExec asyncGraphQL = AsyncGraphQLExec.create(schema);
		Future<JsonObject> queryResult = asyncGraphQL.executeQuery("query { helloList }", null, null, null);
		queryResult.setHandler(res -> {
			JsonObject json = res.result();
			JsonArray jsonArray = json.getJsonArray("helloList");
			JsonArray expected = new JsonArray(Arrays.asList("a", "b", "c"));
			context.assertEquals(expected, jsonArray);
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
			context.assertTrue(res.failed());
			context.assertEquals(AsyncExecutionException.class, res.cause().getClass());
			List<GraphQLError> errors = ((AsyncExecutionException) res.cause()).getErrors();
//			System.out.println(errors);
			context.assertEquals(1, errors.size());
			context.assertEquals(ExceptionWhileDataFetching.class, errors.get(0).getClass());
			context.assertEquals("TestFailure", ((ExceptionWhileDataFetching) errors.get(0)).getException().getMessage());
			
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
			context.assertEquals(AsyncExecutionException.class, res.cause().getClass());
			List<GraphQLError> errors = ((AsyncExecutionException) res.cause()).getErrors();
//			System.out.println(errors);
			context.assertEquals(1, errors.size());
			context.assertEquals(ExceptionWhileDataFetching.class, errors.get(0).getClass());
			context.assertEquals("TestFailure", ((ExceptionWhileDataFetching) errors.get(0)).getException().getMessage());
			async.complete();
		});
	}
	
}
