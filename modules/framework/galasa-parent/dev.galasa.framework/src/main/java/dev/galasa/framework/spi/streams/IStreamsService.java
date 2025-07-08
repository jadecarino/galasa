/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.framework.spi.streams;

import java.util.List;

public interface IStreamsService {
   
    /**
     * Returns a list of all the test streams stored in the Configuration Property Store (CPS).
     *
     * @return a list of all test streams stored in the Configuration Property Store (CPS).
     * @throws StreamsException if there is an issue accessing the CPS.
     */
    List<IStream> getStreams() throws StreamsException;

    /**
     * Returns a test stream stored in the Configuration Property Store (CPS) with a matching name.
     *
     * @param streamName the name of the stream to retrieve.
     * @return a stream stored in the Configuration Property Store (CPS) with a matching name.
     * @throws StreamsException if there is an issue accessing the CPS.
     */
    IStream getStreamByName(String streamName) throws StreamsException;

    /**
     * Deletes a test stream stored in the Configuration Property Store (CPS) with a matching name.
     *
     * @param streamName the name of the stream to delete.
     * @throws StreamsException if there is an issue accessing the CPS.
     */
    void deleteStream(String streamName) throws StreamsException;

    /**
     * Creates or updates a test stream stored in the Configuration Property Store (CPS).
     *
     * @param stream the stream to set into the CPS.
     * @throws StreamsException if there is an issue accessing the CPS.
     */
    void setStream(IStream stream) throws StreamsException;
    
}
