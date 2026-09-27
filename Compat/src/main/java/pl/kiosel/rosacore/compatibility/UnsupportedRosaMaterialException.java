package pl.kiosel.rosacore.compatibility;

public final class UnsupportedRosaMaterialException extends IllegalStateException {

	public UnsupportedRosaMaterialException(String material, MaterialTarget target) {
		super("Material " + material + " is not available for " + target
				+ " on this server version");
	}
}
