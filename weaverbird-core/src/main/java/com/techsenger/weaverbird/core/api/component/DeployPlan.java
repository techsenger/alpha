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

import java.util.List;

/**
 * A precomputed, ordered list of steps for deploying a batch of configs, as returned by
 * {@code ComponentManager#createDeployPlan}.
 *
 * @author Pavel Castornii
 */
public interface DeployPlan {

    /**
     * One step of a {@link DeployPlan}: a config to deploy, together with the parent ids it should be deployed
     * with.
     */
    interface Entry {

        /**
         * Returns the config to deploy for this step.
         *
         * @return the config to deploy
         */
        ComponentConfig getConfig();

        /**
         * Returns the ids this config should be deployed with as parents: ids of already-deployed components,
         * and/or ids predicted for earlier entries of the same plan - see {@link DeployPlan#getStateId()} for the
         * assumption predicted ids rely on.
         *
         * @return the ids of the parent components to deploy this config with
         */
        List<Integer> getParentIds();
    }

    /**
     * Returns the id of the {@code ComponentsState} this plan was computed against. A same-batch parent id in
     * this plan's entries is a prediction, not yet an id any component actually holds - it is only correct if
     * no component is deployed, undeployed, activated, or deactivated between computing this plan and executing
     * it, in the same order this plan lists. Comparing the current state id against this one right before
     * executing the plan detects any such change, since that event also changes the state id; if they differ,
     * discard this plan and recompute it. Executing a plan immediately after computing it, both calls wrapped in
     * one {@code synchronized} block on the manager instance, rules the change out by construction and makes this
     * check unnecessary.
     *
     * @return the state id this plan was computed against
     */
    int getStateId();

    /**
     * Returns this plan's steps, ordered so that every entry appears after every other entry in the same list
     * it depends on.
     *
     * @return the ordered plan entries
     */
    List<Entry> getEntries();
}
