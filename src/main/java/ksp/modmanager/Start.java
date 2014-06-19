package ksp.modmanager;

import java.io.IOException;
import java.util.Scanner;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

public class Start {
	public static String SERVER = "http://ovh.minichan.org:7777/";
	public static HttpRequestFactory requestFactory;

	static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	static final JsonFactory JSON_FACTORY = new JacksonFactory();

	static {
		requestFactory = HTTP_TRANSPORT
				.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
					}
				});
	}

	public static String getVersion() {
		try {
			try (Scanner sc = new Scanner(Start.class.getResource(
					"/res/VERSION").openStream()).useDelimiter("\\A")) {
				return sc.hasNext() ? sc.next() : "INVALID";
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "ERROR";
		}
	}
}
