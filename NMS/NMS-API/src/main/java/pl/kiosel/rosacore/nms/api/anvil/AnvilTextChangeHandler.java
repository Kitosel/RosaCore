package pl.kiosel.rosacore.nms.api.anvil;

@FunctionalInterface
public interface AnvilTextChangeHandler {

	void onTextChange(CustomAnvil anvil, String text);
}
