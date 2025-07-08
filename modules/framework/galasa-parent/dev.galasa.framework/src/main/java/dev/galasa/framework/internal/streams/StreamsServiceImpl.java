/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.framework.internal.streams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.streams.IStream;
import dev.galasa.framework.spi.streams.IStreamsService;
import dev.galasa.framework.spi.streams.Stream;
import dev.galasa.framework.spi.streams.StreamsException;

public class StreamsServiceImpl implements IStreamsService {
    
    private static final String TEST_STREAM_PREFIX = "test.stream.";
    private static final String TEST_STREAM_PREFIX_WITH_NAMESPACE = "framework.test.stream.";

    private StreamPropertiesTransform streamTransform = new StreamPropertiesTransform();
    private IConfigurationPropertyStoreService cpsService;
    
    public StreamsServiceImpl(IConfigurationPropertyStoreService configurationPropertyStoreService) {
        this.cpsService = configurationPropertyStoreService;
    }

    @Override
    public List<IStream> getStreams() throws StreamsException {
        List<IStream> streamsList = new ArrayList<>();
        try {
            // Keys are in the form: test.stream.<stream-name>.property
            streamsList = getStreamsFromCpsProperties(TEST_STREAM_PREFIX);

        } catch (ConfigurationPropertyStoreException e) {
            throw new StreamsException("Failed to get streams from the CPS", e);
        }
        return streamsList;
    }

    @Override
    public IStream getStreamByName(String streamName) throws StreamsException {

        List<IStream> streams = new ArrayList<>();
        IStream stream = null;

        try {

            String testStreamPrefix = TEST_STREAM_PREFIX + streamName + ".";
            streams = getStreamsFromCpsProperties(testStreamPrefix);

            if (!streams.isEmpty()) {
                stream = streams.get(0);
            }

        } catch (ConfigurationPropertyStoreException e) {
            throw new StreamsException("Failed to get a stream with the given name from the CPS", e);
        }

        return stream;

    }

    private IStream createStreamFromProperties(String streamName, Map<String, String> streamProperties)
            throws StreamsException {
        Stream streamBean = new Stream();
        streamBean.setName(streamName);

        for (Map.Entry<String, String> streamProperty : streamProperties.entrySet()) {
            String key = streamProperty.getKey();
            String value = streamProperty.getValue();
            switch (key) {
                case "description":
                    streamBean.setDescription(value);
                    break;
                case "obr":
                    streamBean.setObrsFromCommaSeparatedList(value);
                    break;
                case "location":
                    streamBean.setTestCatalogUrl(value);
                    break;
                case "repo":
                    streamBean.setMavenRepositoryUrl(value);
                    break;
            }
        }
        return streamBean;
    }

    private List<IStream> getStreamsFromCpsProperties(String testStreamPrefix)
            throws ConfigurationPropertyStoreException, StreamsException {
        List<IStream> streamsList = new ArrayList<>();

        Map<String, String> testStreamProperties = cpsService.getPrefixedProperties(testStreamPrefix);

        Map<String, Map<String, String>> groupedStreams = new HashMap<>();
        for (Map.Entry<String, String> entry : testStreamProperties.entrySet()) {
            String key = entry.getKey();
            String propertyWithoutTestStreamPrefix = key.substring(TEST_STREAM_PREFIX.length());

            int dotIndex = propertyWithoutTestStreamPrefix.indexOf('.');
            if (dotIndex > 0) {
                // Extract the stream name and the rest of the key e.g., "description" or
                // "data.repository.url"
                String streamName = propertyWithoutTestStreamPrefix.substring(0, dotIndex);
                String subKey = propertyWithoutTestStreamPrefix.substring(dotIndex + 1);

                // Get or create the sub-map for this stream
                Map<String, String> streamProps = groupedStreams.getOrDefault(streamName, new HashMap<>());
                streamProps.put(subKey, entry.getValue());
                groupedStreams.put(streamName, streamProps);
            }
        }

        // Convert the stream properties into a Stream object
        for (Map.Entry<String, Map<String, String>> streamEntry : groupedStreams.entrySet()) {
            String streamName = streamEntry.getKey();
            Map<String, String> streamProps = streamEntry.getValue();
            IStream streamBean = createStreamFromProperties(streamName, streamProps);
            streamsList.add(streamBean);
        }

        return streamsList;
    }

    @Override
    public void deleteStream(String streamName) throws StreamsException {
        
        String testStreamPrefix = TEST_STREAM_PREFIX_WITH_NAMESPACE + streamName + ".";

        try {
            // Delete all properties associated with the stream
            cpsService.deletePrefixedProperties(testStreamPrefix);

        } catch (ConfigurationPropertyStoreException e) {
            throw new StreamsException("Failed to delete properties for stream: " + streamName, e);
        }
    }

    @Override
    public void setStream(IStream stream) throws StreamsException {
        Map<String, String> streamProperties = streamTransform.getStreamAsProperties(stream);

        try {
            cpsService.setProperties(streamProperties);
        } catch (ConfigurationPropertyStoreException e) {
            throw new StreamsException("Failed to set stream properties into the CPS", e);
        }
    }
}
