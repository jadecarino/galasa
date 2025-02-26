package dev.galasa.docker;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IConfigurationPropertyStore;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;

public class MockCPSStore implements IConfigurationPropertyStore, IConfigurationPropertyStoreService {

    private final String NAMESPACE;
    private Map<String,String> propertiesMap;

    public MockCPSStore(String namespace, Map<String,String> propertiesMap) {
        this.NAMESPACE = namespace;
        this.propertiesMap = propertiesMap;
    }

    @Override
    public @Null String getProperty(@NotNull String prefix, @NotNull String suffix, String... infixes)
            throws ConfigurationPropertyStoreException {
        String infixesString = "";
        for (String infix : infixes) {
            infixesString += "." + infix;
        }

        String propertyName = prefix + infixesString + "." + suffix;
        return getProperty(propertyName);
    }

    @Override
    public @Null String getProperty(@NotNull String key) throws ConfigurationPropertyStoreException {
        return propertiesMap.get(NAMESPACE + "." + key);
    }

    @Override
    public Map<String, String> getAllProperties() throws ConfigurationPropertyStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getAllProperties'");
    }

    @Override
    public String[] reportPropertyVariants(@NotNull String prefix, @NotNull String suffix, String... infixes) {
        throw new UnsupportedOperationException("Unimplemented method 'reportPropertyVariants'");
    }

    @Override
    public String reportPropertyVariantsString(@NotNull String prefix, @NotNull String suffix, String... infixes) {
        throw new UnsupportedOperationException("Unimplemented method 'reportPropertyVariantsString'");
    }

    @Override
    public List<String> getCPSNamespaces() throws ConfigurationPropertyStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getCPSNamespaces'");
    }

    @Override
    public @NotNull Map<String, String> getPrefixedProperties(@NotNull String prefix)
            throws ConfigurationPropertyStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getPrefixedProperties'");
    }

    @Override
    public void setProperty(@NotNull String key, @NotNull String value) throws ConfigurationPropertyStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'setProperty'");
    }

    @Override
    public void deleteProperty(@NotNull String key) throws ConfigurationPropertyStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'deleteProperty'");
    }

    @Override
    public Map<String, String> getPropertiesFromNamespace(String namespace) throws ConfigurationPropertyStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getPropertiesFromNamespace'");
    }

    @Override
    public List<String> getNamespaces() throws ConfigurationPropertyStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getNamespaces'");
    }

    @Override
    public void shutdown() throws ConfigurationPropertyStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'shutdown'");
    }
    
}
