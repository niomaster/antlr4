package org.antlr.v4.codegen.model.extractors.scala;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

public abstract class ScalaNode {
	public abstract void writeTo(PrintStream out);

	protected void writeJoinedBy(PrintStream out, String prefixNonEmpty, String join, List<? extends ScalaNode> xs, String postfixNonEmpty) {
		if(!xs.isEmpty()) {
			out.print(prefixNonEmpty);
		}

		boolean first = true;
		for(ScalaNode node : xs) {
			if(!first) out.print(join);
			node.writeTo(out);
			first = false;
		}

		if(!xs.isEmpty()) {
			out.print(postfixNonEmpty);
		}
	}

	@Override
	public String toString() {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		try(PrintStream out = new PrintStream(byteOut)) {
			writeTo(out);
		}
		return byteOut.toString();
	}
}
