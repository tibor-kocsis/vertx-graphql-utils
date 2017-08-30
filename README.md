# vertx-graphql-utils

This project contains the following helper classes you need to write a GraphQL vert.x http server:

 - a Vert.x RouteHandler for executing GraphQL queries in the vert.x async way (the request/response format follows the GraphiQL requirements)
 - AsyncDataFetcher interface
 - Some util functions to parse IDL schema from file or content
 - RxExecutionStrategy ported to graphql-java 3.0 (https://github.com/tibor-kocsis/graphql-rxjava) 
