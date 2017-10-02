package com.github.xitep.sec_config_backup.storage;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;

class GitBackupDirectory extends BackupDirectory {

	static GitBackupDirectory newInstance(File dir) throws IOException {
		try {
			Repository repo = new FileRepositoryBuilder()
					.setMustExist(true)
					.setWorkTree(dir)
					.build();
			Git gitCmd = new Git(repo);
			return new GitBackupDirectory(dir, gitCmd);
		} catch (RepositoryNotFoundException e) {
			return null;
		}
	}

	private final Git cmd;

	private GitBackupDirectory(File dir, Git cmd) {
		super(dir);
		this.cmd = Objects.requireNonNull(cmd);
	}

	@Override
	public void prepareBackup() throws Exception {
		Status status = cmd.status().call();
		if (!status.isClean()) {
			throw new IllegalStateException("Backup directory is not clean: " + getDirectory());
		}
	}

	@Override
	public void commitBackup(String commitMessage) throws Exception {
		Status status = cmd.status().call();
		if (status.isClean()) {
			// ~ nothing to do
			return;
		}

		// ~ stage all changes
		HashSet<String> dirtyFiles = new HashSet<>();
		dirtyFiles.addAll(status.getUntracked()); // ~ newly created files
		dirtyFiles.addAll(status.getModified()); // ~ existing and modified files
		AddCommand addCmd = cmd.add().setUpdate(false);
		for (String dirtyFile : dirtyFiles) {
			addCmd.addFilepattern(dirtyFile);
		}
		addCmd.call();
		// ~ commit them
		cmd.commit().setMessage(commitMessage).call();
		// ~ if there's a remote ... push the commit (yes, we boldly assume
		// we're the only one pushing here and are always up to date with the
		// remote)
		if (!cmd.remoteList().call().isEmpty()) {
			cmd.push().call();
		}
		// ~ verify the repo is clean again
		status = cmd.status().call();
		if (!status.isClean()) {
			throw new IllegalStateException("Backup directory stayed dirty (after commit): " + getDirectory());
		}
	}
}
