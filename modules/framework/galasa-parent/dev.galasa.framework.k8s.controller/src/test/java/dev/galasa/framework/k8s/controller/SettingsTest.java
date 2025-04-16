/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.framework.k8s.controller;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import io.kubernetes.client.openapi.apis.CoreV1Api;


public class SettingsTest {

    @Test
    public void testCanCreateASettingsObject() throws Exception {
        K8sController controller = new K8sController() {};
        CoreV1Api api = new CoreV1Api() {};
        new  Settings( controller, api);
    }

    @Test
    public void testCanReadDefaultHeapSizeIfMissingFromConfigMap() throws Exception {
        K8sController controller = new K8sController() {};
        CoreV1Api api = new CoreV1Api() {};
        Settings settings = new  Settings( controller, api);
        Map<String,String> configMap = new HashMap<String,String>();
        settings.updateConfigMapProperties(configMap);

        int heapSizeGotBack = settings.getEngineMemoryHeapSizeMegabytes();

        assertThat(heapSizeGotBack).isEqualTo(150);
    }

    @Test
    public void testCanReadNonDefaultHeapSizeIfPresentInConfigMap() throws Exception {
        K8sController controller = new K8sController() {};
        CoreV1Api api = new CoreV1Api() {};
        Settings settings = new  Settings( controller, api);
        Map<String,String> configMap = new HashMap<String,String>();
        configMap.put("engine_memory_heap","450");
        settings.updateConfigMapProperties(configMap);

        int heapSizeGotBack = settings.getEngineMemoryHeapSizeMegabytes();

        assertThat(heapSizeGotBack).isEqualTo(450);
    }
}