package ksp.modmanager;

import java.util.HashMap;
import java.util.List;

import ksp.modmanager.EnabledMods.EnabledMod;

import com.google.api.client.util.Key;

public class EnabledMods extends HashMap<String, EnabledMod> {
	public static class EnabledMod {
		@Key
		public long enableId;
		@Key
		public List<String> files;
	}
	
	public EnabledMod get(Long key) {
		return get(String.valueOf(key));
	}
	
	public void put(Long key, EnabledMod value) {
		put(String.valueOf(key), value);
	}
	
	public boolean containsKey(Long key) {
		return containsKey(String.valueOf(key));
	}
	
	public EnabledMod remove(Long key) {
		return remove(String.valueOf(key));
	}
}
