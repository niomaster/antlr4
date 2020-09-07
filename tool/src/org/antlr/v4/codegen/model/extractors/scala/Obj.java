package org.antlr.v4.codegen.model.extractors.scala;

import java.io.PrintStream;
import java.util.List;

public class Obj extends ScalaNode {
	private final ScalaNode name;
	private final List<ScalaNode> extend;

	public Obj(ScalaNode name, List<ScalaNode> extend) {
		this.name = name;
		this.extend = extend;
	}

	@Override
	public void writeTo(PrintStream out) {
		out.print("sealed trait ");
		name.writeTo(out);
		writeJoinedBy(out, " extends ", " with ", extend, "");
		out.print("; case object ");
		name.writeTo(out);
		out.print(" extends ");
		name.writeTo(out);
	}
}
