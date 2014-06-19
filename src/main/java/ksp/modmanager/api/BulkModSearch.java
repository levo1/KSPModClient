package ksp.modmanager.api;

import java.util.Collection;

import com.google.api.client.util.escape.CharEscapers;
import com.google.common.base.Joiner;

public class BulkModSearch extends KSPUrl {

	public BulkModSearch(Collection<Long> ids) {
		super("mods/");
		appendRawPath(CharEscapers.escapeUriPath(Joiner.on(',').join(ids)));
		System.out.println(toString());
	}

}
