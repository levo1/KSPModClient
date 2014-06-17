package ksp.modmanager;

import javax.swing.SwingWorker;

import ksp.modmanager.api.SearchResult;
import ksp.modmanager.api.SearchUrl;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;

public abstract class SwingWebWorker<T> extends SwingWorker<T, T> {
	protected final GenericUrl url;
	protected final Class<T> clazz;

	public SwingWebWorker(GenericUrl url, Class<T> clazz) {
		this.url = url;
		this.clazz = clazz;
	}

	@Override
	protected T doInBackground() throws Exception {
		HttpRequest request = Start.requestFactory.buildGetRequest(url);
		return request.execute().parseAs(clazz);
	}

	protected abstract void done();
}
