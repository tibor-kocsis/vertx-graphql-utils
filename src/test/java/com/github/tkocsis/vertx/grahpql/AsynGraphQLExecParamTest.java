package com.github.tkocsis.vertx.grahpql;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tkocsis.vertx.graphql.queryexecutor.AsyncGraphQLExec;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
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
public class AsynGraphQLExecParamTest {

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
						.argument(GraphQLArgument.newArgument().name("myparam").type(Scalars.GraphQLString).build())
						.dataFetcher(environment -> {
							return environment.getArgument("myparam");
						}))
				.build(); 

		GraphQLSchema schema = GraphQLSchema.newSchema()
				.query(query)
				.build();

		AsyncGraphQLExec asyncGraphQL = AsyncGraphQLExec.create(schema);
		
		Map<String, Object> params = new HashMap<>();
		params.put("p", "myvalue");
		Future<JsonObject> queryResult = asyncGraphQL.executeQuery("query ($p: String) { hello(myparam: $p) }", null, null, params);
		queryResult.setHandler(res -> {
			JsonObject json = res.result();
			context.assertEquals(new JsonObject().put("hello", "myvalue"), json);
			async.complete();
		});
	}
}
