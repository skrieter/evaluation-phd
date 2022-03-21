package de.ovgu.sple.sampling;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import de.ovgu.featureide.fm.core.analysis.cnf.*;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.*;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.*;
import de.ovgu.featureide.fm.core.analysis.mig.*;
import de.ovgu.featureide.fm.core.base.*;
import de.ovgu.featureide.fm.core.functional.*;
import de.ovgu.featureide.fm.core.init.*;
import de.ovgu.featureide.fm.core.io.manager.*;
import de.ovgu.featureide.fm.core.job.*;

public class SamplingChallenge {

	private static Path out = Paths.get("YASA");
	private static Path in = Paths.get("models");

	private static int t = 2;
	private static int iterations = 1;
	private static int randomSampleSize = 10000;
	private static int logFrequency = 1;
	private static boolean verbosity;

	public static void main(String[] args) throws IOException {
		FMCoreLibrary.getInstance().install();

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-modeldir":
				in = Paths.get(args[++i]);
				break;
			case "-outputdir":
				out = Paths.get(args[++i]);
				break;
			case "-t":
				t = Integer.parseInt(args[++i]);
				break;
			case "-iterations":
				iterations = Integer.parseInt(args[++i]);
				break;
			case "-randomSampleSize":
				randomSampleSize = Integer.parseInt(args[++i]);
				break;
			case "-logFrequency":
				logFrequency = Integer.parseInt(args[++i]);
				break;
			}
		}
		verbosity = logFrequency > 0;

		Files.walk(in).filter(Files::isRegularFile).forEach(SamplingChallenge::processFile);
	}

	private static void processFile(Path modelFile) {
		if (!modelFile.getFileName().toString().equals("model.xml")) {
			return;
		}
		Path relativePath = in.relativize(modelFile).getParent();
		Path systemPath = relativePath.getName(0);
		Path dataPath = systemPath.relativize(relativePath);
		Path relativeOutPath = out.resolve(systemPath).resolve("samples").resolve(dataPath).resolve("YASA");
//		out / system / samples / date / YASA / products
		Path productPath = relativeOutPath.resolve("products_" + t);
		if (Files.exists(productPath)) {
			System.out.println("Skipping model " + modelFile);
			return;
		}
		
		long startTime = System.nanoTime();
		if (verbosity) {
			System.out.println("Read model... " + modelFile);
		}
		
		IFeatureModel fm = FeatureModelIO.getInstance().load(modelFile);


		if (verbosity) {
			System.out.println("Compute CNF... ");
		}
		CNF cnf = new FeatureModelFormula(fm).getCNF();
		
		final List<String> features = Functional
				.toList(fm.getFeatures().stream()
				.map(IFeature::getName)
				.filter(((Predicate<String>)SamplingChallenge::isNumber).negate())
				.filter(name -> !name.contains("="))
				.collect(Collectors.toList()));

		if (verbosity) {
			System.out.println("\t#Features:      " + fm.getNumberOfFeatures());
			System.out.println("\t#Constraints:   " + fm.getConstraintCount());
			System.out.println("\t#Real Features: " + features.size());
		}

		if (verbosity) {
			System.out.println("Run generator... ");
		}
		List<LiteralSet> result = createSample(fm, cnf);
//		List<LiteralSet> result = Collections.emptyList();
		long endTime = System.nanoTime();

		System.out.println(((endTime - startTime) / 1_000_000) / 1000.0);
		write(relativeOutPath, productPath, cnf, result, startTime, endTime);
//		System.out.println(test(fm, cnf, result));
	}

	private static void getCompletelyConnectedVertices(int literal, Set<Vertex> connected, ModalImplicationGraph g) {
		final int var = Math.abs(literal);
		Vertex vPos = g.getVertex(var);
		Vertex vNeg = g.getVertex(-var);
		
		List<Integer> newAdj = new ArrayList<>();
		
		Set<Integer> connectedVars = getConnectedVertices(literal, g);
		for (Integer connectedVar : connectedVars) {
			Set<Integer> reverseConnectedVertices = getConnectedVertices(connectedVar, g);
			connectedVars.retainAll(reverseConnectedVertices);
		}
		
		if (connected.add(null))
		{
			newAdj.add(null);
		}
		g.getComplexClauses().get(vPos.getComplexClauses()[0]);
	}
	
	private static Set<Integer> getConnectedVertices(int literal, ModalImplicationGraph g) {
		Set<Integer> newAdj = new HashSet<>();
		addConnectedVertices(g, literal, newAdj);
		addConnectedVertices(g, -literal, newAdj);
		newAdj.remove(literal);
		return newAdj;
	}

	private static void addConnectedVertices(ModalImplicationGraph g, int literal, Set<Integer> newAdj) {
		Vertex vertex = g.getVertex(literal);
		for (int strongEdge : vertex.getStrongEdges()) {
			newAdj.add(Math.abs(strongEdge));
		}
		for (int complexClauseId : vertex.getComplexClauses()) {
			LiteralSet complexClause = g.getComplexClauses().get(complexClauseId);
			for (int weakEdge : complexClause.getLiterals()) {
				newAdj.add(Math.abs(weakEdge));
			}
		}
	}
	
	private static boolean isNumber(String name) {
		try {
			Integer.parseInt(name);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static List<LiteralSet> createSample(IFeatureModel fm, CNF cnf) {
		TWiseConfigurationGenerator gen = new TWiseConfigurationGenerator(cnf, getExpressions(fm, cnf), t);
		gen.setRandom(new Random(0));
		gen.setIterations(iterations);
		gen.setRandomSampleSize(randomSampleSize);
		gen.setUseMig(true);
		gen.setMigCheckRedundancy(false);
		gen.setMigDetectStrong(false);
		gen.setLogFrequency(logFrequency * 1_000);
		TWiseConfigurationGenerator.VERBOSE = verbosity;
		return LongRunningWrapper.runMethod(gen);
	}

	private static List<List<ClauseList>> getExpressions(IFeatureModel fm, CNF cnf) {
		final List<List<ClauseList>> expressions = TWiseConfigurationGenerator
				.convertLiterals(cnf.getVariables().convertToLiterals(
						Functional
								.toList(fm.getFeatures().stream().map(IFeature::getName).collect(Collectors.toList())),
						true, true));
		return expressions;
	}

	private static void write(Path relativeOutPath, Path productPath, CNF cnf, List<LiteralSet> result, long startTime, long endTime) {
		try {
			Files.createDirectories(productPath);
//			CSVWriter csvWriter = new CSVWriter();
//			csvWriter.setAppend(false);
//			csvWriter.setOutputDirectory(relativeOutPath);
//			csvWriter.setFileName("Sample_Statistics.csv");
//
//			csvWriter.setKeepLines(false);
//			csvWriter.setHeader(Arrays.asList("Coverage (t)", "Sample Size", "Time", "Memory Usage"));
//			csvWriter.setSeparator(";");
//			csvWriter.flush();
//
//			csvWriter.createNewLine();
//			csvWriter.addValue(t);
//			csvWriter.addValue(result.size());
//			csvWriter.addValue(((endTime - startTime) / 1_000_000));
//			csvWriter.addValue("< 1 GiB");
//			csvWriter.flush();

			int productCounter = 1;
			for (LiteralSet literalSet : result) {
				ArrayList<String> configFileContent = new ArrayList<>();
				for (int literal : literalSet.getLiterals()) {
					if (literal > 0) {
						configFileContent.add(cnf.getVariables().getName(literal));
					}
				}
				Path configFile = productPath.resolve(Integer.toString(productCounter++) + ".config");
				Files.write(configFile, configFileContent);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean test(IFeatureModel fm, CNF cnf, List<LiteralSet> result) {
		TWiseConfigurationTester tester = new TWiseConfigurationTester(cnf);
		tester.setNodes(getExpressions(fm, cnf));
		tester.setSample(result);
		tester.setT(t);
		return 1 == tester.getCoverage().getCoverage();
	}

}
