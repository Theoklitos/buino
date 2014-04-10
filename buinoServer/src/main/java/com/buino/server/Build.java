package com.buino.server;

import org.apache.commons.lang.StringUtils;

/**
 * Represents a build in our system
 * 
 * @author takis
 *
 */
public final class Build {

	public enum BuildStatus {
		FAILURE,
		STARTED,
		ABORTED,
		SUCCESS,
		COMPLETED
	}

	public static final String UNSPECIFIED_AUTHOR_NAME = "unspecified";
	
	private final String url;	
	private final String name;	
	private final int number;
	private String author;
	private final BuildStatus status;	
	
	public Build(final String fullUrl, final String name, final int number, final BuildStatus status) {
		this.url = fullUrl;
		this.name = name;
		this.number = number;		
		this.status = status;
		this.author = UNSPECIFIED_AUTHOR_NAME;
	}

	public String getAuthor() {
		return author;
	}
	
	public String getName() {
		return name;
	}
	
	public int getNumber() {
		return number;
	}

	public BuildStatus getStatus() {
		return status;
	}

	public String getUrl() {
		return url;
	}

	public boolean isAuthorUnspecified() {
		return author.equals(UNSPECIFIED_AUTHOR_NAME) || StringUtils.isBlank(author);
	}

	public void setAuthor(final String author) {
		if(StringUtils.isBlank(author)) {
			this.author = UNSPECIFIED_AUTHOR_NAME;
		} else {
			this.author = author;
		}		
	}
	
	@Override
	public String toString() {
		if(isAuthorUnspecified()) {
			return "Build named \"" + name + "\" at revision #" + number + ". Status is " + status + ".";
		} else {
			return "Build named \"" + name + "\" at revision #" + number + ". Author is " + author + " and status is " + status + ".";
		}
		
	}
	
}
