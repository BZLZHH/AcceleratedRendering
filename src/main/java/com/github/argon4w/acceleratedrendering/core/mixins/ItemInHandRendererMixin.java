package com.github.argon4w.acceleratedrendering.core.mixins;

import com.github.argon4w.acceleratedrendering.core.CoreBuffers;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

	@Inject(
			method	= "renderHandsWithItems",
			at		= @At("HEAD")
	)
	public void startRenderingHands(
			float							partialTicks,
			PoseStack						poseStack,
			MultiBufferSource.BufferSource	buffer,
			LocalPlayer						playerEntity,
			int								combinedLight,
			CallbackInfo					ci
	) {
		CoreFeature.setRenderingHand();
	}

	@Inject(
			method	= "renderHandsWithItems",
			at		= @At("RETURN")
	)
	public void stopRenderingHands(
			float							partialTicks,
			PoseStack						poseStack,
			MultiBufferSource.BufferSource	buffer,
			LocalPlayer						playerEntity,
			int								combinedLight,
			CallbackInfo					ci
	) {
		CoreFeature						.resetRenderingHand	();
		CoreBuffers.ENTITY				.drawBuffers		();
		CoreBuffers.BLOCK				.drawBuffers		();
		CoreBuffers.POS					.drawBuffers		();
		CoreBuffers.POS_TEX				.drawBuffers		();
		CoreBuffers.POS_TEX_COLOR		.drawBuffers		();
		CoreBuffers.POS_COLOR_TEX_LIGHT	.drawBuffers		();
	}
}
