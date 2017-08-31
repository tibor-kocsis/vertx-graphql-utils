package com.github.tkocsis.vertx.graphql.routehandler.internal;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.json.JsonArray;

public class ExceptionConvert {
	public static JsonArray toJsonArray(Throwable exception) {
		JsonArray jsonArray = new JsonArray();
		
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		StringBuilder caller = new StringBuilder();
		
		int i;
		for (i = 0; i < stackTrace.length; i++) {
			StackTraceElement stackTraceElement = stackTrace[i];
			if (stackTraceElement.getClassName().indexOf(ExceptionConvert.class.getName()) == -1)
				break;
		}
		
		if (stackTrace.length > i) {
			StackTraceElement stackTraceElement = stackTrace[i];
			String fullClassName = stackTraceElement.getClassName();
			String className = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
			caller.append(className).append("::");
			caller.append(stackTraceElement.getMethodName()).append("():");
			caller.append(stackTraceElement.getLineNumber());
		}
		
		jsonArray.add("Type: " + exception.getClass().getName());
		if (exception.getLocalizedMessage() != null) 
			jsonArray.add("Localized message: " + exception.getLocalizedMessage().replaceAll(System.lineSeparator(), ""));
		
		if (exception.getMessage() != null)
			jsonArray.add("Message: " + exception.getMessage().replaceAll(System.lineSeparator(), ""));

		
		List<Throwable> causes = new ArrayList<>();
		Throwable ex = exception.getCause();
		while (ex != null) {
			causes.add(ex);
			ex = ex.getCause();
			if (causes.contains(ex)) {
				break; // prevent an infinit loop
			}
		}
		
		for (i = causes.size() - 1; i >= 0; i--) {
			StringBuilder indent = new StringBuilder("  ");
			for (int j = 0; j < i; j++) {
				indent.append("  ");
			}
			jsonArray.add("Cause: " + causes.get(i));
			for (StackTraceElement element : causes.get(i).getStackTrace()) {
				jsonArray.add(indent + " C   " + element.toString());
			}
		}
		
		for (StackTraceElement element : exception.getStackTrace()) {
			jsonArray.add("     " + element.toString());
		}
		return jsonArray;
	}

}
