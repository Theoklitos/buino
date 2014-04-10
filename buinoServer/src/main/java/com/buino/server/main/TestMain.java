package com.buino.server.main;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.Gson;

/**
 * jada jada
 */
public class TestMain {

	public final static void main(final String args[]) throws Exception {
		final Gson gson = new Gson();
		//FAILURE SUCCESS
		final String info =	"{\"name\":\"dummy\",\n" + 
							"\"url\":\"job/fitness/\",\n" + 
							"\"build\":{\"number\":3661,\n" +
							"         \"phase\":\"COMPLETED\",\n" +
							"         \"status\":\"SUCCESS\",\n" +
							"         \"url\":\"job/fitnesss/2833/\",\n" +
							"         \"full_url\":\"http://srv.hikuku.de:20080/hudson/job/fitness/2833/\",\n" +
							"         \"parameters\":{\"branch\":\"master\"}\n" +
							"        }\n"+
							"}";
		
		
		final byte[] data = gson.toJson(info).getBytes();
		
		System.out.println("Sending:\n\n" + info);
		
		final URL targetUrl = new URL("http://localhost:20667/buino");
		final URLConnection connection = targetUrl.openConnection();
		
		if (connection instanceof HttpURLConnection) {
			((HttpURLConnection) connection).setFixedLengthStreamingMode(data.length);
		}
		connection.setDoInput(false);
		connection.setDoOutput(true);
		
		final OutputStream output = connection.getOutputStream();
		output.write(data);
		output.flush();
		output.close();
	}
}
