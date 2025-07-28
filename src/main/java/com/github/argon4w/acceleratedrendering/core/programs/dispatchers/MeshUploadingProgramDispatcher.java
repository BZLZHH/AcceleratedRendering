package com.github.argon4w.acceleratedrendering.core.programs.dispatchers;

import com.github.argon4w.acceleratedrendering.core.backends.buffers.IServerBuffer;
import com.github.argon4w.acceleratedrendering.core.backends.programs.ComputeProgram;
import com.github.argon4w.acceleratedrendering.core.backends.programs.Uniform;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.AcceleratedBufferSetPool;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.meshes.MeshUploaderPool;
import com.github.argon4w.acceleratedrendering.core.programs.ComputeShaderProgramLoader;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL46.GL_SHADER_STORAGE_BUFFER;

public class MeshUploadingProgramDispatcher {

	private static	final int													GROUP_SIZE				= 128;
	private static	final int													DISPATCH_COUNT_Y_Z		= 1;
	public static	final int													SMALL_MESH_BUFFER_INDEX	= 5;
	public static	final int													MESH_BUFFER_INDEX		= 7;

	private			final Map<IServerBuffer, List<MeshUploaderPool.MeshUploader>>	denseUploaders;
	private			final Map<IServerBuffer, List<MeshUploaderPool.MeshUploader>>	sparseUploaders;
	private			final ComputeProgram											program;
	private			final Uniform													meshCountUniform;
	private			final Uniform													meshSizeUniform;
	private			final Uniform													vertexOffsetUniform;
	private			final Uniform													varyingOffsetUniform;
	private			final Uniform													meshOffsetUniform;

	public MeshUploadingProgramDispatcher(ResourceLocation key) {
		this.denseUploaders			= new Reference2ObjectLinkedOpenHashMap<>	();
		this.sparseUploaders		= new Reference2ObjectLinkedOpenHashMap<>	();
		this.program				= ComputeShaderProgramLoader.getProgram		(key);
		this.meshCountUniform		= program					.getUniform		("meshCount");
		this.meshSizeUniform		= program					.getUniform		("meshSize");
		this.vertexOffsetUniform	= program					.getUniform		("vertexOffset");
		this.varyingOffsetUniform	= program					.getUniform		("varyingOffset");
		this.meshOffsetUniform		= program					.getUniform		("meshOffset");
	}

	public void dispatch(Collection<AcceleratedBufferBuilder> builders, AcceleratedBufferSetPool.BufferSet bufferSet) {
		var transformProgramDispatcher = bufferSet
				.getBufferEnvironment				()
				.selectTransformProgramDispatcher	();

		for (var builder : builders) {
			var vertexBuffer	= builder	.getVertexBuffer	();
			var varyingBuffer	= builder	.getVaryingBuffer	();
			var vertexSize		= builder	.getVertexSize		();
			var meshVertexCount = builder	.getMeshVertexCount	();

			vertexBuffer					.allocateOffset		(meshVertexCount * vertexSize);
			varyingBuffer					.allocateOffset		(meshVertexCount * AcceleratedBufferBuilder.VARYING_SIZE);
		}

		bufferSet.prepare				();
		bufferSet.bindTransformBuffers	();

		for (var builder : builders) {
			var vertexBuffer	= builder	.getVertexBuffer	();
			var varyingBuffer	= builder	.getVaryingBuffer	();
			var vertexSize		= builder	.getVertexSize		();
			var offset			= 0;
			var sparseStart		= 0;
			var vertexCount		= builder		.getVertexCount		();
			var meshVertexCount	= builder		.getMeshVertexCount	();
			var vertexAddress	= vertexBuffer	.reserve			(meshVertexCount * vertexSize);
			var varyingAddress	= varyingBuffer	.reserve			(meshVertexCount * AcceleratedBufferBuilder.VARYING_SIZE);
			var vertexOffset	= vertexBuffer	.getOffset			() / vertexSize;
			var varyingOffset	= varyingBuffer	.getOffset			() / AcceleratedBufferBuilder.VARYING_SIZE;

			for (var uploader : builder
					.getMeshUploaders	()
					.values				()
			) {
				var serverMesh		= uploader					.getServerMesh	();
				var meshCount		= uploader.getMeshInfos()	.getMeshCount	();
				var buffer			= serverMesh				.meshBuffer		();
				var dense			= denseUploaders			.get			(buffer);
				var sparse			= sparseUploaders			.get			(buffer);

				if (dense == null) {
					dense	= new ReferenceArrayList<>	();
					sparse	= new ReferenceArrayList<>	();
					denseUploaders	.put				(buffer, dense);
					sparseUploaders	.put				(buffer, sparse);
				}

				(meshCount < 128
						? sparse
						: dense).add(uploader);
			}

			for (		var buffer		: sparseUploaders.keySet()) {
				for (	var uploader	: sparseUploaders.get	(buffer)) {
					var mesh			= uploader	.getServerMesh	();
					var meshInfos		= uploader	.getMeshInfos	();
					var meshCount		= meshInfos	.getMeshCount	();
					var meshSize		= mesh		.size			();

					for (var i = 0; i < meshCount; i ++) {
						builder.getColorOffset()					.at(offset).putInt(vertexAddress, FastColor.ABGR32	.fromArgb32		(meshInfos.getColor(i)));
						builder.getUv1Offset()						.at(offset).putInt(vertexAddress, meshInfos			.getOverlay		(i));
						builder.getUv2Offset()						.at(offset).putInt(vertexAddress, meshInfos			.getLight		(i));

						AcceleratedBufferBuilder.VARYING_SHARING	.at(offset).putInt(varyingAddress, meshInfos		.getSharing		(i));
						AcceleratedBufferBuilder.VARYING_MESH		.at(offset).putInt(varyingAddress, (int) mesh		.offset			());
						AcceleratedBufferBuilder.VARYING_SHOULD_CULL.at(offset).putInt(varyingAddress, meshInfos		.getShouldCull	(i));

						for (var j = 0; j < meshSize; j ++) {
							AcceleratedBufferBuilder
									.VARYING_OFFSET
									.at		(offset)
									.at		(j)
									.putInt	(varyingAddress, j);
						}

						offset += (int) meshSize;
					}
				}

				if (offset != 0) {
					buffer						.bindBase(GL_SHADER_STORAGE_BUFFER, SMALL_MESH_BUFFER_INDEX);
					transformProgramDispatcher	.dispatch(
							vertexBuffer,
							varyingBuffer,
							offset,
							sparseStart + vertexCount + vertexOffset,
							sparseStart + vertexCount + varyingOffset
					);
				}

				sparseStart = offset;
			}

			for (var buffer : denseUploaders.keySet()) {
				program	.useProgram	();
				buffer	.bindBase	(GL_SHADER_STORAGE_BUFFER, MESH_BUFFER_INDEX);

				for (var uploader : denseUploaders.get(buffer)) {
					var meshCount	= uploader.getMeshInfos()	.getMeshCount	();
					var mesh		= uploader					.getServerMesh	();
					var meshSize	= mesh						.size			();
					var uploadSize	= (int) (meshCount * meshSize);

					uploader			.upload				();
					uploader			.bindBuffers		();
					meshCountUniform	.uploadUnsignedInt	(meshCount);
					meshSizeUniform		.uploadUnsignedInt	((int) meshSize);
					vertexOffsetUniform	.uploadUnsignedInt	((int) (offset + vertexCount + vertexOffset));
					varyingOffsetUniform.uploadUnsignedInt	((int) (offset + vertexCount + varyingOffset));
					meshOffsetUniform	.uploadUnsignedInt	((int) mesh.offset());

					program.dispatch	(
							(uploadSize + GROUP_SIZE - 1) / GROUP_SIZE,
							DISPATCH_COUNT_Y_Z,
							DISPATCH_COUNT_Y_Z
					);

					offset	+= uploadSize;
				}
			}

			for (var buffer : denseUploaders.keySet()) {
				denseUploaders	.get(buffer).clear();
				sparseUploaders	.get(buffer).clear();
			}
		}

		program.resetProgram();
		program.waitBarriers(transformProgramDispatcher.getBarrierFlags());
	}
}
