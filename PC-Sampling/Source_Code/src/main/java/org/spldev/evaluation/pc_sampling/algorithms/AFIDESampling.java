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

import org.spldev.evaluation.process.*;
import org.spldev.formula.clauses.*;
import org.spldev.formula.io.*;
import org.spldev.util.Result;
import org.spldev.util.io.*;
import org.spldev.util.logging.*;

public abstract class AFIDESampling extends Algorithm<SolutionList> {

	private final Path outputFile;
	private final Path fmFile;

	protected Long seed;
	protected int limit;

	public AFIDESampling(Path outputFile, Path fmFile) {
		this.outputFile = outputFile;
		this.fmFile = fmFile;
	}

	@Override
	protected void addCommandElements() {
		addCommandElement("java");
		addCommandElement("-da");
		addCommandElement("-Xmx14g");
		addCommandElement("-Xms2g");
		addCommandElement("-cp");
		addCommandElement("resources/tools/FIDE/*");
		addCommandElement("org.spldev.util.cli.CLI");
		addCommandElement("genconfig");
		addCommandElement("-o");
		addCommandElement(outputFile.toString());
		addCommandElement("-fm");
		addCommandElement(fmFile.toString());
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
		final Result<SolutionList> parse = FileHandler.load(outputFile, new ConfigurationListFormat());
		if (parse.isEmpty()) {
			Logger.logProblems(parse.getProblems());
			throw new IOException();
		}
		return parse.get();
	}

	public Long getSeed() {
		return seed;
	}

	public void setSeed(Long seed) {
		this.seed = seed;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

}
