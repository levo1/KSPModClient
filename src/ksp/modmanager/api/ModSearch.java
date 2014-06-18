package ksp.modmanager.api;


public class ModSearch extends KSPUrl {
	public ModSearch(long id) {
		super("mod/");
		appendRawPath(String.valueOf(id));
	}
}
