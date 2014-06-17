package ksp.modmanager.api;

import com.google.api.client.util.Key;

public class ApiMod {
	@Key
	private long id;
	@Key
	private String title;
	@Key
	private String description;
	@Key
	private String author;
	@Key
	private long last_update;
	@Key
	private String avatar;
	@Key
	private String sha1sum;

	public long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getAuthor() {
		return author;
	}

	public long getLast_update() {
		return last_update;
	}

	public String getAvatar() {
		return avatar;
	}

	public String getSha1sum() {
		return sha1sum;
	}

	public String getDownloadUrl() {
		return String.format(
				"http://kerbal.curseforge.com/ksp-mods/%s-mod/files/latest",
				getId());
	}

	public String getPageUrl() {
		return String.format("http://kerbal.curseforge.com/ksp-mods/%s",
				getId());
	}

}
