package ksp.modmanager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import ksp.modmanager.api.ApiMod;
import ksp.modmanager.api.BulkModResult;
import ksp.modmanager.api.BulkModSearch;
import ksp.modmanager.api.ModSearch;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.json.JsonGenerator;
import com.google.api.client.util.escape.CharEscapers;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
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
	private Path enabledMods, disabledMods;
	private Cache<Long, ApiMod> serverSideMods;
	private Map<Long, Boolean> updateAvailable = new HashMap<>();
	private Pattern modRegex = Pattern.compile("^mod-(\\d)$",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

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

		enabledMods = Paths.get(kspPath, "GameData");
		disabledMods = Paths.get(kspPath, "GameData-disabled");

		if (!Files.exists(disabledMods))
			try {
				Files.createDirectory(disabledMods);
			} catch (IOException e) {
				e.printStackTrace();
			}

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

		checkForUpdates();
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

	public boolean isModInstalled(ApiMod mod) {
		// System.out.println("Hitting FS"); //TODO cache or something?
		if (Files.exists(enabledMods.resolve(mod.getInfoName())))
			return true;

		if (Files.exists(disabledMods.resolve(mod.getInfoName())))
			return true;

		return false;
	}

	public boolean isModEnabled(ApiMod mod) {
		if (Files.exists(enabledMods.resolve(mod.getInfoName())))
			return true;

		return false;
	}

	public boolean isUpdateAvailable(ApiMod mod) {
		Boolean update = updateAvailable.get(mod.getId());
		if (update == null)
			return false;
		return update;
	}

	public List<ApiMod> getInstalledMods() {
		List<ApiMod> mods = new ArrayList<>();

		try (DirectoryStream<Path> directoryStream = Files
				.newDirectoryStream(enabledMods)) {
			for (Path path : directoryStream) {
				ApiMod mod = getInstalledMod(path);
				if (mod != null)
					mods.add(mod);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		try (DirectoryStream<Path> directoryStream = Files
				.newDirectoryStream(disabledMods)) {
			for (Path path : directoryStream) {
				ApiMod mod = getInstalledMod(path);
				if (mod != null)
					mods.add(mod);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return mods;
	}

	public void checkForUpdates() {
		try {
			List<ApiMod> installedMods = getInstalledMods();

			HttpRequest request = Start.requestFactory
					.buildGetRequest(new BulkModSearch(Collections2.transform(
							installedMods, new Function<ApiMod, Long>() {
								@Override
								public Long apply(ApiMod mod) {
									return mod.getId();
								}
							})));

			serverSideMods.putAll(request.execute()
					.parseAs(BulkModResult.class));

			for (ApiMod local : installedMods) {
				ApiMod remote = serverSideMods.get(local.getId(), null);
				if (remote == null) {
					System.err
							.println(String
									.format("Local mod %s (%s) installed, but cannot find on remote.",
											local.getId(), local.getTitle()));
					updateAvailable.put(local.getId(), false);
					continue;
				}

				updateAvailable.put(local.getId(),
						!remote.getSha1sum().equals(local.getSha1sum()));
			}
		} catch (IOException | ExecutionException ex) {
			ex.printStackTrace();
		}

	}

	public void installMod(ApiMod mod) throws IOException {
		Path disabledModDir = disabledMods.resolve(mod.getDirectoryName());
		Path enabledModDir = enabledMods.resolve(mod.getDirectoryName());
		for (Path dir : new Path[] { enabledModDir, disabledModDir })
			if (Files.exists(dir))
				deleteDirectory(dir);

		System.out.println("Starting download for " + mod.getTitle());

		final Path tempFile = Files.createTempDirectory("ksp-mod-");
		try {
			String url = mod.getDownloadUrl();
			HttpURLConnection connection;
			boolean redirect = false;

			do {
				URL website = new URL(url);
				System.out.println("Accessing " + url);
				connection = (HttpURLConnection) website.openConnection();
				connection.setInstanceFollowRedirects(false);
				String location = connection.getHeaderField("Location");
				System.out.println("Header; " + location + " / "
						+ connection.getResponseCode());
				if (location != null) {
					connection.disconnect();
					url = location.replaceAll("(?i)/(\\d+)\\\\(\\d+)/",
							"/$1/$2/");

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

//			MessageDigest digest;
//			try {
//				digest = MessageDigest.getInstance("SHA1");
//			} catch (NoSuchAlgorithmException e) {
//				throw new RuntimeException(e);
//			}
			// DigestInputStream dis = new DigestInputStream(
			// connection.getInputStream(), digest);

			ZipInputStream is = new ZipInputStream(connection.getInputStream());

			// System.out.println(tempFile.toString());
			unzip(is, tempFile.toFile());

			// System.out.println("SHA-1: " +
			// byteArray2Hex(dis.getMessageDigest().digest())
			// + " vs " + mod.getSha1sum());

			final ModInstallFile root = new ModInstallFile("", null, tempFile, true);
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
					String[] parts = tempFile.relativize(file).toString()
							.split(Pattern.quote(File.separator));
					ModInstallFile parent = root;
					for (int i = 0; i < parts.length - 1; i++) {
						parent = parent.children.get(parts[i]);
					}

					parent.children.put(parts[parts.length - 1],
							new ModInstallFile(file.getFileName().toString(),
									parent, file, directory));
				}
			});

			System.out.println(root);
			if (installModFromTemp(mod, root)) {
				try (FileOutputStream fos = new FileOutputStream(enabledMods
						.resolve(mod.getInfoName()).toFile())) {
					JsonGenerator generator = Start.JSON_FACTORY
							.createJsonGenerator(fos, Charset.forName("UTF-8"));
					generator.enablePrettyPrint();
					generator.serialize(mod);
					generator.close();
				}
			} else {
				JOptionPane.showMessageDialog(null,
						"Install for \"" + mod.getTitle() + "\" failed!");
			}

			System.out.println("Download finished");

		} finally {
			// deleteDirectory(tempFile.toAbsolutePath());
		}

	}

	private boolean installModFromTemp(ApiMod mod, ModInstallFile root)
			throws IOException {
		ModInstallFile gamedata = root.children.get("GameData");
		if (gamedata == null || !gamedata.isDirectory)
			gamedata = root.children.get("gamedata");

		if (gamedata != null && gamedata.isDirectory) {
			for (ModInstallFile file : gamedata.children.values()) {
				installToGameData(mod, file);
			}
			return true;
		}

		for (ModInstallFile dir : root.children.values()) {
			if (dir.isDirectory && dir.name.equalsIgnoreCase("gamedata")) {
				for (ModInstallFile file : dir.children.values()) {
					installToGameData(mod, file);
				}
				return true;
			}
		}

		installToGameData(mod, root);

		return true;
	}

	private void installToGameData(ApiMod mod, ModInstallFile file)
			throws IOException {
		if (file.isDirectory) {
			FileUtils.copyDirectory(file.path.toFile(),
					enabledMods.resolve(mod.getDirectoryName() + "-" + file.name)
							.toFile());
		} else {
			FileUtils.copyFile(file.path.toFile(),
					enabledMods.resolve(mod.getDirectoryName() + "-" + file.name)
							.toFile());
		}
	}

	private ApiMod getInstalledMod(Path path) {
		String name = path.getFileName().toString();
		Matcher matcher = modRegex.matcher(name);
		if (!matcher.find())
			return null;

		Path modInfo = path.resolve("modinfo.json");
		if (!Files.exists(modInfo)) {
			System.err.println("Found " + name + " but no modinfo.json");
			return null;
		}

		try (FileInputStream fis = new FileInputStream(modInfo.toFile())) {
			return Start.JSON_FACTORY.createJsonParser(fis).parseAndClose(
					ApiMod.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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

	private static String byteArray2Hex(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;

	}

}
