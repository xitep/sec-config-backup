package com.github.xitep.sec_config_backup.dao;

import com.github.xitep.sec_config_backup.model.Snapshot;

public interface Fetcher {
	Snapshot fetch() throws Exception;
}
