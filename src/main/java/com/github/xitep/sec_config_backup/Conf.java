package com.github.xitep.sec_config_backup;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// ~ mapped to the application's configuration file; do not rename
// stuff here arbitrarily!
@Data
@Slf4j
public class Conf {

	@Data
	public static class Env {
		private String name;
		private String jdbcUri;
	}

	private String backupDir;
	@Optional String lockFile;
	@Optional private int maxThreads;
	private List<Env> environments;

	static Conf load(String [] args) {
		if (args.length != 1) {
			log.error("Usage: {} <config-file>", Main.class);
			System.exit(1);
		}

		Config cfg = ConfigFactory.defaultOverrides()
				.withFallback(
						ConfigFactory.parseFile(
								new File(args[0]),
								ConfigParseOptions.defaults().setAllowMissing(false)))
				.withFallback(ConfigFactory.defaultReference());
		Conf conf = ConfigBeanFactory.create(cfg.getConfig("config-backup"), Conf.class);
		// ~ validate the environments
		if (conf.environments == null || conf.environments.isEmpty()) {
			log.warn("No environments defined.");
		} else {
			boolean errors = false;
			// ~ environment names must be non-empty
			for (Env env : conf.environments) {
				if (env.name == null || env.name.isEmpty()) {
					log.error("Environment without name!");
					errors = true;
				}
			}
			// ~ environments must be unique by name
			Map<String, List<Env>> grouped = conf.environments.stream()
					.collect(Collectors.groupingBy(Env::getName));
			for (Map.Entry<String, List<Env>> e : grouped.entrySet()) {
				if (e.getValue().size() > 1) {
					log.error("Environment name duplicated: {}", e.getKey());
					errors = true;
				}
			}
			// ~ environments are typically distinct databases from each other;
			// it's rather a user configuration error when two environments point
			// to the same database
			grouped = conf.environments.stream()
					.collect(Collectors.groupingBy(Env::getJdbcUri));
			for (Map.Entry<String, List<Env>> e : grouped.entrySet()) {
				if (e.getValue().size() > 1) {
					log.warn("Environment jdbc-uri duplicated: " + e.getKey());
				}
			}
			if (errors) {
				System.exit(1);
			}
		}
		return conf;
	}
}
