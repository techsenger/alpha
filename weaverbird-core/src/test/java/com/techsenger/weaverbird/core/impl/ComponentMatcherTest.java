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
import com.techsenger.weaverbird.core.api.component.ParentConfig;
import com.techsenger.weaverbird.core.api.component.RepositoryConfig;
import com.techsenger.weaverbird.core.api.component.VersionMatch;
import com.techsenger.weaverbird.core.api.module.ModuleConfig;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Pavel Castornii
 */
class ConfigMatcherTest {

    // -------------------------------------------------------------------------
    // Test doubles
    // -------------------------------------------------------------------------

    record SimpleConfig(String name, Version version) implements ComponentConfig {
        @Override public String getName() {
            return name;
        }

        @Override public Version getVersion() {
            return version;
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
        public List<ParentConfig> getParents() {
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

    record SimpleParentConfig(String name, Version version, VersionMatch versionMatch)
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
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ComponentConfig prov(String name, String version) {
        return new SimpleConfig(name, Version.of(version));
    }

    private static ParentConfig req(String name, String version, VersionMatch match) {
        return new SimpleParentConfig(name, Version.of(version), match);
    }

    // -------------------------------------------------------------------------
    // Edge cases: empty lists
    // -------------------------------------------------------------------------

    @Test
    void emptyRequiredAlwaysMatches() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.0.0")),
                List.of()
        )).isNull();
    }

    @Test
    void emptyProvidedWithRequiredFails() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(),
                List.of(req("a", "1.0.0", VersionMatch.ANY))
        )).isNotNull();
    }

    @Test
    void bothEmptyMatches() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(List.of(), List.of())).isNull();
    }

    // -------------------------------------------------------------------------
    // VersionMatch.ANY
    // -------------------------------------------------------------------------

    @Test
    void anyMatchesLowerVersion() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "0.1.0")),
                List.of(req("a", "5.0.0", VersionMatch.ANY))
        )).isNull();
    }

    @Test
    void anyMatchesHigherVersion() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "9.9.9")),
                List.of(req("a", "1.0.0", VersionMatch.ANY))
        )).isNull();
    }

    @Test
    void anyMatchesSameVersion() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.2.3")),
                List.of(req("a", "1.2.3", VersionMatch.ANY))
        )).isNull();
    }

    // -------------------------------------------------------------------------
    // VersionMatch.MAJOR
    // -------------------------------------------------------------------------

    @Test
    void majorMatchesExactVersion() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.2.0")),
                List.of(req("a", "1.2.0", VersionMatch.MAJOR))
        )).isNull();
    }

    @Test
    void majorMatchesHigherMinor() {
        // 1.5.0 is in [1.2.0, 2.0.0)
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.5.0")),
                List.of(req("a", "1.2.0", VersionMatch.MAJOR))
        )).isNull();
    }

    @Test
    void majorRejectsLowerMinor() {
        // 1.1.0 < 1.2.0
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.1.0")),
                List.of(req("a", "1.2.0", VersionMatch.MAJOR))
        )).isNotNull();
    }

    @Test
    void majorRejectsDifferentMajor() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "2.0.0")),
                List.of(req("a", "1.2.0", VersionMatch.MAJOR))
        )).isNotNull();
    }

    // -------------------------------------------------------------------------
    // VersionMatch.MINOR
    // -------------------------------------------------------------------------

    @Test
    void minorMatchesExactVersion() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.2.0")),
                List.of(req("a", "1.2.0", VersionMatch.MINOR))
        )).isNull();
    }

    @Test
    void minorMatchesHigherPatch() {
        // 1.2.5 is in [1.2.0, 1.3.0)
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.2.5")),
                List.of(req("a", "1.2.0", VersionMatch.MINOR))
        )).isNull();
    }

    @Test
    void minorRejectsLowerPatch() {
        // provided 1.2.0 does not satisfy required 1.2.3 under MINOR
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.2.0")),
                List.of(req("a", "1.2.3", VersionMatch.MINOR))
        )).isNotNull();
    }

    @Test
    void minorRejectsDifferentMinor() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.3.0")),
                List.of(req("a", "1.2.0", VersionMatch.MINOR))
        )).isNotNull();
    }

    @Test
    void minorRejectsDifferentMajor() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "2.2.0")),
                List.of(req("a", "1.2.0", VersionMatch.MINOR))
        )).isNotNull();
    }

    // -------------------------------------------------------------------------
    // VersionMatch.PATCH
    // -------------------------------------------------------------------------

    @Test
    void patchMatchesExactVersion() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.2.3")),
                List.of(req("a", "1.2.3", VersionMatch.PATCH))
        )).isNull();
    }

    @Test
    void patchRejectsDifferentPatch() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.2.4")),
                List.of(req("a", "1.2.3", VersionMatch.PATCH))
        )).isNotNull();
    }

    @Test
    void patchRejectsDifferentMinor() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.3.3")),
                List.of(req("a", "1.2.3", VersionMatch.PATCH))
        )).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Name mismatch
    // -------------------------------------------------------------------------

    @Test
    void nameMismatchFails() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("b", "1.0.0")),
                List.of(req("a", "1.0.0", VersionMatch.ANY))
        )).isNotNull();
    }

    @Test
    void nameMismatchWithMatchingVersionFails() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("b", "1.2.3")),
                List.of(req("a", "1.2.3", VersionMatch.PATCH))
        )).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Bipartite matching — the core cases
    // -------------------------------------------------------------------------

    /**
     * Greedy would fail here: if it picks 1.5.0 for req[0] (MAJOR matches),
     * req[1] (MINOR 1.5.x) is left unsatisfied.
     * Correct matching: 1.2.0 -> req[0], 1.5.0 -> req[1].
     */
    @Test
    void greedyTrapRequiresBacktracking() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.2.0"), prov("a", "1.5.0")),
                List.of(
                        req("a", "1.2.0", VersionMatch.MAJOR),  // matches 1.2.0 and 1.5.0
                        req("a", "1.5.0", VersionMatch.MINOR)   // matches only 1.5.x
                )
        )).isNull();
    }

    @Test
    void oneProviderCannotSatisfyTwoRequirements() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.2.0")),
                List.of(
                        req("a", "1.2.0", VersionMatch.MINOR),
                        req("a", "1.2.0", VersionMatch.MINOR)
                )
        )).isNotNull();
    }

    @Test
    void twoProvidersExactlyMatchTwoRequirements() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.2.0"), prov("a", "1.3.0")),
                List.of(
                        req("a", "1.2.0", VersionMatch.MINOR),
                        req("a", "1.3.0", VersionMatch.MINOR)
                )
        )).isNull();
    }

    @Test
    void multipleNamesAllSatisfied() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("alpha", "1.0.0"), prov("beta", "2.3.0"), prov("gamma", "3.0.0")),
                List.of(
                        req("alpha", "1.0.0", VersionMatch.MAJOR),
                        req("beta",  "2.3.0", VersionMatch.MINOR),
                        req("gamma", "3.0.0", VersionMatch.PATCH)
                )
        )).isNull();
    }

    @Test
    void multipleNamesOneMissing() {
        // "beta" is absent from provided list
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("alpha", "1.0.0"), prov("gamma", "3.0.0")),
                List.of(
                        req("alpha", "1.0.0", VersionMatch.MAJOR),
                        req("beta",  "2.3.0", VersionMatch.MINOR),
                        req("gamma", "3.0.0", VersionMatch.PATCH)
                )
        )).isNotNull();
    }

    /**
     * Three required configs but only two provided — impossible regardless of versions.
     */
    @Test
    void moreRequiredThanProvidedFails() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.0.0"), prov("a", "2.0.0")),
                List.of(
                        req("a", "1.0.0", VersionMatch.ANY),
                        req("a", "1.0.0", VersionMatch.ANY),
                        req("a", "1.0.0", VersionMatch.ANY)
                )
        )).isNotNull();
    }

    /**
     * Deep augmenting path: the algorithm must re-route previously matched nodes
     * across the full chain to find a valid complete matching.
     */
    @Test
    void deepAugmentingPathMatches() {
        assertThat(ConfigMatcher.findFirstUnsatisfied(
                List.of(prov("a", "1.0.0"), prov("a", "1.1.0"), prov("a", "1.2.0")),
                List.of(
                        req("a", "1.0.0", VersionMatch.MAJOR),  // matches all three
                        req("a", "1.1.0", VersionMatch.MINOR),  // matches only 1.1.x
                        req("a", "1.2.0", VersionMatch.PATCH)   // matches only 1.2.0
                )
        )).isNull();
    }
}
