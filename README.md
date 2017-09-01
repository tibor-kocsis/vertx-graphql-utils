# vertx-graphql-utils

This project contains some helper classes you may need to write a GraphQL vert.x http server. Currently it is uses graphql-java 3.0, porting to the new Async interface introduced in graphql-java 4.0 in progress. 

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
  compile 'com.github.tibor-kocsis:vertx-graphql-utils:1.0.0'
}

```

### Short description of the utils

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

### Examples and usages

See the unit tests for detailed examples and use cases.

