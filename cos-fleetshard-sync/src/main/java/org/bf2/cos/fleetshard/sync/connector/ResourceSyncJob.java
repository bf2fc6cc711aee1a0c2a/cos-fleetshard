package org.bf2.cos.fleetshard.sync.connector;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@DisallowConcurrentExecution
public class ResourceSyncJob implements Job {
    @Inject
    ResourceSync rs;

    @Override
    public void execute(JobExecutionContext context) {
        rs.sync();
    }
}
