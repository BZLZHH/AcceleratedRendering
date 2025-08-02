package com.github.argon4w.acceleratedrendering.core.buffers.accelerated;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.DrawContextPool;
import com.github.argon4w.acceleratedrendering.core.buffers.environments.IBufferEnvironment;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.MeshUploadingProgramDispatcher;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;

import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL46.*;

public class AcceleratedBufferSource implements IAcceleratedBufferSource {

	@Getter private	final	IBufferEnvironment										bufferEnvironment;
	private			final	Map<RenderType, DrawContextPool.IndirectDrawContext>	drawContexts;
	private			final	AcceleratedBufferSetPool								acceleratedBufferSetPool;
	private			final	Set<AcceleratedBufferSetPool.BufferSet>					bufferSets;

	private					AcceleratedBufferSetPool.BufferSet						currentBufferSet;
	private 				boolean													used;

	public AcceleratedBufferSource(IBufferEnvironment bufferEnvironment) {
		this.bufferEnvironment			= bufferEnvironment;
		this.drawContexts				= new Object2ObjectLinkedOpenHashMap<>	();
		this.acceleratedBufferSetPool	= new AcceleratedBufferSetPool			(this.bufferEnvironment);
		this.currentBufferSet			= this.acceleratedBufferSetPool	.get	(false);
		this.bufferSets					= ObjectLinkedOpenHashSet		.of		(this.currentBufferSet);
		this.used						= false;
	}

	public void delete() {
		acceleratedBufferSetPool.delete();
	}

	@Override
	public AcceleratedBufferBuilder getBuffer(RenderType renderType) {
		var builders	= currentBufferSet	.getBuilders		();
		var builder		= builders			.getAndMoveToLast	(renderType);

		if (builder != null) {
			return builder;
		}

		var vertexBuffer	= currentBufferSet.getVertexBuffer	();
		var varyingBuffer	= currentBufferSet.getVaryingBuffer	();
		var elementSegment	= currentBufferSet.getElementSegment();

		if (vertexBuffer == null) {
			currentBufferSet	= acceleratedBufferSetPool	.get				(true);
			builders			= currentBufferSet			.getBuilders		();
			vertexBuffer		= currentBufferSet			.getVertexBuffer	();
			varyingBuffer		= currentBufferSet			.getVaryingBuffer	();
			elementSegment		= currentBufferSet			.getElementSegment	();

			bufferSets										.add				(currentBufferSet);
		}

		builder = new AcceleratedBufferBuilder(
				vertexBuffer,
				varyingBuffer,
				elementSegment,
				currentBufferSet,
				renderType
		);

		used = true;
		builders.put(renderType, builder);

		return builder;
	}

	public void drawBuffers() {
		if (!used) {
			return;
		}

		for (var bufferSet : bufferSets) {
			var builders	= bufferSet.getBuilders	();
			var program		= glGetInteger			(GL_CURRENT_PROGRAM);
			var barrier		= 0;

			if (builders.isEmpty()) {
				continue;
			}

			ServerMesh.Builder.INSTANCE	.getBuffer								(bufferEnvironment.getLayout())	.bindBase(GL_SHADER_STORAGE_BUFFER,	MeshUploadingProgramDispatcher.SMALL_MESH_BUFFER_INDEX);
			bufferEnvironment			.selectMeshUploadingProgramDispatcher	()								.dispatch(builders.values(),		bufferSet);
			bufferEnvironment			.selectTransformProgramDispatcher		()								.dispatch(builders.values());

			for (var renderType : builders.keySet()) {
				var builder			= builders	.get				(renderType);
				var elementSegment	= builder	.getElementSegment	();

				if (builder.isEmpty()) {
					continue;
				}

				var mode		= renderType.mode;
				var drawContext	= bufferSet	.getDrawContext();

				builder			.setOutdated		();
				elementSegment	.allocateOffset		();
				bufferSet		.bindElementBuffer	(elementSegment);
				drawContext		.bindComputeBuffers	(elementSegment);
				drawContexts	.put				(renderType, drawContext);

				barrier |= bufferEnvironment.selectProcessingProgramDispatcher	(mode)	.dispatch(builder);
				barrier |= builder			.getCullingProgramDispatcher		()		.dispatch(builder);
			}

			glMemoryBarrier					(barrier);
			glUseProgram					(program);
			BufferUploader	.invalidate		();
			bufferSet		.bindDrawBuffers();

			for (var renderType : drawContexts.keySet()) {
				renderType						.setupRenderState	();

				var drawContext	= drawContexts	.get				(renderType);
				var shader		= RenderSystem	.getShader			();
				var mode		= renderType	.mode;

				shader.setDefaultUniforms(
						mode,
						RenderSystem			.getModelViewMatrix	(),
						RenderSystem			.getProjectionMatrix(),
						Minecraft.getInstance()	.getWindow			()
				);

				shader		.apply				();
				drawContext	.drawElements		(mode);
				shader		.clear				();
				renderType	.clearRenderState	();
			}

			glMemoryBarrier				(GL_ELEMENT_ARRAY_BARRIER_BIT | GL_COMMAND_BARRIER_BIT);
			bufferSet		.reset		();
			bufferSet		.setInFlight();
			drawContexts	.clear		();
		}

		used				= false;
		currentBufferSet	= acceleratedBufferSetPool	.get(false);
		bufferSets										.add(currentBufferSet);
	}
}
