/* -----------------------------------------------------------------------------
 * Evaluation-PC-Sampling - Program for the evaluation of PC-Sampling.
 * Copyright (C) 2021  Sebastian Krieter
 * 
 * This file is part of Evaluation-PC-Sampling.
 * 
 * Evaluation-PC-Sampling is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * Evaluation-PC-Sampling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Evaluation-PC-Sampling.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * See <https://github.com/skrieter/evaluation-pc-sampling> for further information.
 * -----------------------------------------------------------------------------
 */
package org.spldev.evaluation.pc_sampling;

import java.util.*;

import org.spldev.evaluation.*;
import org.spldev.evaluation.process.*;
import org.spldev.formula.clauses.*;
import org.spldev.util.io.csv.*;
import org.spldev.util.logging.*;

/**
 * @author Sebastian Krieter
 */
public abstract class AlgorithmEvaluator<R, A extends Algorithm<R>> extends Evaluator {

	protected List<A> algorithmList;

	private CSVWriter dataCSVWriter, modelCSVWriter, algorithmCSVWriter;

	protected int algorithmIndex;
	protected int algorithmIteration;
	protected Result<R> result;
	protected CNF modelCNF;
	protected CNF randomizedModelCNF;

	@Override
	protected void addCSVWriters() {
		super.addCSVWriters();
		dataCSVWriter = addCSVWriter("data.csv", Arrays.asList("ModelID", "AlgorithmID", "SystemIteration",
			"AlgorithmIteration", "InTime", "NoError", "Time"));
		modelCSVWriter = addCSVWriter("models.csv", Arrays.asList("ModelID", "Name"));
		algorithmCSVWriter = addCSVWriter("algorithms.csv",
			Arrays.asList("ModelID", "AlgorithmID", "Name", "Settings"));
	}

	@Override
	public void evaluate() {
		tabFormatter.setTabLevel(0);
		if (config.systemIterations.getValue() > 0) {
			Logger.logInfo("Start");
			tabFormatter.incTabLevel();

			final ProcessRunner processRunner = new ProcessRunner();
			processRunner.setTimeout(config.timeout.getValue());

			final int systemIndexEnd = config.systemNames.size();

			systemLoop: for (systemIndex = 0; systemIndex < systemIndexEnd; systemIndex++) {
				logSystem();
				tabFormatter.setTabLevel(2);
				try {
					algorithmList = prepareAlgorithms();
				} catch (final Exception e) {
					Logger.logError(e);
					continue systemLoop;
				}
				algorithmIndex = 0;
				for (final A algorithm : algorithmList) {
					if (algorithm.getIterations() < 0) {
						algorithm.setIterations(config.algorithmIterations.getValue());
					}
					writeCSV(algorithmCSVWriter, this::writeAlgorithm);
					algorithmIndex++;
				}
				try {
					modelCNF = prepareModel();
					writeCSV(modelCSVWriter, this::writeModel);
				} catch (final Exception e) {
					Logger.logError(e);
					continue systemLoop;
				}
				for (systemIteration = 1; systemIteration <= config.systemIterations.getValue(); systemIteration++) {
					try {
						randomizedModelCNF = adaptModel();
					} catch (final Exception e) {
						Logger.logError(e);
						continue systemLoop;
					}
					config.algorithmIterations.getValue();
					algorithmIndex = -1;
					algorithmLoop: for (final A algorithm : algorithmList) {
						algorithmIndex++;
						for (algorithmIteration = 1; algorithmIteration <= algorithm
							.getIterations(); algorithmIteration++) {
							try {
								adaptAlgorithm(algorithm);
							} catch (final Exception e) {
								Logger.logError(e);
								continue algorithmLoop;
							}
							try {
								logRun();
								result = processRunner.run(algorithm);
								writeCSV(dataCSVWriter, this::writeData);
							} catch (final Exception e) {
								Logger.logError(e);
								continue algorithmLoop;
							}
						}
					}
				}
			}
			tabFormatter.setTabLevel(0);
			Logger.logInfo("Finished");
		} else {
			Logger.logInfo("Nothing to do");
		}
	}

	protected void writeModel(CSVWriter modelCSVWriter) {
		modelCSVWriter.addValue(config.systemIDs.get(systemIndex));
		modelCSVWriter.addValue(config.systemNames.get(systemIndex));
		modelCSVWriter.addValue(-1);
		modelCSVWriter.addValue(modelCNF.getVariableMap().size());
		modelCSVWriter.addValue(modelCNF.getClauses().size());
	}

	protected void writeAlgorithm(CSVWriter algorithmCSVWriter) {
		final Algorithm<?> algorithm = algorithmList.get(algorithmIndex);
		algorithmCSVWriter.addValue(config.systemIDs.get(systemIndex));
		algorithmCSVWriter.addValue(algorithmIndex);
		algorithmCSVWriter.addValue(algorithm.getName());
		algorithmCSVWriter.addValue(algorithm.getParameterSettings());
	}

	protected void writeData(CSVWriter dataCSVWriter) {
		dataCSVWriter.addValue(config.systemIDs.get(systemIndex));
		dataCSVWriter.addValue(algorithmIndex);
		dataCSVWriter.addValue(systemIteration);
		dataCSVWriter.addValue(algorithmIteration);
		dataCSVWriter.addValue(result.isTerminatedInTime());
		dataCSVWriter.addValue(result.isNoError());
		dataCSVWriter.addValue(result.getTime());
	}

	private void logRun() {
		final StringBuilder sb = new StringBuilder();
		sb.append(systemIndex + 1);
		sb.append("/");
		sb.append(config.systemNames.size());
		sb.append(" | ");
		sb.append(systemIteration);
		sb.append("/");
		sb.append(config.systemIterations.getValue());
		sb.append(" | (");
		sb.append(algorithmIndex + 1);
		sb.append("/");
		sb.append(algorithmList.size());
		sb.append(") ");
		sb.append(algorithmList.get(algorithmIndex).getFullName());
		sb.append(" | ");
		sb.append(algorithmIteration);
		sb.append("/");
		sb.append(algorithmList.get(algorithmIndex).getIterations());
		Logger.logInfo(sb.toString());
	}

	protected abstract CNF prepareModel() throws Exception;

	protected abstract CNF adaptModel() throws Exception;

	protected abstract void adaptAlgorithm(A algorithm) throws Exception;

	protected abstract List<A> prepareAlgorithms() throws Exception;

	public CSVWriter getDataCSVWriter() {
		return dataCSVWriter;
	}

	public CSVWriter getModelCSVWriter() {
		return modelCSVWriter;
	}

	public CSVWriter getAlgorithmCSVWriter() {
		return algorithmCSVWriter;
	}

}
