/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.framework.api.cps.internal.mocks;

import dev.galasa.framework.IFileSystem;
import dev.galasa.framework.api.cps.internal.CpsServlet;
import dev.galasa.framework.spi.IFramework;

/** 
 * A subclass of the servlet we want to test, so that we can inject the mock framework in without 
 * adding any extra code to the production servlet class. The framework field is protected scope, 
 * so a subclass can do the injection instead of the injection framework.
 */
public class MockRasServlet extends CpsServlet implements IServletUnderTest {

	@Override
	public void setFramework(IFramework framework) {
		super.framework = framework;
	}

	@Override
	public void setFileSystem(IFileSystem fileSystem) {
		super.fileSystem = fileSystem;
	}
}
