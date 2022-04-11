package org.bf2.cos.fleetshard.sync.resources;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@DisallowConcurrentExecution
public class AddonCleanupJob implements Job {
    @Inject
    AddonCleanup cleanup;

    @Override
    public void execute(JobExecutionContext context) {
        cleanup.run();
    }
}
