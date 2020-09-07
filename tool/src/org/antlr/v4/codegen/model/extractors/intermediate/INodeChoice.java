package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.Name;
import org.antlr.v4.codegen.model.extractors.scala.ScalaNode;
import org.antlr.v4.codegen.model.extractors.scala.Snippet;
import org.antlr.v4.codegen.model.extractors.scala.Trait;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

public class INodeChoice extends INode {
	private final List<INode> nodes;
	private final boolean labeled;

	public INodeChoice(String definingGrammar, List<INode> nodes, boolean labeled) {
		super(definingGrammar);
		this.nodes = nodes;
		this.labeled = labeled;
	}

	public INode flatten() {
		if(nodes.size() == 1) {
			return nodes.get(0);
		} else {
			return this;
		}
	}

	@Override
	public String toString() {
		return "(" + INode.join(nodes, " | ") + ")";
	}

	@Override
	public HashSet<IFirst> first() {
		HashSet<IFirst> result = new HashSet<>();

		for(INode node : nodes) {
			result.addAll(node.first());
		}

		return result;
	}

	@Override
	public boolean isRepresentable() {
		for(INode node : nodes) {
			if(!node.isRepresentable()) return false;
		}

		if(this.labeled) return true;

		HashSet<IFirst> joinedFirst = new HashSet<>();
		int separateCount = 0;
		for(INode node : nodes) {
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
		if(labeled) {
			// match on class name
			Snippet[] branches = new Snippet[nodes.size()];

			for (int i = 0; i < nodes.size(); i++) {
				String specificType = Namespace.capitalize(nodes.get(i).getLabel()) + "Context";
				branches[i] = new Snippet(
						"case \"",
						new Name(specificType),
						"\" => ", nodes.get(i).getNode(namespace, alternatives), "\n");
			}

			return new Snippet("ctx.getClass.getSimpleName match {\n", new Snippet((Object[]) branches), "}");
		} else /* not labeled */ {
			// match on rule or token index
			INode epsilon = null;
			HashMap<Integer, INode> token = new HashMap<>();
			HashMap<Integer, INode> rule = new HashMap<>();

			for(INode node : nodes) {
				for(IFirst first : node.first()) {
					if(first instanceof INodeRule) {
						rule.put(((INodeRule) first).getIndex(), (INode) first);
					} else if(first instanceof INodeToken) {
						token.put(((INodeToken) first).getIndex(), (INode) first);
					} else if(first instanceof INodeEpsilon) {
						epsilon = (INode) first;
					} else {
						throw new NotImplementedException();
					}
				}
			}

			List<Snippet> tokenBranches = new ArrayList<>();

			for(Map.Entry<Integer, INode> entry : token.entrySet()) {
				tokenBranches.add(new Snippet(
						"case ", entry.getKey().toString(), " => it.previous; ", entry.getValue().getNode(namespace, alternatives), "\n"
				));
			}

			List<Snippet> ruleBranches = new ArrayList<>();

			for(Map.Entry<Integer, INode> entry : rule.entrySet()) {
				ruleBranches.add(new Snippet(
						"case ", entry.getKey().toString(), " => it.previous; ", entry.getValue().getNode(namespace, alternatives), "\n"
				));
			}

			if(epsilon != null) {
				tokenBranches.add(new Snippet(
						"case _ => it.previous; ", epsilon.getNode(namespace, alternatives), "\n"
				));
				ruleBranches.add(new Snippet(
						"case _ => it.previous; ", epsilon.getNode(namespace, alternatives), "\n"
				));
			}

			return new Snippet(
					"if(!it.hasNext) {\n",
					epsilon == null ? "???" : epsilon.getNode(namespace, alternatives), "\n",
					"} else { it.next match {\n",
					"case tok: TerminalNode => tok.getSymbol.getType match {\n",
					new Snippet(tokenBranches.toArray()),
					"}\n",
					"case rule: ParserRuleContext => rule.getRuleIndex match {\n",
					new Snippet(ruleBranches.toArray()),
					"}}\n",
					"}"
			);
		}
	}

	@Override
	public void collectNames(Namespace namespace, String baseName) {
		String name = namespace.setName(this, baseName + "InnerChoice");
		for(INode node : nodes) {
			node.collectNames(namespace, namespace.getName(this));
			if(!namespace.contains(node)) {
				namespace.setNameNumbered(node, name + "Alt");
			}
		}
	}

	@Override
	public void collectAlts(Namespace namespace, Relation<String> alternatives) {
		String me = namespace.getName(this);
		for(INode node : nodes) {
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
