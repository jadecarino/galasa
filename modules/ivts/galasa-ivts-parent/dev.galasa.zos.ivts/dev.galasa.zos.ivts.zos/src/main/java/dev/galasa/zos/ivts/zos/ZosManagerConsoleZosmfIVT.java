/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.zos.ivts.zos;

import static org.assertj.core.api.Assertions.*;

import dev.galasa.Test;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos.ZosImage;
import dev.galasa.zosconsole.IZosConsole;
import dev.galasa.zosconsole.ZosConsole;

@Test
public class ZosManagerConsoleZosmfIVT {

    @ZosImage(imageTag="PRIMARY")
    public IZosImage zosImageA;

    @ZosConsole(imageTag="PRIMARY")
    public IZosConsole zosConsoleA;

    @Test
    public void preFlightTests() {
        assertThat(zosImageA).isNotNull();
        assertThat(zosConsoleA).isNotNull();
    }

}
