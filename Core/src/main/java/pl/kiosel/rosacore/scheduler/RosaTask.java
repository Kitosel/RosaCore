package pl.kiosel.rosacore.scheduler;

public interface RosaTask {

	void cancel();

	boolean isCancelled();

	boolean isScheduled();
}
