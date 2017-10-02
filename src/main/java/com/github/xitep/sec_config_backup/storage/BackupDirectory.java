package com.github.xitep.sec_config_backup.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.Objects;

public abstract class BackupDirectory {

	public static BackupDirectory of(File dir) throws IOException {
		if (!dir.isDirectory()) {
			throw new NotDirectoryException(dir.toString());
		}
		BackupDirectory bd = GitBackupDirectory.newInstance(dir);
		return bd == null
				? new PlainOldDirectoryBackup(dir)
				: bd;
	}

	// ~ ---------------------------------------------------------------------

	private final File dir;

	BackupDirectory(File dir) {
		this.dir = Objects.requireNonNull(dir);
	}

	public final File getDirectory() {
		return dir;
	}

	public void prepareBackup() throws Exception {
		// ~ no-op for non-versioned implementations
	}

	public void commitBackup(String message) throws Exception {
		// ~ no-op for non-versioned implementations
	}
}
