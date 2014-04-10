package com.buino.server.call;

/**
 * When something goes wrong during communcation with Prase
 * 
 * @author takis
 * 
 */
public class ParseException extends Exception {

	private static final long serialVersionUID = 1L;

	public ParseException(final Exception exception) {
		super(exception);
	}

	public ParseException(final String message) {
		super(message);
	}

}
