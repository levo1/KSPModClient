package ksp.modmanager.api;
import com.google.api.client.util.escape.CharEscapers;


public class SearchUrl extends KSPUrl {

	public SearchUrl(String query) {
		super("search/");
		appendRawPath(CharEscapers.escapeUriPath(query));
	}

}
