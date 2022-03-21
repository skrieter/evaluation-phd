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

import org.spldev.evaluation.*;
import org.spldev.evaluation.util.*;
import org.spldev.formula.clauses.*;
import org.spldev.formula.expression.*;
import org.spldev.formula.expression.io.*;
import org.spldev.pc_extraction.convert.*;
import org.spldev.pc_extraction.convert.Grouper.*;
import org.spldev.util.*;
import org.spldev.util.io.*;
import org.spldev.util.io.binary.*;
import org.spldev.util.io.csv.*;
import org.spldev.util.io.format.*;
import org.spldev.util.logging.*;

public class PCGrouper extends Evaluator {

	protected CSVWriter groupingWriter;

	@Override
	public String getId() {
		return "eval-pc-grouper";
	}

	@Override
	protected void addCSVWriters() {
		super.addCSVWriters();
		groupingWriter = addCSVWriter("grouping.csv",
			Arrays.asList("ID", "Mode", "Iteration", "Time", "Size", "Error"));
	}

	@Override
	public void evaluate() {
		tabFormatter.setTabLevel(0);
		if (config.systemIterations.getValue() > 0) {
			Logger.logInfo("Start");
			tabFormatter.incTabLevel();

			final int systemIndexEnd = config.systemNames.size();
			for (systemIndex = 0; systemIndex < systemIndexEnd; systemIndex++) {
				logSystem();
				tabFormatter.incTabLevel();
				final String systemName = config.systemNames.get(systemIndex);

				final ModelReader<Formula> fmReader = new ModelReader<>();
				fmReader.setPathToFiles(config.modelPath);
				fmReader.setFormatSupplier(FormatSupplier.of(new DIMACSFormat()));
				final Result<CNF> fm = fmReader.read(systemName).map(Clauses::convertToCNF);
				if (fm.isEmpty()) {
					Logger.logInfo("No feature model!");
				}
				final CNF cnf = fm.get();

				try {
					if (cnf != null) {
						evalGroup(Grouping.FM_ONLY, cnf, systemName);
						evalGroup(Grouping.PC_ALL_FM, cnf, systemName);
						evalGroup(Grouping.PC_ALL_FM_FM, cnf, systemName);
						evalGroup(Grouping.PC_FOLDER_FM, cnf, systemName);
						evalGroup(Grouping.PC_FILE_FM, cnf, systemName);
						evalGroup(Grouping.PC_VARS_FM, cnf, systemName);
					}
					evalGroup(Grouping.PC_ALL, null, systemName);
					evalGroup(Grouping.PC_FOLDER, null, systemName);
					evalGroup(Grouping.PC_FILE, null, systemName);
					evalGroup(Grouping.PC_VARS, null, systemName);
				} catch (final Exception e) {
					Logger.logError(e);
				}
				tabFormatter.decTabLevel();
			}
			tabFormatter.setTabLevel(0);
			Logger.logInfo("Finished");
		} else {
			Logger.logInfo("Nothing to do");
		}
	}

	private Expressions evalGroup(Grouping groupingValue, CNF cnf, String systemName) throws Exception {
		Expressions expressions = null;
		final PresenceConditionList pcList = TWiseEvaluator
			.readPCList(cnf == null ? Constants.convertedPCFileName : Constants.convertedPCFMFileName, systemName);
		final Grouper grouper = new Grouper();
		for (int i = 0; i < config.systemIterations.getValue(); i++) {
			groupingWriter.createNewLine();
			try {
				groupingWriter.addValue(config.systemIDs.get(systemIndex));
				groupingWriter.addValue(groupingValue);
				groupingWriter.addValue(i);

				final long localTime = System.nanoTime();
				expressions = grouper.group(pcList, groupingValue);
				final long timeNeeded = System.nanoTime() - localTime;

				groupingWriter.addValue(timeNeeded);

				if (expressions != null) {
					final HashSet<ClauseList> pcs = new HashSet<>();
					for (final List<ClauseList> group : expressions.getExpressions()) {
						pcs.addAll(group);
					}
					groupingWriter.addValue(pcs.size());
					groupingWriter.addValue(false);
				} else {
					groupingWriter.addValue(0);
					groupingWriter.addValue(true);
				}

				Logger.logInfo(groupingValue + " -> " + Double.toString((timeNeeded / 1_000_000) / 1_000.0));
			} catch (final FileNotFoundException e) {
				groupingWriter.removeLastLine();
			} catch (final Exception e) {
				groupingWriter.removeLastLine();
				Logger.logError(e);
			} finally {
				groupingWriter.flush();
			}
		}

		if (expressions != null) {
			final SerializableObjectFormat<Expressions> format = new SerializableObjectFormat<>();
			final Path expFile = Constants.expressionsOutput.resolve(config.systemNames.get(systemIndex))
				.resolve(Constants.groupedPCFileName + groupingValue + "." + format.getFileExtension());
			FileHandler.save(expressions, expFile, format);
			Logger.logInfo(Constants.groupedPCFileName + groupingValue + " OK");
		} else {
			Logger.logInfo(Constants.groupedPCFileName + groupingValue + " FAIL");
		}
		return expressions;
	}

}
