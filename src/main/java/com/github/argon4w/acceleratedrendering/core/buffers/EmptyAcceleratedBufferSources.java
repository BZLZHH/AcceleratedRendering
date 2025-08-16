package com.github.argon4w.acceleratedrendering.core.buffers;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.IAcceleratedBufferSource;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import net.minecraft.client.renderer.RenderType;

public class EmptyAcceleratedBufferSources implements IAcceleratedBufferSource {

	public static final IAcceleratedBufferSource INSTANCE = new EmptyAcceleratedBufferSources();

	@Override
	public AcceleratedBufferBuilder getBuffer(
			RenderType	renderType,
			Runnable	before,
			Runnable	after,
			int			layer
	) {
		return null;
	}
}
