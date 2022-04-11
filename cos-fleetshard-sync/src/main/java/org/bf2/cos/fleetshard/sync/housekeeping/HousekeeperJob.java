package org.bf2.cos.fleetshard.sync.housekeeping;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@DisallowConcurrentExecution
public class HousekeeperJob implements Job {
    @Inject
    Housekeeper target;

    @Override
    public void execute(JobExecutionContext context) {
        target.run();
    }
}
