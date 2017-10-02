package com.github.xitep.sec_config_backup.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "snapshot")
@XmlAccessorType(XmlAccessType.FIELD)
public class Snapshot {
	@XmlElement(name = "entry")
	List<Entry> entries;
}
