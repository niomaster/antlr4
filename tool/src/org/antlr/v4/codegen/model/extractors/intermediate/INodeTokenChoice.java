package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.Name;
import org.antlr.v4.codegen.model.extractors.scala.ScalaNode;
import org.antlr.v4.codegen.model.extractors.scala.Snippet;
import org.antlr.v4.codegen.model.extractors.scala.Trait;

import java.util.*;

public class INodeTokenChoice extends INode {
	private final List<INodeToken> tokens;

	public INodeTokenChoice(String definingGrammar, List<INodeToken> tokens) {
		super(definingGrammar);
		this.tokens = tokens;
	}

	public INode flatten() {
		if(tokens.size() == 1) {
			return tokens.get(0);
		} else {
			return this;
		}
	}

	@Override
	public String toString() {
		return "(" + INode.join(this.tokens, "|") + ")";
	}

	@Override
	public HashSet<IFirst> first() {
		HashSet<IFirst> result = new HashSet<>();

		for(INodeToken token : tokens) {
			result.addAll(token.first());
		}

		return result;
	}

	@Override
	public boolean isRepresentable() {
		HashSet<IFirst> joinedFirst = new HashSet<>();
		int separateCount = 0;
		for(INode node : tokens) {
			Set<IFirst> first = node.first();
			separateCount += first.size();
			joinedFirst.addAll(first);
		}
		return joinedFirst.size() == separateCount;
	}

	@Override
	public ScalaNode computeType(Namespace namespace) {
		throw new RuntimeException();
	}

	@Override
	public ScalaNode getNamed(Namespace namespace, Relation<String> alternatives) {
		return getAnon(namespace, alternatives);
	}

	@Override
	public ScalaNode getAnon(Namespace namespace, Relation<String> alternatives) {
		ArrayList<Snippet> tokenBranches = new ArrayList<>();

		for(INodeToken token : tokens) {
			tokenBranches.add(new Snippet("case ", ""+token.getIndex(), " => it.previous; ",
					token.getNode(namespace, alternatives)));
		}

		return new Snippet("it.next.asInstanceOf[TerminalNode].getSymbol.getType match {\n",
				new Snippet(tokenBranches.toArray()), "}");
	}

	@Override
	public void collectNames(Namespace namespace, String baseName) {
		String name = namespace.setName(this, baseName + "InnerChoice");
		for(INode node : tokens) {
			node.collectNames(namespace, baseName + namespace.getName(this));
			if(!namespace.contains(node)) {
				namespace.setNameNumbered(node, name + "Alt");
			}
		}
	}

	@Override
	public void collectAlts(Namespace namespace, Relation<String> alternatives) {
		String me = namespace.getName(this);
		for(INode node : tokens) {
			alternatives.add(namespace.getName(node), me);
			node.collectAlts(namespace, alternatives);
		}
	}

	@Override
	public ScalaNode getDecl(String as, Namespace namespace, Relation<String> alternatives) {
		List<ScalaNode> extend = new ArrayList<>();
		for(String rel : alternatives.valuesOf(namespace.getName(this))) {
			extend.add(new Name(rel));
		}
		return new Trait(new Name(as), extend);
	}
}
