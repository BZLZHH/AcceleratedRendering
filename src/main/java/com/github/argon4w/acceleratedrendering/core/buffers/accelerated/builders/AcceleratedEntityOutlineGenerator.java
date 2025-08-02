package com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders;

import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.ExtensionMethod;

import java.nio.ByteBuffer;


@AllArgsConstructor
@EqualsAndHashCode	(callSuper = false)
@ExtensionMethod	(VertexConsumerExtension.class)
public class AcceleratedEntityOutlineGenerator extends AcceleratedVertexConsumerWrapper {

	private final VertexConsumer	delegate;
	private final int				color;

	@Override
	public VertexConsumer getDelegate() {
		return delegate;
	}

	@Override
	public VertexConsumer decorate(VertexConsumer buffer) {
		return new AcceleratedEntityOutlineGenerator(
				getDelegate				()
						.getAccelerated	()
						.decorate		(buffer),
				color
		);
	}

	@Override
	public void addClientMesh(
			ByteBuffer meshBuffer,
			int			size,
			int			color,
			int			light,
			int			overlay
	) {
		getDelegate				()
				.getAccelerated	()
				.addClientMesh	(
						meshBuffer,
						size,
						this.color,
						light,
						overlay
				);
	}

	@Override
	public void addServerMesh(
			ServerMesh serverMesh,
			int			color,
			int			light,
			int			overlay
	) {
		getDelegate				()
				.getAccelerated	()
				.addServerMesh	(
						serverMesh,
						this.color,
						light,
						overlay
				);
	}

	@Override
	public VertexConsumer addVertex(
			float pX,
			float pY,
			float pZ
	) {
		delegate.addVertex(
				pX,
				pY,
				pZ
		).setColor(color);
		return this;
	}

	@Override
	public VertexConsumer addVertex(
			PoseStack.Pose	pPose,
			float			pX,
			float			pY,
			float			pZ
	) {
		delegate.addVertex(
				pPose,
				pX,
				pY,
				pZ
		).setColor(color);
		return this;
	}

	@Override
	public VertexConsumer setColor(
			int pRed,
			int pGreen,
			int pBlue,
			int pAlpha
	) {
		return this;
	}

	@Override
	public VertexConsumer setUv1(int pU, int pV) {
		return this;
	}

	@Override
	public VertexConsumer setUv2(int pU, int pV) {
		return this;
	}

	@Override
	public VertexConsumer setNormal(
			float pNormalX,
			float pNormalY,
			float pNormalZ
	) {
		return this;
	}

	@Override
	public VertexConsumer setNormal(
			PoseStack.Pose	pPose,
			float			pNormalX,
			float			pNormalY,
			float			pNormalZ
	) {
		return this;
	}

	@Override
	public void addVertex(
			float	x,
			float	y,
			float	z,
			int		color,
			float	u,
			float	v,
			int		packedOverlay,
			int		packedLight,
			float	normalX,
			float	normalY,
			float	normalZ
	) {
		getDelegate().addVertex(
				x,
				y,
				z,
				this.color,
				u,
				v,
				packedOverlay,
				packedLight,
				normalX,
				normalY,
				normalZ
		);
	}
}
