package jmplib.sourcecode;


/**
 * This class acts like a wrapper of needed information of each class. All
 * classes cached have to store the same information on the cache and this class
 * supports this activity. 
 * 
 * @author Ignacio Lagartos
 * 
 */
public class ClassContent {
	private Class<?> clazz;
	private String content;
	private String path;
	private boolean updated;
	private int version;

	public Class<?> getClazz() {
		return clazz;
	}

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isUpdated() {
		return updated;
	}

	public void setUpdated(boolean updated) {
		this.updated = updated;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clazz == null) ? 0 : clazz.getName().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClassContent other = (ClassContent) obj;
		if (clazz == null) {
			if (other.clazz != null)
				return false;
		} else if (!clazz.getName().equals(other.clazz.getName()))
			return false;
		return true;
	}
}