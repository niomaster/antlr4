package org.antlr.v4.codegen.model.extractors;

import org.antlr.v4.codegen.CodeGenerator;
import org.antlr.v4.codegen.model.extractors.intermediate.IGrammar;
import org.antlr.v4.codegen.model.extractors.intermediate.INode;
import org.antlr.v4.codegen.model.extractors.scala.Name;
import org.antlr.v4.codegen.model.extractors.scala.ScalaNode;
import org.antlr.v4.codegen.model.extractors.scala.Trait;
import org.antlr.v4.runtime.misc.Pair;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class Extractors {
	public static void writeExtractors(CodeGenerator generator) {
		IGrammar g = IGrammar.from(generator.g);
		Namespace namespace = new Namespace();
		g.collectNames(namespace);
		Relation<String> alternatives = new Relation<>();
		g.collectAlts(namespace, alternatives);

		HashSet<Pair<String, String>> alternativeHatchFixes = new HashSet<>();
		HashSet<String> hatchedTypes = new HashSet<>();

		// Collect cross-grammar alternatives
		for(String left : alternatives.keySet()) {
			for(String right : alternatives.valuesOf(left)) {
				INode declLeft = namespace.getDecl(left);
				INode declRight = namespace.getDecl(right);
				String defLeft = declLeft.getDefiningGrammar();
				String defRight = declRight.getDefiningGrammar();
				if(defLeft == null) defLeft = generator.g.name;
				if(defRight == null) defRight = generator.g.name;
				if(!defLeft.equals(defRight)) {
					alternativeHatchFixes.add(new Pair<>(left, right));
					hatchedTypes.add(right);
				}
			}
		}

		// Redirect alternative via unsealed trait
		for(Pair<String, String> fix : alternativeHatchFixes) {
			alternatives.valuesOf(fix.a).remove(fix.b);
			alternatives.valuesOf(fix.a).add(fix.b + "EscapeHatch");
		}

		HashMap<ScalaNode, String> decls = new HashMap<>();
		g.collectDecls(namespace, alternatives, decls);

		for(String fix : hatchedTypes) {
			decls.put(
					new Trait(new Name(fix + "EscapeHatch"),
							Collections.<ScalaNode>singletonList(new Name(fix)),
							false),
					namespace.getDecl(fix).getDefiningGrammar());
		}

		HashMap<String, HashSet<ScalaNode>> files = new HashMap<>();

		for(Map.Entry<ScalaNode, String> decl : decls.entrySet()) {
			String file = decl.getValue();
			if(file == null) {
				file = generator.g.name;
			}

			if(!files.containsKey(file)) {
				files.put(file, new HashSet<ScalaNode>());
			}

			files.get(file).add(decl.getKey());
		}

		System.out.println(files.keySet());

		for(Map.Entry<String, HashSet<ScalaNode>> file : files.entrySet()) {
			try(Writer w = generator.tool.getOutputFileWriter(generator.g, file.getKey() + "Models.scala")) {
				w.write("package ");
				w.write(generator.tool.genPackage);
				w.write("\n");

				w.write("import ");
				w.write(generator.tool.genPackage);
				w.write(".");
				w.write(generator.g.name);
				w.write("._\n");

				for(String imp : files.keySet()) {
					if(!imp.equals(file.getKey())) {
						w.write("import ");
						w.write(generator.tool.genPackage);
						w.write(".");
						w.write(imp + "Models");
						w.write("._\n");
					}
				}

				w.write("object ");
				w.write(file.getKey() + "Models");
				w.write(" {\n");
				for(ScalaNode node : file.getValue()) {
					w.write(node.toString());
					w.write("\n");
				}
				w.write("}\n");
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		try(Writer w = generator.tool.getOutputFileWriter(generator.g, generator.g.name + "Converters.scala")) {
			w.write("package ");
			w.write(generator.tool.genPackage);
			w.write("\n");
			w.write("import scala.collection.mutable\n");
			w.write("import org.antlr.v4.runtime.{ParserRuleContext, CommonToken}\n");
			w.write("import org.antlr.v4.runtime.tree.{ParseTree, TerminalNode}\n");
//			w.write("import ");
//			w.write(generator.tool.genPackage);
//			w.write(".");
//			w.write(generator.g.name);
//			w.write("._\n");

			for(String imp : files.keySet()) {
				w.write("import ");
				w.write(generator.tool.genPackage);
				w.write(".");
				w.write(imp + "Models");
				w.write("._\n");
			}

			w.write("object ");
			w.write(generator.g.name);
			w.write("Converters {\n");
			List<ScalaNode> converters = new ArrayList<>();
			g.collectConverters(namespace, alternatives, converters);

			for (ScalaNode converter : converters) {
				w.write(converter.toString());
				w.write("\n");
			}

			w.write("}\n");
		} catch(IOException e) {
			e.printStackTrace();
		}

//		System.out.println(alternativeTraits);


//		Grammar g = factory.getGrammar();
//	  	this.genPackage = g.tool.genPackage;
//	  	this.name = factory.getGrammar().getRecognizerName();
//	  	this.rules = new ArrayList<>();
//	  	for(Rule rule : g.rules.values()) {
//	  	  	this.rules.add(new ExtractorsRule(factory, rule));
//		}
	}
}
