package ksp.modmanager;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModInstallFile {
	public final String name;
	public final boolean isDirectory;
	public final ModInstallFile parent;
	public final Map<String, ModInstallFile> children;
	public final Path path;

	public ModInstallFile(String name, ModInstallFile parent, Path path,
			boolean isDirectory) {
		this.name = name;
		this.isDirectory = isDirectory;
		this.parent = parent;
		this.path = path;

		if (isDirectory)
			children = new HashMap<>();
		else
			children = null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append(name);
		if (isDirectory) {
			builder.append("\n");
			for (ModInstallFile child : children.values()) {
				for (String line : child.toString().split("\n")) {
					builder.append("  ").append(line).append("\n");
				}
			}
		}

		return builder.toString();
	}

	public String getFullName() {
		StringBuilder builder = new StringBuilder();
		ModInstallFile parent = this;
		while (parent.parent != null) {
			builder.insert(0, File.separator);
			builder.insert(1, parent.name);
			parent = parent.parent;
		}

		return builder.toString().substring(1);
	}

	public List<ModInstallFile> getContainedFiles() {
		List<ModInstallFile> files = new ArrayList<>();
		if (!isDirectory) {
			files.add(this);
		} else {
			for (ModInstallFile file : this.children.values()) {
				files.addAll(file.getContainedFiles());
			}
		}
		return files;
	}
}
