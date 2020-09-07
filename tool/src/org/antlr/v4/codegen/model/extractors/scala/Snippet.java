package org.antlr.v4.codegen.model.extractors.scala;

import java.io.PrintStream;

public class Snippet extends ScalaNode {
	private final Object[] objs;

	public Snippet(Object... objs) {
		this.objs = objs;
	}

	@Override
	public void writeTo(PrintStream out) {
		for(Object obj : objs) {
			if(obj instanceof ScalaNode) {
				((ScalaNode) obj).writeTo(out);
			} else if(obj instanceof String) {
				out.print(obj);
			} else {
				throw new RuntimeException();
			}
		}
	}
}
