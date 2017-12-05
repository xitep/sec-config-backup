package com.github.xitep.sec_config_backup;

import com.github.xitep.sec_config_backup.dao.Fetcher;
import com.github.xitep.sec_config_backup.dao.JdbcFetcher;
import com.github.xitep.sec_config_backup.model.Entry;
import com.github.xitep.sec_config_backup.model.Snapshot;
import com.github.xitep.sec_config_backup.storage.BackupDirectory;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Main {

	public static void main(String [] args) {
		Conf cfg = Conf.load(args);
		try {
			guarded(cfg);
		} catch (Failure e) {
			log.error("{}", e.getMessage(), e);
		}
	}

	private static void guarded(Conf conf) throws Failure {
		String lockFile = conf.getLockFile();
		if (lockFile == null) {
			backup(conf);
		} else {
			try {
				RandomAccessFile ra = new RandomAccessFile(lockFile, "rw");
				FileLock lock = ra.getChannel().tryLock();
				if (lock == null) {
					log.warn("Lock ({}) held by another process! Quitting without performing any backup.", lockFile);
					return;
				}
				try {
					backup(conf);
				} finally {
					lock.release();
				}
			} catch (IOException e) {
				throw new Failure("Failure on lock file (" + lockFile + ")", e);
			}
		}
	}

	private static void backup(Conf cfg) throws Failure {
		// ~ the commit message contains a stamp; we'd like to track the
		// time when we started the backup; so, we're generating the message
		// already at this point in time
		String commitMessage = newCommitMessage();
		BackupDirectory backupDir;
		try {
			backupDir = BackupDirectory.of(new File(cfg.getBackupDir()));
		} catch (IOException e) {
			throw new Failure("Failed to determine backup directory", e);
		}

		// ~ give the backup directory a chance to signal errors
		// already at this stage of the process
		try {
			backupDir.prepareBackup();
		} catch (Exception e) {
			throw new Failure("Failed to prepare backup directory", e);
		}

		// ~ prepare a pool of workers to execute the dumping process in parallel
		ExecutorService executor = cfg.getMaxThreads() > 0
			? Executors.newFixedThreadPool(cfg.getMaxThreads())
			: Executors.newCachedThreadPool();
		ExecutorCompletionService<DumpResult> cs =
				new ExecutorCompletionService<>(executor);
		// ~ now, let the work begin
		cfg.getEnvironments().forEach(e ->
				cs.submit(() -> {
					Fetcher fetcher = new JdbcFetcher(e.getJdbcUri(), cfg.createJdbcProps(e));
					File outputFile = new File(backupDir.getDirectory(), e.getName() + ".xml");
					return dumpSnapshot(e.getName(), fetcher, outputFile);
				}));
		// ~ wait for results and report progress
		try {
			for (int i = 0, n = cfg.getEnvironments().size(); i < n; i++) {
				DumpResult r;
				try {
					r = cs.take().get();
				} catch (InterruptedException e) {
					// ~ requested to quit working as soon as possible
					return;
				} catch (ExecutionException e) {
					throw new Failure("Unexpected program failure (likely a bug!)", e);
				}

				if (r.getFailure() == null) {
					log.info("Successfully dumped data for {} (exec-time: {} millis) [{}/{}])",
							r.getEnvName(), r.getDurationMillis(), i + 1, n);
				} else {
					log.warn(
							"Failed to dump data for {} (exec-time: {} millis) (error: {}) [{}/{}]",
							r.getEnvName(), r.getDurationMillis(), r.getFailure(), i + 1, n);
					// ~ log the same on debug level with the exception stacktrace rendered;
					// we are expecting network hiccups regularly
					log.debug(
							"Failed to dump data for {} (exec-time: {} millis) (error: {}) [{}/{}]",
							r.getEnvName(), r.getDurationMillis(), r.getFailure(), i + 1, n, r.getFailure());
				}
			}
		} finally {
			// ~ either all tasks are already finished or we are about to
			// quit execution; tear down the executor as quickly as possible
			executor.shutdownNow();
		}
		try {
			// ~ commit potential changes to the backup directory
			backupDir.commitBackup(commitMessage);
		} catch (Exception e) {
			throw new Failure("Failed to commit backup directory", e);
		}
	}

	static class Failure extends Exception {
		Failure(String message, Exception cause) {
			super(message, cause);
		}
	}

	private static String newCommitMessage() {
		return "[config-backup] Backup updates from "
				+ new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date());
	}

	// ~ ---------------------------------------------------------------------

	private final static JAXBContext jaxbContext;
	static {
		try {
			jaxbContext = JAXBContext.newInstance(Snapshot.class);
		} catch (JAXBException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final static Comparator<Entry> CMP_BY_KEY_AND_USERNAME =
			(e1, e2) -> {
				int cmp = e1.getKey().compareTo(e2.getKey());
				if (cmp == 0) {
					cmp = e1.getUserName().compareTo(e2.getUserName());
				}
				return cmp;
			};

	@Value
	static class DumpResult {
		String envName;
		long durationMillis;
		// ~ null upon success
		Exception failure;
	}

	private static DumpResult
	dumpSnapshot(String envName, Fetcher fetcher, File outFile) {
		long start = System.nanoTime();
		Exception failure = null;
		try {
			Snapshot snapshot = fetcher.fetch();
			if (snapshot.getEntries() != null) {
				snapshot.getEntries().sort(CMP_BY_KEY_AND_USERNAME);
			}

			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(snapshot, outFile);
		} catch (Exception e) {
			failure = e;
		}
		long end = System.nanoTime();
		return new DumpResult(envName, TimeUnit.NANOSECONDS.toMillis(end - start), failure);
	}
}
