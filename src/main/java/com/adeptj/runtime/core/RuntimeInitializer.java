/*
###############################################################################
#                                                                             # 
#    Copyright 2016, AdeptJ (http://www.adeptj.com)                           #
#                                                                             #
#    Licensed under the Apache License, Version 2.0 (the "License");          #
#    you may not use this file except in compliance with the License.         #
#    You may obtain a copy of the License at                                  #
#                                                                             #
#        http://www.apache.org/licenses/LICENSE-2.0                           #
#                                                                             #
#    Unless required by applicable law or agreed to in writing, software      #
#    distributed under the License is distributed on an "AS IS" BASIS,        #
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. #
#    See the License for the specific language governing permissions and      #
#    limitations under the License.                                           #
#                                                                             #
###############################################################################
*/

package com.adeptj.runtime.core;

import com.adeptj.runtime.common.ServletContextHolder;
import com.adeptj.runtime.exception.RuntimeInitializationException;
import com.adeptj.runtime.osgi.FrameworkShutdownHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.annotation.HandlesTypes;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * An SCI(ServletContainerInitializer) that is called by the Container while startup is in progress.
 * This will further call onStartup method of all of the {@link HandlesTypes} classes registered with this SCI.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
@HandlesTypes(StartupAware.class)
public class RuntimeInitializer implements ServletContainerInitializer {

    private static final String SYS_PROP_SCAN_STARTUP_AWARE_CLASSES = "scan.startup.aware.classes";

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartup(Set<Class<?>> startupAwareClasses, ServletContext context) {
        ServletContextHolder.getInstance().setServletContext(context);
        Logger logger = LoggerFactory.getLogger(this.getClass());
        for (Class<?> startupAwareClass : startupAwareClasses) {
            logger.info("@HandlesTypes: [{}]", startupAwareClass);
            try {
                ((StartupAware) startupAwareClass.getConstructor().newInstance()).onStartup(context);
            } catch (Exception ex) { // NOSONAR
                logger.error("Exception while executing StartupAware#onStartup!!", ex);
                throw new RuntimeInitializationException(ex);
            }
        }
        context.addListener(FrameworkShutdownHandler.class);
        this.handleServiceLoaderBasedStartupAware(context, logger);
    }

    private void handleServiceLoaderBasedStartupAware(ServletContext context, Logger logger) {
        if (Boolean.getBoolean(SYS_PROP_SCAN_STARTUP_AWARE_CLASSES)) {
            for (StartupAware startupAware : ServiceLoader.load(StartupAware.class)) {
                logger.info("Found ServiceLoader based StartupAware: [{}]", startupAware);
                try {
                    startupAware.onStartup(context);
                } catch (Exception ex) { // NOSONAR
                    logger.error("Exception while executing ServiceLoader based StartupAware#onStartup!!", ex);
                }
            }
        }
    }
}
