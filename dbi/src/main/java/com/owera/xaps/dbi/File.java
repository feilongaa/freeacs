package com.owera.xaps.dbi;

import com.owera.common.db.ConnectionProperties;
import com.owera.common.db.ConnectionProvider;
import com.owera.common.db.NoAvailableConnectionException;

import java.sql.*;
import java.util.Date;

public class File {

	private Unittype unittype;
	private Integer id;
	private String name;
	private String oldName;
	private FileType type;
	private String description;
	private String version;
	private Date timestamp;
	private int length;
	private String targetName;
	private User owner;
	private byte[] content;

	private boolean validateInput = true;

	private ConnectionProperties connectionProperties;

	// code-order: id, unittype, name, type, desc, version, timestamp, targetname, (content)
	public File() {
	}

	public File(Unittype unittype, String name, FileType type, String description, String version, Date timestamp, String targetName, User owner) {
		setUnittype(unittype);
		setName(name);
		setType(type);
		setDescription(description);
		setVersion(version);
		setTimestamp(timestamp);
		setTargetName(targetName);
		setOwner(owner);
	}

	/* GET methods */
	// code-order: id, unittype, name, type, desc, version, timestamp, targetname, (content)

	public Integer getId() {
		return id;
	}

	public Unittype getUnittype() {
		return unittype;
	}

	public String getName() {
		return name;
	}

	public String getOldName() {
		return oldName;
	}

	public FileType getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public String getVersion() {
		return version;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public String getTargetName() {
		return targetName;
	}

	protected byte[] getContentProtected() {
		return content;
	}

	public User getOwner() {
		return owner;
	}

	public byte[] getContent() throws SQLException, NoAvailableConnectionException {
		if (content == null) {
			Connection c = null;
			Statement s = null;
			ResultSet rs = null;
			SQLException sqlex = null;
			try {
				c = ConnectionProvider.getConnection(connectionProperties);
				s = c.createStatement();
				s.setQueryTimeout(60);
				rs = s.executeQuery("SELECT content FROM filestore WHERE id = '" + id + "'");
				if (rs.next()) {
					Blob blob = rs.getBlob("content");
					content = blob.getBytes(1, (int) blob.length());
				}
			} catch (SQLException sqle) {
				sqlex = sqle;
				throw sqle;
			} finally {

				if (rs != null)
					rs.close();
				if (s != null)
					s.close();
				if (c != null)
					ConnectionProvider.returnConnection(c, sqlex);
			}
			if (content == null)
				content = new byte[0];
		}
		return content;
	}

	public int getLength() {
		return length;
	}

	/* SET methods */
	// code-order: id, unittype, name, type, desc, version, timestamp, targetname, (content)

	protected void setId(Integer id) {
		this.id = id;
	}

	public void setUnittype(Unittype unittype) {
		if (unittype == null)
			throw new IllegalArgumentException("Unittype cannot be null");
		this.unittype = unittype;
	}

	public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException("File name cannot be null");
		if (!name.equals(this.name))
			this.oldName = this.name;
		this.name = name;
	}

	protected void setOldName(String oldName) {
		this.oldName = oldName;
	}

	public void setType(FileType type) {
		if (validateInput && type == null)
			throw new IllegalArgumentException("File type cannot be null");
		this.type = type;
	}

	public void setDescription(String s) {
		this.description = s;
	}

	public void setTimestamp(Date created) {
		if (created == null)
			this.timestamp = new Date();
		else
			this.timestamp = created;
	}

	public void setVersion(String version) {
		if (validateInput && version == null)
			throw new IllegalArgumentException("File version cannot be null");
		this.version = version;
	}

	public void setTargetName(String targetName) {
		if (validateInput && type == FileType.TR069_SCRIPT && targetName == null)
			throw new IllegalArgumentException("File target name cannot be null if File type is " + FileType.TR069_SCRIPT);
		if (type == FileType.TR069_SCRIPT && targetName == null)
			targetName = name;
		this.targetName = targetName;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public void setBytes(byte[] bytes) {
		//		if (type == FileType.SHELL_SCRIPT) {
		//			User user = unittype.getXaps().getSyslog().getIdentity().getUser();
		//			String tmp = new String(bytes);
		//			if (!tmp.startsWith("#!fusionuser:"))
		//				tmp = "#!fusionuser:" + user.getUsername() + ":!#\n" + tmp;
		//			bytes = tmp.getBytes();
		//		}
		this.content = bytes;
		this.length = bytes.length;
	}

	protected void setLength(int length) {
		if (length < 0)
			length = 0;
		this.length = length;
	}

	/* MISC methods */

	// Necessary to retrieve content - we do not cache content as default action
	protected void setConnectionProperties(ConnectionProperties connectionProperties) {
		this.connectionProperties = connectionProperties;
	}

	// Used by Web
	public String getNameAndVersion() {
		return name + " (ver: " + version + ")";
	}

	//	// Used by ScriptExecutor in Core-server
	//	public String getFusionUser() {
	//		if (type == FileType.SHELL_SCRIPT) {
	//			byte[] tmp = getContentProtected();
	//			if (tmp == null) {
	//				try {
	//					tmp = getContent();
	//				} catch (Throwable t) {
	//					// Ignore
	//				}
	//			}
	//			if (tmp != null) {
	//				String tmpStr = new String(tmp);
	//				if (tmpStr.startsWith("#!fusionuser:")) {
	//					int endPos = tmpStr.indexOf(":!#");
	//					return tmpStr.substring(13, endPos);
	//				}
	//			}
	//		}
	//		return null;
	//	}

	public void validate() {
		setUnittype(unittype);
		setName(name);
		setType(type);
		setDescription(description);
		setVersion(version);
		setTimestamp(timestamp);
		setTargetName(targetName);
	}

	protected void validateInput(boolean validateInput) {
		this.validateInput = validateInput;
	}

	// To avoid storing file content in XAPS-object - this must always be used with care!
	public void resetContentToNull() {
		content = null;
	}

	@Override
	public String toString() {
		return "[" + name + "] [" + type + "] [" + version + "]";
	}
}
