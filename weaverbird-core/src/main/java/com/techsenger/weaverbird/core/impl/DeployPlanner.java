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

import com.techsenger.weaverbird.core.api.component.ComponentConfig;
import com.techsenger.weaverbird.core.api.component.ComponentDescriptor;
import com.techsenger.weaverbird.core.api.component.ComponentException;
import com.techsenger.weaverbird.core.api.component.DeployPlan;
import com.techsenger.weaverbird.core.api.component.ParentConfig;
import com.techsenger.weaverbird.core.impl.component.DefaultDeployPlan;
import com.techsenger.toolkit.core.StringUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes a {@link DeployPlan} for a batch of configs: a valid deployment order plus, for each entry, the parent
 * ids it should be deployed with.
 *
 * <p>Each config's declared parents are resolved against two pools, in this precedence order: an already-deployed
 * component (no ordering constraint - it is available immediately), then another config in the same batch (creates
 * an ordering constraint - the batch entry must be deployed first). An unresolved
 * {@link ParentConfig#isOptional() optional} parent is dropped; an unresolved mandatory one fails plan creation.
 * The batch order itself is computed with Kahn's algorithm over the resulting dependency graph, which also detects
 * cycles among the batch. A same-batch parent's id is predicted from the component id counter's current value
 * plus its position in the computed order, since the framework assigns component ids from that same counter, in
 * deployment order, one per deploy - see {@link DeployPlan#getStateId()} for the assumption this prediction relies
 * on.
 *
 * @author Pavel Castornii
 */
public final class DeployPlanner {

    /**
     * Computes a deploy plan for {@code configs}.
     *
     * @param existingDescriptors the descriptors of the currently deployed components, used to resolve parents
     *     that need no ordering constraint
     * @param configs the configs to plan for; need not be exhaustive - a config may declare a parent satisfied
     *     by an already-deployed component that never appears in this list
     * @param stateId the id to stamp the returned plan with, normally the current {@code ComponentsState#getId()}
     * @param idCounter the component id counter's current value, i.e. the id assigned to the most recently
     *     deployed component, normally read from the same manager instance {@code existingDescriptors} came from
     * @throws ComponentException if a non-optional parent of some config is satisfied by neither an already
     *     deployed component nor another entry in {@code configs}, or if the configs form a dependency cycle
     * @return the resulting deploy plan
     */
    public static DeployPlan createPlan(List<ComponentDescriptor> existingDescriptors, List<ComponentConfig> configs,
            int stateId, int idCounter) throws ComponentException {
        var dependsOn = new ArrayList<List<Integer>>(configs.size());
        var dependents = new ArrayList<List<Integer>>(configs.size());
        for (int i = 0; i < configs.size(); i++) {
            dependsOn.add(new ArrayList<>());
            dependents.add(new ArrayList<>());
        }
        for (int i = 0; i < configs.size(); i++) {
            var config = configs.get(i);
            for (var parent : config.getParents()) {
                if (findExisting(existingDescriptors, parent) != null) {
                    continue;
                }
                var batchIndex = findBatchIndex(configs, i, parent);
                if (batchIndex != -1) {
                    dependsOn.get(i).add(batchIndex);
                    dependents.get(batchIndex).add(i);
                } else if (!parent.isOptional()) {
                    throw new ComponentException(StringUtils.format(
                            "Cannot resolve mandatory parent - {}:{} - for component - {}:{}", parent.getName(),
                            parent.getVersion(), config.getName(), config.getVersion()));
                }
            }
        }

        var order = sortTopologically(configs, dependsOn, dependents);
        var predictedIdsByIndex = new int[configs.size()];
        for (int position = 0; position < order.size(); position++) {
            predictedIdsByIndex[order.get(position)] = idCounter + position + 1;
        }

        var entries = new ArrayList<DeployPlan.Entry>(configs.size());
        for (var index : order) {
            var config = configs.get(index);
            var parentIds = new ArrayList<Integer>();
            for (var parent : config.getParents()) {
                var existing = findExisting(existingDescriptors, parent);
                if (existing != null) {
                    parentIds.add(existing.getId());
                    continue;
                }
                var batchIndex = findBatchIndex(configs, index, parent);
                if (batchIndex != -1) {
                    parentIds.add(predictedIdsByIndex[batchIndex]);
                }
            }
            entries.add(new DefaultDeployPlan.DefaultEntry(config, parentIds));
        }
        return new DefaultDeployPlan(stateId, entries);
    }

    /**
     * Orders {@code configs} with Kahn's algorithm so that every config appears after every batch-internal
     * dependency it has, breaking ties between simultaneously-ready configs by their original list order.
     *
     * @throws ComponentException if the batch-internal dependency graph contains a cycle
     */
    private static List<Integer> sortTopologically(List<ComponentConfig> configs, List<List<Integer>> dependsOn,
            List<List<Integer>> dependents) throws ComponentException {
        var remaining = new int[configs.size()];
        var queue = new ArrayDeque<Integer>();
        for (int i = 0; i < configs.size(); i++) {
            remaining[i] = dependsOn.get(i).size();
            if (remaining[i] == 0) {
                queue.add(i);
            }
        }
        var order = new ArrayList<Integer>(configs.size());
        while (!queue.isEmpty()) {
            var index = queue.poll();
            order.add(index);
            for (var dependent : dependents.get(index)) {
                remaining[dependent]--;
                if (remaining[dependent] == 0) {
                    queue.add(dependent);
                }
            }
        }
        if (order.size() != configs.size()) {
            var stuckNames = new ArrayList<String>();
            for (int i = 0; i < configs.size(); i++) {
                if (remaining[i] != 0) {
                    stuckNames.add(configs.get(i).getName());
                }
            }
            throw new ComponentException(StringUtils.format(
                    "Dependency cycle detected among components - {}", stuckNames));
        }
        return order;
    }

    /**
     * Returns the first already-deployed descriptor compatible with {@code parent}, or {@code null} if none is.
     */
    private static ComponentDescriptor findExisting(List<ComponentDescriptor> existingDescriptors,
            ParentConfig parent) {
        for (var descriptor : existingDescriptors) {
            if (ConfigMatcher.isCompatible(descriptor.getConfig(), parent)) {
                return descriptor;
            }
        }
        return null;
    }

    /**
     * Returns the index, other than {@code selfIndex}, of the first config in {@code configs} compatible with
     * {@code parent}, or {@code -1} if none is.
     */
    private static int findBatchIndex(List<ComponentConfig> configs, int selfIndex, ParentConfig parent) {
        for (int i = 0; i < configs.size(); i++) {
            if (i != selfIndex && ConfigMatcher.isCompatible(configs.get(i), parent)) {
                return i;
            }
        }
        return -1;
    }

    private DeployPlanner() {
        // empty
    }
}
