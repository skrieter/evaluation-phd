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
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

import org.spldev.formula.expression.*;
import org.spldev.formula.expression.compound.*;
import org.spldev.formula.expression.io.parse.*;

public class TestSuiteParser {

	private static final Pattern regex = Pattern.compile("\\ATestCase (\\d+) with configurations (.+):\\Z");

	private static final Pattern regex2 = Pattern.compile("\\ASolution \\[literals=\\[(.+)\\]\\]\\Z");

	private static final NodeReader nr = new NodeReader();
	static {
		nr.setSymbols(PropositionalModelSymbols.INSTANCE);
	}

	private static class FileFilter implements Predicate<Path> {

		private final Pattern regex;

		public FileFilter(Pattern regex) {
			this.regex = regex;
		}

		public FileFilter(String regex) {
			this.regex = Pattern.compile(regex);
		}

		@Override
		public boolean test(Path t) {
			return regex.matcher(t.getFileName().toString()).matches();
		}
	}

	private static class MinDir implements Comparator<Path> {

		@Override
		public int compare(Path o1, Path o2) {
			final int n1 = Integer.parseInt(o1.getFileName().toString());
			final int n2 = Integer.parseInt(o2.getFileName().toString());
			return n1 - n2;
		}
	}

	public static void main(String[] args) throws IOException {
		final String systemName = "lcm";
		final int t = 2;
		final String mode = "presenceCondition_all";

		final Path filePath = Paths.get("mutation_tests/" + systemName);

		final FileFilter fileFilter = new FileFilter("testSuite_mutation_\\d+[.]txt");
		final List<Formula> mutations = Files.walk(filePath) //
			.filter(fileFilter) // Filter mutation files
			.peek(System.out::println) // Console output
			.map(TestSuiteParser::extractNodes) // Extract test conditions
			.collect(Collectors.toList()); // To list
		mutations.forEach(System.out::println);

		final Path resultDir = Paths.get("gen/results/");
		final MinDir minDir = new MinDir();
		final FileFilter fileFilter2 = new FileFilter("\\d+");
		final Optional<Path> lastResultPath = Files.walk(resultDir).filter(Files::isDirectory).filter(fileFilter2)
			.min(minDir);
		if (lastResultPath.isPresent()) {
			final Path sampleDir = lastResultPath.get().resolve("/samples/");
			final String name = mode + "_" + t + "_" + systemName;
			final ArrayList<String> features = getFeatures(name, sampleDir);
			final List<List<String>> selectedFeatureList = getSamples(sampleDir, name, features);
		}
	}

	private static List<List<String>> getSamples(Path sampleDir, String name, final ArrayList<String> features)
		throws IOException {
		final Path sampleFile = sampleDir.resolve(name + ".sample");
		final List<List<String>> selectedFeatureList = new ArrayList<>();
		for (final String line : Files.readAllLines(sampleFile)) {
			final ArrayList<String> selectedFeatures = new ArrayList<>();
			final Matcher matcher = regex2.matcher(line);
			if (matcher.find()) {
				for (final String string : matcher.group(1).split(", ")) {
					final int index = Integer.parseInt(string);
					if (index > 0) {
						selectedFeatures.add(features.get(index - 1));
					}
				}
			}
			selectedFeatureList.add(selectedFeatures);
		}
		return selectedFeatureList;
	}

	private static ArrayList<String> getFeatures(String name, Path sampleDir) throws IOException {
		final Path featureFile = sampleDir.resolve(name + ".features");
		final ArrayList<String> features = new ArrayList<>();
		for (final String line : Files.readAllLines(featureFile)) {
			final String featureName = line.trim();
			if (!featureName.isEmpty()) {
				features.add(featureName.substring("__SELECTED_FEATURE_".length()));
			}
		}
		return features;
	}

	private static Formula extractNodes(Path file) {
		final List<Formula> nodeList = new ArrayList<>();
		try {
			for (final String line : Files.readAllLines(file)) {
				final Matcher matcher = regex.matcher(line);
				if (matcher.find()) {
					nodeList.add(nr.read(matcher.group(2)).flatMap(Formulas::toCNF).get());
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return new Or(nodeList);
	}

}
