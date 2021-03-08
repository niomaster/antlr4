package org.antlr.v4.codegen.model;

import org.antlr.runtime.tree.Tree;
import org.antlr.v4.codegen.OutputModelFactory;
import org.antlr.v4.misc.Utils;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.parse.ToolANTLRParser;
import org.antlr.v4.tool.ast.*;

import java.util.*;

public class ExtractorsAlt extends OutputModelObject {
	public String text;
	public String name;

	private abstract static class MiniGrammar extends OutputModelObject {
		abstract Terminal first();
		abstract String asString(String suffix);

		public abstract String scalaType();
	}

	private enum Quantifier { STAR, PLUS, OPT }

	private static class Quantify extends MiniGrammar {
		final Quantifier quantifier;
		final Terminal term;

		Quantify(Quantifier quantifier, Terminal term) {
			this.quantifier = quantifier;
			this.term = term;
		}

		@Override
		Terminal first() {
			return term;
		}

		@Override
		String asString(String suffix) {
			String result =
					String.format("  def read%s(it: java.util.ListIterator[org.antlr.v4.runtime.tree.ParseTree]): Option[%s] = {%n", suffix, scalaType());

			result += String.format("    var nextElem = readOne%s(it)%n", suffix);

			switch(quantifier) {
				case STAR:
				case PLUS:
					result += String.format("    val buf = mutable.ArrayBuffer[%s]()%n", term.scalaType());
					result += String.format("    while(!nextElem.isEmpty) {%n");
					result += String.format("      buf += nextElem.get%n");
					result += String.format("      nextElem = readOne%s(it)%n", suffix);
					result += String.format("    }%n");
					result += String.format("    Some(buf.toSeq)%n");
					break;
				case OPT:
					// The result from an optional cannot be "invalid" (at worst we read no tokens)
					result += String.format("    Some(nextElem)%n");
					break;
			}
			result += String.format("  }%n");
			return result + this.term.asString("One" + suffix);
		}

		@Override
		public String scalaType() {
			switch(quantifier) {
				case STAR:
				case PLUS:
					return "Seq[" + term.scalaType() + "]";
				case OPT:
					return "Option[" + term.scalaType() + "]";
			}

			return null; // unreachable
		}
	}

	private static abstract class Terminal extends MiniGrammar {
		@Override
		Terminal first() {
			// Incorrect in the case of epsilon, but this is resolved by episilon colliding with everything.
			return this;
		}

		abstract boolean collides(Terminal other);
	}

	private static class EpsilonTerminal extends Terminal {
		@Override
		boolean collides(Terminal other) {
			return true;
		}

		@Override
		String asString(String suffix) {
			return String.format("  def read%s(it: java.util.ListIterator[org.antlr.v4.runtime.tree.ParseTree]): Option[Unit] = Some(())%n", suffix);
		}

		@Override
		public String scalaType() {
			return "Unit";
		}
	}

	private static class ContextTerminal extends Terminal {
		final String rule;

		ContextTerminal(String rule) {
			this.rule = Utils.capitalize(rule);
		}

		@Override
		boolean collides(Terminal other) {
			return (other instanceof ContextTerminal) && (((ContextTerminal) other).rule.equals(this.rule));
		}

		@Override
		String asString(String suffix) {
			String result = "";
			result += String.format("  def read%s(it: java.util.ListIterator[org.antlr.v4.runtime.tree.ParseTree]): Option[%s] = {%n", suffix, scalaType());
			result += String.format("    if(!it.hasNext) { return None }%n");
			result += String.format("    it.next match {%n");
			result += String.format("      case yes: %s => Some(yes)%n", scalaType());
			result += String.format("      case other => it.previous; None%n");
			result += String.format("    }%n");
			result += String.format("  }%n");
			return result;
		}

		@Override
		public String scalaType() {
			return rule + "Context";
		}
	}

	private static class TerminalChoiceTerminal extends Terminal {
		final Set<String> terminals;

		static String fromTerminalAST(TerminalAST ast) {
			if(ast.token.getType() == ANTLRParser.STRING_LITERAL) {
				return ast.g.getTokenName(ast.token.getText());
			} else if(ast.token.getType() == ANTLRParser.TOKEN_REF) {
				return ast.token.getText();
			} else {
				return null;
			}
		}

		TerminalChoiceTerminal(String terminal) {
			this(new HashSet<>(Collections.singleton(terminal)));
		}

		TerminalChoiceTerminal(Set<String> terminals) {
			this.terminals = terminals;
		}

		@Override
		boolean collides(Terminal other) {
			if(!(other instanceof TerminalChoiceTerminal)) {
				return false;
			}

			HashSet<String> overlap = new HashSet<>(this.terminals);
			overlap.retainAll(((TerminalChoiceTerminal) other).terminals);

			return !overlap.isEmpty();
		}

		private String tokenSet() {
			String result = "Set(";

			boolean comma = false;
			for(String terminal : terminals) {
				if(comma) result += ", ";
				comma = true;
				result += terminal;
			}

			result += ")";
			return result;
		}

		@Override
		String asString(String suffix) {
			String result = "";
			result += String.format("  def read%s(it: java.util.ListIterator[org.antlr.v4.runtime.tree.ParseTree]): Option[%s] = {%n", suffix, scalaType());
			result += String.format("    if(!it.hasNext) { return None }%n");
			result += String.format("    it.next match {%n");
			result += String.format("      case yes: org.antlr.v4.runtime.tree.TerminalNode if %s.contains(yes.getSymbol.getType) => Some(yes.getSymbol.getText)%n", tokenSet());
			result += String.format("      case other => it.previous; None%n");
			result += String.format("    }%n");
			result += String.format("  }%n");
			return result;

		}

		@Override
		public String scalaType() {
			return "String";
		}
	}

	private static TerminalChoiceTerminal getTerminalSet(GrammarAST set) {
		if(set.token.getType() != ANTLRParser.SET) {
			return null;
		}

		HashSet<String> terminals = new HashSet<>();

		for(Object child : set.getChildren()) {
			if(!(child instanceof TerminalAST)) {
				return null;
			}

			terminals.add(TerminalChoiceTerminal.fromTerminalAST((TerminalAST) child));
		}

		return new TerminalChoiceTerminal(terminals);
	}

	private static Terminal getTerminal(Tree tree) {
		if (tree instanceof AltAST || tree instanceof BlockAST || tree instanceof NotAST) {
			if (tree.getChildCount() == 1) {
				// The type of a block or alt of len one can be determined completely by its child, as well as the
				// inversion of a character class
				return getTerminal(tree.getChild(0));
			} else {
				return null;
			}
		}

		// rules and terminals have a determined type
		if(tree instanceof TerminalAST) {
			return new TerminalChoiceTerminal(TerminalChoiceTerminal.fromTerminalAST((TerminalAST) tree));
		}

		if(tree instanceof RuleRefAST) {
			return new ContextTerminal(tree.getText());
		}

		if(tree instanceof GrammarAST) {
			Terminal result = getTerminalSet((GrammarAST) tree);
			if(result != null) {
				// Terminals have a common type, so a choice of terminal is fine.
				return result;
			}

			if(((GrammarAST) tree).token.getType() == ANTLRParser.EPSILON) {
				// Epsilon has the unit type
				return new EpsilonTerminal();
			}
		}

		// We assume all other structures are too complicated
		return null;
	}

	private static Quantify getQuantifiable(Tree tree) {
		Quantifier quantifier = null;

		if(tree instanceof StarBlockAST) {
			quantifier = Quantifier.STAR;
		} else if(tree instanceof PlusBlockAST) {
			quantifier = Quantifier.PLUS;
		} else if(tree instanceof OptionalBlockAST) {
			quantifier = Quantifier.OPT;
		} else {
			return null;
		}

		if(tree.getChildCount() != 1) {
			return null;
		}

		Tree child = tree.getChild(0);
		Terminal innerTerminal = getTerminal(child);

		if(innerTerminal == null) {
			return null;
		}

		return new Quantify(quantifier, innerTerminal);
	}

	private static MiniGrammar getMiniGrammar(Tree tree) {
		MiniGrammar result = getTerminal(tree);
		if(result != null) return result;
		result = getQuantifiable(tree);
		if(result != null) return result;

		return null;
	}

	public ExtractorsAlt(OutputModelFactory factory, List<MiniGrammar> grammar, String label) {
		String parserName = factory.getGrammar().getRecognizerName();
		this.name = Utils.capitalize(label);
		this.text = "";
		String returnType;
		switch(grammar.size()) {
			case 0:
				returnType = "Unit";
				break;
			case 1:
				returnType = grammar.get(0).scalaType();
				break;
			default:
				returnType = "(";
				for(int i = 0; i < grammar.size(); i++) {
					if(i > 0) returnType += ", ";
					returnType += grammar.get(i).scalaType();
				}
				returnType += ")";
		}
		for(int i = 0; i < grammar.size(); i++) {
			this.text += grammar.get(i).asString(i + "");
		}
		this.text += String.format("  def unapply(cls: %sContext): Option[%s] = {%n", parserName + "." + name, returnType);
		this.text += "    val nodes = if(cls.children == null) { new java.util.ArrayList[org.antlr.v4.runtime.tree.ParseTree]() } else { cls.children }\n";
		this.text += "    val it = nodes.listIterator\n";

		switch(grammar.size()) {
			case 0:
				this.text += "    Some(())\n";
				break;
			case 1:
				this.text += "    read0(it)\n"; // Forward the option from this method
				break;
			default:
				this.text += "    Some((\n";
				for(int i = 0;i < grammar.size(); i++) {
					this.text += String.format("      read%d(it) match {%n", i);
					this.text += String.format("        case None => return None%n");
					this.text += String.format("        case Some(x) => x%n");
					this.text += String.format("      }%s%n", i == grammar.size()-1 ? "" : ",");
				}
				this.text += "    ))\n";
		}

		this.text += "  }\n";
	}

	public static Map<String, String> extractElementOptions(Tree tree) {
		if (tree.getType() != ANTLRParser.ELEMENT_OPTIONS) {
			throw new IllegalArgumentException("Tree can only be of type ELEMENT_OPTIONS");
		}

		Map<String, String> opts = new HashMap<>();

		for (int i = 0; i < tree.getChildCount(); i++) {
			Tree child = tree.getChild(i);
			if (child.getType() != ANTLRParser.ASSIGN) {
				throw new IllegalArgumentException("Unexpected ELEMENT_OPTIONS structure");
			}
			if (child.getChildCount() != 2) {
				throw new IllegalArgumentException("Unexpected ELEMENT_OPTIONS structure");
			}

			Tree key = child.getChild(0);
			Tree value = child.getChild(1);

			// Assuming that calling toString() on the key/value trees yields a readable/useable string also for complex values like multiline strings and integers.
			opts.put(key.toString(), value.toString());
		}

		return opts;
	}

	public static ExtractorsAlt createFromAltAST(OutputModelFactory factory, AltAST ast, String label) {
		List<MiniGrammar> parts = new ArrayList<>();

		for(Object child : ast.getChildren()) {
			if(child instanceof ActionAST) {
				continue; // Safely ignore all predicates and actions
			}

			Tree tree = (Tree) child;

			// Ignore settings like "<assoc=right>"
			if (tree.getType() == ANTLRParser.ELEMENT_OPTIONS) {
				Map<String, String> opts = extractElementOptions(tree);
				if (opts.size() == 1 && opts.keySet().contains("assoc")) {
					continue;
				} else {
					throw new IllegalArgumentException("Only grammar option allowed is assoc");
				}
			}

			MiniGrammar grammar = getMiniGrammar(tree);
			if(grammar == null) {
				return null;
			}
			parts.add(grammar);
		}

		for(int i = 0; i < parts.size() - 1; i++) {
			if(parts.get(i) instanceof Quantify) {
				if(parts.get(i).first().collides(parts.get(i+1).first())) {
					return null;
				}
			}
		}

		return new ExtractorsAlt(factory, parts, label);
	}
}
