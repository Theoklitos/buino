package com.buino.client.util;

import java.util.Date;

import org.json.JSONException;

import com.buino.client.data.Build;
import com.buino.client.data.Build.BuildStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.parse.ParseObject;

/**
 * Converts ParseObject back and forth to strings
 * 
 * @author takis
 * 
 */
public final class JsonUtils {

	public static Build fromJsonString(final String jsonString) {
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final Build parsedObject = gson.fromJson(jsonString, Build.class);
		return parsedObject;
	}
	
	/**
	 * Converts a {@link ParseObject} to a {@link Build}
	 * 
	 * @throws JSONException
	 */
	public static Build parseFromParseObject(final ParseObject parseObject) throws JSONException {		
		final String author = parseObject.getString("author");
		final String name = parseObject.getString("name");
		final int number = parseObject.getInt("number");
		final BuildStatus buildStatus = BuildStatus.valueOf(parseObject.getString("status"));
		final String url = parseObject.getString("url");
		final Date dateOfUpdate = parseObject.getUpdatedAt();
		try {
		final Build build = new Build(author, name, number, buildStatus, url, dateOfUpdate);
		return build;
		} catch(final IllegalArgumentException e) {
			throw new JSONException("Error while converting ParseObject to json: " + e.getMessage());
		}		
	}

	public static String toJsonString(final Build build) {
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final String buildAsJsonString = gson.toJson(build);
		return buildAsJsonString;
	}

	/**
	 * Converts a {@link ParseObject} to a json string with the help of {@link Gson}
	 * 
	 * @throws JSONException if something goes wrong during parsing
	 */
	public static String toJsonString(final ParseObject parseObject) throws JSONException {		
		final Build build = parseFromParseObject(parseObject);		
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final String buildAsJsonString = gson.toJson(build);
		return buildAsJsonString;
	}
}
