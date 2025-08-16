package com.github.argon4w.acceleratedrendering.core.buffers.memory;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class VertexFormatMemoryLayout implements IMemoryLayout<VertexFormatElement> {

	private final int					mask;
	private final long					size;
	private final IMemoryInterface	[]	byId;
	private final int				[]	offsets;

	public VertexFormatMemoryLayout(VertexFormat vertexFormat) {
		var offsets		= vertexFormat	.getOffsetsByElement();
		var count		= offsets		.length;

		this.mask		= vertexFormat	.getElementsMask();
		this.size		= vertexFormat	.getVertexSize	();
		this.byId		= new IMemoryInterface			[count];
		this.offsets	= new int						[count];

		for (var i = 0; i < count; i ++) {
			var offset		= offsets[i];
			this.byId	[i]	= offset == -1 ? NullMemoryInterface.INSTANCE : new SimpleMemoryInterface(offset, size);
			this.offsets[i]	= offset;
		}
	}

	@Override
	public IMemoryInterface getElement(VertexFormatElement element) {
		return byId[element.id()];
	}

	@Override
	public int getElementOffset(VertexFormatElement element) {
		return offsets[element.id()];
	}

	@Override
	public boolean containsElement(VertexFormatElement element) {
		return (mask & element.mask()) != 0;
	}

	@Override
	public long getSize() {
		return size;
	}
}
