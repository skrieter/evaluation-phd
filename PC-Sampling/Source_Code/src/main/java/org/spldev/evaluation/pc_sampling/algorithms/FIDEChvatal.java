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

public class FIDEChvatal extends AFIDESampling_Org {

	private final int t;

	public FIDEChvatal(int t, Path outputFile, Path fmFile) {
		super(outputFile, fmFile);
		this.t = t;
	}

	@Override
	protected void addCommandElements() {
		super.addCommandElements();
		addCommandElement("-a");
		addCommandElement("Chvatal");
		addCommandElement("-t");
		addCommandElement(Integer.toString(t));
	}

	@Override
	public String getName() {
		return "FIDE-Chvatal";
	}

	@Override
	public String getParameterSettings() {
		return "t" + t;
	}

}
