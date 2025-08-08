package com.github.argon4w.acceleratedrendering.core.buffers.accelerated;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import net.minecraft.client.renderer.RenderType;

public interface IAcceleratedBufferSource {

	AcceleratedBufferBuilder getBuffer(int layer, RenderType renderType, Runnable before, Runnable after);
}
