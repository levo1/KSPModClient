package ksp.modmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.api.client.json.JsonGenerator;
import com.google.api.client.util.Key;

public class Config {
	public static final Config INSTANCE;
	private static final File datadir = new File(
			System.getProperty("user.home") + "/.ksp-mm/");
	private static final File configFile = new File(datadir, "config.json");

	static {
		if (!datadir.exists())
			datadir.mkdir();

		if (configFile.exists()) {
			try (FileInputStream fis = new FileInputStream(configFile)) {
				INSTANCE = Start.JSON_FACTORY.createJsonParser(fis)
						.parseAndClose(Config.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			INSTANCE = new Config();
		}
	}

	@Key
	private String kspDirectory = null;

	public Config() {
		
	}

	public void save() {
		try (FileOutputStream fos = new FileOutputStream(configFile)) {
			JsonGenerator generator = Start.JSON_FACTORY.createJsonGenerator(
					fos, Charset.forName("UTF-8"));
			generator.enablePrettyPrint();
			generator.serialize(this);
			generator.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public String getKspDirectory() {
		return kspDirectory;
	}

	public void setKspDirectory(String kspDirectory) {
		this.kspDirectory = kspDirectory;
		save();
	}
}
