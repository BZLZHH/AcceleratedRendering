package com.github.argon4w.acceleratedrendering.core.buffers.memory;

public interface IMemoryLayout<T> {

	IMemoryInterface	getElement		(T element);
	int					getElementOffset(T element);
	boolean				containsElement	(T element);
	long				getSize			();
}
