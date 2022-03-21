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
import java.util.function.*;

public class Constants {

	public final static Path systems = Paths.get("resources/systems");

	public final static Path output = Paths.get("gen");

	public final static Path kbuildOutput = output.resolve("kbuild");
	public final static Path expressionsOutput = output.resolve("presenceConditions");

	public final static String convertedPCFileName = "pclist";
	public final static String convertedPCFMFileName = "pclist_fm";
	public final static String groupedPCFileName = "grouped_";

	public final static String pcFileExtension = "s";

	public static final Function<String, Predicate<Path>> fileFilterCreator = regex -> file -> Files.isReadable(file)
		&& Files.isRegularFile(file) && file.getFileName().toString().matches(regex);

	public static final String FileNameRegex = ".+[.](c|h|cxx|hxx)\\Z";
	public static final Predicate<Path> fileFilter = fileFilterCreator.apply(FileNameRegex);

}
