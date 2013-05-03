package com.bytestorm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Settings {
	public static final File SETTINGS_FILE = new File(System.getProperty( "user.home" ), ".rcryptfs.properties");
	private static Settings settings = new Settings();
	private Properties properties = new Properties();

	private Settings() {
		// Read properties file
		try {
			System.out.println("Loading "+SETTINGS_FILE.getPath());
			properties.load(new FileInputStream(SETTINGS_FILE));
		} catch (IOException e) {
		}
	}

	public static String getString(String key) {
		return settings._getString(key);
	}

	private String _getString(String key) {
		return properties.getProperty(key);
	}

	public static String getString(String key, String defaultValue) {
		return settings._getString(key, defaultValue);
	}

	private String _getString(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	public static void setString(String key, String value) {
		settings._setString(key, value);
	}

	private void _setString(String key, String value) {
		properties.setProperty(key, value);
		// Write properties file
		try
		{
			properties.store(new FileOutputStream(SETTINGS_FILE), null);
		} catch (IOException e) {
			System.err.println("Failed to save settings file: "+e.getLocalizedMessage());
		}
	}

}
