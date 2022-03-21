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
package org.spldev.evaluation.pc_sampling.algorithms;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.spldev.evaluation.process.*;
import org.spldev.formula.clauses.*;
import org.spldev.formula.clauses.LiteralList.*;
import org.spldev.formula.expression.atomic.literal.*;
import org.spldev.util.logging.*;

public abstract class ASPLCATSampling extends Algorithm<org.spldev.formula.clauses.SolutionList> {

	private final Path outputFile;
	private final Path fmFile;

	protected final int t;

	public ASPLCATSampling(int t, Path outputFile, Path fmFile) {
		this.outputFile = outputFile;
		this.fmFile = fmFile;
		this.t = t;
	}

	@Override
	protected void addCommandElements() {
		addCommandElement("java");
		addCommandElement("-da");
		addCommandElement("-Xmx14g");
		addCommandElement("-Xms2g");
		addCommandElement("-cp");
		addCommandElement("resources/tools/SPLCAT/*");
		addCommandElement("no.sintef.ict.splcatool.SPLCATool");
		addCommandElement("-t");
		addCommandElement("t_wise");
		addCommandElement("-fm");
		addCommandElement(fmFile.toString());
		addCommandElement("-s");
		addCommandElement(Integer.toString(t));
		addCommandElement("-o");
		addCommandElement(outputFile.toString());
	}

	@Override
	public void postProcess() {
		try {
			Files.deleteIfExists(outputFile);
		} catch (final IOException e) {
			Logger.logError(e);
		}
	}

	@Override
	public SolutionList parseResults() throws IOException {
		if (!Files.isReadable(outputFile)) {
			return null;
		}

		final List<String> lines = Files.readAllLines(outputFile);
		if (lines.isEmpty()) {
			return null;
		}

		int numberOfConfigurations = 0;
		final String header = lines.get(0);
		final int length = header.length();
		if (length > 1) {
			final int lastSeparatorIndex = header.lastIndexOf(';', length - 2);
			if (lastSeparatorIndex > -1) {
				final String lastColumn = header.substring(lastSeparatorIndex + 1, length - 1);
				numberOfConfigurations = Integer.parseInt(lastColumn) + 1;
			}
		}

		final List<String> featureLines = lines.subList(1, lines.size());
		final int numberOfFeatures = featureLines.size();
		final ArrayList<String> featureNames = new ArrayList<>(numberOfFeatures);
		for (final String line : featureLines) {
			final String featureName = line.substring(0, line.indexOf(";"));
			featureNames.add(featureName);
		}
		final VariableMap variables = VariableMap.fromNames(featureNames);

		final List<int[]> configurationList = new ArrayList<>(numberOfConfigurations);
		for (int i = 0; i < numberOfConfigurations; i++) {
			configurationList.add(new int[variables.size()]);
		}

		for (final String line : featureLines) {
			final String[] columns = line.split(";");
			final int variable = variables.getIndex(columns[0]).get();
			final int variableIndex = variable - 1;
			int columnIndex = 1;
			for (final int[] configuration : configurationList) {
				configuration[variableIndex] = "X".equals(columns[columnIndex++]) ? variable : -variable;
			}
		}

		final ArrayList<LiteralList> configurationList2 = new ArrayList<>(numberOfConfigurations);
		for (final int[] configuration : configurationList) {
			configurationList2.add(new LiteralList(configuration, Order.INDEX));
		}
		return new SolutionList(variables, configurationList2);
	}

	@Override
	public String getParameterSettings() {
		return "t" + t;
	}

}
