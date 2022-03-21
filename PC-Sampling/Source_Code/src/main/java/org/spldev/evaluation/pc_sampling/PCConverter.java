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

import java.nio.file.*;
import java.util.*;

import org.spldev.evaluation.*;
import org.spldev.evaluation.util.*;
import org.spldev.formula.clauses.*;
import org.spldev.formula.expression.*;
import org.spldev.formula.expression.io.*;
import org.spldev.pc_extraction.convert.*;
import org.spldev.util.*;
import org.spldev.util.io.*;
import org.spldev.util.io.binary.*;
import org.spldev.util.io.csv.*;
import org.spldev.util.io.format.*;
import org.spldev.util.logging.*;

public class PCConverter extends Evaluator {

	protected CSVWriter conversionWriter;

	@Override
	public String getId() {
		return "eval-pc-converter";
	}

	@Override
	protected void addCSVWriters() {
		super.addCSVWriters();
		conversionWriter = addCSVWriter("conversion.csv",
			Arrays.asList("ID", "Mode", "Iteration", "Time", "Size", "Error", "Clauses", "Literals"));
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
//						.getElement(new NoAbstractCNFCreator()).normalize();
//				CNFSlicer slicer = new CNFSlicer(null);

				try {
					if (cnf != null) {
						evalConvert(Constants.convertedPCFMFileName, cnf, systemName);
					}
					evalConvert(Constants.convertedPCFileName, null, systemName);
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

	private PresenceConditionList evalConvert(String fileName, CNF cnf, String systemName) throws Exception {
		PresenceConditionList pcList = null;
		final Converter pcProcessor = new Converter();
		final Path extractionPath = Constants.expressionsOutput.resolve(systemName);
		for (int i = 0; i < config.systemIterations.getValue(); i++) {
			conversionWriter.createNewLine();
			try {
				conversionWriter.addValue(config.systemIDs.get(systemIndex));
				conversionWriter.addValue(fileName);
				conversionWriter.addValue(i);

				final long localTime = System.nanoTime();
				pcList = pcProcessor.convert(cnf, extractionPath);
				final long timeNeeded = System.nanoTime() - localTime;

				conversionWriter.addValue(timeNeeded);
				if (pcList != null) {
					final HashSet<CNF> pcs = new HashSet<>();
					long countClauses = 0;
					long countLiterals = 0;
					for (final PresenceCondition pc : pcList) {
						final CNF dnf = pc.getDnf();
						if (pcs.add(dnf)) {
							countClauses += dnf.getClauses().size();
							for (final LiteralList clause : dnf.getClauses()) {
								countLiterals += clause.size();
							}
						}
						final CNF ndnf = pc.getNegatedDnf();
						if (pcs.add(ndnf)) {
							countClauses += ndnf.getClauses().size();
							for (final LiteralList clause : ndnf.getClauses()) {
								countLiterals += clause.size();
							}
						}
					}
					conversionWriter.addValue(pcs.size());
					conversionWriter.addValue(false);
					conversionWriter.addValue(countClauses);
					conversionWriter.addValue(countLiterals);
				} else {
					conversionWriter.addValue(0);
					conversionWriter.addValue(true);
					conversionWriter.addValue(0);
					conversionWriter.addValue(0);
				}

				Logger.logInfo("convert -> " + Double.toString((timeNeeded / 1_000_000) / 1_000.0));
			} catch (final Exception e) {
				conversionWriter.removeLastLine();
				Logger.logError(e);
			} finally {
				conversionWriter.flush();
			}
		}

		if (pcList != null) {
			final SerializableObjectFormat<PresenceConditionList> format = new SerializableObjectFormat<>();
			final Path pcListFile = Constants.expressionsOutput.resolve(config.systemNames.get(systemIndex))
				.resolve(fileName + "." + format.getFileExtension());
			FileHandler.save(pcList, pcListFile, format);
		}
		return pcList;
	}

}
