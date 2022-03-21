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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import org.spldev.evaluation.*;
import org.spldev.evaluation.pc_sampling.properties.*;
import org.spldev.evaluation.properties.*;
import org.spldev.formula.analysis.sat4j.twise.*;
import org.spldev.formula.analysis.sat4j.twise.PresenceCondition;
import org.spldev.formula.analysis.sat4j.twise.TWiseStatisticGenerator.*;
import org.spldev.formula.clauses.*;
import org.spldev.formula.clauses.LiteralList.*;
import org.spldev.formula.expression.io.*;
import org.spldev.formula.solver.sat4j.*;
import org.spldev.pc_extraction.convert.*;
import org.spldev.util.*;
import org.spldev.util.io.*;
import org.spldev.util.io.binary.*;
import org.spldev.util.io.csv.*;
import org.spldev.util.logging.*;

public class TWiseEvaluator extends Evaluator {

	protected static final ListProperty<String> coverageT = new ListProperty<>("t", Property.StringConverter);
	protected static final GroupingProperty coverageGrouping = new GroupingProperty("grouping");

	protected CSVWriter evaluationWriter;

	private CNF modelCNF;
	private TWiseStatisticGenerator tWiseStatisticGenerator;

	private List<int[]> sampleArguments;
	private List<ValidityStatistic> sampleValidityStatistics;
	private List<CoverageStatistic> coverageStatistics;
	private String coverageCriterion;

	@Override
	public String getId() {
		return "eval-twise-evaluator";
	}

	@Override
	protected void addCSVWriters() {
		super.addCSVWriters();
		evaluationWriter = addCSVWriter("evaluation.csv", Arrays.asList("ModelID", "AlgorithmID", "SystemIteration",
			"AlgorithmIteration", "SamplePercentage", "Criterion", "Value"));
	}

	protected final HashMap<String, PresenceConditionManager> expressionMap = new HashMap<>();

	@Override
	public void evaluate() {
		tabFormatter.setTabLevel(0);
		if (config.systemIterations.getValue() > 0) {
			Logger.logInfo("Start");
			tabFormatter.incTabLevel();

			final Path samplesDir = config.outputPath.resolve("samples");
			List<Path> dirList;
			try (Stream<Path> fileStream = Files.list(samplesDir)) {
				dirList = fileStream.filter(Files::isReadable).filter(Files::isDirectory).collect(Collectors.toList());
			} catch (final IOException e) {
				Logger.logError(e);
				return;
			}
			Collections.sort(dirList, (p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()));

			tabFormatter.incTabLevel();
			dirList.forEach(this::readSamples);
			tabFormatter.setTabLevel(0);
			Logger.logInfo("Finished");
		} else {
			Logger.logInfo("Nothing to do");
		}
	}

	private void readSamples(Path sampleDir) {
		try {
			systemIndex = Integer.parseInt(sampleDir.getFileName().toString());
		} catch (final Exception e) {
			Logger.logError(e);
			return;
		}

		Logger.logInfo("System " + (systemIndex + 1));
		Logger.logInfo("Preparing...");
		tabFormatter.incTabLevel();

		final DIMACSFormat format = new DIMACSFormat();
		final Path modelFile = sampleDir.resolve("model." + format.getFileExtension());
		final Result<CNF> parseResult = FileHandler.load(modelFile, format).map(Clauses::convertToCNF);
//		if (parseResult.isEmpty()) {
//			Logger.logProblems(parseResult.getProblems());
//			return;
//		}

		modelCNF = parseResult.get();

		final TWiseConfigurationUtil util;
		if (!modelCNF.getClauses().isEmpty()) {
			util = new TWiseConfigurationUtil(modelCNF, new Sat4JSolver(modelCNF));
		} else {
			util = new TWiseConfigurationUtil(modelCNF, null);
		}

		util.computeRandomSample(1000);
		if (!modelCNF.getClauses().isEmpty()) {
			util.computeMIG(false, false);
		}
		tWiseStatisticGenerator = new TWiseStatisticGenerator(util);

		List<Path> sampleFileList;
		try (Stream<Path> fileStream = Files.list(sampleDir)) {
			sampleFileList = fileStream.filter(Files::isReadable).filter(Files::isRegularFile)
				.filter(file -> file.getFileName().toString().endsWith(".sample")).collect(Collectors.toList());
		} catch (final IOException e) {
			Logger.logError(e);
			tabFormatter.decTabLevel();
			return;
		}
		Collections.sort(sampleFileList,
			(p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()));

		tabFormatter.decTabLevel();
		Logger.logInfo("Reading Samples...");
		tabFormatter.incTabLevel();
		final List<List<? extends LiteralList>> samples = new ArrayList<>(sampleFileList.size());
		sampleArguments = new ArrayList<>(sampleFileList.size());
		for (final Path sampleFile : sampleFileList) {

			final List<LiteralList> sample;
			int[] argumentValues;
			try {
				final String fileName = sampleFile.getFileName().toString();
				final String[] arguments = fileName.substring(0, fileName.length() - ".sample".length()).split("_");
				sample = Files.lines(sampleFile).map(this::parseConfiguration).collect(Collectors.toList());

				argumentValues = new int[4];
				argumentValues[0] = Integer.parseInt(arguments[1]);
				argumentValues[1] = Integer.parseInt(arguments[2]);
				argumentValues[2] = Integer.parseInt(arguments[3]);
				argumentValues[3] = 100;

			} catch (final Exception e) {
				Logger.logError(e);
				continue;
			}
			// if Random
			if (argumentValues[1] == 8) {
				for (int p = 5; p <= 100; p += 5) {
					samples.add(sample.subList(0, (sample.size() * p) / 100));
					final int[] argumentValues2 = new int[4];
					argumentValues2[0] = argumentValues[0];
					argumentValues2[1] = argumentValues[1];
					argumentValues2[2] = argumentValues[2];
					argumentValues2[3] = p;
					sampleArguments.add(argumentValues2);
				}
			} else {
				samples.add(sample);
				sampleArguments.add(argumentValues);
			}
		}

		tabFormatter.decTabLevel();
		Logger.logInfo("Testing Validity...");
		tabFormatter.incTabLevel();
		sampleValidityStatistics = tWiseStatisticGenerator.getValidity(samples);
		for (int i = 0; i < sampleArguments.size(); i++) {
			final int i2 = i;
			writeCSV(evaluationWriter, writer -> writeValidity(writer, i2));
		}

		final int tSize = coverageT.getValue().size();
		final int gSize = coverageGrouping.getValue().size();
		int gIndex = 0;
		for (final String groupingValue : coverageGrouping.getValue()) {
			gIndex++;
			final List<List<PresenceCondition>> nodes = readExpressions(groupingValue, util)
				.getGroupedPresenceConditions();
			int tIndex = 0;
			for (final String tValue : coverageT.getValue()) {
				tIndex++;
				logCoverage(tSize, gSize, tIndex, gIndex);

				coverageCriterion = groupingValue + "_t" + tValue;
				coverageStatistics = tWiseStatisticGenerator.getCoverage(samples, nodes, Integer.parseInt(tValue),
					ConfigurationScore.NONE, true);
				for (int i = 0; i < sampleArguments.size(); i++) {
					final int i2 = i;
					writeCSV(evaluationWriter, writer -> writeCoverage(writer, i2));
				}

			}
		}
		tabFormatter.decTabLevel();
	}

	private PresenceConditionManager readExpressions(String group, TWiseConfigurationUtil util) {
		try {
			return new PresenceConditionManager(util,
				readExpressions(group, config.systemNames.get(systemIndex)).getExpressions());
		} catch (final Exception e) {
			Logger.logError(e);
			return null;
		}
	}

	public static PresenceConditionList readPCList(String name, String systemName) throws Exception {
		final SerializableObjectFormat<PresenceConditionList> format = new SerializableObjectFormat<>();
		final Path pcListFile = Constants.expressionsOutput.resolve(systemName)
			.resolve(name + "." + format.getFileExtension());
		return FileHandler.load(pcListFile, format).orElseThrow();
	}

	public static Expressions readExpressions(String group, String systemName) throws Exception {
		final SerializableObjectFormat<Expressions> format = new SerializableObjectFormat<>();
		final Path expFile = Constants.expressionsOutput.resolve(systemName)
			.resolve(Constants.groupedPCFileName + group + "." + format.getFileExtension());
		return FileHandler.load(expFile, format).orElseThrow();
	}

	private LiteralList parseConfiguration(String configuration) {
		final String[] literalStrings = configuration.split(",");
		final int[] literals = new int[literalStrings.length];
		for (int i = 0; i < literalStrings.length; i++) {
			literals[i] = Integer.parseInt(literalStrings[i]);
		}
		final LiteralList solution = new LiteralList(literals, Order.INDEX, false);
		return solution;
	}

	private void writeValidity(CSVWriter csvWriter, int i) {
		final int[] argumentValues = sampleArguments.get(i);
		final ValidityStatistic validityStatistic = sampleValidityStatistics.get(i);
		csvWriter.addValue(systemIndex);
		csvWriter.addValue(argumentValues[1]);
		csvWriter.addValue(argumentValues[0]);
		csvWriter.addValue(argumentValues[2]);
		csvWriter.addValue(argumentValues[3]);
		csvWriter.addValue("validity");
		csvWriter.addValue(validityStatistic.getValidInvalidRatio());
	}

	private void writeCoverage(CSVWriter csvWriter, int i) {
		final int[] argumentValues = sampleArguments.get(i);
		final CoverageStatistic coverageStatistic = coverageStatistics.get(i);
		csvWriter.addValue(systemIndex);
		csvWriter.addValue(argumentValues[1]);
		csvWriter.addValue(argumentValues[0]);
		csvWriter.addValue(argumentValues[2]);
		csvWriter.addValue(argumentValues[3]);
		csvWriter.addValue(coverageCriterion);
		csvWriter.addValue(coverageStatistic.getCoverage());
	}

	private void logCoverage(final int tSize, final int gSize, int tIndex, int gIndex) {
		final StringBuilder sb = new StringBuilder();
		sb.append("t: ");
		sb.append(tIndex);
		sb.append("/");
		sb.append(tSize);
		sb.append(" | g: ");
		sb.append(gIndex);
		sb.append("/");
		sb.append(gSize);
		Logger.logInfo(sb.toString());
	}

}
