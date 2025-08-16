package com.github.argon4w.acceleratedrendering.core.mixins;

import com.github.argon4w.acceleratedrendering.core.CoreBuffers;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.meshes.ClientMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.core.programs.ComputeShaderProgramLoader;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
		value		= LevelRenderer.class,
		priority	= 998
)
public class LevelRendererMixin {

	@Inject(
			method	= "renderLevel",
			at		= @At("HEAD")
	)
	public void startRenderLevel(
			DeltaTracker	deltaTracker,
			boolean			renderBlockOutline,
			Camera			camera,
			GameRenderer	gameRenderer,
			LightTexture	lightTexture,
			Matrix4f		frustumMatrix,
			Matrix4f		projectionMatrix,
			CallbackInfo	ci
	) {
		CoreFeature.setRenderingLevel();
	}

	@Inject(
			method	= "renderLevel",
			at		= @At("RETURN")
	)
	public void stopRenderLevel(
			DeltaTracker	deltaTracker,
			boolean			renderBlockOutline,
			Camera			camera,
			GameRenderer	gameRenderer,
			LightTexture	lightTexture,
			Matrix4f		frustumMatrix,
			Matrix4f		projectionMatrix,
			CallbackInfo	ci
	) {
		CoreFeature.resetRenderingLevel();
	}

	@Inject(
			method	= "renderLevel",
			at		= @At(
					value	= "INVOKE",
					target	= "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"
			)
	)
	public void endOutlineBatches(
			DeltaTracker	pDeltaTracker,
			boolean			pRenderBlockOutline,
			Camera			pCamera,
			GameRenderer	pGameRenderer,
			LightTexture	pLightTexture,
			Matrix4f		pFrustumMatrix,
			Matrix4f		pProjectionMatrix,
			CallbackInfo	ci
	) {
		CoreBuffers.POS_TEX_COLOR_OUTLINE.drawBuffers();
	}

	@WrapOperation(
			method	= "renderLevel",
			at		= @At(
					value	= "INVOKE",
					target	= "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V"
			)
	)
	public void drawCoreBuffers(MultiBufferSource.BufferSource instance, Operation<Void> original) {
		CoreBuffers.ENTITY				.drawBuffers();
		CoreBuffers.BLOCK				.drawBuffers();
		CoreBuffers.POS					.drawBuffers();
		CoreBuffers.POS_TEX				.drawBuffers();
		CoreBuffers.POS_TEX_COLOR		.drawBuffers();
		CoreBuffers.POS_COLOR_TEX_LIGHT	.drawBuffers();

		original.call(instance);
	}

	@Inject(
			method	= "close",
			at		= @At("TAIL")
	)
	public void deleteBuffers(CallbackInfo ci) {
		CoreBuffers.ENTITY				.delete();
		CoreBuffers.BLOCK				.delete();
		CoreBuffers.POS					.delete();
		CoreBuffers.POS_TEX				.delete();
		CoreBuffers.POS_TEX_COLOR		.delete();
		CoreBuffers.POS_COLOR_TEX_LIGHT	.delete();
		ComputeShaderProgramLoader		.delete();
		ServerMesh.Builder.INSTANCE		.delete();
		ClientMesh.Builder.INSTANCE		.delete();
	}
}
