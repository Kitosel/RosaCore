package pl.kiosel.rosacore.nms.api.tablist;

import java.util.Objects;

public final class SimpleTabListSkin implements TabListSkin {

	private final String value;
	private final String signature;

	public SimpleTabListSkin(String value, String signature) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("Skin value cannot be empty");
		}
		this.value = value;
		this.signature = signature == null || signature.isEmpty() ? null : signature;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public String getSignature() {
		return signature;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof SimpleTabListSkin)) return false;
		SimpleTabListSkin that = (SimpleTabListSkin) object;
		return value.equals(that.value) && Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, signature);
	}

	@Override
	public String toString() {
		return "SimpleTabListSkin{valueLength=" + value.length() + ", signed=" + (signature != null) + '}';
	}
}
