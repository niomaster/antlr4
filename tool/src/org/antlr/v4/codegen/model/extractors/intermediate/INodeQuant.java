package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.*;
import org.antlr.v4.codegen.model.extractors.scala.Class;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

public abstract class INodeQuant extends INode {
	private final INode body;

	public INodeQuant(String definingGrammar, INode body) {
		super(definingGrammar);
		this.body = body;
	}

	public INode getBody() {
		return body;
	}

	@Override
	public void collectNames(Namespace namespace, String baseName) {
		getBody().collectNames(namespace, baseName);
	}

	@Override
	public void collectAlts(Namespace namespace, Relation<String> alternatives) {
		getBody().collectAlts(namespace, alternatives);
	}

	protected ScalaNode consumeMore(Namespace namespace, Relation<String> alternatives) {
		HashSet<IFirst> firstSet = first();
		firstSet.remove(INodeEpsilon.INSTANCE);
		HashMap<Integer, INode> token = new HashMap<>();
		HashMap<Integer, INode> rule = new HashMap<>();

		for(IFirst first : firstSet) {
			if(first instanceof INodeRule) {
				rule.put(((INodeRule) first).getIndex(), (INode) first);
			} else if(first instanceof INodeToken) {
				token.put(((INodeToken) first).getIndex(), (INode) first);
			} else {
				throw new NotImplementedException();
			}
		}

		List<Snippet> tokenBranches = new ArrayList<>();

		for(Map.Entry<Integer, INode> entry : token.entrySet()) {
			tokenBranches.add(new Snippet(
					"case ", entry.getKey().toString(), " => it.previous; true\n"
			));
		}

		List<Snippet> ruleBranches = new ArrayList<>();

		for(Map.Entry<Integer, INode> entry : rule.entrySet()) {
			ruleBranches.add(new Snippet(
					"case ", entry.getKey().toString(), " => it.previous; true\n"
			));
		}

		tokenBranches.add(new Snippet(
				"case _ => it.previous; false\n"
		));
		ruleBranches.add(new Snippet(
				"case _ => it.previous; false\n"
		));

		return new Snippet(
				"it.hasNext && (it.next match {\n",
				"case tok: TerminalNode => tok.getSymbol.getType match {\n",
				new Snippet(tokenBranches.toArray()),
				"}\n",
				"case rule: ParserRuleContext => rule.getRuleIndex match {\n",
				new Snippet(ruleBranches.toArray()),
				"}\n",
				"})"
		);
	}

	@Override
	public ScalaNode getNamed(Namespace namespace, Relation<String> alternatives) {
		return new Snippet(namespace.getName(this), "(", getAnon(namespace, alternatives), ")");
	}

	@Override
	public ScalaNode getDecl(String as, Namespace namespace, Relation<String> alternatives) {
		List<ScalaNode> extend = new ArrayList<>();
		for(String rel : alternatives.valuesOf(namespace.getName(this))) {
			extend.add(new Name(rel));
		}
		return new Class(
			new Name(as),
			Collections.singletonList(new Decl(computeType(namespace), new Name("content"))),
			extend
		);
	}
}
