
public class AndroidImage {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((drawableLevel == null) ? 0 : drawableLevel.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((platform == null) ? 0 : platform.hashCode());
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
		AndroidImage other = (AndroidImage) obj;
		if (drawableLevel == null) {
			if (other.drawableLevel != null)
				return false;
		} else if (!drawableLevel.equals(other.drawableLevel))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (platform == null) {
			if (other.platform != null)
				return false;
		} else if (!platform.equals(other.platform))
			return false;
		return true;
	}
	public String sha;
	public int width,height;
	public long length;
	public String name;
	public String platform;
	public String drawableLevel;
	@Override
	public String toString() {
		return "AndroidImage [sha=" + sha + ", width=" + width + ", height="
				+ height + ", length=" + length + ", name=" + name
				+ ", platform=" + platform + ", drawableLevel=" + drawableLevel
				+ "]";
	}
	
}
