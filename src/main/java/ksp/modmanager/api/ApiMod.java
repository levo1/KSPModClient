package ksp.modmanager.api;

import java.util.List;

import com.google.api.client.util.Key;

public class ApiMod {
	public boolean installing = false;
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
	@Key
	private long size;
	@Key
	public List<String> files;

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

	public long getSize() {
		return size;
	}

	public String getHumanReadableSize() {
		boolean si = true;

		int unit = si ? 1000 : 1024;
		if (size < unit)
			return size + " B";
		int exp = (int) (Math.log(size) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
				+ (si ? "" : "i");
		return String.format("%.1f %sB", size / Math.pow(unit, exp), pre);

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
	
	public String getInfoName() {
		return String.format("mod-%s.modjson", getId());
	}

}
