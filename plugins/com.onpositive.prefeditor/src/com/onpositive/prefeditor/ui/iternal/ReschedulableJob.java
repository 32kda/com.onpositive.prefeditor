package com.onpositive.prefeditor.ui.iternal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

public abstract class ReschedulableJob extends Job {

		private long rescheduleDelay;

		private long scheduledTimestamp = 0;

		public ReschedulableJob(String name, long rescheduleDelay) {
			super(name);
			this.rescheduleDelay = rescheduleDelay;
		}

		public void checkReschedule() {
			synchronized (this) {
				long stamp = System.currentTimeMillis();
				if (stamp - scheduledTimestamp < rescheduleDelay) {
					cancel();
					schedule(rescheduleDelay);
					scheduledTimestamp = stamp;
				} else {
					schedule(rescheduleDelay);
				}
			}
		}

		@Override
		protected IStatus run(IProgressMonitor arg0) {
			synchronized (this) {
				scheduledTimestamp = 0;
				return execute();
			}
		}

		protected abstract IStatus execute();

	}