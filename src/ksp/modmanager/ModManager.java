package ksp.modmanager;

import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import ksp.modmanager.api.ApiMod;

public class ModManager {
	private static String[] kspPaths = new String[] {
			"C:/Program Files (x86)/Steam/",
			"C:/Program Files/Steam/",
			System.getProperty("user.home")
					+ "/Library/Application Support/Steam/",
			System.getProperty("user.home") + "/Steam/",
			System.getProperty("user.home") + "/.local/share/Steam/" };
	private String kspPath = Config.INSTANCE.getKspDirectory();

	public ModManager() {
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

			String selectedPath = null;
			while (kspPath == null) {
				JFileChooser fileChooser = new JFileChooser(selectedPath);
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = fileChooser.showOpenDialog(null);
				if (returnVal != JFileChooser.APPROVE_OPTION) {
					System.exit(1);
				}

				selectedPath = fileChooser.getSelectedFile().getAbsolutePath();
				if (testKspDir(selectedPath))
					kspPath = selectedPath;
				else if (testKspDir(selectedPath + "/../"))
					kspPath = selectedPath + "/../";

				if (kspPath == null) {
					int option = JOptionPane
							.showConfirmDialog(
									null,
									"The selected directory \""
											+ selectedPath
											+ "\" does not appear to be a valid Kerbal Space Program installation directory. Do you want to try again?",
									null, JOptionPane.YES_NO_OPTION);
					if (option != JOptionPane.YES_OPTION)
						System.exit(1);
				}
			}

			Config.INSTANCE.setKspDirectory(kspPath);
		}
	}

	private boolean testKspDir(String path) {
		if (!Files.exists(Paths.get(path)))
			return false;

		if (!Files.exists(Paths.get(path, "GameData"))
				&& !Files.exists(Paths.get(path, "gamedata")))
			return false;

		return true;
	}

	public boolean isModInstalled(ApiMod mod) {
		return true;
	}

	public boolean isModEnabled(ApiMod mod) {
		return true;
	}

	public boolean isUpdateAvailable(ApiMod mod) {
		return true;
	}
}
