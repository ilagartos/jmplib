package jmplib.classversions.util;

/**
 * Class that identifies a member
 * 
 * @author Ignacio Lagartos
 *
 */
public class MemberKey {

	private String className;
	private String memberName;
	private String descriptor;

	public MemberKey(String className, String memberName, String descriptor) {
		super();
		this.className = className;
		this.memberName = memberName;
		this.descriptor = descriptor;
	}

	public MemberKey(String className, String memberName) {
		super();
		this.className = className;
		this.memberName = memberName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
		result = prime * result
				+ ((descriptor == null) ? 0 : descriptor.hashCode());
		result = prime * result
				+ ((memberName == null) ? 0 : memberName.hashCode());
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
		MemberKey other = (MemberKey) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (descriptor == null) {
			if (other.descriptor != null)
				return false;
		} else if (!descriptor.equals(other.descriptor))
			return false;
		if (memberName == null) {
			if (other.memberName != null)
				return false;
		} else if (!memberName.equals(other.memberName))
			return false;
		return true;
	}

}
