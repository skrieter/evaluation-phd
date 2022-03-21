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
import java.util.regex.*;

import org.spldev.evaluation.pc_sampling.*;
import org.spldev.evaluation.process.*;
import org.spldev.formula.clauses.*;
import org.spldev.formula.clauses.LiteralList.*;
import org.spldev.formula.expression.atomic.literal.*;
import org.spldev.util.logging.*;

public abstract class PLEDGE extends Algorithm<org.spldev.formula.clauses.SolutionList> {

	private final Path outputFile;
	private final Path fmFile;

	private long numberOfConfigurations = 10;
	private long timeout = 1000;

	public PLEDGE(Path outputFile, Path fmFile) {
		this.outputFile = outputFile;
		this.fmFile = fmFile;
	}

	public long getNumberOfConfigurations() {
		return numberOfConfigurations;
	}

	public void setNumberOfConfigurations(long numberOfConfigurations) {
		this.numberOfConfigurations = numberOfConfigurations;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public void preProcess() throws Exception {
		numberOfConfigurations = TWiseSampler.YASA_MAX_SIZE;
		super.preProcess();
	}

	@Override
	protected void addCommandElements() {
		addCommandElement("java");
		addCommandElement("-da");
		addCommandElement("-Xmx14g");
		addCommandElement("-Xms2g");
		addCommandElement("-cp");
		addCommandElement("tools/Pledge/*");
		addCommandElement("pledge.Main");
		addCommandElement("generate_products");
		addCommandElement("-dimacs");
		addCommandElement("-fm");
		addCommandElement(fmFile.toString());
		addCommandElement("-o");
		addCommandElement(outputFile.toString());
		addCommandElement("-timeAllowedMS");
		addCommandElement(Long.toString(timeout));
		addCommandElement("-nbProds");
		addCommandElement(Long.toString(numberOfConfigurations));
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

		final Pattern variableNamePattern = Pattern.compile("\\A\\d+->(.*)\\Z");

		final ArrayList<String> featureNames = new ArrayList<>();
		final ListIterator<String> it = lines.listIterator();
		while (it.hasNext()) {
			final String line = it.next().trim();
			final Matcher matcher = variableNamePattern.matcher(line);
			if (matcher.matches()) {
				featureNames.add(matcher.group(1));
			} else {
				it.previous();
				break;
			}
		}
		final VariableMap variables = VariableMap.fromNames(featureNames);

		final ArrayList<LiteralList> configurationList = new ArrayList<>();
		while (it.hasNext()) {
			final String line = it.next().trim();
			final int[] configurationArray = new int[variables.size()];
			final String[] featureSelections = line.split(";");
			for (int i = 0; i < configurationArray.length; i++) {
				configurationArray[i] = Integer.parseInt(featureSelections[i]);
			}
			configurationList.add(new LiteralList(configurationArray, Order.INDEX));
		}
		return new SolutionList(variables, configurationList);
	}

	@Override
	public String getParameterSettings() {
		return "";
	}

}
