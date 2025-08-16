package com.github.argon4w.acceleratedrendering.core.buffers.accelerated;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.layers.CustomLayerFunction;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.layers.EmptyLayerFunction;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.layers.LayerKey;
import com.github.argon4w.acceleratedrendering.core.buffers.environments.IBufferEnvironment;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.MeshUploadingProgramDispatcher;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;

import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL46.*;

public class AcceleratedBufferSource implements IAcceleratedBufferSource {

	@Getter private	final	IBufferEnvironment					environment;
	private			final	AcceleratedRingBuffers				ringBuffers;
	private			final	Set<AcceleratedRingBuffers.Buffers>	buffers;
	private			final	IntSet								activeLayers;

	private					AcceleratedRingBuffers.Buffers		currentBuffer;
	private 				boolean								used;

	public AcceleratedBufferSource(IBufferEnvironment bufferEnvironment) {
		this.environment	= bufferEnvironment;
		this.ringBuffers	= new AcceleratedRingBuffers		(this.environment);
		this.currentBuffer	= this.ringBuffers			.get	(false);
		this.buffers		= ObjectLinkedOpenHashSet	.of		(this.currentBuffer);
		this.activeLayers	= new IntAVLTreeSet					();

		this.used			= false;
	}

	public void delete() {
		ringBuffers.delete();
	}

	@Override
	public AcceleratedBufferBuilder getBuffer(
			RenderType	renderType,
			Runnable	before,
			Runnable	after,
			int			layerIndex
	) {
		var layerKey	= new LayerKey					(layerIndex, renderType);
		var builders	= currentBuffer	.getBuilders	();
		var functions	= currentBuffer	.getFunctions	();
		var layers		= currentBuffer	.getLayers		();
		var function	= functions		.get			(layerIndex);
		var layer		= layers		.get			(layerIndex);
		var builder		= builders		.get			(layerKey);

		if (builder != null) {
			function.addBefore	(before);
			function.addAfter	(after);
			return builder;
		}

		var vertexBuffer	= currentBuffer.getVertexBuffer		();
		var varyingBuffer	= currentBuffer.getVaryingBuffer	();
		var elementSegment	= currentBuffer.getElementSegment	();

		if (vertexBuffer == null) {
			currentBuffer	= ringBuffers	.get				(true);
			builders		= currentBuffer	.getBuilders		();
			functions		= currentBuffer	.getFunctions		();
			layers			= currentBuffer	.getLayers			();
			function		= functions		.get				(layerIndex);
			layer			= layers		.get				(layerIndex);

			vertexBuffer	= currentBuffer	.getVertexBuffer	();
			varyingBuffer	= currentBuffer	.getVaryingBuffer	();
			elementSegment	= currentBuffer	.getElementSegment	();

			buffers.add(currentBuffer);
		}

		if (layer == null) {
			function	= new CustomLayerFunction	();
			layer 		= new ReferenceArrayList<>	();
			layers		.put						(layerIndex, layer);
			functions	.put						(layerIndex, function);
		}

		builder = new AcceleratedBufferBuilder(
				vertexBuffer,
				varyingBuffer,
				elementSegment,
				currentBuffer,
				renderType
		);

		used = true;

		builders	.put		(layerKey, builder);
		function	.addBefore	(before);
		function	.addAfter	(after);
		activeLayers.add		(layerIndex);

		return builder;
	}

	public void drawBuffers() {
		if (!used) {
			return;
		}

		for (var buffer : buffers) {
			var builders	= buffer.getBuilders();
			var program		= glGetInteger		(GL_CURRENT_PROGRAM);
			var barrier		= 0;

			if (builders.isEmpty()) {
				continue;
			}

			ServerMesh.Builder.BUFFERS.get(environment.getLayout())	.getFirst								().bindBase(GL_SHADER_STORAGE_BUFFER,	MeshUploadingProgramDispatcher.SMALL_MESH_BUFFER_INDEX);
			environment												.selectMeshUploadingProgramDispatcher	().dispatch(builders.values(),			buffer);
			environment												.selectTransformProgramDispatcher		().dispatch(builders.values());

			for (var layerKey : builders.keySet()) {
				var builder = builders.get(layerKey);

				if (builder.isEmpty()) {
					continue;
				}

				var drawContext		= buffer	.getDrawContext		();
				var elementSegment	= builder	.getElementSegment	();
				var renderType		= layerKey	.renderType			();
				var layer			= layerKey	.layer				();

				builder							.setOutdated		();
				elementSegment					.allocateOffset		();
				buffer							.bindElementBuffer	(elementSegment);
				drawContext						.bindComputeBuffers	(elementSegment);
				drawContext						.setRenderType		(renderType);
				buffer.getLayers().get(layer)	.add				(drawContext);

				barrier |= environment	.selectProcessingProgramDispatcher	(renderType.mode)	.dispatch(builder);
				barrier |= builder		.getCullingProgramDispatcher		()					.dispatch(builder);
			}

			glMemoryBarrier	(barrier);
			glUseProgram	(program);
		}

		for (		int layerIndex	: activeLayers) {
			for (	var buffer		: buffers) {
				var function	= buffer.getFunctions	().getOrDefault(layerIndex, EmptyLayerFunction	.INSTANCE);
				var layer		= buffer.getLayers		().getOrDefault(layerIndex, List				.of());

				if (layer.isEmpty()) {
					continue;
				}

				glMemoryBarrier					(GL_ELEMENT_ARRAY_BARRIER_BIT | GL_COMMAND_BARRIER_BIT);
				BufferUploader	.invalidate		();
				buffer			.bindDrawBuffers();
				function		.runBefore		();

				for (var drawContext : layer) {
					var renderType	= drawContext	.getRenderType		();
					renderType						.setupRenderState	();

					var mode	= renderType	.mode;
					var shader	= RenderSystem	.getShader();

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

				function.runAfter			();
				function.reset				();
				layer	.clear				();
				buffer	.unbindVertexArray	();
			}
		}

		for (var buffer : buffers) {
			buffer.reset		();
			buffer.setInFlight	();
		}

		used			= false;
		currentBuffer	= ringBuffers	.get	(false);
		activeLayers					.clear	();
		buffers							.clear	();
		buffers							.add	(currentBuffer);
	}
}
