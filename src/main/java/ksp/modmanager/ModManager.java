package ksp.modmanager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.text.DefaultEditorKit.CopyAction;

import ksp.modmanager.EnabledMods.EnabledMod;
import ksp.modmanager.api.ApiMod;
import ksp.modmanager.api.BulkModResult;
import ksp.modmanager.api.BulkModSearch;
import ksp.modmanager.api.ModSearch;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.json.JsonGenerator;
import com.google.api.client.util.IOUtils;
import com.google.api.client.util.escape.CharEscapers;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;

public class ModManager {
	private static String[] kspPaths = new String[] {
			"C:/Program Files (x86)/Steam/",
			"C:/Program Files/Steam/",
			System.getProperty("user.home")
					+ "/Library/Application Support/Steam/",
			System.getProperty("user.home") + "/Steam/",
			System.getProperty("user.home") + "/.local/share/Steam/" };
	private String kspPath = Config.get.getKspDirectory();
	private Path gameData, enabledModsFile;
	private LoadingCache<Long, ApiMod> serverSideMods, installedMods;

	private Map<Long, Boolean> updateAvailable = new HashMap<>();

	private Pattern modRegex = Pattern.compile("^mod-(\\d)$",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private List<ModEventListener> listeners = new ArrayList<>();

	public ModManager() {
		if (kspPath != null && !testKspDir(kspPath))
			kspPath = null;

		if (kspPath == null) {
			for (String path : kspPaths) {
				path += "SteamApps/common/Kerbal Space Program";
				if (testKspDir(path)) {
					kspPath = path;
					break;
				}
			}

			if (kspPath == null) {
				JOptionPane
						.showMessageDialog(
								null,
								"The Mod Manager has not managed to detect your KSP installation directory. Please select your Kerbal Space Program directory which contains your GameData directory");

			}

			while (kspPath == null) {
				kspPath = manualKspPath(true, kspPath);
			}

			Config.get.setKspDirectory(kspPath).save();
		}

		gameData = Paths.get(kspPath, "GameData");
		enabledModsFile = gameData.resolve("enabled-mods.json");

		serverSideMods = CacheBuilder.newBuilder().concurrencyLevel(2)
				.expireAfterWrite(30, TimeUnit.MINUTES)
				.build(new CacheLoader<Long, ApiMod>() {
					public ApiMod load(Long key) throws IOException {
						System.out.println("Downloading info about " + key);
						HttpRequest request = Start.requestFactory
								.buildGetRequest(new ModSearch(key));
						return request.execute().parseAs(ApiMod.class);
					}
				});

		installedMods = CacheBuilder.newBuilder().concurrencyLevel(2)
				.expireAfterWrite(30, TimeUnit.SECONDS)
				.build(new CacheLoader<Long, ApiMod>() {
					public ApiMod load(Long key) throws IOException {
						File file = new File(Config.cachedir, "mod-" + key
								+ ".modjson");
						return getInstalledModFromFile(file);
					}
				});

		checkForUpdates();
	}

	public boolean isModInstalled(ApiMod mod) {
		// System.out.println("Hitting FS"); //TODO cache or something?
		try {
			return installedMods.get(mod.getId()) != null;
		} catch (ExecutionException e) {
			return false;
		}
	}

	public boolean isModEnabled(ApiMod mod) {
		return installedMods.getUnchecked(mod.getId()).enableInfo != null;
	}

	public boolean isUpdateAvailable(ApiMod mod) {
		Boolean update = updateAvailable.get(mod.getId());
		if (update == null)
			return false;
		return update;
	}

	private ApiMod getInstalledModFromFile(File file) throws IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			ApiMod mod = Start.JSON_FACTORY.createJsonParser(fis)
					.parseAndClose(ApiMod.class);

			EnabledMods enabled = getEnabledMods();
			mod.enableInfo = enabled.get(mod.getId());

			return mod;
		}
	}

	private EnabledMods _enabledMods;

	private EnabledMods getEnabledMods() throws IOException {
		if (_enabledMods != null) {
			return _enabledMods;
		}
		System.out.println("Refreshing enabledMods");
		try (FileInputStream fis = new FileInputStream(enabledModsFile.toFile())) {
			return _enabledMods = Start.JSON_FACTORY.createJsonParser(fis)
					.parseAndClose(EnabledMods.class);
		} catch (FileNotFoundException ex) {
			return _enabledMods = new EnabledMods();
		}
	}

	public Map<Long, ApiMod> getAllInstalledMods() {
		installedMods.invalidateAll();

		try (DirectoryStream<Path> directoryStream = Files
				.newDirectoryStream(Config.cachedir.toPath())) {
			for (Path file : directoryStream) {
				String name = file.getFileName().toString();
				if (name.startsWith("mod-") && name.endsWith(".modjson")) {
					ApiMod mod = getInstalledModFromFile(file.toFile());
					installedMods.put(mod.getId(), mod);
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return installedMods.asMap();
	}

	public void checkForUpdates() {
		try {
			Collection<ApiMod> installedMods = getAllInstalledMods().values();

			HttpRequest request = Start.requestFactory
					.buildGetRequest(new BulkModSearch(Collections2.transform(
							installedMods, new Function<ApiMod, Long>() {
								@Override
								public Long apply(ApiMod mod) {
									return mod.getId();
								}
							})));

			BulkModResult result = request.execute().parseAs(
					BulkModResult.class);
			for (ApiMod found : result.values()) {
				serverSideMods.put(found.getId(), found);
			}

			for (ApiMod local : installedMods) {
				ApiMod remote = serverSideMods.get(local.getId());
				if (remote == null) {
					System.err
							.println(String
									.format("Local mod %s (%s) installed, but cannot find on remote.",
											local.getId(), local.getTitle()));
					updateAvailable.put(local.getId(), false);
					continue;
				}

				boolean update = !remote.getSha1sum()
						.equals(local.getSha1sum());

				System.out.println("Update for " + local.getId() + ": "
						+ update);

				updateAvailable.put(local.getId(), update);
			}
		} catch (IOException | ExecutionException ex) {
			ex.printStackTrace();
		}
	}

	private void saveEnabledMods(EnabledMods mods) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(
				enabledModsFile.toFile())) {
			JsonGenerator generator = Start.JSON_FACTORY.createJsonGenerator(
					fos, Charset.forName("UTF-8"));
			generator.enablePrettyPrint();
			generator.serialize(mods);
			generator.close();
			_enabledMods = mods;
		}
	}

	// protected File getConfigFileForMod(ApiMod mod) {
	// return gameData.resolve(mod.getInfoName()).toFile();
	// }

	public void installMod(ApiMod mod) throws IOException {
		downloadMod(mod);
		enableMod(mod);
	}

	public void uninstallMod(ApiMod mod) throws IOException {
		disableMod(mod);
		File cacheFile = new File(Config.cachedir, "mod-" + mod.getId()
				+ ".zip");
		File infoFile = new File(Config.cachedir, mod.getInfoName());
		cacheFile.delete();
		infoFile.delete();

		emit(new ModUninstallEvent(mod));
	}

	public void disableMod(ApiMod mod) throws IOException {
		EnabledMods enabledMods = getEnabledMods();
		EnabledMod enabled = enabledMods.get(mod.getId());
		if (enabled != null) {
			enabledMods.remove(mod.getId());

			for (String filePath : enabled.files) {
				File file = new File(gameData.toFile(), filePath);
				while (file.delete()) {
					file = file.getParentFile();
				}
			}

			saveEnabledMods(enabledMods);
			List<ApiMod> sortedEnabled = new ArrayList<>();
			for (Entry<String, EnabledMod> entry : enabledMods.entrySet()) {
				sortedEnabled.add(installedMods.getUnchecked(Long.valueOf(entry
						.getKey())));
			}
			Collections.sort(sortedEnabled, new Comparator<ApiMod>() {
				@Override
				public int compare(ApiMod o1, ApiMod o2) {
					return Long.valueOf(o2.getId()).compareTo(o1.getId()); // reverse
				}
			});

			Set<String> fileSet = new HashSet<>(mod.enableInfo.files);

			for (ApiMod e : sortedEnabled) {
				Set<String> relevantFiles = new HashSet<>();

				for (String file : e.enableInfo.files) {
					if (fileSet.contains(file)) {
						relevantFiles.add(file);
					}
				}

				if (relevantFiles.size() > 0) {
					enableMod(e, relevantFiles);
					fileSet.removeAll(relevantFiles);
				}
			}
		}

		emit(new ModDisableEvent(mod));
	}

	private File downloadMod(ApiMod mod) throws IOException {
		System.out.println("Starting download for " + mod.getTitle());

		String url = mod.getDownloadUrl();
		HttpURLConnection connection;
		boolean redirect = false;

		do {
			URL website = new URL(url);
			System.out.println("Accessing " + url);
			connection = (HttpURLConnection) website.openConnection();
			connection.setInstanceFollowRedirects(false);
			String location = connection.getHeaderField("Location");
			if (location != null) {
				connection.disconnect();
				url = location.replaceAll("(?i)/(\\d+)\\\\(\\d+)/", "/$1/$2/");

				if (!url.startsWith("http"))
					url = "http:" + url;

				URL u = new URL(url);
				String[] parts = u.getPath().split("/");
				for (int i = 0; i < parts.length; i++) {
					parts[i] = CharEscapers.escapeUriPath(parts[i]);
				}

				url = u.getProtocol() + "://" + u.getHost() + "/"
						+ Joiner.on('/').join(parts)
						+ (u.getQuery() != null ? "?" + u.getQuery() : "");
				redirect = true;
			} else {
				redirect = false;
			}
		} while (redirect);

		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		File cacheFile = new File(Config.cachedir, "mod-" + mod.getId()
				+ ".zip");
		File cacheConfigFile = new File(Config.cachedir, mod.getInfoName());

		try (DigestInputStream dis = new DigestInputStream(
				connection.getInputStream(), digest);
				FileOutputStream fos = new FileOutputStream(cacheFile)) {
			IOUtils.copy(dis, fos);
		}

		try (FileOutputStream fos = new FileOutputStream(cacheConfigFile)) {
			JsonGenerator generator = Start.JSON_FACTORY.createJsonGenerator(
					fos, Charset.forName("UTF-8"));
			generator.enablePrettyPrint();
			generator.serialize(mod);
			generator.close();
		}

		System.out.println("Download finished "
				+ byteArray2Hex(digest.digest()) + " vs " + mod.getSha1sum());
		return cacheFile;
	}

	public void enableMod(ApiMod mod) throws IOException {
		enableMod(mod, null);
	}

	public void enableMod(ApiMod mod, Set<String> relevantFiles)
			throws IOException {
		final Path tempFile = Files.createTempDirectory("ksp-mod-");
		try {
			File cacheFile = new File(Config.datadir, "cache/mod-"
					+ mod.getId() + ".zip");

			if (!cacheFile.exists())
				cacheFile = downloadMod(mod);

			try (ZipInputStream is = new ZipInputStream(new FileInputStream(
					cacheFile))) {
				unzip(is, tempFile.toFile());
			}

			final ModInstallFile root = new ModInstallFile(
					"mod-" + mod.getId(), null, tempFile, true);
			Files.walkFileTree(tempFile, new SimpleFileVisitor<Path>() {
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					if (dir.equals(tempFile))
						return FileVisitResult.CONTINUE;
					addToTree(dir, true);
					return FileVisitResult.CONTINUE;
				}

				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					addToTree(file, false);
					return FileVisitResult.CONTINUE;
				}

				private void addToTree(Path file, boolean directory) {
					String path = tempFile.relativize(file).toString();

					String[] parts = path.split(Pattern.quote(File.separator));
					ModInstallFile parent = root;
					for (int i = 0; i < parts.length - 1; i++) {
						parent = parent.children.get(parts[i]);
					}

					parent.children.put(parts[parts.length - 1],
							new ModInstallFile(file.getFileName().toString(),
									parent, file, directory));
				}
			});

			EnabledMods enabledMods = getEnabledMods();
			if (relevantFiles == null && enabledMods.containsKey(mod.getId())) {
				disableMod(mod);
			}

			try {
				EnabledMod origEnabled = mod.enableInfo;

				EnabledMod enabledMod = new EnabledMod();
				mod.enableInfo = enabledMod;

				boolean result = installModFromTemp(mod, root, relevantFiles);
				if (result) {
					if (relevantFiles == null) {
						Long latestEnableOrder = 0l;
						for (EnabledMod other : enabledMods.values()) {
							if (other.enableId > latestEnableOrder)
								latestEnableOrder = other.enableId;
						}

						enabledMod.enableId = latestEnableOrder + 1;

						enabledMods.put(mod.getId(), enabledMod);
						saveEnabledMods(enabledMods);
					} else {
						mod.enableInfo = origEnabled;
					}
				} else {
					if (relevantFiles != null) {
						mod.enableInfo = origEnabled;
					}

					JOptionPane.showMessageDialog(null,
							"Install for \"" + mod.getTitle() + "\" failed!");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(
						null,
						"Install for \"" + mod.getTitle() + "\" failed! "
								+ ex.getMessage());
			}

			emit(new ModEnableEvent(mod));
		} finally {
			deleteDirectory(tempFile.toAbsolutePath());
		}
	}

	private boolean installModFromTemp(ApiMod mod, ModInstallFile root,
			Set<String> relevantFiles) throws IOException,
			InstallFailedException {
		ModInstallFile gamedata = root.children.get("GameData");
		List<ModInstallFile> toInstall = new ArrayList<>();

		if (gamedata == null || !gamedata.isDirectory)
			gamedata = root.children.get("gamedata");

		if (gamedata != null && gamedata.isDirectory) {
			for (ModInstallFile file : gamedata.children.values()) {
				toInstall.add(file);
			}
			return doInstall(mod, toInstall, relevantFiles);
		}

		for (ModInstallFile dir : root.children.values()) {
			if (dir.isDirectory && dir.name.equalsIgnoreCase("gamedata")) {
				for (ModInstallFile file : dir.children.values()) {
					toInstall.add(file);
				}
				return doInstall(mod, toInstall, relevantFiles);
			}
		}

		boolean found = false;

		String[] validFiles = { "asset", "flag", "gfx", "icon", "internal",
				"part", "planetresourcedata", "plugin", "plugindata", "prop",
				"resource", "ship", "sound", "space", "tb_icon", "texture",
				"texturecache" };

		for (ModInstallFile dir : root.children.values()) {
			if (dir.isDirectory) {
				boolean match = false;

				for (ModInstallFile file : dir.children.values()) {
					String name = file.name.toLowerCase();
					for (String validFile : validFiles)
						if (name.equals(validFile)
								|| name.equals(validFile + "s")) {
							match = true;
							break;
						}
					if ((name.endsWith(".cfg") && !name.equals("part.cfg"))
							|| name.endsWith(".dll") || match) {
						match = true;
						break;
					}
				}

				if (match) {
					found = true;
					toInstall.add(dir);
				}
			}
		}

		if (!found) {
			for (ModInstallFile child : root.children.values()) {
				toInstall.add(child);
			}
		}

		return doInstall(mod, toInstall, relevantFiles);
	}

	private enum OverwriteStatus {
		Overwrite("Overwrite"), OverwriteAll("Overwrite All"), KeepOld(
				"Keep Old"), Abort("Abort");
		private String name;

		OverwriteStatus(String name) {
			this.name = name;
		}

		public String toString() {
			if (this.name != null)
				return this.name;
			else
				return super.toString();
		}
	}

	private OverwriteStatus checkOverwrite(ApiMod mod, ModInstallFile file)
			throws InstallFailedException {
		int result = JOptionPane
				.showOptionDialog(
						null,
						mod.getTitle()
								+ " wants to overwrite the file \""
								+ file.getFullName()
								+ "\" in GameData. If you disable this mod the original will be restored.",
						"Conflict", 0, JOptionPane.INFORMATION_MESSAGE, null,
						OverwriteStatus.values(), null);

		return OverwriteStatus.values()[result];
		// if (result == 0) {
		// return true;
		// } else if (result == 1) {
		// return false;
		// } else {
		// throw new InstallFailedException(
		// "User chose to abort installation at " + file.name, mod);
		// }
	}

	private boolean doInstall(ApiMod mod, List<ModInstallFile> roots,
			Set<String> relevantFiles) throws IOException,
			InstallFailedException {
		List<ModInstallFile> actualFiles = new ArrayList<>();
		Set<ModInstallFile> skippedFiles = new HashSet<>();

		OverwriteStatus lastOverwriteStatus = null;

		for (ModInstallFile root : roots) {
			List<ModInstallFile> containedFiles;
			if (root.isDirectory)
				containedFiles = root.getContainedFiles();
			else {
				containedFiles = new ArrayList<>(1);
				containedFiles.add(root);
			}

			for (ModInstallFile child : containedFiles) {
				File to = getFilePathInGameData(child, roots).toFile();
				actualFiles.add(child);
				if (to.exists() && relevantFiles == null
						&& lastOverwriteStatus != OverwriteStatus.OverwriteAll) {
					lastOverwriteStatus = checkOverwrite(mod, child);
					if (lastOverwriteStatus == OverwriteStatus.Abort)
						throw new InstallFailedException(
								"User chose to abort installation at "
										+ child.getFullName(), mod);

					if (lastOverwriteStatus == OverwriteStatus.KeepOld)
						skippedFiles.add(root);
				}
			}
		}

		if (actualFiles.size() == 0) {
			throw new InstallFailedException(
					"Could not figure out how to install mod!", mod);
		}

		if (actualFiles.size() == skippedFiles.size()) {
			throw new InstallFailedException("User chose to skip all files!",
					mod);
		}

		mod.enableInfo.files = new ArrayList<>();

		for (ModInstallFile file : actualFiles) {
			File from = file.path.toFile();

			Path to = getFilePathInGameData(file, roots);
			File toFile = to.toFile();
			String relFile = gameData.relativize(to).toString();
			if (relevantFiles != null && !relevantFiles.contains(relFile))
				continue;

			mod.enableInfo.files.add(relFile);

			if (!skippedFiles.contains(file)) {
				toFile.getParentFile().mkdirs();
				Files.move(from.toPath(), toFile.toPath(),
						StandardCopyOption.REPLACE_EXISTING);
			}
		}

		return true;
	}

	private Path getFilePathInGameData(ModInstallFile file,
			List<ModInstallFile> roots) {
		List<String> parts = new ArrayList<>();
		ModInstallFile parent = file;
		while (parent != null) {
			parts.add(parent.name);
			if (roots.contains(parent))
				break;
			parent = parent.parent;
		}
		Path to = gameData;
		for (int i = parts.size() - 1; i >= 0; i--) {
			to = to.resolve(parts.get(i));
		}

		return to;
	}

	private void deleteDirectory(Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}

	private String validateFilename(String filename, File iD)
			throws IOException {
		File f = new File(iD, filename);
		String canonicalPath = f.getCanonicalPath();

		String canonicalID = iD.getCanonicalPath();

		if (canonicalPath.startsWith(canonicalID)) {
			f.getParentFile().mkdirs();
			return canonicalPath;
		} else {
			throw new IllegalStateException(
					"File is outside extraction target directory.");
		}
	}

	public final void unzip(ZipInputStream zis, File targetDir)
			throws IOException {
		ZipEntry entry;
		try {
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					zis.closeEntry();
					continue;
				}
				int count;
				byte data[] = new byte[2048];
				String name = validateFilename(entry.getName(), targetDir);

				FileOutputStream fos = new FileOutputStream(name);
				BufferedOutputStream dest = new BufferedOutputStream(fos, 2048);
				while ((count = zis.read(data, 0, 2048)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
				zis.closeEntry();
			}
		} finally {
			while (zis.read() != -1)
				;
			zis.close();
		}
	}

	public String manualKspPath(boolean exitOnFail, String originalPath) {
		String selectedPath = null;
		String kspPathTemp = null;
		JFileChooser fileChooser = new JFileChooser(originalPath);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = fileChooser.showOpenDialog(null);
		if (returnVal != JFileChooser.APPROVE_OPTION && exitOnFail == true) {
			System.exit(1);
		} else if (returnVal != JFileChooser.APPROVE_OPTION) {
			return originalPath;
		}

		selectedPath = fileChooser.getSelectedFile().getAbsolutePath();
		if (testKspDir(selectedPath))
			kspPathTemp = selectedPath;
		else if (testKspDir(selectedPath + "/../"))
			kspPathTemp = selectedPath + "/../";

		if (kspPathTemp == null) {
			int option = JOptionPane
					.showConfirmDialog(
							null,
							"The selected directory \""
									+ selectedPath
									+ "\" does not appear to be a valid Kerbal Space Program installation directory. Do you want to try again?",
							null, JOptionPane.YES_NO_OPTION);
			if (option != JOptionPane.YES_OPTION && exitOnFail == true) {
				System.exit(1);
			} else if (option == JOptionPane.YES_OPTION) {
				this.manualKspPath(exitOnFail, originalPath);
			} else {
				kspPathTemp = originalPath;
			}
		}
		return kspPathTemp;
	}

	private boolean testKspDir(String path) {
		if (!Files.exists(Paths.get(path)))
			return false;

		if (!Files.exists(Paths.get(path, "GameData")))
			return false;

		return true;
	}

	private static String byteArray2Hex(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;

	}

	public static class InstallFailedException extends Exception {
		public final ApiMod mod;

		public InstallFailedException(String message, ApiMod mod) {
			super(message);
			this.mod = mod;
		}
	}

	public interface ModEventListener {
		public void onModEvent(ModEvent event);
	}

	public static abstract class ModEvent {

	}

	public class ModEnableEvent extends ModEvent {
		public final ApiMod mod;

		public ModEnableEvent(ApiMod mod) {
			this.mod = mod;
		}
	}

	public class ModDisableEvent extends ModEvent {
		public final ApiMod mod;

		public ModDisableEvent(ApiMod mod) {
			this.mod = mod;
		}
	}

	public class ModUninstallEvent extends ModEvent {
		public final ApiMod mod;

		public ModUninstallEvent(ApiMod mod) {
			this.mod = mod;
		}
	}

	public void addListener(ModEventListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(ModEventListener listener) {
		this.listeners.remove(listener);
	}

	protected void emit(ModEvent event) {
		for (ModEventListener listener : listeners) {
			try {
				listener.onModEvent(event);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
