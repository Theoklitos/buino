package com.buino.client.data;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

/**
 * Simple build POJO
 * 
 * @author takis
 * 
 */
public class Build {

	public enum BuildStatus {
		SUCCESS,
		FAILURE
	}

	private String author;
	private final String name;
	private int number;
	private BuildStatus status;
	private String url;
	private Date dateOfUpdate;

	/**
	 * @throws IllegalArgumentException when one of its parameters is null or empty (string)
	 */
	public Build(final String author, final String name, final int number,
			final BuildStatus status, final String url, final Date dateOfUpdate) {
		if (StringUtils.isEmpty(author) || StringUtils.isEmpty(name) || (status == null)
			|| StringUtils.isEmpty(url) || (dateOfUpdate == null)) {
			throw new IllegalArgumentException(
					"One or more parameters during build initialization are null");
		}
		this.author = author;
		this.name = name;
		this.number = number;
		this.status = status;
		this.url = url;
		this.dateOfUpdate = dateOfUpdate;
	}
	
	public String getAuthor() {
		return author;
	}

	public Date getDateOfUpdate() {
		return dateOfUpdate;
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

	/**
	 * Changes the data in THIS object to match with the one in the parameter. Names are not changed! 
	 * 
	 * @return true if changed
	 */
	public boolean merge(final Build build) {
		boolean hasChanged = false;
		if(!author.equals(build.getAuthor())) {
			author = build.getAuthor();
			hasChanged = true;
		}		
		if(number !=build.getNumber()) {
			number = build.getNumber();
			hasChanged = true;
		}
		if(!status.equals(build.getStatus())) {
			status = build.getStatus();
			hasChanged = true;
		}		
		if(!url.equals(build.getUrl())) {
			url = build.getUrl();
			hasChanged = true;
		}
		if(!dateOfUpdate.equals(build.getDateOfUpdate())) {
			dateOfUpdate = build.getDateOfUpdate();
			hasChanged = true;
		}		
		return hasChanged;
	}

	public void setDateOfUpdate(final Date date) {
		this.dateOfUpdate = date;
	}

	@Override
	public String toString() {
		return "Build \"" + name + "\" #" + number + ", status: " + status + ". Author: " + author; 
	}

}
