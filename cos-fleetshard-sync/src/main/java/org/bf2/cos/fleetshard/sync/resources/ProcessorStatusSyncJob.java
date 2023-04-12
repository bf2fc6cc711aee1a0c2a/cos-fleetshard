package org.bf2.cos.fleetshard.sync.resources;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@DisallowConcurrentExecution
public class ProcessorStatusSyncJob implements Job {
    @Inject
    ProcessorStatusSync sync;

    @Override
    public void execute(JobExecutionContext context) {
        sync.run();
    }
}
