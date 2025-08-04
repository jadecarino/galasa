package dev.galasa.framework.internal.dss;


/**
 * An enum of Dss property suffixes we use when manipulating properties pertaining to a single run.
 * 
 * The general format of the whole dss key is "run.{runName}.{suffix}"
 * 
 * This is a list of all the suffixes used.
 */
public enum DssPropertyKeyRunNameSuffix{
    INTERRUPT_REASON("interruptReason"),
    INTERRUPTED_AT("interruptedAt"),
    STATUS("status"),
    RAS_RUN_ID("rasrunid"),
    HEARTBEAT("heartbeat"),
    RESULT("result"),
    FINISHED_DATETIME("finished"),
    RAS_ACTIONS("rasActions"),
    QUEUED("queued"),
    TEST_BUNDLE("testbundle"),
    TEST_CLASS("testclass"),
    REQUEST_TYPE("request.type"),
    LOCAL("local"),
    TRACE("trace"),
    REPOSITORY("repository"),
    OBR("obr"),
    STREAM("stream"),
    GROUP("group"),
    SUBMISSION_ID("submissionId"),
    TAGS("tags"),
    REQUESTOR("requestor"),
    SHARED_ENVIRONMENT("shared.environment"),
    GHERKIN("gherkin"),
    OVERRIDES("overrides"),
    TEST("test"), 
    ALLOCATED("allocated"),
    ;

    private String name ;

    private DssPropertyKeyRunNameSuffix(String name) {
        this.name = name ;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return name ;
    }

}