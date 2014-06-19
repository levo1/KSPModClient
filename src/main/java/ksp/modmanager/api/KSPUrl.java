package ksp.modmanager.api;
import ksp.modmanager.Start;

import com.google.api.client.http.GenericUrl;


public class KSPUrl extends GenericUrl {
	public KSPUrl(String api) {
		super(Start.SERVER);
		appendRawPath(api);
	}
}