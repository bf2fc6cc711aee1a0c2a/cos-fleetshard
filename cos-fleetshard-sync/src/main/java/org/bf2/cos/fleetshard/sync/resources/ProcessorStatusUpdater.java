package org.bf2.cos.fleetshard.sync.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.bf2.cos.fleet.manager.model.ProcessorDeploymentStatus;
import org.bf2.cos.fleetshard.api.ManagedProcessor;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClient;
import org.bf2.cos.fleetshard.sync.client.FleetManagerClientException;
import org.bf2.cos.fleetshard.sync.client.FleetShardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProcessorStatusUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorStatusUpdater.class);
    @Inject
    FleetManagerClient fleetManagerClient;
    @Inject
    FleetShardClient processorClient;

    public void update(ManagedProcessor processor) {
        LOGGER.debug("Update processor status (name: {}, phase: {})",
            processor.getMetadata().getName(),
            processor.getStatus().getPhase());

        try {
            ProcessorDeploymentStatus processorDeploymentStatus = ProcessorStatusExtractor.extract(processor);

            fleetManagerClient.updateProcessorStatus(processor.getSpec().getClusterId(),
                processor.getSpec().getDeploymentId(),
                processorDeploymentStatus);

        } catch (FleetManagerClientException e) {
            if (e.getStatusCode() == 410) {
                LOGGER.info("Processor " + processor.getMetadata().getName() + " does not exists anymore, deleting it");
                if (processorClient.deleteProcessor(processor)) {
                    LOGGER.info("Processor " + processor.getMetadata().getName() + " deleted");
                }
            } else {
                LOGGER.warn("Error updating status of processor " + processor.getMetadata().getName(), e);
            }
        } catch (Exception e) {
            LOGGER.warn("Error updating status of processor " + processor.getMetadata().getName(), e);
        }
    }
}
