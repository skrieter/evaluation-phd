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

import java.nio.file.*;

import org.spldev.evaluation.pc_sampling.*;

public class FIDERandom extends AFIDESampling {

	public FIDERandom(Path outputFile, Path fmFile) {
		super(outputFile, fmFile);
	}

	@Override
	public void preProcess() throws Exception {
		if (limit == 0) {
			limit = TWiseSampler.YASA_MAX_SIZE;
		}
		super.preProcess();
	}

	@Override
	protected void addCommandElements() {
		super.addCommandElements();
		addCommandElement("-a");
		addCommandElement("Random");
		addCommandElement("-l");
		addCommandElement(Integer.toString(limit));
		if (seed != null) {
			addCommandElement("-s");
			addCommandElement(seed.toString());
		}
	}

	@Override
	public String getName() {
		return "FIDE-Random";
	}

	@Override
	public String getParameterSettings() {
		return "";
	}

}
