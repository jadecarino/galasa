package dev.galasa.docker;

import javax.validation.constraints.NotNull;

import dev.galasa.framework.Framework;
import dev.galasa.framework.spi.creds.CredentialsException;
import dev.galasa.framework.spi.creds.ICredentialsService;

public class MockFramework extends Framework {

    private MockDSSStore mockDss;
    private MockCPSStore mockCps;
    private MockCREDSStore mockCreds;

    public MockFramework(MockCPSStore mockCps, MockDSSStore mockDss) {
        this.mockCps = mockCps;
        this.mockDss = mockDss;
    }

    public MockFramework(MockCPSStore mockCps, MockDSSStore mockDss, MockCREDSStore mockCreds) {
        this(mockCps, mockDss);
        this.mockCreds = mockCreds;
    }

    @Override
    public @NotNull ICredentialsService getCredentialsService() throws CredentialsException {
        return this.mockCreds;
    }

}
