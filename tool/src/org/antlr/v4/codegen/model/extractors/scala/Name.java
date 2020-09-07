package org.antlr.v4.codegen.model.extractors.scala;

import java.io.PrintStream;

public class Name extends ScalaNode {
	private final String name;

	public Name(String name) {
		this.name = name;
	}

	@Override
	public void writeTo(PrintStream out) {
		out.print(name);
	}
}
