package dev.galasa.docker;

import javax.validation.constraints.NotNull;

import dev.galasa.http.IHttpClient;
import dev.galasa.http.spi.IHttpManagerSpi;

public class MockHttpManager implements IHttpManagerSpi {

    private IHttpClient clientMock;

    public MockHttpManager(IHttpClient clientMock) {
        this.clientMock = clientMock;
    }

    @Override
    public @NotNull IHttpClient newHttpClient() {
        return this.clientMock;
    }

    @Override
    public @NotNull IHttpClient newHttpClient(int timeout) {
        throw new UnsupportedOperationException("Unimplemented method 'newHttpClient'");
    }
    
}
