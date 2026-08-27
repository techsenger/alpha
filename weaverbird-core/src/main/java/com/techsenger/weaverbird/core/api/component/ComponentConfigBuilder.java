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

package com.techsenger.weaverbird.core.api.component;

import com.techsenger.toolkit.core.version.Version;
import com.techsenger.weaverbird.core.api.module.DirectiveType;
import com.techsenger.weaverbird.core.api.module.ModuleConfig;
import com.techsenger.weaverbird.core.api.module.ModuleDirective;
import com.techsenger.weaverbird.core.api.module.ModuleType;
import com.techsenger.weaverbird.core.impl.component.DefaultComponentConfig;
import com.techsenger.weaverbird.core.impl.component.DefaultParentConfig;
import com.techsenger.weaverbird.core.impl.module.DefaultModuleConfig;
import com.techsenger.weaverbird.core.impl.module.DefaultModuleDirective;
import com.techsenger.weaverbird.core.impl.repo.DefaultRepositoryConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 *
 * @author Pavel Castornii
 */
public final class ComponentConfigBuilder {

    private String name;
    private Version version;
    private String type;
    private final Map<String, String> metadata = new LinkedHashMap<>();
    private final List<RepositoryConfig> repositories = new ArrayList<>();
    private final List<ParentConfig> parents = new ArrayList<>();
    private final List<ModuleConfig> modules = new ArrayList<>();

    ComponentConfigBuilder() { }

    public ComponentConfigBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ComponentConfigBuilder version(Version version) {
        this.version = version;
        return this;
    }

    public ComponentConfigBuilder type(String type) {
        this.type = type;
        return this;
    }

    public ComponentConfigBuilder metadata(Map<String, String> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    public ComponentConfigBuilder metadata(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }

    @SafeVarargs
    public final ComponentConfigBuilder repositories(Consumer<RepositoryBuilder>... configurers) {
        for (var configurer : configurers) {
            var builder = new RepositoryBuilder();
            configurer.accept(builder);
            this.repositories.add(builder.build());
        }
        return this;
    }

    @SafeVarargs
    public final ComponentConfigBuilder parents(Consumer<ParentBuilder>... configurers) {
        for (var configurer : configurers) {
            var builder = new ParentBuilder();
            configurer.accept(builder);
            this.parents.add(builder.build());
        }
        return this;
    }

    @SafeVarargs
    public final ComponentConfigBuilder modules(Consumer<ModuleBuilder>... configurers) {
        for (var configurer : configurers) {
            var builder = new ModuleBuilder();
            configurer.accept(builder);
            this.modules.add(builder.build());
        }
        return this;
    }

    public ComponentConfig build() {
        Objects.requireNonNull(name, "Name is required");
        Objects.requireNonNull(version, "Version is required");
        var config = new DefaultComponentConfig(name, version, type);
        config.setMetadata(metadata);
        config.setRepositories(repositories);
        config.setParents(Collections.unmodifiableList(parents));
        config.setModules(modules);
        return config;
    }

    // -------------------------------------------------------------------------

    public static final class RepositoryBuilder {

        private String name;
        private String url;

        private RepositoryBuilder() { }

        public RepositoryBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RepositoryBuilder url(String url) {
            this.url = url;
            return this;
        }

        private RepositoryConfig build() {
            Objects.requireNonNull(name, "Repository name is required");
            Objects.requireNonNull(url, "Repository url is required");
            return new DefaultRepositoryConfig(name, url);
        }
    }

    // -------------------------------------------------------------------------

    public static final class ParentBuilder {

        private String name;
        private Version version;
        private VersionMatch versionMatch = VersionMatch.ANY;
        private boolean optional = false;

        private ParentBuilder() { }

        public ParentBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ParentBuilder version(Version version) {
            this.version = version;
            return this;
        }

        public ParentBuilder versionMatch(VersionMatch versionMatch) {
            this.versionMatch = versionMatch;
            return this;
        }

        public ParentBuilder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        private ParentConfig build() {
            Objects.requireNonNull(name, "Parent name is required");
            Objects.requireNonNull(version, "Parent version is required");
            return new DefaultParentConfig(name, version, versionMatch, optional);
        }
    }

    // -------------------------------------------------------------------------

    public static final class ModuleBuilder {

        private String groupId;
        private String artifactId;
        private String version;
        private String classifier;
        private ModuleType type = ModuleType.JAR;
        private boolean active = false;
        private boolean nativeAccessEnabled = false;
        private final List<ModuleDirective> directives = new ArrayList<>();

        private ModuleBuilder() { }

        public ModuleBuilder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public ModuleBuilder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public ModuleBuilder version(String version) {
            this.version = version;
            return this;
        }

        public ModuleBuilder classifier(String classifier) {
            this.classifier = classifier;
            return this;
        }

        public ModuleBuilder type(ModuleType type) {
            this.type = type;
            return this;
        }

        public ModuleBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public ModuleBuilder nativeAccess(boolean enabled) {
            this.nativeAccessEnabled = enabled;
            return this;
        }

        @SafeVarargs
        public final ModuleBuilder directives(Consumer<DirectiveBuilder>... configurers) {
            for (var configurer : configurers) {
                var builder = new DirectiveBuilder();
                configurer.accept(builder);
                this.directives.add(builder.build());
            }
            return this;
        }

        private ModuleConfig build() {
            Objects.requireNonNull(artifactId, "ArtifactId is required");
            Objects.requireNonNull(groupId, "GroupId is required");
            Objects.requireNonNull(version, "Module version is required");
            var module = new DefaultModuleConfig(groupId, artifactId, version, classifier, type);
            module.setActive(active);
            module.setNativeAccessEnabled(nativeAccessEnabled);
            module.setDirectives(List.copyOf(directives));
            return module;
        }
    }

    // -------------------------------------------------------------------------

    public static final class DirectiveBuilder {

        private DirectiveType type;
        private String pkg;
        private String module;
        private String layer;

        private DirectiveBuilder() { }

        public DirectiveBuilder type(DirectiveType type) {
            this.type = type;
            return this;
        }

        public DirectiveBuilder pkg(String pkg) {
            this.pkg = pkg;
            return this;
        }

        public DirectiveBuilder module(String module) {
            this.module = module;
            return this;
        }

        public DirectiveBuilder layer(String layer) {
            this.layer = layer;
            return this;
        }

        private ModuleDirective build() {
            Objects.requireNonNull(type, "Directive type is required");
            return new DefaultModuleDirective(type, pkg, module, layer);
        }
    }
}
