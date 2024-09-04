/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import java.util.*;
import static org.assertj.core.api.Assertions.*;

import org.apache.http.*;
import org.apache.http.client.methods.HttpPost;
import org.junit.*;
import org.junit.rules.TestName;

import dev.galasa.framework.spi.utils.GalasaGson;
import dev.galasa.extensions.common.couchdb.pojos.Welcome;
import dev.galasa.extensions.common.impl.HttpRequestFactoryImpl;
import dev.galasa.extensions.common.couchdb.CouchdbException;
import dev.galasa.extensions.common.couchdb.CouchdbValidator;
import dev.galasa.extensions.common.api.HttpRequestFactory;
import dev.galasa.extensions.mocks.*;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures.BaseHttpInteraction;;


public class CouchDbVersionTest {

    @Test
    public void testCanCreateAVersion() {
        CouchDbVersion version = new CouchDbVersion(1,2,3);
        assertThat(version.getVersion()).isEqualTo(1);
        assertThat(version.getRelease()).isEqualTo(2);
        assertThat(version.getModification()).isEqualTo(3);        
    }

    @Test
    public void testCanCreateAVersionFromAString() throws Exception {
        CouchDbVersion version = new CouchDbVersion("1.2.3");
        assertThat(version.getVersion()).isEqualTo(1);
        assertThat(version.getRelease()).isEqualTo(2);
        assertThat(version.getModification()).isEqualTo(3);        
    }

    @Test
    public void testInvalidVersionStringThrowsParsingError() throws Exception {
        CouchdbException ex = catchThrowableOfType( ()->{ new CouchDbVersion("1.2..3"); },
             CouchdbException.class );
        assertThat(ex).hasMessageContaining("1.2..3");
        // TODO: Assert that more of the message is in here.
    }

    @Test
    public void testInvalidLeadingDotVersionStringThrowsParsingError() throws Exception {
        CouchdbException ex = catchThrowableOfType( ()->{ new CouchDbVersion(".1.2.3"); },
             CouchdbException.class );
        assertThat(ex).hasMessageContaining(".1.2.3");
        // TODO: Assert that more of the message is in here.
    }


    @Test
    public void testCanICompareTheSameThingIsEqualToItself() throws Exception {
        CouchDbVersion version = new CouchDbVersion("1.2.3");
        assertThat(version.equals(version)).as("Could not compare the version with itself.").isTrue();
    }

    @Test
    public void testHashCodeOfTwoSameVersionsIsTheSame() throws Exception {
        CouchDbVersion version1 = new CouchDbVersion("1.2.3");
        CouchDbVersion version2 = new CouchDbVersion("1.2.3");
        assertThat(version1.hashCode()).isEqualTo(version2.hashCode());
    }

    @Test
    public void testTwoSameVersionsDifferentObjectsAreTheSame() throws Exception {
        CouchDbVersion version1 = new CouchDbVersion("1.2.3");
        CouchDbVersion version2 = new CouchDbVersion("1.2.3");
        assertThat(version1).isEqualTo(version2);
    }

    @Test
    public void testAddingTwoSameVersionsDifferentObjectsIntoASetResultInOneObjectInTheSet() throws Exception {
        CouchDbVersion version1 = new CouchDbVersion("1.2.3");
        CouchDbVersion version2 = new CouchDbVersion("1.2.3");
        Set<CouchDbVersion> mySet = new HashSet<>();
        mySet.add(version1);
        mySet.add(version2);

        assertThat(mySet).hasSize(1);
    }
}