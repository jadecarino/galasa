/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package demo;

import org.apache.commons.logging.Log;

import dev.galasa.Test;
import dev.galasa.core.CoreManager;
import dev.galasa.core.ICoreManager;
import dev.galasa.core.Logger;
import dev.galasa.core.RunName;

public class SlowCore {
    
    @Logger
    public Log logger;
    
    @Test
    public void delay() throws Exception {
        logger.info("Delaying for 1 minute");
        
        Thread.sleep(60000);
        
        logger.info("Delay finished");
    }

}
