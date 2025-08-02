package com.github.argon4w.acceleratedrendering.features.filter;

import java.util.Set;

public enum FilterType {

	BLACKLIST,
	WHITELIST;

	public <T> boolean test(Set<T> values, T value) {
		return switch (this) {
			case WHITELIST -> 	values.contains(value);
			case BLACKLIST -> !	values.contains(value);
		};
	}
}
