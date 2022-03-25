package org.bf2.cos.fleetshard.sync.resources;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@DisallowConcurrentExecution
public class ConnectorClusterStatusSyncJob implements Job {
    @Inject
    ConnectorClusterStatusSync sync;

    @Override
    public void execute(JobExecutionContext context) {
        sync.run();
    }
}
