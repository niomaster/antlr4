package org.antlr.v4.codegen.model.extractors.scala;

import java.io.PrintStream;

public class Option extends ScalaNode {
	private final ScalaNode t;

	public Option(ScalaNode t) {
		this.t = t;
	}

	@Override
	public void writeTo(PrintStream out) {
		out.print("Option[");
		t.writeTo(out);
		out.print("]");
	}
}
