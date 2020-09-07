package org.antlr.v4.codegen.model.extractors;

import org.antlr.v4.codegen.model.extractors.intermediate.INode;
import org.antlr.v4.codegen.model.extractors.intermediate.INodeChoice;
import org.antlr.v4.runtime.misc.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Namespace {
	private final HashMap<INode, String> name = new HashMap<>();
	private final HashMap<String, INode> decl = new HashMap<>();

	public static String capitalize(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	public String setName(INode obj, String preferredName) {
		if(name.containsKey(obj)) {
			return name.get(obj);
		}

		preferredName = capitalize(preferredName);

		if(decl.containsKey(preferredName)) {
			return setNameNumbered(obj, preferredName);
		} else {
			decl.put(preferredName, obj);
			name.put(obj, preferredName);
			return preferredName;
		}
	}

	public String setNameSuffix(INode obj, String preferredName) {
		if(name.containsKey(obj)) {
			return name.get(obj);
		}

		preferredName = capitalize(preferredName);

		while(decl.containsKey(preferredName)) {
			preferredName += "_";
		}

		decl.put(preferredName, obj);
		name.put(obj, preferredName);
		return preferredName;
	}

	public String getName(INode obj) {
		return name.get(obj);
	}
	public INode getDecl(String name) { return decl.get(name); }

	public String setNameNumbered(INode obj, String preferredName) {
		if(name.containsKey(obj)) {
			return name.get(obj);
		}

		preferredName = capitalize(preferredName);

		int number = 0;

		while(decl.containsKey(preferredName + number)) {
			number++;
		}

		decl.put(preferredName + number, obj);
		name.put(obj, preferredName + number);
		return preferredName + number;
	}

	public boolean contains(INode obj) {
		return name.containsKey(obj);
	}
	public boolean containsName(String name) { return decl.containsKey(name); }

	public Set<Map.Entry<INode, String>> entrySet() {
		return name.entrySet();
	}

	public void force(INode content, String preferredName) {
		name.put(content, preferredName);
	}
}
