package com.github.xitep.sec_config_backup;

import com.github.xitep.sec_config_backup.model.Entry;
import com.github.xitep.sec_config_backup.model.Snapshot;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.text.SimpleDateFormat;

public class RenderAsInserts {

	public static void main(String[] args) throws Exception {
		String backupFile = args[0];

		Unmarshaller unmarshaller = JAXBContext.newInstance(Snapshot.class).createUnmarshaller();
		Snapshot snapshot = (Snapshot) unmarshaller.unmarshal(new File(backupFile));

		SimpleDateFormat tsFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		System.out.println("--");
		System.out.println("-- alter trigger CONFIGURATION_LMT disable;");
		System.out.println("-- delete from configuration;\n");

		StringBuilder buf = new StringBuilder();
		for (Entry entry : snapshot.getEntries()) {
			buf.setLength(0);
			buf.append(
					"INSERT INTO CONFIGURATION" +
					"(" +
					"CONF_KEY," +
					" CONF_VALUE," +
					" CONF_TYPE," +
					" CONF_DEFAULT_VALUE," +
					" CONF_REMARKS," +
					" CONF_USER_NAME," +
					" FLAGS," +
					" CONF_LAST_MODIFICATION_TIME," +
					" VERSION" +
					")" +
					" VALUES" +
					"(");
			// ~ key
			appendStringLiteral(buf, entry.getKey()).append(", ");
			// ~ value
			appendStringLiteral(buf, entry.getValue()).append(", ");
			// ~ type
			appendStringLiteral(buf, entry.getType()).append(", ");
			// ~ default-value
			appendStringLiteral(buf, entry.getDefaultValue()).append(", ");
			// ~ remarks
			appendStringLiteral(buf, entry.getRemarks()).append(", ");
			// ~ username
			appendStringLiteral(buf, entry.getUserName()).append(", ");
			// ~ flags
			appendStringLiteral(buf, entry.getFlags()).append(", ");

			// last-modification-time
			buf.append(" TIMESTAMP '");
			buf.append(tsFmt.format(entry.getLastModification()));
			buf.append("', ");
			// ~ version
			buf.append(entry.getVersion()).append(");");

			System.out.println(buf);
		}

		System.out.println("\n-- alter trigger CONFIGURATION_LMT enable;");
		System.out.println("--");
	}

	private static StringBuilder appendStringLiteral(StringBuilder buf, String value) {
		if (value == null) {
			buf.append("NULL");
		} else {
			buf.append('\'');
			for (int i = 0, n = value.length(); i < n; i++) {
				char c = value.charAt(i);
				if (c == '\'') {
					buf.append("''");
				} else {
					buf.append(c);
				}
			}
			buf.append('\'');
		}
		return buf;
	}
}
