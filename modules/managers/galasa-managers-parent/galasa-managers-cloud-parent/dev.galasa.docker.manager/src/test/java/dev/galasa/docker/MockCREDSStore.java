package dev.galasa.docker;

import java.util.Map;

import dev.galasa.ICredentials;
import dev.galasa.framework.spi.creds.CredentialsException;
import dev.galasa.framework.spi.creds.ICredentialsService;
import dev.galasa.framework.spi.creds.ICredentialsStore;

public class MockCREDSStore implements ICredentialsStore, ICredentialsService {

    @Override
    public ICredentials getCredentials(String credsId) throws CredentialsException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCredentials'");
    }

    @Override
    public Map<String, ICredentials> getAllCredentials() throws CredentialsException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllCredentials'");
    }

    @Override
    public void setCredentials(String credsId, ICredentials credentials) throws CredentialsException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCredentials'");
    }

    @Override
    public void deleteCredentials(String credsId) throws CredentialsException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteCredentials'");
    }

    @Override
    public void shutdown() throws CredentialsException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'shutdown'");
    }
    
}
