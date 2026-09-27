package pl.kiosel.rosacore.scheduler;

final class RejectedRosaTask implements RosaTask {

	static final RejectedRosaTask INSTANCE = new RejectedRosaTask();

	private RejectedRosaTask() {
	}

	@Override
	public void cancel() {
	}

	@Override
	public boolean isCancelled() {
		return true;
	}

	@Override
	public boolean isScheduled() {
		return false;
	}
}
