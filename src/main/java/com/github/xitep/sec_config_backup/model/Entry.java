package com.github.xitep.sec_config_backup.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
		"key",
		"value",
		"type",
		"defaultValue",
		"remarks",
		"userName",
		"flags",
		"lastModification",
		"version" })
@XmlRootElement(name = "entry")
public class Entry {
	private String key;
	private String value;
	private String type;
	private String defaultValue;
	private String remarks;
	private String userName;
	private String flags;
	private Date lastModification;
	private long version;
}
