/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.docker.internal;

import static org.assertj.core.api.Assertions.*;

import dev.galasa.docker.DockerManagerException;
import dev.galasa.docker.DockerProvisionException;
import dev.galasa.docker.MockCPSStore;
import dev.galasa.docker.MockDSSStore;
import dev.galasa.docker.MockDockerManagerImpl;
import dev.galasa.docker.MockFramework;
import dev.galasa.docker.MockHttpClient;
import dev.galasa.docker.MockHttpManager;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestDockerEngineImpl {

    private final String busyboxImageDefault = "library/busybox:latest";
    private final String NAMESPACE = "docker";

    private DockerEngineImpl setUpDockerEngineImplObject(Map<String,String> cpsProps) throws DockerProvisionException {
        MockDSSStore mockDss = new MockDSSStore(new HashMap<>());
        MockCPSStore mockCps = new MockCPSStore(NAMESPACE, cpsProps);
        MockFramework mockFramework = new MockFramework(mockCps, mockDss);

        MockHttpClient mockHttpClient = new MockHttpClient();
        MockHttpManager mockHttpManager = new MockHttpManager(mockHttpClient);
        MockDockerManagerImpl mockDockerManager = new MockDockerManagerImpl(mockHttpManager, mockCps);
        String dockerEngineTag = "MYENGINE";

        DockerEngineImpl dockerEngine = new DockerEngineImpl(mockFramework, mockDockerManager, dockerEngineTag, mockDss);
        return dockerEngine;
    }


    @Test
    public void testBusyboxImageIsSetWhen1RegistryAnd1BusyboxImageProvided() throws DockerProvisionException, ConfigurationPropertyStoreException, DockerManagerException {
        // Given...
        String providedBusyboxImageName = "myorg/busybox:latest";
        Map<String,String> cpsProps = Map.of(
            "docker.dse.engine.MYENGINE","MYENGINE",
            "docker.engine.MYENGINE.hostname", "https://my.hostname",
            "docker.engine.MYENGINE.port", "2375",
            "docker.default.registries", "GHCR",
            "docker.registry.GHCR.busybox.image", providedBusyboxImageName
        );
        DockerEngineImpl dockerEngine = setUpDockerEngineImplObject(cpsProps);

        // When...
        String busybox = dockerEngine.getFullyQualifiedBusyboxImageName();

        // Then...
        assertThat(busybox).isEqualTo(providedBusyboxImageName);
    }

    @Test
    public void testFirstBusyboxImageIsSetWhenMultipleRegistriesAndMultipleBusyboxImagesProvided() throws DockerProvisionException, ConfigurationPropertyStoreException, DockerManagerException {
        // This tests that the busybox image for the first registry is used, as the default
        // registries property should be an ordered list of image registries to look in.

        // Given...
        String providedBusyboxImageName = "harbororg/busybox:latest";
        Map<String,String> cpsProps = Map.of(
            "docker.dse.engine.MYENGINE","MYENGINE",
            "docker.engine.MYENGINE.hostname", "https://my.hostname",
            "docker.engine.MYENGINE.port", "2375",
            "docker.default.registries", "HARBOR,GHCR",
            "docker.registry.GHCR.busybox.image", "githuborg/busybox:latest",
            "docker.registry.HARBOR.busybox.image", providedBusyboxImageName
        );
        DockerEngineImpl dockerEngine = setUpDockerEngineImplObject(cpsProps);

        // When...
        String busybox = dockerEngine.getFullyQualifiedBusyboxImageName();

        // Then...
        assertThat(busybox).isEqualTo(providedBusyboxImageName);
    }

    @Test
    public void testBusyboxImageIsSetWhenMultipleRegistriesAnd1BusyboxImageProvided() throws DockerProvisionException, ConfigurationPropertyStoreException, DockerManagerException {
        // This tests that even if a busybox image for the first registry is not found,
        // the Manager keeps looking through the next registries to find the busybox image.

        // Given...
        String providedBusyboxImageName = "myorg/busybox:latest";
        Map<String,String> cpsProps = Map.of(
            "docker.dse.engine.MYENGINE","MYENGINE",
            "docker.engine.MYENGINE.hostname", "https://my.hostname",
            "docker.engine.MYENGINE.port", "2375",
            "docker.default.registries", "HARBOR,GHCR",
            "docker.registry.GHCR.busybox.image", providedBusyboxImageName
        );
        DockerEngineImpl dockerEngine = setUpDockerEngineImplObject(cpsProps);

        // When...
        String busybox = dockerEngine.getFullyQualifiedBusyboxImageName();

        // Then...
        assertThat(busybox).isEqualTo(providedBusyboxImageName);
    }

    @Test
    public void testBusyboxImageIsDefaultedWhen0RegistriesAnd0BusyboxImagesProvided() throws DockerProvisionException, ConfigurationPropertyStoreException, DockerManagerException {
        // Given...
        Map<String,String> cpsProps = Map.of(
            "docker.dse.engine.MYENGINE","MYENGINE",
            "docker.engine.MYENGINE.hostname", "https://my.hostname",
            "docker.engine.MYENGINE.port", "2375"
        );
        DockerEngineImpl dockerEngine = setUpDockerEngineImplObject(cpsProps);

        // When...
        String busybox = dockerEngine.getFullyQualifiedBusyboxImageName();

        // Then...
        assertThat(busybox).isEqualTo(busyboxImageDefault);
    }

    @Test
    public void testBusyboxImageIsDefaultedWhen0RegistriesAnd1BlankBusyboxImageProvided() throws DockerProvisionException, ConfigurationPropertyStoreException, DockerManagerException {
        // Given...
        Map<String,String> cpsProps = Map.of(
            "docker.dse.engine.MYENGINE","MYENGINE",
            "docker.engine.MYENGINE.hostname", "https://my.hostname",
            "docker.engine.MYENGINE.port", "2375",
            "docker.registry.GHCR.busybox.image",""
        );
        DockerEngineImpl dockerEngine = setUpDockerEngineImplObject(cpsProps);

        // When...
        String busybox = dockerEngine.getFullyQualifiedBusyboxImageName();

        // Then...
        assertThat(busybox).isEqualTo(busyboxImageDefault);
    }

    @Test
    public void testBusyboxImageIsDefaultedWhen1RegistryAnd0BusyboxImagesProvided() throws DockerProvisionException, ConfigurationPropertyStoreException, DockerManagerException {
        // Given...
        Map<String,String> cpsProps = Map.of(
            "docker.dse.engine.MYENGINE","MYENGINE",
            "docker.engine.MYENGINE.hostname", "https://my.hostname",
            "docker.engine.MYENGINE.port", "2375",
            "docker.default.registries", "GHCR"
        );
        DockerEngineImpl dockerEngine = setUpDockerEngineImplObject(cpsProps);

        // When...
        String busybox = dockerEngine.getFullyQualifiedBusyboxImageName();

        // Then...
        assertThat(busybox).isEqualTo(busyboxImageDefault);
    }

}
