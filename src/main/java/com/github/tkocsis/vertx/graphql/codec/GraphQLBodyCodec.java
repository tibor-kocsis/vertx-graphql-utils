package com.github.tkocsis.vertx.graphql.codec;

import com.github.tkocsis.vertx.graphql.model.GraphQLQueryResult;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.impl.BodyCodecImpl;

public class GraphQLBodyCodec {

	public static BodyCodecImpl<GraphQLQueryResult> queryResult() {
		return new BodyCodecImpl<>(buff -> {
			JsonObject result = new JsonObject(buff.toString());
			GraphQLQueryResult decoded = new GraphQLQueryResult(result.getJsonObject("data"), result.getJsonArray("errors"));  
			return decoded;
		});
	}
}
