package com.github.argon4w.acceleratedrendering.core.meshes;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.backends.GLConstants;
import com.github.argon4w.acceleratedrendering.core.backends.buffers.EmptyServerBuffer;
import com.github.argon4w.acceleratedrendering.core.backends.buffers.IServerBuffer;
import com.github.argon4w.acceleratedrendering.core.backends.buffers.MappedBuffer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.IMemoryLayout;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.IMeshCollector;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.lwjgl.system.MemoryUtil;

import java.util.Map;

public record ServerMesh(
		long			size,
		long			offset,
		IServerBuffer	meshBuffer
) implements IMesh {

	@Override
	public void write(
			IAcceleratedVertexConsumer	extension,
			int							color,
			int							light,
			int							overlay
	) {
		extension.addServerMesh(
				this,
				color,
				light,
				overlay
		);
	}

	public static class Builder implements IMesh.Builder {

		public static final Builder																		INSTANCE	= new Builder					();
		public static final Map<IMemoryLayout<VertexFormatElement>, ReferenceArrayList<MappedBuffer>>	BUFFERS		= new Object2ObjectOpenHashMap<>();

		private Builder() {

		}

		@Override
		public IMesh build(IMeshCollector collector) {
			var vertexCount		= collector.getVertexCount();

			if (vertexCount == 0) {
				return EmptyMesh.INSTANCE;
			}

			var builder			= collector	.getBuffer	();
			var result			= builder	.build		();

			if (result == null) {
				builder.close();
				return EmptyMesh.INSTANCE;
			}

			var clientBuffer	= result		.byteBuffer	();
			var layout			= collector		.getLayout	();
			var meshBuffers		= BUFFERS		.get		(layout);

			if (meshBuffers == null) {
				meshBuffers = new ReferenceArrayList<>	();
				BUFFERS.put 							(layout, meshBuffers);
			}

			var meshBuffer		= meshBuffers.isEmpty() ? null : meshBuffers	.getLast	();
			var capacity		= clientBuffer									.capacity	();

			if (		meshBuffer == null
					||	(meshBuffer.getPosition() + capacity >= GLConstants.MAX_SHADER_STORAGE_BLOCK_SIZE && checkMeshBuffer())
			) {
				meshBuffer = new MappedBuffer	(64L);
				meshBuffers.add					(meshBuffer);
			}

			var position		= meshBuffer	.getPosition();
			var srcAddress		= MemoryUtil	.memAddress0(clientBuffer);
			var destAddress		= meshBuffer	.reserve	(capacity);

			MemoryUtil	.memCopy(
					srcAddress,
					destAddress,
					capacity
			);
			builder		.close	();

			return new ServerMesh(
					vertexCount,
					position / layout.getSize(),
					meshBuffer
			);
		}

		@Override
		public void delete() {
			for (		var buffers	: BUFFERS.values()) {
				for (	var buffer	: buffers) {
					buffer.delete();
				}
			}
		}

		public IServerBuffer getBuffer(IMemoryLayout<VertexFormatElement> layout) {
			var buffers = BUFFERS.get(layout);

			if (buffers == null) {
				return EmptyServerBuffer.INSTANCE;
			}

			if (buffers.isEmpty()) {
				return EmptyServerBuffer.INSTANCE;
			}

			return buffers.getFirst();
		}

		public static boolean checkMeshBuffer() {
			if (CoreFeature.shouldUploadMeshImmediately()) {
				throw new OutOfMemoryError("Mesh buffer exceeds limit.");
			}

			return true;
		}
	}
}
