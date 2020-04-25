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
package com.adeptj.runtime.osgi;

import com.adeptj.runtime.common.BundleContextHolder;
import com.adeptj.runtime.common.Environment;
import com.adeptj.runtime.common.Times;
import com.adeptj.runtime.config.Configs;
import com.typesafe.config.Config;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import static org.apache.felix.framework.util.FelixConstants.LOG_LEVEL_PROP;

/**
 * FrameworkManager: Handles the OSGi Framework(Apache Felix) lifecycle such as startup, shutdown etc.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
public enum FrameworkManager {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkManager.class);

    private static final String FRAMEWORK_PROPERTIES = "/framework.properties";

    private static final String FELIX_CM_DIR = "felix.cm.dir";

    private static final String CFG_KEY_FELIX_CM_DIR = "felix-cm-dir";

    private static final String MEM_DUMP_LOC = "felix.memoryusage.dump.location";

    private static final String CFG_KEY_MEM_DUMP_LOC = "memoryusage-dump-loc";

    private Framework framework;

    private FrameworkLifecycleListener frameworkListener;

    public void startFramework() {
        try {
            long startTime = System.nanoTime();
            LOGGER.info("Starting the OSGi Framework!!");
            FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
            this.framework = frameworkFactory.newFramework(this.newFrameworkConfigs());
            long startTimeFramework = System.nanoTime();
            this.framework.start();
            LOGGER.info("OSGi Framework creation took [{}] ms!!", Times.elapsedMillis(startTimeFramework));
            BundleContext systemBundleContext = this.framework.getBundleContext();
            this.frameworkListener = new FrameworkLifecycleListener();
            systemBundleContext.addFrameworkListener(this.frameworkListener);
            BundleContextHolder.getInstance().setBundleContext(systemBundleContext);
            ServiceRegistrations.getInstance().registerLogbackManager(systemBundleContext);
            this.provisionBundles();
            LOGGER.info("OSGi Framework [Apache Felix v{}] started in [{}] ms!!",
                    systemBundleContext.getBundle().getVersion(), Times.elapsedMillis(startTime));
        } catch (Exception ex) { // NOSONAR
            LOGGER.error("Failed to start OSGi Framework!!", ex);
            // Stop the Framework if the Bundles throws exception.
            this.stopFramework();
        }
    }

    public void stopFramework() {
        try {
            if (this.framework == null) {
                LOGGER.info("OSGi Framework not started yet, nothing to stop!!");
            } else {
                this.removeServicesAndListeners();
                this.framework.stop();
                // A value of zero will wait indefinitely.
                FrameworkEvent event = this.framework.waitForStop(0);
                LOGGER.info("OSGi FrameworkEvent: [{}]", FrameworkEvents.asString(event.getType())); // NOSONAR
            }
        } catch (Exception ex) { // NOSONAR
            LOGGER.error("Error Stopping OSGi Framework!!", ex);
        }
    }

    private void removeServicesAndListeners() {
        ServiceRegistrations.getInstance().unregisterLogbackManager();
        BundleContext bundleContext = BundleContextHolder.getInstance().getBundleContext();
        if (bundleContext != null) {
            try {
                LOGGER.info("Removing OSGi FrameworkListener!!");
                bundleContext.removeFrameworkListener(this.frameworkListener);
            } catch (Exception ex) { // NOSONAR
                LOGGER.error(ex.getMessage(), ex);
            }
        }
    }

    private void provisionBundles() throws IOException {
        // config directory will not yet be created if framework is being provisioned first time.
        if (!Boolean.getBoolean("provision.bundles.explicitly")
                && Paths.get(Configs.of().felix().getString(CFG_KEY_FELIX_CM_DIR)).toFile().exists()) {
            LOGGER.info("As per configuration, bundles provisioning is skipped on server restart!!");
        } else {
            new BundleInstaller().installAndStartBundles();
        }
    }

    private Map<String, String> newFrameworkConfigs() {
        Map<String, String> configs = this.loadFrameworkProperties();
        Config felixConf = Configs.of().felix();
        configs.put(FELIX_CM_DIR, felixConf.getString(CFG_KEY_FELIX_CM_DIR));
        configs.put(MEM_DUMP_LOC, felixConf.getString(CFG_KEY_MEM_DUMP_LOC));
        String felixLogLevel = System.getProperty(LOG_LEVEL_PROP);
        if (StringUtils.isNotEmpty(felixLogLevel)) {
            configs.put(LOG_LEVEL_PROP, felixLogLevel);
        }
        LOGGER.debug("OSGi Framework Configurations: {}", configs);
        return configs;
    }

    private Map<String, String> loadFrameworkProperties() {
        Properties props = new Properties();
        Map<String, String> configs = new HashMap<>();
        Path frameworkConfPath = Environment.getFrameworkConfPath();
        if (Environment.isFrameworkConfFileExists()) {
            try (InputStream is = Files.newInputStream(frameworkConfPath)) {
                props.load(is);
                props.forEach((key, val) -> configs.put((String) key, (String) val));
            } catch (IOException ex) {
                LOGGER.error("IOException while loading framework.properties from file system!!", ex);
                // Fallback is try to load the classpath framework.properties
                this.loadClasspathFrameworkProperties(props, configs);
            }
        } else {
            this.loadClasspathFrameworkProperties(props, configs);
            this.createFrameworkPropertiesFile(frameworkConfPath);
        }
        return configs;
    }

    private void loadClasspathFrameworkProperties(Properties props, Map<String, String> configs) {
        try (InputStream is = FrameworkManager.class.getResourceAsStream(FRAMEWORK_PROPERTIES)) {
            props.load(is);
            props.forEach((key, val) -> configs.put((String) key, (String) val));
        } catch (IOException exception) {
            LOGGER.error("IOException while loading framework.properties from classpath!!", exception);
        }
    }

    private void createFrameworkPropertiesFile(Path frameworkConfPath) {
        try (InputStream is = FrameworkManager.class.getResourceAsStream(FRAMEWORK_PROPERTIES)) {
            Files.write(frameworkConfPath, IOUtils.toByteArray(is));
        } catch (IOException ex) {
            LOGGER.error("IOException!!", ex);
        }
    }

    public static FrameworkManager getInstance() {
        return INSTANCE;
    }
}