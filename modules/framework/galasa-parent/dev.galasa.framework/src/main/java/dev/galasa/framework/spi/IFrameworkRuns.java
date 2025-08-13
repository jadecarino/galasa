/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.framework.spi;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.validation.constraints.NotNull;

import dev.galasa.framework.TestRunLifecycleStatus;

public interface IFrameworkRuns {
    
    public enum SharedEnvironmentPhase {
        BUILD,
        DISCARD
    }
    

    @NotNull
    List<IRun> getActiveRuns() throws FrameworkException;

    @NotNull
    List<IRun> getQueuedRuns() throws FrameworkException;;

    @NotNull
    List<IRun> getAllRuns() throws FrameworkException;

    @NotNull
    List<IRun> getAllGroupedRuns(@NotNull String groupName) throws FrameworkException;

    @NotNull
    Set<String> getActiveRunNames() throws FrameworkException;

    @NotNull
    IRun submitRun(String type, String requestor, String bundleName, String testName, String groupName,
            String mavenRepository, String obr, String stream, boolean local, boolean trace, Set<String> tags, Properties overrides,
            SharedEnvironmentPhase sharedEnvironmentPhase, String sharedEnvironmentRunName, String language, String submissionId) throws FrameworkException;

    boolean delete(String runname) throws DynamicStatusStoreException;

    IRun getRun(String runname) throws DynamicStatusStoreException;

    boolean reset(String runname) throws DynamicStatusStoreException;

    boolean markRunInterrupted(String runName, String interruptReason) throws DynamicStatusStoreException;

    void markRunFinished(String runName, String result) throws DynamicStatusStoreException;

    /**
     * Marks the specified run as finished in the DSS. Only if the state of the test run is as we expect.
     * Other processes may have moved the status of the test run without us knowing.
     * @param runName
     * @param result
     * @param currentState The current status of the test run, the status we want to change it from
     * @return True if the test was marked as finished, false if not. For example, someother process marked it as
     * starting or building ahead of us marking it as finished here.
     * @throws DynamicStatusStoreException
     */
    boolean markRunCancelling(String runName, TestRunLifecycleStatus currentStatus) throws DynamicStatusStoreException ;

    void addRunRasAction(IRun run, RunRasAction rasActionToAdd) throws DynamicStatusStoreException;
}
