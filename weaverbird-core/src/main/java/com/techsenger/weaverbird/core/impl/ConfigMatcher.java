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
import com.techsenger.weaverbird.core.api.component.ParentConfig;
import com.techsenger.weaverbird.core.api.component.VersionMatch;
import static com.techsenger.weaverbird.core.api.component.VersionMatch.ANY;
import static com.techsenger.weaverbird.core.api.component.VersionMatch.MAJOR;
import static com.techsenger.weaverbird.core.api.component.VersionMatch.MINOR;
import static com.techsenger.weaverbird.core.api.component.VersionMatch.PATCH;
import com.techsenger.toolkit.core.version.Version;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Determines whether every {@link ParentConfig} in a required list can be matched to a distinct {@link ComponentConfig}
 * from a provided list, respecting name equality and {@link VersionMatch} constraints.
 *
 * <p>The matching problem is non-trivial because a single provided config may be compatible with multiple required
 * configs (e.g. version {@code 1.5.0} satisfies both a {@code MAJOR} requirement on {@code 1.2.0} and a
 * {@code MINOR} requirement on {@code 1.5.0}). A greedy first-fit approach can therefore produce false negatives.
 *
 * <p>To guarantee correctness the class models the problem as a <b>bipartite matching</b> task and solves it with
 * <b>Kuhn's algorithm</b> (depth-first augmenting paths). The two disjoint sets are:
 * <ul>
 *   <li><em>Left nodes</em>  — required configs (indexed {@code 0..reqSize-1})</li>
 *   <li><em>Right nodes</em> — provided configs (indexed {@code 0..provSize-1})</li>
 * </ul>
 * An edge exists between a left node {@code i} and a right node {@code j} when
 * {@code providedConfigs[j]} is compatible with {@code requiredConfigs[i]}.
 * The algorithm searches for a <em>perfect matching on the left side</em>: every required config must be
 * assigned exactly one provided config, and no provided config may be shared.
 *
 * <p>Time complexity: {@code O(V * E)} where {@code V = requiredConfigs.size()} and
 * {@code E} is the total number of compatible pairs — acceptable for the typical sizes seen in config resolution.
 *
 * @author Pavel Castornii
 */
public final class ConfigMatcher {

    /**
     * Checks that every required config can be matched to a <em>distinct</em> provided config.
     *
     * <p>Algorithm outline:
     * <ol>
     *   <li>Build an adjacency list: for each required config, collect the indices of all compatible
     *       provided configs.</li>
     *   <li>Run Kuhn's algorithm: iterate over required configs and attempt to find an augmenting path
     *       for each one. An augmenting path either finds a free provided slot or recursively re-routes
     *       a previously matched required config to a different provided slot, freeing the current one.</li>
     *   <li>If all required configs are matched, returns {@code null}. Otherwise returns the first
     *       required config for which no provided config could be assigned.</li>
     * </ol>
     *
     * @param providedConfigs configs that are available for assignment; may contain more entries than required
     * @param requiredConfigs configs that must each be satisfied by a unique provided config; a config for
     *     which {@link ParentConfig#isOptional()} is {@code true} should be filtered out by the caller before
     *     calling this method, since this method treats every entry it receives as mandatory
     * @return {@code null} if a complete one-to-one matching exists;
     *         the first unmatched {@link ParentConfig} otherwise
     */
    public static ParentConfig findFirstUnsatisfied(List<ComponentConfig> providedConfigs,
            List<ParentConfig> requiredConfigs) {
        int reqSize  = requiredConfigs.size();
        int provSize = providedConfigs.size();

        // Build adjacency list for the bipartite graph.
        // adj.get(i) contains the indices j of every provided config that is compatible with required config i.
        List<List<Integer>> adj = new ArrayList<>(reqSize);
        for (int i = 0; i < reqSize; i++) {
            List<Integer> compatible = new ArrayList<>();
            ParentConfig req = requiredConfigs.get(i);
            for (int j = 0; j < provSize; j++) {
                if (isCompatible(providedConfigs.get(j), req)) {
                    compatible.add(j);
                }
            }
            adj.add(compatible);
        }

        // matchProv[j] stores the index of the required config currently matched to provided config j,
        // or -1 if provided config j is not yet assigned.
        int[] matchProv = new int[provSize];
        Arrays.fill(matchProv, -1);

        for (int i = 0; i < reqSize; i++) {
            // visited prevents revisiting the same provided node within a single augmenting-path search,
            // which would cause infinite recursion in cycles.
            boolean[] visited = new boolean[provSize];
            if (!tryMatch(i, adj, matchProv, visited)) {
                return requiredConfigs.get(i);
            }
        }
        return null;
    }

    /**
     * Attempts to find an augmenting path starting from required config {@code reqIdx} using DFS.
     *
     * <p>For each provided config {@code j} adjacent to {@code reqIdx}:
     * <ul>
     *   <li>If {@code j} has not been visited in this search pass, it is marked visited.</li>
     *   <li>If {@code j} is currently unmatched ({@code matchProv[j] == -1}), the edge
     *       {@code reqIdx → j} is added to the matching and the method returns {@code true}.</li>
     *   <li>If {@code j} is already matched to some other required config {@code k}, the method
     *       recursively tries to find an alternative provided config for {@code k}. If successful,
     *       {@code j} is re-assigned to {@code reqIdx} (augmenting path found).</li>
     * </ul>
     *
     * @param reqIdx  index of the required config for which we are seeking a provided config
     * @param adj      adjacency list built in {@link #allRequiredConfigsSatisfied}
     * @param matchProv current matching state: {@code matchProv[j]} = required index assigned to provided {@code j}
     * @param visited  per-search-pass flag to avoid revisiting provided nodes
     * @return {@code true} if an augmenting path was found and the matching was extended
     */
    private static boolean tryMatch(int reqIdx, List<List<Integer>> adj, int[] matchProv, boolean[] visited) {
        for (int provIdx : adj.get(reqIdx)) {
            if (visited[provIdx]) {
                continue;
            }
            visited[provIdx] = true;

            // Slot is free, or its current owner can be re-routed to another provided config.
            if (matchProv[provIdx] == -1 || tryMatch(matchProv[provIdx], adj, matchProv, visited)) {
                matchProv[provIdx] = reqIdx;
                return true;
            }
        }
        return false;
    }

    /**
     * Decides whether {@code provided} satisfies the name and version constraints declared by {@code required}.
     *
     * <p>Name equality is checked first (case-sensitive {@link Objects#equals}). If names differ the configs
     * are incompatible regardless of versions.
     *
     * <p>Version compatibility depends on {@link VersionMatch}:
     * <ul>
     *   <li>{@link VersionMatch#ANY}   — any provided version is accepted.</li>
     *   <li>{@link VersionMatch#MAJOR} — {@code provided.major == required.major}
     *                                    and {@code provided >= required}.</li>
     *   <li>{@link VersionMatch#MINOR} — {@code provided.major == required.major},
     *                                    {@code provided.minor == required.minor}
     *                                    and {@code provided >= required}.</li>
     *   <li>{@link VersionMatch#PATCH} — all three segments must be identical
     *                                    ({@code provided == required} exactly).</li>
     * </ul>
     *
     * <p>A {@code null} {@link VersionMatch} is treated as {@link VersionMatch#ANY}.
     *
     * @param provided the candidate config from the provided list
     * @param required the requirement that must be satisfied
     * @return {@code true} if {@code provided} meets both the name and version constraints of {@code required}
     */
    private static boolean isCompatible(ComponentConfig provided, ParentConfig required) {
        if (!Objects.equals(provided.getName(), required.getName())) {
            return false;
        }

        Version req   = required.getVersion();
        Version prov  = provided.getVersion();
        VersionMatch match = required.getVersionMatch() != null
                ? required.getVersionMatch()
                : VersionMatch.ANY;

        return switch (match) {
            case ANY   -> true;
            case MAJOR -> Objects.equals(prov.getMajor(), req.getMajor())
                    && isProvAtLeast(prov, req);
            case MINOR -> Objects.equals(prov.getMajor(), req.getMajor())
                    && Objects.equals(prov.getMinor(), req.getMinor())
                    && isProvAtLeast(prov, req);
            case PATCH -> Objects.equals(prov.getMajor(), req.getMajor())
                    && Objects.equals(prov.getMinor(), req.getMinor())
                    && Objects.equals(prov.getPatch(), req.getPatch());
        };
    }

    /**
     * Returns {@code true} when {@code prov} is greater than or equal to {@code req},
     * comparing segments in {@code major → minor → patch} order.
     *
     * <p>{@code null} segments are treated as {@code 0} so that a partially specified version
     * (e.g. {@code 1.2} with no patch) does not cause a {@link NullPointerException} and is
     * considered equivalent to {@code 1.2.0}.
     *
     * @param prov the version to test
     * @param req  the minimum acceptable version
     * @return {@code true} if {@code prov >= req}
     */
    private static boolean isProvAtLeast(Version prov, Version req) {
        int majCmp = Integer.compare(
                prov.getMajor() != null ? prov.getMajor() : 0,
                req.getMajor()  != null ? req.getMajor()  : 0);
        if (majCmp != 0) {
            return majCmp > 0;
        }

        int minCmp = Integer.compare(
                prov.getMinor() != null ? prov.getMinor() : 0,
                req.getMinor()  != null ? req.getMinor()  : 0);
        if (minCmp != 0) {
            return minCmp > 0;
        }

        int patCmp = Integer.compare(
                prov.getPatch() != null ? prov.getPatch() : 0,
                req.getPatch()  != null ? req.getPatch()  : 0);
        return patCmp >= 0;
    }

    private ConfigMatcher() {
        // empty
    }
}
