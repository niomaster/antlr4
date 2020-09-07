package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.Name;
import org.antlr.v4.codegen.model.extractors.scala.ScalaNode;
import org.antlr.v4.codegen.model.extractors.scala.Snippet;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.ast.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class INode {
	private final String definingGrammar;
	private String label;

	public INode(String definingGrammar) {
		this.definingGrammar = definingGrammar;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public abstract HashSet<IFirst> first();
	public abstract boolean isRepresentable();
	public abstract void collectNames(Namespace namespace, String baseName);
	public abstract void collectAlts(Namespace namespace, Relation<String> alternatives);
	public abstract ScalaNode getDecl(String as, Namespace namespace, Relation<String> alternatives);
	public abstract ScalaNode getNamed(Namespace namespace, Relation<String> alternatives);
	public abstract ScalaNode getAnon(Namespace namespace, Relation<String> alternatives);

	public ScalaNode getType(Namespace namespace) {
		if(namespace.contains(this)) {
			return new Name(namespace.getName(this));
		} else {
			return computeType(namespace);
		}
	}

	public abstract ScalaNode computeType(Namespace namespace);

	public static String join(Iterable<?> xs, String join) {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for(Object x : xs) {
			if(!first) result.append(join);
			first = false;
			result.append(x.toString());
		}
		return result.toString();
	}

	public static String definingGrammar(GrammarAST ast) {
		while(ast.token.getType() != ANTLRParser.RULE) {
			ast = (GrammarAST) ast.parent;
		}
		return ast.g.name;
	}

	public static INode fromList(Grammar root, IGrammar grammar, GrammarAST xs) {
		List<INode> body = new ArrayList<>();

		for(Object x : xs.getChildren()) {
			if(x instanceof ActionAST) {
				continue;
			}
			body.add(INode.from(root, grammar, (GrammarAST)x));
		}

		return new INodeSeq(definingGrammar(xs), body).flatten();
	}

	private static INodeToken getToken(Grammar g, TerminalAST ast) {
		String text = ast.getText();

		int index;
		String name;

		if(g.tokenNameToTypeMap.containsKey(text)) {
			index = g.tokenNameToTypeMap.get(text);
			name = text;
		} else {
			index = g.stringLiteralToTypeMap.get(text);
			name = g.typeToTokenList.get(index);
		}

		boolean constant =
				index == Token.EOF
				|| (index >= 0
				&& index < g.typeToStringLiteralList.size()
				&& g.typeToStringLiteralList.get(index) != null);

		return new INodeToken(definingGrammar(ast), index, constant, name);
	}

	public static INode from(Grammar root, IGrammar grammar, GrammarAST ast) {
		if(ast instanceof AltAST) {
			INode result = fromList(root, grammar, ast);
			if(((AltAST) ast).altLabel != null) {
				result.setLabel(((AltAST) ast).altLabel.getText());
			}
			return result;
		} else if (ast instanceof RuleRefAST) {
			assert ast.getChildCount() == 0;
			return new INodeRule(definingGrammar(ast), root.getRule(ast.getText()).index, ast.getText(), grammar);
		} else if(ast instanceof TerminalAST) {
			assert ast.getChildCount() == 0;
			return getToken(root, (TerminalAST) ast);
		} else if(ast.token != null && ast.token.getType() == ANTLRParser.SET) {
			List<INodeToken> tokens = new ArrayList<>();
			for(Object x : ast.getChildren()) {
				tokens.add(getToken(root, (TerminalAST) x));
			}
			return new INodeTokenChoice(definingGrammar(ast), tokens).flatten();
		} else if(ast instanceof PlusBlockAST) {
			assert ast.getChildCount() == 1;
			return new INodeQuantPlus(definingGrammar(ast), INode.from(root, grammar, (GrammarAST) ast.getChild(0)));
		} else if(ast instanceof OptionalBlockAST) {
			assert ast.getChildCount() == 1;
			return new INodeQuantOpt(definingGrammar(ast), INode.from(root, grammar, (GrammarAST) ast.getChild(0)));
		} else if(ast instanceof StarBlockAST) {
			assert ast.getChildCount() == 1;
			return new INodeQuantStar(definingGrammar(ast), INode.from(root, grammar, (GrammarAST) ast.getChild(0)));
		} else if(ast instanceof BlockAST) {
			List<INode> nodes = new ArrayList<>();

			boolean allLabeled = true;

			for(Object x : ast.getChildren()) {
				assert x instanceof AltAST;
				if(((AltAST) x).altLabel == null) allLabeled = false;
				nodes.add(INode.from(root, grammar, (AltAST) x));
			}

			return new INodeChoice(definingGrammar(ast), nodes, allLabeled).flatten();
		} else if(ast.token != null && ast.token.getType() == ANTLRParser.EPSILON) {
			return INodeEpsilon.INSTANCE;
		} else if(ast instanceof NotAST) {
			return new INodeBail(definingGrammar(ast));
		} else {
			return new INodeBail(definingGrammar(ast));
		}
	}

	public ScalaNode getNode(Namespace namespace, Relation<String> alternatives) {
		if(namespace.contains(this)) {
			return getNamed(namespace, alternatives);
		} else {
			return getAnon(namespace, alternatives);
		}
	}

	public String getLabel() {
		return label;
	}

	public String getDefiningGrammar() {
		return definingGrammar;
	}
}
