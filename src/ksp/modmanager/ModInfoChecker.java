package ksp.modmanager;

import ksp.modmanager.api.ApiMod;

public interface ModInfoChecker {
	public boolean isModInstalled(ApiMod mod);
	public boolean isModEnabled(ApiMod mod);
	public boolean isUpdateAvailable(ApiMod mod);
}