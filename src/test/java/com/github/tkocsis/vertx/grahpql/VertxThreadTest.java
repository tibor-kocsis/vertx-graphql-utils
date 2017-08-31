package com.github.tkocsis.vertx.grahpql;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
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
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class VertxThreadTest {

	@Rule
	public RunTestOnContext rule = new RunTestOnContext();
	Vertx vertx;
	

	@Before
	public void setup() throws IOException {
		vertx = rule.vertx();
	}

	@After	
	public void teardown(TestContext context) {
		vertx.close(context.asyncAssertSuccess());
	}

	@Test
	public void test_vertxCallbackTest(TestContext context) {
		Async async = context.async();
		
		context.assertTrue(vertx.getOrCreateContext().isEventLoopContext());
		long threadId = Thread.currentThread().getId();
		
		GraphQLObjectType query = GraphQLObjectType.newObject()
		        .name("query")
		        .field(GraphQLFieldDefinition.newFieldDefinition()
		                .name("hello")
		                .type(Scalars.GraphQLString)
		                .dataFetcher(environment -> {
		                	context.assertTrue(vertx.getOrCreateContext().isEventLoopContext());
		                	context.assertEquals(threadId, Thread.currentThread().getId());
		                	return "world";
		        		}))
		        .build(); 
		
		GraphQLSchema schema = GraphQLSchema.newSchema()
				.query(query)
				.build();
		
		AsyncGraphQLExec asyncGraphQL = AsyncGraphQLExec.create(schema);
		Future<JsonObject> queryResult = asyncGraphQL.executeQuery("query { hello }", null, null, null);
		queryResult.setHandler(res -> {
			context.assertTrue(vertx.getOrCreateContext().isEventLoopContext());
        	context.assertEquals(threadId, Thread.currentThread().getId());
			async.complete();
		});
	}
	
	@Test
	public void test_vertxCallbackAsyncTest(TestContext context) {
		Async async = context.async();
		
		context.assertTrue(vertx.getOrCreateContext().isEventLoopContext());
		long threadId = Thread.currentThread().getId();
		
		AsyncDataFetcher<String> asyncDataFetcher = (env, handler) -> {
			context.assertTrue(vertx.getOrCreateContext().isEventLoopContext());
			context.assertEquals(threadId, Thread.currentThread().getId());
			
			vertx.<String> executeBlocking(fut -> {
				context.assertNotEquals(threadId, Thread.currentThread().getId());
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
			context.assertTrue(vertx.getOrCreateContext().isEventLoopContext());
        	context.assertEquals(threadId, Thread.currentThread().getId());
			async.complete();
		});
	}
}
