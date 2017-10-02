package com.github.xitep.sec_config_backup.dao;

import com.github.xitep.sec_config_backup.model.Entry;
import com.github.xitep.sec_config_backup.model.Snapshot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JdbcFetcher implements Fetcher {
	private final String jdbcUri;

	public JdbcFetcher(String jdbcUri) {
		this.jdbcUri = Objects.requireNonNull(jdbcUri);
	}

	@Override
	public Snapshot fetch() throws Exception {
		try (Connection conn = DriverManager.getConnection(jdbcUri)) {
			PreparedStatement stmt = conn.prepareStatement(
							/* 1 */ "SELECT CONF_KEY," +
							/* 2 */ "       CONF_VALUE," +
							/* 3 */ "       CONF_TYPE," +
							/* 4 */ "       CONF_DEFAULT_VALUE," +
							/* 5 */ "       CONF_REMARKS," +
							/* 6 */ "       CONF_USER_NAME," +
							/* 7 */ "       FLAGS," +
							/* 8 */ "       CONF_LAST_MODIFICATION_TIME," +
							/* 9 */ "       VERSION" +
									" FROM CONFIGURATION");
			ResultSet rs = stmt.executeQuery();
			List<Entry> entries = new ArrayList<>(1000);
			while (rs.next()) {
				entries.add(new Entry(
						rs.getString(1),
						rs.getString(2),
						rs.getString(3),
						rs.getString(4),
						rs.getString(5),
						rs.getString(6),
						rs.getString(7),
						rs.getDate(8),
						rs.getShort(9)));
			}
			return new Snapshot(entries);
		}
	}
}
