package dev.galasa.docker;

import dev.galasa.docker.internal.DockerManagerImpl;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.http.spi.IHttpManagerSpi;

public class MockDockerManagerImpl extends DockerManagerImpl {

    private IHttpManagerSpi mockHttpManager;
    private MockCPSStore cps;

    public MockDockerManagerImpl(IHttpManagerSpi mockHttpManager, MockCPSStore cps) {
        this.mockHttpManager = mockHttpManager;
        this.cps = cps;
    }

    @Override
    protected IHttpManagerSpi getHttpManager() {
        return this.mockHttpManager;
    }

    @Override
    public IConfigurationPropertyStoreService getCps() {
        return this.cps;
    }

    public String getCpsProperty(String fullPropertyName) throws DockerManagerException {
        try {
            return cps.getProperty(fullPropertyName);
        } catch (ConfigurationPropertyStoreException e) {
            throw new DockerManagerException();
        }
    }
    
}
