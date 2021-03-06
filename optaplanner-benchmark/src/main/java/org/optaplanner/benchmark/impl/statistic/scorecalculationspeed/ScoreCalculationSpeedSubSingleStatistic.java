/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.benchmark.impl.statistic.scorecalculationspeed;

import java.util.List;

import org.optaplanner.benchmark.config.statistic.ProblemStatisticType;
import org.optaplanner.benchmark.impl.result.SubSingleBenchmarkResult;
import org.optaplanner.benchmark.impl.statistic.ProblemBasedSubSingleStatistic;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import org.optaplanner.core.impl.phase.scope.AbstractStepScope;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;
import org.optaplanner.core.impl.solver.AbstractSolver;
import org.optaplanner.core.impl.solver.scope.SolverScope;

public class ScoreCalculationSpeedSubSingleStatistic<Solution_>
        extends ProblemBasedSubSingleStatistic<Solution_, ScoreCalculationSpeedStatisticPoint> {

    private final long timeMillisThresholdInterval;

    private final ScoreCalculationSpeedSubSingleStatisticListener listener;

    public ScoreCalculationSpeedSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        this(subSingleBenchmarkResult, 1000L);
    }

    public ScoreCalculationSpeedSubSingleStatistic(SubSingleBenchmarkResult benchmarkResult, long timeMillisThresholdInterval) {
        super(benchmarkResult, ProblemStatisticType.SCORE_CALCULATION_SPEED);
        if (timeMillisThresholdInterval <= 0L) {
            throw new IllegalArgumentException("The timeMillisThresholdInterval (" + timeMillisThresholdInterval
                    + ") must be bigger than 0.");
        }
        this.timeMillisThresholdInterval = timeMillisThresholdInterval;
        listener = new ScoreCalculationSpeedSubSingleStatisticListener();
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void open(Solver<Solution_> solver) {
        ((AbstractSolver<Solution_>) solver).addPhaseLifecycleListener(listener);
    }

    @Override
    public void close(Solver<Solution_> solver) {
        ((AbstractSolver<Solution_>) solver).removePhaseLifecycleListener(listener);
    }

    private class ScoreCalculationSpeedSubSingleStatisticListener extends PhaseLifecycleListenerAdapter<Solution_> {

        private long nextTimeMillisThreshold = timeMillisThresholdInterval;
        private long lastTimeMillisSpent = 0L;
        private long lastCalculationCount = 0L;

        @Override
        public void stepEnded(AbstractStepScope<Solution_> stepScope) {
            long timeMillisSpent = stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow();
            if (timeMillisSpent >= nextTimeMillisThreshold) {
                SolverScope<Solution_> solverScope = stepScope.getPhaseScope().getSolverScope();
                long calculationCount = solverScope.getScoreCalculationCount();
                long calculationCountInterval = calculationCount - lastCalculationCount;
                long timeMillisSpentInterval = timeMillisSpent - lastTimeMillisSpent;
                if (timeMillisSpentInterval == 0L) {
                    // Avoid divide by zero exception on a fast CPU
                    timeMillisSpentInterval = 1L;
                }
                long scoreCalculationSpeed = calculationCountInterval * 1000L / timeMillisSpentInterval;
                pointList.add(new ScoreCalculationSpeedStatisticPoint(timeMillisSpent, scoreCalculationSpeed));
                lastCalculationCount = calculationCount;

                lastTimeMillisSpent = timeMillisSpent;
                nextTimeMillisThreshold += timeMillisThresholdInterval;
                if (nextTimeMillisThreshold < timeMillisSpent) {
                    nextTimeMillisThreshold = timeMillisSpent;
                }
            }
        }

    }

    // ************************************************************************
    // CSV methods
    // ************************************************************************

    @Override
    protected String getCsvHeader() {
        return ScoreCalculationSpeedStatisticPoint.buildCsvLine("timeMillisSpent", "scoreCalculationSpeed");
    }

    @Override
    protected ScoreCalculationSpeedStatisticPoint createPointFromCsvLine(ScoreDefinition scoreDefinition,
            List<String> csvLine) {
        return new ScoreCalculationSpeedStatisticPoint(Long.parseLong(csvLine.get(0)),
                Long.parseLong(csvLine.get(1)));
    }

}
