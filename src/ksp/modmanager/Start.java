package ksp.modmanager;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

public class Start {
	public static String SERVER = "http://127.0.0.1:8080/";

	static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	static final JsonFactory JSON_FACTORY = new JacksonFactory();

	public static void main(String[] args) throws Exception {
		HttpRequestFactory requestFactory = HTTP_TRANSPORT
				.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
					}
				});

		// HttpRequest request = requestFactory.buildGetRequest(new SearchUrl(
		// "Mechjeb"));
		// SearchResult result = request.execute().parseAs(SearchResult.class);
		// System.out.println(String.format("Got %s results", result.size()));
		// for (ApiMod mod : result) {
		// System.out.println(mod.getTitle());
		// }

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager
							.getSystemLookAndFeelClassName());
				} catch (Throwable t) {

				}
				new ModManager().setVisible(true);
			}
		});
	}
}
