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

package com.techsenger.weaverbird.core.impl.component;

import com.techsenger.weaverbird.core.api.component.ComponentConfig;
import com.techsenger.weaverbird.core.api.component.DeployPlan;
import java.util.List;

/**
 *
 * @author Pavel Castornii
 */
public final class DefaultDeployPlan implements DeployPlan {

    /**
     *
     * @author Pavel Castornii
     */
    public static final class DefaultEntry implements Entry {

        private final ComponentConfig config;

        private final List<Integer> parentIds;

        public DefaultEntry(ComponentConfig config, List<Integer> parentIds) {
            this.config = config;
            this.parentIds = parentIds;
        }

        @Override
        public ComponentConfig getConfig() {
            return config;
        }

        @Override
        public List<Integer> getParentIds() {
            return parentIds;
        }
    }

    private final int stateId;

    private final List<Entry> entries;

    public DefaultDeployPlan(int stateId, List<Entry> entries) {
        this.stateId = stateId;
        this.entries = entries;
    }

    @Override
    public int getStateId() {
        return stateId;
    }

    @Override
    public List<Entry> getEntries() {
        return entries;
    }
}
