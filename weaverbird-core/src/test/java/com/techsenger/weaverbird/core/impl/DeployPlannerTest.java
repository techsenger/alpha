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

import com.techsenger.toolkit.core.version.Version;
import com.techsenger.weaverbird.core.api.component.ComponentConfig;
import com.techsenger.weaverbird.core.api.component.ComponentDescriptor;
import com.techsenger.weaverbird.core.api.component.ComponentException;
import com.techsenger.weaverbird.core.api.component.ParentConfig;
import com.techsenger.weaverbird.core.api.component.RepositoryConfig;
import com.techsenger.weaverbird.core.api.component.VersionMatch;
import com.techsenger.weaverbird.core.api.module.ModuleConfig;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Pavel Castornii
 */
class DeployPlannerTest {

    // -------------------------------------------------------------------------
    // Test doubles
    // -------------------------------------------------------------------------

    record SimpleConfig(String name, Version version, List<ParentConfig> parents) implements ComponentConfig {
        @Override public String getName() {
            return name;
        }

        @Override public Version getVersion() {
            return version;
        }

        @Override public List<ParentConfig> getParents() {
            return parents;
        }

        @Override
        public Map<String, String> getMetadata() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RepositoryConfig> getRepositories() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ModuleConfig> getModules() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getFullName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getType() {
            throw new UnsupportedOperationException();
        }
    }

    record SimpleParentConfig(String name, Version version, VersionMatch versionMatch, boolean optional)
            implements ParentConfig {
        @Override public String getName() {
            return name;
        }

        @Override public Version getVersion() {
            return version;
        }

        @Override public VersionMatch getVersionMatch() {
            return versionMatch;
        }

        @Override public boolean isOptional() {
            return optional;
        }
    }

    record SimpleDescriptor(Integer id, ComponentConfig config) implements ComponentDescriptor {
        @Override public Integer getId() {
            return id;
        }

        @Override public ComponentConfig getConfig() {
            return config;
        }

        @Override
        public String getAlias() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isActivated() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<? extends ComponentDescriptor> getParents() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isParentClassLoaderUsed() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<ComponentDescriptor> findAncestors() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<ComponentDescriptor> findDescendants() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Path> getModulePaths() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsWarModules() {
            throw new UnsupportedOperationException();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ComponentConfig config(String name, ParentConfig... parents) {
        return new SimpleConfig(name, Version.of("1.0.0"), List.of(parents));
    }

    private static ParentConfig mandatory(String name) {
        return new SimpleParentConfig(name, Version.of("1.0.0"), VersionMatch.ANY, false);
    }

    private static ParentConfig optional(String name) {
        return new SimpleParentConfig(name, Version.of("1.0.0"), VersionMatch.ANY, true);
    }

    private static ComponentDescriptor existing(int id, String name) {
        return new SimpleDescriptor(id, new SimpleConfig(name, Version.of("1.0.0"), List.of()));
    }

    // -------------------------------------------------------------------------
    // Basic cases
    // -------------------------------------------------------------------------

    @Test
    void createPlan_configWithNoParents_singleEntryWithEmptyParentIds() throws ComponentException {
        var a = config("a");
        var plan = DeployPlanner.createPlan(List.of(), List.of(a), 7, 0);

        assertThat(plan.getStateId()).isEqualTo(7);
        assertThat(plan.getEntries()).hasSize(1);
        assertThat(plan.getEntries().get(0).getConfig()).isEqualTo(a);
        assertThat(plan.getEntries().get(0).getParentIds()).isEmpty();
    }

    @Test
    void createPlan_parentSatisfiedByExisting_resolvesToExistingId() throws ComponentException {
        var a = config("a", mandatory("core"));
        var plan = DeployPlanner.createPlan(List.of(existing(42, "core")), List.of(a), 1, 0);

        var entry = plan.getEntries().get(0);
        assertThat(entry.getParentIds()).containsExactly(42);
    }

    @Test
    void createPlan_unresolvedOptionalParent_dropped() throws ComponentException {
        var a = config("a", optional("missing"));
        var plan = DeployPlanner.createPlan(List.of(), List.of(a), 1, 0);

        var entry = plan.getEntries().get(0);
        assertThat(entry.getParentIds()).isEmpty();
    }

    @Test
    void createPlan_unresolvedMandatoryParent_throwsComponentException() {
        var a = config("a", mandatory("missing"));

        assertThatThrownBy(() -> DeployPlanner.createPlan(List.of(), List.of(a), 1, 0))
                .isInstanceOf(ComponentException.class);
    }

    // -------------------------------------------------------------------------
    // Batch ordering
    // -------------------------------------------------------------------------

    @Test
    void createPlan_batchInternalDependency_orderedWithPredictedParentId() throws ComponentException {
        // idCounter starts at 10, so the first deploy in order ("a") is predicted to get id 11.
        var a = config("a");
        var b = config("b", mandatory("a"));
        var plan = DeployPlanner.createPlan(List.of(), List.of(b, a), 1, 10);

        assertThat(plan.getEntries()).extracting(e -> e.getConfig().getName()).containsExactly("a", "b");
        assertThat(plan.getEntries().get(0).getParentIds()).isEmpty();
        assertThat(plan.getEntries().get(1).getParentIds()).containsExactly(11);
    }

    @Test
    void createPlan_chainOfThreeDependencies_fullyOrderedWithPredictedParentIds() throws ComponentException {
        var a = config("a");
        var b = config("b", mandatory("a"));
        var c = config("c", mandatory("b"));
        var plan = DeployPlanner.createPlan(List.of(), List.of(c, b, a), 1, 0);

        assertThat(plan.getEntries()).extracting(e -> e.getConfig().getName()).containsExactly("a", "b", "c");
        assertThat(plan.getEntries().get(1).getParentIds()).containsExactly(1);
        assertThat(plan.getEntries().get(2).getParentIds()).containsExactly(2);
    }

    @Test
    void createPlan_independentConfigs_keepOriginalOrder() throws ComponentException {
        var a = config("a");
        var b = config("b");
        var c = config("c");
        var plan = DeployPlanner.createPlan(List.of(), List.of(a, b, c), 1, 0);

        assertThat(plan.getEntries()).extracting(e -> e.getConfig().getName()).containsExactly("a", "b", "c");
    }

    @Test
    void createPlan_existingAndBatchBothSatisfyParent_existingTakesPrecedence() throws ComponentException {
        // "core" exists both as a deployed component and as a same-named entry in the batch; the existing one
        // must win, so "a" is resolved to its real id, not a predicted one.
        var coreInBatch = config("core");
        var a = config("a", mandatory("core"));
        var plan = DeployPlanner.createPlan(List.of(existing(99, "core")), List.of(a, coreInBatch), 1, 0);

        var aEntry = plan.getEntries().stream().filter(e -> e.getConfig().getName().equals("a")).findFirst()
                .orElseThrow();
        assertThat(aEntry.getParentIds()).containsExactly(99);
    }

    @Test
    void createPlan_cycleAmongBatch_throwsComponentException() {
        var a = config("a", mandatory("b"));
        var b = config("b", mandatory("a"));

        assertThatThrownBy(() -> DeployPlanner.createPlan(List.of(), List.of(a, b), 1, 0))
                .isInstanceOf(ComponentException.class);
    }
}
