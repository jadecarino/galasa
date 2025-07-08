/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.framework;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;

import dev.galasa.framework.beans.Property;
import dev.galasa.framework.beans.SubmitRunRequest;
import dev.galasa.framework.spi.AbstractManager;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.DynamicStatusStoreException;
import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IDynamicStatusStoreService;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IFrameworkRuns;
import dev.galasa.framework.spi.IRun;
import dev.galasa.framework.spi.RunRasAction;
import dev.galasa.framework.spi.utils.GalasaGson;
import dev.galasa.framework.spi.utils.GalasaGsonBuilder;
import dev.galasa.framework.spi.utils.ITimeService;
import dev.galasa.framework.spi.utils.SystemTimeService;

public class FrameworkRuns implements IFrameworkRuns {

    private final static Log                         logger       = LogFactory.getLog(FrameworkRuns.class);

    private final Pattern                            runPattern   = Pattern.compile("^\\Qrun.\\E(\\w+)\\Q.\\E.*$");

    private final IFramework                         framework;
    private final IDynamicStatusStoreService         dss;
    private final IConfigurationPropertyStoreService cps;

    private final String                             NO_GROUP     = "none";
    private final String                             NO_BUNDLE     = "none";
    private final String                             NO_RUNTYPE   = "UNKNOWN";
    // private final String                             NO_REQUESTER = "unknown";

    private final String                             RUN_PREFIX   = "run.";

    private ITimeService timeService = new SystemTimeService();

    private final GalasaGson gson = new GalasaGson();

    public FrameworkRuns(IFramework framework) throws FrameworkException {
        this.framework = framework;
        this.dss = framework.getDynamicStatusStoreService("framework");
        this.cps = framework.getConfigurationPropertyService("framework");
        gson.setGsonBuilder(new GalasaGsonBuilder(false));
    }

    @Override
    public List<IRun> getActiveRuns() throws FrameworkException {

        List<IRun> runs = getAllRuns();
        Iterator<IRun> iruns = runs.iterator();
        while (iruns.hasNext()) {
            IRun run = iruns.next();

            if (run.getHeartbeat() != null) {
                continue;
            }

            if ("allocated".equals(run.getStatus())) {
                continue;
            }

            if (run.isSharedEnvironment()) {
                continue;
            }

            iruns.remove();
        }

        return runs;
    }

    @Override
    public @NotNull List<IRun> getQueuedRuns() throws FrameworkException {
        List<IRun> runs = getAllRuns();
        Iterator<IRun> iruns = runs.iterator();
        while (iruns.hasNext()) {
            IRun run = iruns.next();

            if (!"queued".equals(run.getStatus())) {
                iruns.remove();
            }
        }

        return runs;
    }

    @Override
    public List<IRun> getAllRuns() throws FrameworkException {
        HashMap<String, IRun> runs = new HashMap<>();

        logger.trace("Fetching all runs from DSS");
        Map<String, String> runProperties = dss.getPrefix(RUN_PREFIX);
        logger.trace("Fetched all runs from DSS");
        for (String key : runProperties.keySet()) {
            Matcher matcher = runPattern.matcher(key);
            if (matcher.find()) {
                String runName = matcher.group(1);

                if (!runs.containsKey(runName)) {
                    runs.put(runName, new RunImpl(runName, this.dss));
                }
            }
        }

        LinkedList<IRun> returnRuns = new LinkedList<>(runs.values());

        return returnRuns;
    }

    @Override
    public List<IRun> getAllGroupedRuns(@NotNull String groupName) throws FrameworkException {
        List<IRun> allRuns = this.getAllRuns();
        List<IRun> groupedRuns = new LinkedList<IRun>();

        for (IRun run : allRuns) {
            if (groupName.equals(run.getGroup())) {
                groupedRuns.add(run);
            }
        }
        return groupedRuns;
    }

    @Override
    public @NotNull Set<String> getActiveRunNames() throws FrameworkException {
        List<IRun> runs = getActiveRuns();

        HashSet<String> runNames = new HashSet<>();
        for (IRun run : runs) {
            runNames.add(run.getName());
        }

        return runNames;
    }

    private @NotNull IRun submitRun(SubmitRunRequest runRequest) throws FrameworkException {
        IRun run = null;
        setRunRequestDefaultsIfNotSet(runRequest);

        if (runRequest.getSharedEnvironmentPhase() != null) {
            run = submitSharedEnvironmentRun(runRequest);
        } else {
            try {
                String runName = assignNewRunName(runRequest);
                run = new RunImpl(runName, this.dss);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new FrameworkException("Interrupted", e);
            } catch (Exception e) {
                throw new FrameworkException("Problem submitting job", e);
            }
        }
        return run;
    }

    @Override
    @NotNull
    public @NotNull IRun submitRun(String runType, String requestor, String bundleName,
            @NotNull String testName, String groupName, String mavenRepository, String obr, String stream,
            boolean local, boolean trace, Set<String> tags, Properties overrides, SharedEnvironmentPhase sharedEnvironmentPhase, String sharedEnvironmentRunName,
            String language, String submissionId) throws FrameworkException {
        SubmitRunRequest runRequest = new SubmitRunRequest(
            runType,
            requestor,
            bundleName,
            testName,
            groupName,
            submissionId,
            mavenRepository,
            obr,
            stream,
            local,
            trace,
            tags,
            overrides,
            sharedEnvironmentPhase,
            sharedEnvironmentRunName,
            language
        );
        return submitRun(runRequest);
    }

    private boolean storeRun(String runName, SubmitRunRequest runRequest) throws DynamicStatusStoreException {
        String bundleName = runRequest.getBundleName();
        String testName = runRequest.getTestName();
        String bundleTest = runRequest.getBundleTest();
        String gherkinTest = runRequest.getGherkinTest();
        String runType = runRequest.getRunType();
        String mavenRepository = runRequest.getMavenRepository();
        String obr = runRequest.getObr();
        String stream = runRequest.getStream();
        String groupName = runRequest.getGroupName();
        String submissionId = runRequest.getSubmissionId();
        String requestor = runRequest.getRequestor();
        SharedEnvironmentPhase sharedEnvironmentPhase = runRequest.getSharedEnvironmentPhase();
        Properties overrides = runRequest.getOverrides();
        boolean local = runRequest.isLocalRun();
        boolean trace = runRequest.isTraceEnabled();
        Set<String> tags = runRequest.getTags();

        String runPropertyPrefix = RUN_PREFIX + runName;

        // *** Set up the otherRunProperties that will go with the Run number
        HashMap<String, String> otherRunProperties = new HashMap<>();
        otherRunProperties.put(runPropertyPrefix + ".status", "queued");
        otherRunProperties.put(runPropertyPrefix + ".queued", Instant.now().toString());
        otherRunProperties.put(runPropertyPrefix + ".testbundle", bundleName);
        otherRunProperties.put(runPropertyPrefix + ".testclass", testName);
        otherRunProperties.put(runPropertyPrefix + ".request.type", runType);
        otherRunProperties.put(runPropertyPrefix + ".local", Boolean.toString(local));
        if (trace) {
            otherRunProperties.put(runPropertyPrefix + ".trace", "true");
        }
        if (mavenRepository != null) {
            otherRunProperties.put(runPropertyPrefix + ".repository", mavenRepository);
        }
        if (obr != null) {
            otherRunProperties.put(runPropertyPrefix + ".obr", obr);
        }
        if (stream != null) {
            otherRunProperties.put(runPropertyPrefix + ".stream", stream);
        }
        if (groupName != null) {
            otherRunProperties.put(runPropertyPrefix + ".group", groupName);
        } else {
            otherRunProperties.put(runPropertyPrefix + ".group", UUID.randomUUID().toString());
        }

        otherRunProperties.put(runPropertyPrefix + ".submissionId", submissionId);

        if (tags != null) {
            String tagsAsString = gson.toJson(tags);
            otherRunProperties.put(runPropertyPrefix + ".tags",tagsAsString);
        }

        otherRunProperties.put(runPropertyPrefix + ".requestor", requestor);

        if (sharedEnvironmentPhase != null) {
            otherRunProperties.put(runPropertyPrefix + ".shared.environment", "true");
            overrides.put("framework.run.shared.environment.phase", sharedEnvironmentPhase.toString());
        }
        if (gherkinTest != null) {
            otherRunProperties.put(runPropertyPrefix + ".gherkin", gherkinTest);
        }

        // *** Add in the overrides as a single property
        if (!overrides.isEmpty()) {
            JsonArray overridesArray = new JsonArray();
            for (Map.Entry<Object, Object> entry : overrides.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                overridesArray.add(gson.toJsonTree(new Property(key, value)));
            }

            otherRunProperties.put(runPropertyPrefix + ".overrides", gson.toJson(overridesArray));
        }

        // *** See if we can setup the runnumber properties (clashes possible if low max
        // number or sharing prefix
        return this.dss.putSwap(runPropertyPrefix + ".test", null, bundleTest, otherRunProperties);
    }

    @Override
    public boolean delete(String runname) throws DynamicStatusStoreException {
        String prefix = getRunDssPrefix(runname);

        Map<String, String> properties = this.dss.getPrefix(prefix);
        if (properties.isEmpty()) {
            return false;
        }

        this.dss.deletePrefix(prefix);
        return true;
    }

    @Override
    public boolean reset(String runName) throws DynamicStatusStoreException {
        boolean isReset = false;

        IRun run = getRun(runName);
        if (run != null && !run.isLocal()) {

            // Remove the run's heartbeat and interrupt reason
            Set<String> keysToDelete = new HashSet<>();
            keysToDelete.add(getSuffixedRunDssKey(runName, "heartbeat"));
            keysToDelete.add(getSuffixedRunDssKey(runName, "interruptReason"));
            this.dss.delete(keysToDelete);

            // Set the status of the run back to 'queued'
            this.dss.put(getSuffixedRunDssKey(runName, "status"), TestRunLifecycleStatus.QUEUED.toString());
            isReset = true;
        }

        return isReset;
    }

    @Override
    public void addRunRasAction(IRun run, RunRasAction rasActionToAdd) throws DynamicStatusStoreException {
        String runName = run.getName();

        List<RunRasAction> updatedRasActions = new ArrayList<>(run.getRasActions());
        updatedRasActions.add(rasActionToAdd);

        String encodedRasActions = encodeRasActionsToBase64(updatedRasActions);

        this.dss.put(getSuffixedRunDssKey(runName, "rasActions"), encodedRasActions);
    }

    private void interruptRun(IRun run, String interruptReason) throws DynamicStatusStoreException {
        String runName = run.getName();

        Map<String, String> propertiesToSet = new HashMap<>();

        String runId = run.getRasRunId();
        if (runId != null) {
            // Add a RAS action to the run's existing RAS actions so that the RAS records for any re-runs can be updated correctly
            List<RunRasAction> rasActions = new ArrayList<>(run.getRasActions());
            RunRasAction rasActionToAdd = new RunRasAction(runId, TestRunLifecycleStatus.FINISHED.toString(), interruptReason);
            rasActions.add(rasActionToAdd);
            String encodedRasActions = encodeRasActionsToBase64(rasActions);

            propertiesToSet.put(getSuffixedRunDssKey(runName, "rasActions"), encodedRasActions);
        }

        propertiesToSet.put(getSuffixedRunDssKey(runName, "interruptReason"), interruptReason);

        this.dss.put(propertiesToSet);
    }

    private String encodeRasActionsToBase64(List<RunRasAction> rasActions) {
        String rasActionsJsonStr = gson.toJson(rasActions);
        String encodedRasActions = Base64.getEncoder().encodeToString(rasActionsJsonStr.getBytes(StandardCharsets.UTF_8));
        return encodedRasActions;
    }

    @Override
    public boolean markRunInterrupted(String runName, String interruptReason) throws DynamicStatusStoreException {
        boolean isMarkedInterrupted = false;

        IRun run = getRun(runName);
        if (run != null) {
            if (!interruptReason.equals(run.getInterruptReason()) && !interruptReason.equals(run.getResult())) {
                // Only update the DSS if the run hasn't been interrupted already
                interruptRun(run, interruptReason);
            }
            isMarkedInterrupted = true;
        }
        return isMarkedInterrupted;
    }

    @Override
    public void markRunFinished(String runName, String result) throws DynamicStatusStoreException {
        if (isRunInDss(runName)) {
            Map<String, String> propertiesToSet = new HashMap<>();
            propertiesToSet.put(getSuffixedRunDssKey(runName, "status"), TestRunLifecycleStatus.FINISHED.toString());
            propertiesToSet.put(getSuffixedRunDssKey(runName, "result"), result);
            propertiesToSet.put(getSuffixedRunDssKey(runName, "finished"), timeService.now().toString());

            this.dss.put(propertiesToSet);
        }
    }

    @Override
    public IRun getRun(String runname) throws DynamicStatusStoreException {
        IRun run = null;

        if (isRunInDss(runname)) {
            run = new RunImpl(runname, this.dss);
        }

        return run;
    }

    /**
     * Get the prefix of a given run type
     */
    private String getRunTypePrefix(String runType) throws ConfigurationPropertyStoreException {
        String typePrefix = AbstractManager.nulled(this.cps.getProperty("request.type." + runType, "prefix"));
        if (typePrefix == null) {
            if ("local".equalsIgnoreCase(runType)) {
                typePrefix = "L";
            } else {
                typePrefix = "U"; // *** For unknown prefix
            }
        }
        return typePrefix;
    }

    /**
     * Get the maximum number for the given type prefix
     */
    private int getPrefixMaxNumber(String typePrefix) throws ConfigurationPropertyStoreException {
        int maxNumber = Integer.MAX_VALUE;
        String sMaxNumber = AbstractManager.nulled(this.cps.getProperty("request.prefix", "maximum", typePrefix));
        if (sMaxNumber != null) {
            maxNumber = Integer.parseInt(sMaxNumber);
        }
        return maxNumber;
    }

    /**
     * Sets the relevant DSS properties when a shared environment run is being discarded
     */
    private void setDiscardSharedEnvironmentPhaseProperties(IRun run, String sharedEnvironmentRunName, String groupName) throws FrameworkException {
        if (!run.isSharedEnvironment()) {
            throw new FrameworkException("Run " + sharedEnvironmentRunName + " is not a shared environment");
        }

        if (!"UP".equalsIgnoreCase(run.getStatus())) {
            throw new FrameworkException("Shared Environment " + sharedEnvironmentRunName + " is not up and running");
        }

        HashMap<String, String> otherProperties = new HashMap<>();
        String runPropertyPrefix = RUN_PREFIX + sharedEnvironmentRunName;
        otherProperties.put(runPropertyPrefix + ".overrides", "framework.run.shared.environment.phase=" + SharedEnvironmentPhase.DISCARD.toString());
        if (groupName != null) {
            otherProperties.put(runPropertyPrefix + ".group", groupName);
        }
        if (!this.dss.putSwap(runPropertyPrefix + ".status", "up", "queued", otherProperties)) {
            throw new FrameworkException("Failed to switch Shared Environment " + sharedEnvironmentRunName + " to discard");
        }
    }

    private String assignNewRunName(SubmitRunRequest runRequest) throws FrameworkException, InterruptedException {
        String typePrefix = getRunTypePrefix(runRequest.getRunType());
        int maxNumber = getPrefixMaxNumber(typePrefix);
        runRequest.setSharedEnvironmentPhase(null);

        // *** Now loop until we find the next free number for this run type
        String runName = null;
        boolean maxlooped = false;
        while (runName == null) {
            String pLastused = "request.prefix." + typePrefix + ".lastused";
            String sLatestNumber = this.dss.get(pLastused);
            int latestNumber = 0;
            if (sLatestNumber != null && !sLatestNumber.trim().isEmpty()) {
                latestNumber = Integer.parseInt(sLatestNumber);
            }

            // *** Add 1 to the run number and see if we get it
            latestNumber++;
            if (latestNumber > maxNumber) { // *** have we gone past the maximum number
                if (maxlooped) {
                    throw new FrameworkException("Not enough request type numbers available, looped twice");
                }
                latestNumber = 1;
                maxlooped = true; // *** Safety check to make sure we havent gone through all the numbers again
            }

            String sNewNumber = Integer.toString(latestNumber);
            if (!this.dss.putSwap(pLastused, sLatestNumber, sNewNumber)) {
                Thread.sleep(this.framework.getRandom().nextInt(200)); // *** Wait for a bit, to avoid race
                // conditions
                continue; // Try again with the new latest number
            }

            String tempRunName = typePrefix + sNewNumber;

            if (!storeRun(tempRunName, runRequest)) {
                Thread.sleep(this.framework.getRandom().nextInt(200)); // *** Wait for a bit, to avoid race
                // conditions
                continue; // *** Try again
            }

            runName = tempRunName; // *** Got it
        }
        return runName;
    }

    private IRun submitSharedEnvironmentRun(SubmitRunRequest runRequest) throws FrameworkException {
        String sharedEnvironmentRunName = runRequest.getSharedEnvironmentRunName();
        SharedEnvironmentPhase sharedEnvironmentPhase = runRequest.getSharedEnvironmentPhase();
        IRun run = new RunImpl(sharedEnvironmentRunName, this.dss);

        if (sharedEnvironmentPhase == SharedEnvironmentPhase.BUILD
                && !storeRun(sharedEnvironmentRunName, runRequest)) {
            throw new FrameworkException("Unable to submit shared environment run " + sharedEnvironmentRunName + ", is there a duplicate runname?");
        } else if (sharedEnvironmentPhase == SharedEnvironmentPhase.DISCARD) {
            //*** If this is discard,  tweak the current run parameters
            setDiscardSharedEnvironmentPhaseProperties(run, sharedEnvironmentRunName, runRequest.getGroupName());
        }
        return run;
    }

    private void setRunRequestDefaultsIfNotSet(SubmitRunRequest runRequest) throws FrameworkException {
        setRunRequestTestDetails(runRequest);

        if (AbstractManager.nulled(runRequest.getGroupName()) == null) {
            runRequest.setGroupName(NO_GROUP);
        }

        String runType = AbstractManager.nulled(runRequest.getRunType());
        if (runType == null) {
            runType = NO_RUNTYPE;
        }
        runRequest.setRunType(runType.toUpperCase());

        setRunRequestRequestorDefault(runRequest);

        runRequest.setStream(AbstractManager.nulled(runRequest.getStream()));

        if (runRequest.getOverrides() == null) {
            runRequest.setOverrides(new Properties());
        }

        formatSharedEnvironmentRunName(runRequest);
    }

    private void setRunRequestTestDetails(SubmitRunRequest runRequest) throws FrameworkException {
        String language = runRequest.getLanguage();
        if (language == null) {
            language = "java";
            runRequest.setLanguage(language);
        }

        if (language.equals("java")) {
            if (runRequest.getBundleName() == null) {
                throw new FrameworkException("Missing bundle name");
            }
            runRequest.setBundleTest(runRequest.getBundleName() + "/" + runRequest.getTestName());
        } else if (language.equals("gherkin")) {
            runRequest.setBundleTest(NO_BUNDLE);
            runRequest.setGherkinTest(runRequest.getTestName());
            runRequest.setBundleName(NO_BUNDLE);
        }
    }

    private void setRunRequestRequestorDefault(SubmitRunRequest runRequest) throws FrameworkException {
        String requestor = AbstractManager.nulled(runRequest.getRequestor());
        if (requestor == null) {
            requestor = AbstractManager.nulled(cps.getProperty("run", "requestor"));
            if (requestor == null) {
                requestor = "unknown";
            }
            runRequest.setRequestor(requestor);
        }
    }

    private void formatSharedEnvironmentRunName(SubmitRunRequest runRequest) throws FrameworkException {
        SharedEnvironmentPhase sharedEnvironmentPhase = runRequest.getSharedEnvironmentPhase();
        if (sharedEnvironmentPhase != null) {
            String sharedEnvironmentRunName = runRequest.getSharedEnvironmentRunName();

            if (sharedEnvironmentRunName == null || sharedEnvironmentRunName.trim().isEmpty()) {
                throw new FrameworkException("Missing run name for shared environment");
            }

            runRequest.setSharedEnvironmentRunName(sharedEnvironmentRunName.trim().toUpperCase());
        }
    }

    private boolean isRunInDss(String runName) throws DynamicStatusStoreException {
        String prefix = getRunDssPrefix(runName);
        Map<String, String> properties = this.dss.getPrefix(prefix);
        return !properties.isEmpty();
    }

    private String getRunDssPrefix(String runName) throws DynamicStatusStoreException {
        return RUN_PREFIX + runName + ".";
    }

    private String getSuffixedRunDssKey(String runName, String suffix) throws DynamicStatusStoreException {
        return getRunDssPrefix(runName) + suffix;
    }
}