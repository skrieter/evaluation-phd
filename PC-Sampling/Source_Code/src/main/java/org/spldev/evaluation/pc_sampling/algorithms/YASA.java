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

public class YASA extends AFIDESampling {

	private Path expressionFile;
	private String groupingValue;

	private int t;
	private int m;

	public YASA(Path outputFile, Path fmFile) {
		super(outputFile, fmFile);
	}

	@Override
	protected void addCommandElements() {
		super.addCommandElements();
		addCommandElement("-a");
		addCommandElement("YASA");
		addCommandElement("-t");
		addCommandElement(Integer.toString(t));
		addCommandElement("-m");
		addCommandElement(Integer.toString(m));
		if (expressionFile != null) {
			addCommandElement("-e");
			addCommandElement(expressionFile.toString());
		}
		if (seed != null) {
			addCommandElement("-s");
			addCommandElement(seed.toString());
		}
	}

	@Override
	public String getName() {
		return "YASA";
	}

	@Override
	public String getParameterSettings() {
		return "t" + t + "_m" + m + "_" + groupingValue;
	}

	public Path getExpressionFile() {
		return expressionFile;
	}

	public void setExpressionFile(Path expressionFile) {
		this.expressionFile = expressionFile;
	}

	public String getGroupingValue() {
		return groupingValue;
	}

	public void setGroupingValue(String groupingValue) {
		this.groupingValue = groupingValue;
	}

	public int getT() {
		return t;
	}

	public void setT(int t) {
		this.t = t;
	}

	public int getM() {
		return m;
	}

	public void setM(int m) {
		this.m = m;
	}

}
