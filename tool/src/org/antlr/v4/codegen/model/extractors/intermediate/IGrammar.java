package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.*;
import org.antlr.v4.codegen.model.extractors.scala.Class;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LeftRecursiveRule;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarAST;
import org.antlr.v4.tool.ast.RuleAST;

import java.util.*;

public class IGrammar {
	private final HashMap<INodeRule, INode> prods;

	public IGrammar(HashMap<INodeRule, INode> prods) {
		this.prods = prods;
	}

	public static IGrammar from(Grammar oldGrammar) {
		HashMap<INodeRule, INode> prods = new HashMap<>();
		IGrammar grammar = new IGrammar(prods);

		for(Rule rule : oldGrammar.rules.values()) {
			RuleAST ast = rule.ast;
			if(rule instanceof LeftRecursiveRule) {
				ast = ((LeftRecursiveRule) rule).originalAST;
			}

			assert ast.getChildCount() == 2;
			String name = ast.getChild(0).getText();
			INode content = INode.from(oldGrammar, grammar, (GrammarAST) ast.getChild(1));
			INodeRule ruleNode = new INodeRule(content.getDefiningGrammar(), oldGrammar.getRule(name).index, name, grammar);
			prods.put(ruleNode, content);
		}

		return grammar;
	}

	public void collectNames(Namespace namespace) {
		for(Map.Entry<INodeRule, INode> entry : prods.entrySet()) {
			INodeRule rule = entry.getKey();
			INode content = entry.getValue();

			namespace.setName(rule, rule.getName());
			if (!(content instanceof INodeRule)) {
				namespace.force(content, namespace.getName(rule));
			}
		}

		for(Map.Entry<INodeRule, INode> entry : prods.entrySet()) {
			INodeRule rule = entry.getKey();
			INode content = entry.getValue();

			if(content.isRepresentable()) {
				content.collectNames(namespace, rule.getName());
			}
		}
	}

	public void collectAlts(Namespace namespace, Relation<String> alternatives) {
		for(Map.Entry<INodeRule, INode> entry : prods.entrySet()) {
			INodeRule rule = entry.getKey();
			INode content = entry.getValue();

			if(content instanceof INodeRule) {
				alternatives.add(namespace.getName(content), namespace.getName(rule));
			}

			if(content.isRepresentable()) {
				content.collectAlts(namespace, alternatives);
			}
		}
	}

	public void collectDecls(Namespace namespace, Relation<String> alternatives, HashMap<ScalaNode, String> decls) {
		for(Map.Entry<INodeRule, INode> entry : prods.entrySet()) {
			INodeRule rule = entry.getKey();
			INode content = entry.getValue();

			if(content instanceof INodeRule) {
				List<ScalaNode> extend = new ArrayList<>();
				for(String rel : alternatives.valuesOf(namespace.getName(rule))) {
					extend.add(new Name(rel));
				}
				ScalaNode decl = new Trait(new Name(namespace.getName(rule)), extend);
				decls.put(decl, rule.getDefiningGrammar());
			}

			if(!content.isRepresentable()) {
				List<ScalaNode> extend = new ArrayList<>();
				for(String rel : alternatives.valuesOf(namespace.getName(rule))) {
					extend.add(new Name(rel));
				}
				Decl param = new Decl(new Name(Namespace.capitalize(rule.getName()) + "Context"), new Name("ctx"));
				ScalaNode decl = new Class(new Name(namespace.getName(rule)), Collections.singletonList(param), extend);
				decls.put(decl, rule.getDefiningGrammar());
			}
		}

		for(Map.Entry<INode, String> entry : namespace.entrySet()) {
			if(entry.getKey().isRepresentable()) {
				ScalaNode decl = entry.getKey().getDecl(entry.getValue(), namespace, alternatives);
				if (decl != null) {
					decls.put(decl, entry.getKey().getDefiningGrammar());
				}
			}
		}
	}

	public INode getProd(INodeRule rule) {
		return prods.get(rule);
	}

	public void collectConverters(Namespace namespace, Relation<String> alternatives, List<ScalaNode> decls) {
		for(Map.Entry<INodeRule, INode> entry : prods.entrySet()) {
			INodeRule rule = entry.getKey();
			INode content = entry.getValue();

			if(content instanceof INodeRule) {
				decls.add(new Snippet(
						"def read",
						new Name(namespace.getName(rule)),
						"(ctx: ParserRuleContext): ", new Name(namespace.getName(rule)), " = read",
						new Name(namespace.getName(content)),
						"(ctx.getChild(0).asInstanceOf[ParserRuleContext])"
				));
			}

			if(!content.isRepresentable()) {
				decls.add(new Snippet(
						"def read",
						new Name(namespace.getName(rule)),
						"(ctx: ParserRuleContext): ", new Name(namespace.getName(rule)), " = ",
						new Name(namespace.getName(rule)),
						"(ctx.asInstanceOf[", new Name(namespace.getName(rule)), "Context])"
				));
			}
		}

		for(Map.Entry<INode, String> entry : namespace.entrySet()) {
			if(entry.getKey() instanceof INodeRule) continue;

			if(entry.getKey().isRepresentable()) {
				decls.add(new Snippet(
						"def read",
						new Name(entry.getValue()),
						"(ctx: ParserRuleContext): ", new Name(entry.getValue()), " = {\n",
						"val it = (if (ctx.children == null) {new java.util.ArrayList[ParseTree]()} else {ctx.children}).listIterator\n",
						"val res: ", new Name(entry.getValue()), " = ", entry.getKey().getNamed(namespace, alternatives), "\n",
						"res\n",
						"}"
				));
			}
		}
	}
}
