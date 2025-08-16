package com.github.argon4w.acceleratedrendering.features.entities;

import com.github.argon4w.acceleratedrendering.configs.FeatureConfig;
import com.github.argon4w.acceleratedrendering.configs.FeatureStatus;
import com.github.argon4w.acceleratedrendering.configs.PipelineSetting;
import com.github.argon4w.acceleratedrendering.core.meshes.MeshType;

import java.util.ArrayDeque;
import java.util.Deque;

public class AcceleratedEntityRenderingFeature {

	private static final Deque<PipelineSetting>	PIPELINE_CONTROLLER_STACK			= new ArrayDeque<>();
	private static final Deque<FeatureStatus>	GUI_ACCELERATION_CONTROLLER_STACK	= new ArrayDeque<>();

	public static boolean isEnabled() {
		return FeatureConfig.CONFIG.acceleratedEntityRenderingFeatureStatus.get() == FeatureStatus.ENABLED;
	}

	public static boolean shouldUseAcceleratedPipeline() {
		return getPipelineSetting() == PipelineSetting.ACCELERATED;
	}

	public static boolean shouldAccelerateInGui() {
		return getGuiAccelerationSetting() == FeatureStatus.ENABLED;
	}

	public static MeshType getMeshType() {
		return FeatureConfig.CONFIG.acceleratedEntityRenderingMeshType.get();
	}

	public static void useVanillaPipeline() {
		PIPELINE_CONTROLLER_STACK.push(PipelineSetting.VANILLA);
	}

	public static void dontAccelerateInGui() {
		GUI_ACCELERATION_CONTROLLER_STACK.push(FeatureStatus.DISABLED);
	}

	public static void forceUseAcceleratedPipeline() {
		PIPELINE_CONTROLLER_STACK.push(PipelineSetting.ACCELERATED);
	}

	public static void forceAccelerateInGui() {
		GUI_ACCELERATION_CONTROLLER_STACK.push(FeatureStatus.ENABLED);
	}

	public static void forceSetPipeline(PipelineSetting pipeline) {
		PIPELINE_CONTROLLER_STACK.push(pipeline);
	}

	public static void forceSetGuiAcceleration(FeatureStatus status) {
		GUI_ACCELERATION_CONTROLLER_STACK.push(status);
	}

	public static void resetPipeline() {
		PIPELINE_CONTROLLER_STACK.pop();
	}

	public static void resetGuiAcceleration() {
		GUI_ACCELERATION_CONTROLLER_STACK.pop();
	}

	public static PipelineSetting getPipelineSetting() {
		return PIPELINE_CONTROLLER_STACK.isEmpty() ? getDefaultPipelineSetting() : PIPELINE_CONTROLLER_STACK.peek();
	}

	public static FeatureStatus getGuiAccelerationSetting() {
		return GUI_ACCELERATION_CONTROLLER_STACK.isEmpty() ? getDefaultGuiAccelerationSetting() : GUI_ACCELERATION_CONTROLLER_STACK.peek();
	}

	public static PipelineSetting getDefaultPipelineSetting() {
		return FeatureConfig.CONFIG.acceleratedEntityRenderingDefaultPipeline.get();
	}

	public static FeatureStatus getDefaultGuiAccelerationSetting() {
		return FeatureConfig.CONFIG.acceleratedEntityRenderingGuiAcceleration.get();
	}
}
