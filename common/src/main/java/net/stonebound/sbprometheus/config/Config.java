package net.stonebound.sbprometheus.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import dev.architectury.platform.Platform;
import net.minecraft.server.MinecraftServer;
import net.stonebound.sbprometheus.SbPrometheus;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

//ripped from phit's cwl
public final class Config {
	public static int jedisPort = 9200;
	public static final CommentedConfigSpec commonSpec;
	public static CommentedFileConfig commonConfig;

	private static final Path commonPath = Platform.getConfigFolder().resolve("sbprometheus-common.toml");

	private Config() {
	}

	static {
		System.setProperty("nightconfig.preserveInsertionOrder", "true");

		commonSpec = new CommentedConfigSpec();

		commonSpec.comment("jedisPort",
				"jedisPort");
		commonSpec.defineInRange("jedisPort", jedisPort, 1, 32000);
	}

	private static final FileNotFoundAction MAKE_DIRECTORIES_AND_FILE = (file, configFormat) -> {
		Files.createDirectories(file.getParent());
		Files.createFile(file);
		configFormat.initEmptyFile(file);
		return false;
	};

	private static CommentedFileConfig buildFileConfig(Path path) {
		return CommentedFileConfig.builder(path)
				.onFileNotFound(MAKE_DIRECTORIES_AND_FILE)
				.preserveInsertionOrder()
				.build();
	}

	private static void saveConfig(UnmodifiableConfig config, CommentedConfigSpec spec, Path path) {
		try (CommentedFileConfig fileConfig = buildFileConfig(path)) {
			fileConfig.putAll(config);
			spec.correct(fileConfig);
			fileConfig.save();
		}
	}

	public static void save() {
		if (commonConfig != null) {
			saveConfig(commonConfig, commonSpec, commonPath);
		}
	}

	public static void Start() {
		try (CommentedFileConfig config = buildFileConfig(commonPath)) {
			config.load();
			commonSpec.correct(config, Config::correctionListener);
			config.save();
			commonConfig = config;
			sync();
		}
	}

	public static void serverStopping(MinecraftServer server) {
		commonConfig = null;
	}

	private static void correctionListener(ConfigSpec.CorrectionAction action, List<String> path, Object incorrectValue,
										   Object correctedValue) {
		String key = String.join(".", path);
		switch (action) {
			case ADD:
				SbPrometheus.LOGGER.warn("Config key {} missing -> added default value.", key);
				break;
			case REMOVE:
				SbPrometheus.LOGGER.warn("Config key {} not defined -> removed from config.", key);
				break;
			case REPLACE:
				SbPrometheus.LOGGER.warn("Config key {} not valid -> replaced with default value.", key);
		}
	}

	public static void sync() {
		if (commonConfig != null) {
			Config.jedisPort = commonConfig.<Integer>get("jedisPort");
		}
	}
}