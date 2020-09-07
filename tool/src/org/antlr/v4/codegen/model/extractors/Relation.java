package org.antlr.v4.codegen.model.extractors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Relation<T> {
	private final HashMap<T, HashSet<T>> store = new HashMap<>();

	private HashSet<T> get(T key) {
		HashSet<T> set = store.get(key);
		if(set == null) {
			set = new HashSet<>();
			store.put(key, set);
		}
		return set;
	}

	public void add(T key, T value) {
		get(key).add(value);
	}

	public Set<T> keySet() {
		return store.keySet();
	}

	public Set<T> valuesOf(T key) {
		return get(key);
	}
}
