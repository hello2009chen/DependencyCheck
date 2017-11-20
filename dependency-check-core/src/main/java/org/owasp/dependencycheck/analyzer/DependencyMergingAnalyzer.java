/*
 * This file is part of dependency-check-core.
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
 *
 * Copyright (c) 2012 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.analyzer;

import java.io.File;
import java.util.Set;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Evidence;
import org.owasp.dependencycheck.dependency.EvidenceType;
import org.owasp.dependencycheck.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This analyzer will merge dependencies, created from different source, into a
 * single dependency.</p>
 *
 * @author Jeremy Long
 */
public class DependencyMergingAnalyzer extends AbstractDependencyComparingAnalyzer {

    /**
     * The Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyMergingAnalyzer.class);
    /**
     * The name of the analyzer.
     */
    private static final String ANALYZER_NAME = "Dependency Merging Analyzer";
    /**
     * The phase that this analyzer is intended to run in.
     */
    private static final AnalysisPhase ANALYSIS_PHASE = AnalysisPhase.POST_INFORMATION_COLLECTION;

    /**
     * Returns the name of the analyzer.
     *
     * @return the name of the analyzer.
     */
    @Override
    public String getName() {
        return ANALYZER_NAME;
    }

    /**
     * Returns the phase that the analyzer is intended to run in.
     *
     * @return the phase that the analyzer is intended to run in.
     */
    @Override
    public AnalysisPhase getAnalysisPhase() {
        return ANALYSIS_PHASE;
    }

    /**
     * <p>
     * Returns the setting key to determine if the analyzer is enabled.</p>
     *
     * @return the key for the analyzer's enabled property
     */
    @Override
    protected String getAnalyzerEnabledSettingKey() {
        return Settings.KEYS.ANALYZER_DEPENDENCY_MERGING_ENABLED;
    }

    /**
     * Evaluates the dependencies
     *
     * @param dependency a dependency to compare
     * @param nextDependency a dependency to compare
     * @param dependenciesToRemove a set of dependencies that will be removed
     * @return true if a dependency is removed; otherwise false
     */
    @Override
    protected boolean evaluateDependencies(final Dependency dependency, final Dependency nextDependency, final Set<Dependency> dependenciesToRemove) {
        Dependency main;
        //CSOFF: InnerAssignment
        if ((main = getMainGemspecDependency(dependency, nextDependency)) != null) {
            if (main == dependency) {
                mergeDependencies(dependency, nextDependency, dependenciesToRemove);
            } else {
                mergeDependencies(nextDependency, dependency, dependenciesToRemove);
                return true; //since we merged into the next dependency - skip forward to the next in mainIterator
            }
        } else if ((main = getMainSwiftDependency(dependency, nextDependency)) != null) {
            if (main == dependency) {
                mergeDependencies(dependency, nextDependency, dependenciesToRemove);
            } else {
                mergeDependencies(nextDependency, dependency, dependenciesToRemove);
                return true; //since we merged into the next dependency - skip forward to the next in mainIterator
            }
        }
        //CSON: InnerAssignment
        return false;
    }

    /**
     * Adds the relatedDependency to the dependency's related dependencies.
     *
     * @param dependency the main dependency
     * @param relatedDependency a collection of dependencies to be removed from
     * the main analysis loop, this is the source of dependencies to remove
     * @param dependenciesToRemove a collection of dependencies that will be
     * removed from the main analysis loop, this function adds to this
     * collection
     */
    private void mergeDependencies(final Dependency dependency, final Dependency relatedDependency, final Set<Dependency> dependenciesToRemove) {
        LOGGER.debug("Merging '{}' into '{}'", relatedDependency.getFilePath(), dependency.getFilePath());
        dependency.addRelatedDependency(relatedDependency);
        for (Evidence e : relatedDependency.getEvidence(EvidenceType.VENDOR)) {
            dependency.addEvidence(EvidenceType.VENDOR, e);
        }
        for (Evidence e : relatedDependency.getEvidence(EvidenceType.PRODUCT)) {
            dependency.addEvidence(EvidenceType.PRODUCT, e);
        }
        for (Evidence e : relatedDependency.getEvidence(EvidenceType.VERSION)) {
            dependency.addEvidence(EvidenceType.VERSION, e);
        }

        for (Dependency d : relatedDependency.getRelatedDependencies()) {
            dependency.addRelatedDependency(d);
            relatedDependency.removeRelatedDependencies(d);
        }
        if (dependency.getSha1sum().equals(relatedDependency.getSha1sum())) {
            dependency.addAllProjectReferences(relatedDependency.getProjectReferences());
        }
        dependenciesToRemove.add(relatedDependency);
    }

    /**
     * Bundling Ruby gems that are identified from different .gemspec files but
     * denote the same package path. This happens when Ruby bundler installs an
     * application's dependencies by running "bundle install".
     *
     * @param dependency1 dependency to compare
     * @param dependency2 dependency to compare
     * @return true if the the dependencies being analyzed appear to be the
     * same; otherwise false
     */
    private boolean isSameRubyGem(Dependency dependency1, Dependency dependency2) {
        if (dependency1 == null || dependency2 == null
                || !dependency1.getFileName().endsWith(".gemspec")
                || !dependency2.getFileName().endsWith(".gemspec")
                || dependency1.getPackagePath() == null
                || dependency2.getPackagePath() == null) {
            return false;
        }
        return dependency1.getPackagePath().equalsIgnoreCase(dependency2.getPackagePath());
    }

    /**
     * Ruby gems installed by "bundle install" can have zero or more *.gemspec
     * files, all of which have the same packagePath and should be grouped. If
     * one of these gemspec is from <parent>/specifications/*.gemspec, because
     * it is a stub with fully resolved gem meta-data created by Ruby bundler,
     * this dependency should be the main one. Otherwise, use dependency2 as
     * main.
     *
     * This method returns null if any dependency is not from *.gemspec, or the
     * two do not have the same packagePath. In this case, they should not be
     * grouped.
     *
     * @param dependency1 dependency to compare
     * @param dependency2 dependency to compare
     * @return the main dependency; or null if a gemspec is not included in the
     * analysis
     */
    private Dependency getMainGemspecDependency(Dependency dependency1, Dependency dependency2) {
        if (isSameRubyGem(dependency1, dependency2)) {
            final File lFile = dependency1.getActualFile();
            final File left = lFile.getParentFile();
            if (left != null && left.getName().equalsIgnoreCase("specifications")) {
                return dependency1;
            }
            return dependency2;
        }
        return null;
    }

    /**
     * Bundling same swift dependencies with the same packagePath but identified
     * by different file type analyzers.
     *
     * @param dependency1 dependency to test
     * @param dependency2 dependency to test
     * @return <code>true</code> if the dependencies appear to be the same;
     * otherwise <code>false</code>
     */
    private boolean isSameSwiftPackage(Dependency dependency1, Dependency dependency2) {
        if (dependency1 == null || dependency2 == null
                || (!dependency1.getFileName().endsWith(".podspec")
                && !dependency1.getFileName().equals("Package.swift"))
                || (!dependency2.getFileName().endsWith(".podspec")
                && !dependency2.getFileName().equals("Package.swift"))
                || dependency1.getPackagePath() == null
                || dependency2.getPackagePath() == null) {
            return false;
        }
        return dependency1.getPackagePath().equalsIgnoreCase(dependency2.getPackagePath());
    }

    /**
     * Determines which of the swift dependencies should be considered the
     * primary.
     *
     * @param dependency1 the first swift dependency to compare
     * @param dependency2 the second swift dependency to compare
     * @return the primary swift dependency
     */
    private Dependency getMainSwiftDependency(Dependency dependency1, Dependency dependency2) {
        if (isSameSwiftPackage(dependency1, dependency2)) {
            if (dependency1.getFileName().endsWith(".podspec")) {
                return dependency1;
            }
            return dependency2;
        }
        return null;
    }
}
