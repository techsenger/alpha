/*
 * Copyright 2018-2026 Pavel Castornii.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.techsenger.weaverbird.core.impl;

import com.techsenger.toolkit.core.StringUtils;
import com.techsenger.toolkit.core.version.Version;
import com.techsenger.weaverbird.core.api.ComponentManager;
import com.techsenger.weaverbird.core.api.Constants;
import com.techsenger.weaverbird.core.api.Framework;
import com.techsenger.weaverbird.core.api.component.Component;
import com.techsenger.weaverbird.core.api.component.ComponentConfig;
import com.techsenger.weaverbird.core.api.component.ComponentConfigInfo;
import com.techsenger.weaverbird.core.api.component.ComponentConfigUtils;
import com.techsenger.weaverbird.core.api.component.ComponentDescriptor;
import com.techsenger.weaverbird.core.api.component.ComponentException;
import com.techsenger.weaverbird.core.api.component.ComponentObserver;
import com.techsenger.weaverbird.core.api.component.DeployPlan;
import com.techsenger.weaverbird.core.api.component.ParentConfig;
import com.techsenger.weaverbird.core.api.component.UnknownComponentException;
import com.techsenger.weaverbird.core.api.module.ArtifactEventListener;
import com.techsenger.weaverbird.core.api.module.ModuleArtifact;
import com.techsenger.weaverbird.core.api.module.ModuleConfig;
import com.techsenger.weaverbird.core.api.registry.ComponentEntry;
import com.techsenger.weaverbird.core.api.state.ComponentsState;
import com.techsenger.weaverbird.core.api.state.DefaultComponentsState;
import com.techsenger.weaverbird.core.impl.component.ConfigXmlReader;
import com.techsenger.weaverbird.core.impl.component.ConfigXmlWriter;
import com.techsenger.weaverbird.core.impl.component.DefaultComponent;
import com.techsenger.weaverbird.core.impl.component.DefaultComponentDescriptor;
import com.techsenger.weaverbird.core.impl.registry.DefaultRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pavel Castornii
 */
public class DefaultComponentManager implements ComponentManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultComponentManager.class);

    private final Framework framework;

    private final LayerBuilder layerBuilder;

    private final ModuleActivatorManager activatorManager;

    private final List<ComponentListener> listeners = new ArrayList();

    private volatile int componentIdCounter = 0;

    private final DefaultComponentsState componentsState = new DefaultComponentsState();

    /**
     * Running components by component id.
     */
    private final Map<Integer, Component> componentsById = new ConcurrentHashMap<>();

    private final ComponentConfigInfo configInfo = new ComponentConfigInfo();

    private final ComponentConfigUtils configUtils = new ComponentConfigUtils();

    public DefaultComponentManager(Framework framework) {
        this.framework = framework;
        this.layerBuilder = new LayerBuilder(framework);
        this.activatorManager = new ModuleActivatorManager(framework);
    }

    @Override
    public synchronized Path buildComponent(String name, Version version, Path path, String extension)
            throws ComponentException, UnknownComponentException {
        ComponentConfig config = readConfig(name, version);
        return buildComponent(config, path, extension);
    }

    @Override
    public synchronized Path buildComponent(ComponentConfig config, Path path, String extension)
            throws ComponentException {
        notifyObservers((o) -> o.onBuilding(config), null);
        var fileManager = new ComponentFileManager(framework.getPathManager(), this.configInfo, this.configUtils);
        var archivePath = fileManager.build(config, path, extension);
        notifyObservers((o) -> o.onBuilt(config), null);
        logger.info("Built component {}{}{} at {}", config.getName(),
                Constants.NAME_VERSION_SEPARATOR, config.getVersion(), archivePath);
        return archivePath;
    }

    @Override
    public synchronized ComponentConfig addComponent(Path zipPath) throws ComponentException {
        var fileManager = new ComponentFileManager(framework.getPathManager(), this.configInfo, this.configUtils);
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            var config = fileManager.readConfig(zipFile);
            if (getRegistry().isComponentAdded(config.getName(), config.getVersion())) {
                throw new ComponentException(StringUtils.format("Component {}{}{} is already added",
                        config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion()));
            }
            notifyObservers((o) -> o.onAdding(config), null);
            fileManager.addComponent(config, zipPath, zipFile);
            getRegistry().getModifiableAddedComponents().add(new ComponentEntry(config));
            getRegistry().save();
            notifyObservers((o) -> o.onAdded(config), null);
            logger.info("Added component {}{}{} from {}; total added components: {}", config.getName(),
                    Constants.NAME_VERSION_SEPARATOR, config.getVersion(), zipPath,
                    getRegistry().getModifiableAddedComponents().size());
            return config;
        } catch (IOException ex) {
            throw new ComponentException(StringUtils.format("Error adding component from {}", zipPath), ex);
        }
    }

    @Override
    public synchronized ComponentConfig addComponent(String xmlConfig) throws ComponentException {
        ConfigXmlReader xmlReader = new ConfigXmlReader();
        ComponentConfig config = null;
        try {
            InputStream is = new ByteArrayInputStream(xmlConfig.getBytes(StandardCharsets.UTF_8));
            config = xmlReader.read(is, this.configInfo, this.configUtils);
        } catch (Exception ex) {
            throw new ComponentException("Error reading provided XML configuration", ex);
        }
        addComponent(config, xmlConfig);
        return config;
    }

    @Override
    public synchronized String addComponent(ComponentConfig config) throws ComponentException {
        ConfigXmlWriter xmlWriter = new ConfigXmlWriter();
        String xmlConfig = null;
        try {
            var writer = new StringWriter();
            xmlWriter.write(config, writer);
            xmlConfig = writer.toString();
        } catch (Exception ex) {
            throw new ComponentException("Error writing provided configuration", ex);
        }
        addComponent(config, xmlConfig);
        return xmlConfig;
    }

    @Override
    public synchronized ComponentConfig resolveComponent(String name, Version version,
            ArtifactEventListener listener) throws ComponentException, UnknownComponentException {
        var config = readConfig(name, version);
        resolveComponent(config, listener);
        return config;
    }

    @Override
    public synchronized void resolveComponent(ComponentConfig config, ArtifactEventListener listener)
            throws ComponentException {
        //check if component is not resolved yet.
        if (getRegistry().isComponentResolved(config.getName(), config.getVersion())) {
            throw new ComponentException(StringUtils.format("Component {}{}{} is already resolved",
                    config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion()));
        }
        //there can be situations when all files are provided in archive
        boolean resolved = false;
        if (config.getRepositories().isEmpty()) {
            resolved = true;
        } else {
            var repoService = framework.getRepoService();
            if (repoService == null) {
                throw new IllegalStateException("No repo service provided");
            }
            notifyObservers((o) -> o.onResolving(config), null);
            Map<String, String> reposByName = new LinkedHashMap<>(); //keeping order!
            config.getRepositories().forEach(r -> reposByName.put(r.getName(), r.getUrl()));
            resolved = repoService.resolve(reposByName, (List<ModuleArtifact>) (List<?>) config.getModules(), listener);
        }
        if (resolved) {
            getRegistry().getModifiableResolvedComponents().add(new ComponentEntry(config));
            getRegistry().save();
            notifyObservers((o) -> o.onResolved(config), null);
            logger.info("Resolved component {}{}{}; total resolved components: {}", config.getName(),
                    Constants.NAME_VERSION_SEPARATOR, config.getVersion(),
                    getRegistry().getModifiableResolvedComponents().size());
        } else {
            throw new ComponentException(StringUtils.format("Error resolving {}{}{}",
                    config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion()));
        }
    }

    @Override
    public synchronized ComponentDescriptor deployComponent(final String name, final Version version)
            throws ComponentException, UnknownComponentException {
        return deployComponent(name, version, null, null, null, false);
    }

    @Override
    public synchronized ComponentDescriptor deployComponent(final String name, final Version version,
            final String alias) throws ComponentException, UnknownComponentException {
        return deployComponent(name, version, alias, null, null, false);
    }

    @Override
    public synchronized ComponentDescriptor deployComponent(final String name, final Version version,
            final String alias, final List<Integer> parentIds, final List<String> parentAliases,
            boolean useParentClassLoader) throws ComponentException, UnknownComponentException {
        var config = readConfig(name, version);
        return deployComponent(config, alias, parentIds, parentAliases, useParentClassLoader);
    }

    @Override
    public synchronized ComponentDescriptor deployComponent(ComponentConfig config, final String alias,
            final List<Integer> parentIds, final List<String> parentAliases, boolean useParentClassLoader) throws
            ComponentException, UnknownComponentException {
        List<DefaultComponentDescriptor> parentDescriptors = new ArrayList<>();
        if (parentIds != null) {
            for (Integer parentId : parentIds) {
                parentDescriptors.add(provideDescriptor(parentId));
            }
        }
        if (parentAliases != null) {
            for (String parentAlias : parentAliases) {
                parentDescriptors.add(provideDescriptor(parentAlias));
            }
        }
        try {
            matchParents(parentDescriptors, config.getParents());
            final DefaultComponentDescriptor descriptor = new DefaultComponentDescriptor(this, config, alias,
                    ++componentIdCounter, parentDescriptors, useParentClassLoader);
            DefaultComponent component = new DefaultComponent(descriptor);
            //here we need to check if component modules are not loaded in boot layer...
            notifyObservers((o) -> o.onDeploying(config), null); //doesn't throw exceptions
            layerBuilder.build(component);
            componentsById.put(component.getDescriptor().getId(), component);
            for (var l : listeners) {
                l.onComponentDeployed(component); //doesn't throw exceptions
            }
            notifyObservers((o) -> o.onDeployed(component), component.getDescriptor().getId()); //no exceptions
            this.componentsState.setDeployedCount(this.componentsById.size());
            this.componentsState.setId(this.componentsState.getId() + 1);
            logger.info("Deployed component {}{}{} with id: {}; total deployed components: {}; componentsStateId: {}",
                config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion(),
                component.getDescriptor().getId(), this.componentsById.size(), this.componentsState.getId());
            return descriptor;
        } catch (Exception ex) {
            throw new ComponentException(StringUtils.format("Failed to deploy component {}{}{}", config.getName(),
                    Constants.NAME_VERSION_SEPARATOR, config.getVersion()), ex);
        }
    }

    private void matchParents(List<DefaultComponentDescriptor> descriptors, List<ParentConfig> configs)
            throws ComponentException {
        if (descriptors.isEmpty() && configs.isEmpty()) {
            return;
        }
        // An optional parent config (see ParentConfig#isOptional()) may legitimately have no provided
        // descriptor, so it's dropped before matching - ConfigMatcher itself treats every config it receives
        // as mandatory.
        var mandatoryConfigs = configs.stream().filter(c -> !c.isOptional()).toList();
        if (descriptors.size() > configs.size()) {
            throw new ComponentException("The provided parents don't match the parents in the configuration");
        }
        var providedConfigs = descriptors.stream().map(d -> d.getConfig()).toList();
        var notMatched = ConfigMatcher.findFirstUnsatisfied(providedConfigs, mandatoryConfigs);
        if (notMatched != null) {
            throw new ComponentException(StringUtils.format("Didn't find parent - {}{}{} " + notMatched.getName(),
                    Constants.NAME_VERSION_SEPARATOR, notMatched.getVersion()));
        }
    }

    @Override
    public synchronized ComponentDescriptor activateComponent(final int id)
            throws ComponentException, UnknownComponentException {
        var desriptor = provideDescriptor(id);
        activateComponent(desriptor);
        return desriptor;
    }

    @Override
    public synchronized ComponentDescriptor activateComponent(final String alias)
            throws ComponentException, UnknownComponentException {
        var desriptor = provideDescriptor(alias);
        activateComponent(desriptor);
        return desriptor;
    }

    @Override
    public synchronized void activateComponent(ComponentDescriptor descriptor) throws ComponentException {
        DefaultComponent component = (DefaultComponent) getComponent(descriptor.getId());
        var config = descriptor.getConfig();
        if (descriptor.isActivated()) {
            throw new ComponentException(StringUtils.format("Component {}{}{} with id: {}, is already activated",
                    config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion(), descriptor.getId()));
        }
        //we need to activate activators with their layer classloader, but not of the weaverbird
        //saving classloader
        ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            notifyObservers((o) -> o.onActivating(component), component.getDescriptor().getId());
            activatorManager.activateActivators(component); //throws exceptions
            component.getDescriptor().setActivated(true);
            notifyObservers((o) -> o.onActivated(component), component.getDescriptor().getId());
            this.componentsState.setActivatedCount(this.componentsState.getActivatedCount() + 1);
            this.componentsState.setId(this.componentsState.getId() + 1);
            logger.info("Activated component {}{}{} with id: {}; total activated components: {}; componentsStateId: {}",
                config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion(), descriptor.getId(),
                this.componentsState.getActivatedCount(), this.componentsState.getId());
        } catch (Exception ex) {
            throw new ComponentException("Some modules couldn't be activated", ex);
        } finally {
            //restoring here if we had exceptions in activators.
            Thread.currentThread().setContextClassLoader(initialClassLoader);
        }
    }

    @Override
    public synchronized ComponentDescriptor deactivateComponent(int id)
            throws ComponentException, UnknownComponentException {
        var descriptor = provideDescriptor(id);
        deactivateComponent(descriptor);
        return descriptor;
    }

    @Override
    public synchronized ComponentDescriptor deactivateComponent(String alias)
            throws ComponentException, UnknownComponentException {
        var descriptor = provideDescriptor(alias);
        deactivateComponent(descriptor);
        return descriptor;
    }

    @Override
    public synchronized void deactivateComponent(ComponentDescriptor descriptor)
            throws ComponentException {
        DefaultComponent component = (DefaultComponent) getComponent(descriptor.getId());
        var config = descriptor.getConfig();
        if (!descriptor.isActivated()) {
            throw new ComponentException(StringUtils.format("Component {}{}{} with id: {} not activated",
                    config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion(), descriptor.getId()));
        }
        //we need to deactivate activators with their layer classloader, but not of the weaverbird
        //saving classloader
        ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            notifyObservers((o) -> o.onDeactivating(component), descriptor.getId());
            this.activatorManager.deactivateActivators(component);
            component.getDescriptor().setActivated(false);
            notifyObservers((o) -> o.onDeactivated(component), descriptor.getId());
            this.componentsState.setActivatedCount(this.componentsState.getActivatedCount() - 1);
            this.componentsState.setId(this.componentsState.getId() + 1);
            logger.info("Deactivated component {}{}{} with id: {}; total deployed components: {};"
                    + " componentsStateId: {}", config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion(),
                    descriptor.getId(), this.componentsById.size(), this.componentsState.getId());
        } catch (Exception ex) {
            throw new ComponentException("Some modules couldn't be stopped", ex);
        } finally {
            //restoring here if we had exceptions in activators.
            Thread.currentThread().setContextClassLoader(initialClassLoader);
        }
    }

    @Override
    public synchronized ComponentDescriptor undeployComponent(final int id)
            throws ComponentException, UnknownComponentException {
        var descriptor = provideDescriptor(id);
        undeployComponent(descriptor);
        return descriptor;
    }

    @Override
    public synchronized ComponentDescriptor undeployComponent(final String alias)
            throws ComponentException, UnknownComponentException {
        var descriptor = provideDescriptor(alias);
        undeployComponent(descriptor);
        return descriptor;
    }

    @Override
    public synchronized void undeployComponent(ComponentDescriptor descriptor) throws ComponentException {
        var config = descriptor.getConfig();
        try {
            DefaultComponent component = (DefaultComponent) getComponent(descriptor.getId());
            notifyObservers((o) -> o.onUndeploying(component), component.getDescriptor().getId());
            for (var l : listeners) {
                l.onComponentUndeployed(component);
            }
            componentsById.remove(component.getDescriptor().getId());
            notifyObservers((o) -> o.onUndeployed(component), component.getDescriptor().getId());
            this.componentsState.setDeployedCount(this.componentsById.size());
            this.componentsState.setId(this.componentsState.getId() + 1);
            logger.info("Undeployed component {}{}{} with id: {}; total deployed components: {}; componentsStateId: {}",
                config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion(), descriptor.getId(),
                this.componentsById.size(), this.componentsState.getId());
        } catch (Exception ex) {
            throw new ComponentException(StringUtils.format("Failed to undeploy component {}{}{}", config.getName(),
                    Constants.NAME_VERSION_SEPARATOR, config.getVersion()), ex);
        }
    }

    @Override
    public synchronized ComponentConfig unresolveComponent(String name, Version version,
            ArtifactEventListener listener) throws ComponentException, UnknownComponentException {
        var config = readConfig(name, version);
        unresolveComponent(config, listener);
        return config;
    }

    @Override
    public synchronized void unresolveComponent(ComponentConfig config, ArtifactEventListener listener)
            throws ComponentException {
        //check if component is not resolved yet.
        var regEntryIndex = getRegistry().getModifiableResolvedComponents().indexOf(new ComponentEntry(config));
        if (regEntryIndex == -1) {
            throw new ComponentException(StringUtils.format("Component {}{}{} is not resolved",
                    config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion()));
        }
        var repoService = framework.getRepoService();
        if (repoService == null) {
            throw new IllegalStateException("No repo service provided");
        }
        //now we need all installed configs
        List<ComponentConfig> otherResolvedConfigs = new ArrayList<>();
        for (var entry : getRegistry().getModifiableResolvedComponents()) {
            try {
                ComponentConfig someConfig = readConfig(entry.getName(), entry.getVersion());
                if (!config.equals(someConfig)) {
                    otherResolvedConfigs.add(someConfig);
                }
            } catch (Exception e) {
                throw new ComponentException(StringUtils
                        .format("Component {}{}{} is in registry, but its config not found", entry.getName(),
                        Constants.NAME_VERSION_SEPARATOR, entry.getVersion()), e);
            }
        }
        //now we need to find the modules that are used by other installed components
        Set<ModuleConfig> otherConfigModules = new HashSet<>();
        for (var otherConfig : otherResolvedConfigs) {
            for (var module : otherConfig.getModules()) {
                otherConfigModules.add(module);
            }
        }
        List<ModuleConfig> unresolvedModules = new ArrayList<>();
        //now we can find modules that must be uninstalled
        for (var module : config.getModules()) {
            if (!otherConfigModules.contains(module)) {
                unresolvedModules.add(module);
            }
        }
        boolean result;
        if (unresolvedModules.isEmpty()) {
            result = true;
        } else {
            notifyObservers((o) -> o.onUnresolving(config), null);
            result = repoService.unresolve((List) unresolvedModules, listener);
            notifyObservers((o) -> o.onUnresolved(config), null);
        }
        if (result) {
            getRegistry().getModifiableResolvedComponents().remove(regEntryIndex);
            getRegistry().save();
            logger.info("Unresolved component {}{}{}; total resolved components: {}",
                    config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion(),
                    getRegistry().getModifiableResolvedComponents().size());
        }
    }

    @Override
    public synchronized ComponentConfig removeComponent(String name, Version version)
            throws ComponentException, UnknownComponentException {
        var config = readConfig(name, version);
        removeComponent(config);
        return config;
    }

    @Override
    public synchronized void removeComponent(ComponentConfig config) throws ComponentException {
        var regEntryIndex = getRegistry().getModifiableAddedComponents().indexOf(new ComponentEntry(config));
        if (regEntryIndex == -1) {
            throw new ComponentException(StringUtils.format("Component {}{}{} is not added",
                    config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion()));
        }
        notifyObservers((o) -> o.onRemoving(config), null);
        var fileManager = new ComponentFileManager(framework.getPathManager(), this.configInfo, this.configUtils);
        fileManager.removeComponent(config);
        getRegistry().getModifiableAddedComponents().remove(regEntryIndex);
        getRegistry().save();
        notifyObservers((o) -> o.onRemoved(config), null);
        logger.info("Removed component {}{}{}; total added components: {}",
                config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion(),
                getRegistry().getModifiableAddedComponents().size());
    }

    @Override
    public synchronized ComponentConfig installComponent(Path path, ArtifactEventListener listener)
            throws ComponentException {
        var config = addComponent(path);
        resolveComponent(config, listener);
        return config;
    }

    @Override
    public synchronized ComponentConfig installComponent(String xmlConfig, ArtifactEventListener listener)
            throws ComponentException {
        var config = addComponent(xmlConfig);
        resolveComponent(config, listener);
        return config;
    }

    @Override
    public String installComponent(ComponentConfig config, ArtifactEventListener listener)
            throws ComponentException {
        var xmlConfig = addComponent(config);
        resolveComponent(config, listener);
        return xmlConfig;
    }

    @Override
    public synchronized ComponentDescriptor startComponent(final String name, final Version version)
            throws ComponentException, UnknownComponentException {
        var component = deployComponent(name, version);
        activateComponent(component);
        return component;
    }

    @Override
    public synchronized ComponentDescriptor startComponent(final String name, final Version version,
            final String alias) throws ComponentException, UnknownComponentException {
        return startComponent(name, version, alias, null, null, false);
    }

    @Override
    public synchronized ComponentDescriptor startComponent(final String name, final Version version,
            final String alias, final List<Integer> parentIds, final List<String> parentAliases,
            boolean useParentClassLoader) throws ComponentException, UnknownComponentException {
        var config = readConfig(name, version);
        return startComponent(config, alias, parentIds, parentAliases, useParentClassLoader);
    }

    @Override
    public synchronized ComponentDescriptor startComponent(ComponentConfig config, String alias,
            List<Integer> parentIds, List<String> parentAliases, boolean useParentClassLoader)
            throws ComponentException, UnknownComponentException {
        var descriptor = deployComponent(config, alias, parentIds, parentAliases, useParentClassLoader);
        activateComponent(descriptor);
        return descriptor;
    }

    @Override
    public synchronized ComponentDescriptor stopComponent(final int id)
            throws ComponentException, UnknownComponentException {
        var descriptor = provideDescriptor(id);
        stopComponent(descriptor);
        return descriptor;
    }

    @Override
    public synchronized ComponentDescriptor stopComponent(final String alias)
            throws ComponentException, UnknownComponentException {
        var descriptor = provideDescriptor(alias);
        stopComponent(descriptor);
        return descriptor;
    }

    @Override
    public synchronized void stopComponent(ComponentDescriptor descriptor) throws ComponentException {
        deactivateComponent(descriptor);
        undeployComponent(descriptor);
    }

    @Override
    public synchronized ComponentConfig uninstallComponent(final String name, final Version version,
            ArtifactEventListener listener) throws ComponentException, UnknownComponentException  {
        var config = readConfig(name, version);
        uninstallComponent(config, listener);
        return config;
    }

    @Override
    public synchronized void uninstallComponent(final ComponentConfig config, ArtifactEventListener listener)
            throws ComponentException  {
        unresolveComponent(config, listener);
        removeComponent(config);
    }

    @Override
    public Component getComponent(int id) {
        return componentsById.get(id);
    }

    @Override
    public Collection<Component> getComponents() {
        return Collections.unmodifiableCollection(componentsById.values());
    }

    @Override
    public Collection<ComponentDescriptor> getDescriptors() {
        return componentsById.values().stream().map(c -> c.getDescriptor()).collect(Collectors.toList());
    }

    @Override
    public Component findComponent(final ModuleLayer layer) {
        Component component = null;
        for (var current : componentsById.values()) {
            if (current.getLayer() == layer) {
                component = current;
                break;
            }
        }
        return component;
    }

    @Override
    public Component findComponent(String alias) {
        Component component = null;
        for (var c : this.componentsById.values()) {
            var d = c.getDescriptor();
            if (d.getAlias() != null && d.getAlias().equals(alias)) {
                component = c;
                break;
            }
        }
        return component;
    }

    @Override
    public Collection<Component> findComponents(String name, Version version) {
        return this.componentsById.values().stream()
                .filter(e -> e.getDescriptor().getConfig().getName().equals(name)
                        && e.getDescriptor().getConfig().getVersion().equals(version))
                .collect(Collectors.toList());
    }

    @Override
    public ComponentConfig readConfig(String name, Version version)
            throws ComponentException, UnknownComponentException {
        Path componentConfigPath = framework.getPathManager().getPathResolver().resolveConfigFile(name, version);
        if (!getRegistry().isComponentAdded(name, version)) {
            throw new UnknownComponentException(StringUtils.format("Component {}{}{} not found", name,
                    Constants.NAME_VERSION_SEPARATOR, version));
        }
        if (!Files.exists(componentConfigPath)) {
            throw new UnknownComponentException(StringUtils.format("Not found config file for {}{}{} at {}",
                name, Constants.NAME_VERSION_SEPARATOR, version, componentConfigPath));
        }
        ConfigXmlReader xmlReader = new ConfigXmlReader();
        try {
            var config = xmlReader.read(componentConfigPath, this.configInfo, this.configUtils);
            return config;
        } catch (Exception ex) {
            throw new ComponentException(StringUtils.format("Error reading configuration for {}{}{}",
                    name, Constants.NAME_VERSION_SEPARATOR, version), ex);
        }
    }

    @Override
    public synchronized DeployPlan createDeployPlan(List<ComponentConfig> configs) throws ComponentException {
        return DeployPlanner.createPlan(new ArrayList<>(getDescriptors()), configs, componentsState.getId(),
                componentIdCounter);
    }

    @Override
    public synchronized ComponentsState getComponentsState() {
        return new DefaultComponentsState(this.componentsState.getId(), this.componentsState.getDeployedCount(),
                this.componentsState.getActivatedCount());
    }

    @Override
    public ComponentConfigInfo getConfigInfo() {
        return configInfo;
    }

    @Override
    public ComponentConfigUtils getConfigUtils() {
        return configUtils;
    }

    public synchronized void addListener(ComponentListener listener) {
        this.listeners.add(listener);
    }

    public synchronized void removeListener(ComponentListener listener) {
        this.listeners.remove(listener);
    }

    private void addComponent(ComponentConfig config, String xmlConfig) throws ComponentException {
        if (getRegistry().isComponentAdded(config.getName(), config.getVersion())) {
            throw new ComponentException(StringUtils.format("Component {}{}{} is already added",
                    config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion()));
        }
        var finalConfig = config;
        notifyObservers((o) -> o.onAdding(finalConfig), null);
        var fileManager = new ComponentFileManager(framework.getPathManager(), this.configInfo, this.configUtils);
        try {
            fileManager.addComponent(config, xmlConfig);
        } catch (Exception ex) {
            throw new ComponentException(StringUtils.format("Error adding component {}{}{} with XML configuration",
                    config.getName(), Constants.NAME_VERSION_SEPARATOR, config.getVersion()), ex);
        }
        getRegistry().getModifiableAddedComponents().add(new ComponentEntry(config));
        getRegistry().save();
        notifyObservers((o) -> o.onAdded(finalConfig), null);
        logger.info("Added component {}{}{} with XML configuration; total added components: {}", config.getName(),
                Constants.NAME_VERSION_SEPARATOR, config.getVersion(),
                getRegistry().getModifiableAddedComponents().size());
    }

    /**
     * Notifies component observers.
     *
     * @param consumer
     * @param eventComponentId the id of the component on which the event was invoked, or null if there is no component
     */
    private void notifyObservers(final Consumer<ComponentObserver> consumer, Integer eventComponentId) {
        ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
        logger.trace("ClassLoader before observers: {}", initialClassLoader);
        //call component observers
        for (Component c : this.componentsById.values()) {
            DefaultComponent component = (DefaultComponent) c;
            //started component knows in activators
            if (eventComponentId != null && component.getDescriptor().getId() == eventComponentId) {
                continue;
            }
            if (component.getObservers() == null || component.getObservers().isEmpty()) {
                continue;
            }
            try {
                component.getObservers().forEach(e -> {
                    Thread.currentThread().setContextClassLoader(e.getClass().getModule().getClassLoader());
                    consumer.accept(e);
                });
            } catch (Exception ex) {
                logger.error("Error calling component observer", ex);
            } finally {
                Thread.currentThread().setContextClassLoader(initialClassLoader);
            }
        }
    }

    private DefaultComponentDescriptor provideDescriptor(final int id) throws UnknownComponentException {
        var component = componentsById.get(id);
        if (component == null) {
            throw new UnknownComponentException(StringUtils.format("Component with id:{} wasn't found", id));
        }
        return (DefaultComponentDescriptor) component.getDescriptor();
    }

    private DefaultComponentDescriptor provideDescriptor(final String alias) throws UnknownComponentException {
        var component = findComponent(alias);
        if (component == null) {
            throw new UnknownComponentException(StringUtils.format("Component with alias:{} wasn't found", alias));
        }
        return (DefaultComponentDescriptor) component.getDescriptor();
    }

    private DefaultRegistry getRegistry() {
        return (DefaultRegistry) this.framework.getRegistry();
    }
}
