# vertx-graphql-utils

This project contains the following helper classes you may need to write a GraphQL vert.x http server:

 - a Vert.x RouteHandler for executing GraphQL queries with a Vertx HttpServer (only POST supported for now, for request/response formats see the [GraphQL docs](http://graphql.org/learn/serving-over-http/))
 
 
```java
GraphQLSchema schema = ...
router.route().handler(BodyHandler.create()); // we need the body
router.post("/graphql").handler(GraphQLPostRouteHandler.create(schema));
```

 - AsyncDataFetcher interface


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
 
 - An async interface for executing GraphQL queries
 
```java
GraphQLSchema schema = ...
AsyncGraphQLExec asyncGraphQL = AsyncGraphQLExec.create(schema);
Future<JsonObject> queryResult = asyncGraphQL.executeQuery("query { hello }", null, null, null); // query, operationName, context, variables
queryResult.setHandler(res -> {
	JsonObject json = res.result();
	context.assertEquals(new JsonObject().put("hello", "world"), json);
	async.complete();
}); 
```
 
 - Some util functions to parse IDL schema from file or content
 
 ```java
public Future<GraphQLSchema> fromFile(String path, RuntimeWiring runtimeWiring);
public GraphQLSchema fromString(String schemaString, RuntimeWiring runtimeWiring);
  ```
 
 - The [graphql-rxjava](https://github.com/nfl/graphql-rxjava)'s RxExecutionStrategy ported to [graphql-java 3.0](https://github.com/tibor-kocsis/graphql-rxjava) 

## Examples

See the unit tests for detailed examples and use cases.

 