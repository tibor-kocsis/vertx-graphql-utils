# vertx-graphql-utils

This project contains some helper classes you may need to write a GraphQL vert.x http server. It uses graphql-java async execution environment combined with vert.x Future.  

### Getting started with gradle

Make sure jcenter is among your repos:

```
repositories {
    jcenter()
}

```

Dependency:

```
dependencies {
  compile 'com.github.tibor-kocsis:vertx-graphql-utils:2.0.6'
}

```

### Project contents

 - Vert.x RouteHandler for executing GraphQL queries with a Vertx HttpServer (only POST supported for now, for request/response formats see the [GraphQL docs](http://graphql.org/learn/serving-over-http/))
 
 
```java
Router router = Router.router(vertx); // vert.x router
GraphQLSchema schema = ...
router.post("/graphql").handler(BodyHandler.create()); // we need the body
router.post("/graphql").handler(GraphQLPostRouteHandler.create(schema));
```

 - AsyncDataFetcher interface with a Handler<AsyncResult> parameter you should use


```java
AsyncDataFetcher<String> helloFieldFetcher = (env, handler) -> {
	vertx.<String> executeBlocking(fut -> {
		fut.complete("world");
	}, handler);
};

GraphQLObjectType query = newObject()
    .name("query")
    .field(newFieldDefinition()
            .name("hello")
            .type(GraphQLString)
            .dataFetcher(helloFieldFetcher))
    .build(); 
```
 
 - A vert.x Future based interface for executing GraphQL queries
 
```java
GraphQLSchema schema = ...
AsyncGraphQLExec asyncGraphQL = AsyncGraphQLExec.create(schema);
Future<JsonObject> queryResult = asyncGraphQL.executeQuery("query { hello }", null, null, null); // query, operationName, context, variables
queryResult.setHandler(res -> {
	JsonObject json = res.result();
}); 
```
 
 - Some util functions to parse IDL schema from file or content
 
 ```java
Future<GraphQLSchema> fromFile(String path, RuntimeWiring runtimeWiring);
GraphQLSchema fromString(String schemaString, RuntimeWiring runtimeWiring);
 ```

 - A vert.x web BodyCodec and a basic query builder to perform graphql queries
 
 ```java
class Hero {
	// fields: id, name, age
} 
 
JsonObject query = GraphQLQueryBuilder
		.newQuery("query ($id: ID!) { hero(id: $id) { id name age } }")
		.var("id", 10)
		.build();

webClient.post("/graphql").as(GraphQLBodyCodec.queryResult()).sendJson(query, res -> {
	GraphQLQueryResult body = res.result().body();
	Hero hero = body.getData("hero", Hero.class);
}
 ```
 
### Minimal Vert.x HttpServer example

```java
import com.github.tkocsis.vertx.graphql.codec.GraphQLBodyCodec;
import com.github.tkocsis.vertx.graphql.datafetcher.AsyncDataFetcher;
import com.github.tkocsis.vertx.graphql.model.GraphQLQueryResult;
import com.github.tkocsis.vertx.graphql.routehandler.GraphQLPostRouteHandler;
import com.github.tkocsis.vertx.graphql.utils.GraphQLQueryBuilder;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
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
		
		router.post("/graphql").handler(BodyHandler.create()); // we need the body
		// create the graphql endpoint
		router.post("/graphql").handler(GraphQLPostRouteHandler.create(schema));
		
		// start the http server and make a call
		vertx.createHttpServer().requestHandler(router::accept).listen(8080);
		
		// test with vert.x webclient
		WebClient webClient= WebClient.create(vertx, new WebClientOptions().setDefaultPort(8080));
		JsonObject gqlQuery = GraphQLQueryBuilder.newQuery("query { hello }").build();
		webClient.post("/graphql").as(GraphQLBodyCodec.queryResult()).sendJson(gqlQuery, res -> {
			GraphQLQueryResult queryResult = res.result().body();
			System.out.println(queryResult.getData("hello", String.class)); // prints world
		});
	}
}
```

Fetching data with GraphiQL Chrome extension: 

![GrapiQL Chrome extension](https://raw.githubusercontent.com/tibor-kocsis/vertx-graphql-utils/master/doc/graphiql.png "GraphiQL Chrome extension")

See the unit tests for more detailed examples and use cases.

