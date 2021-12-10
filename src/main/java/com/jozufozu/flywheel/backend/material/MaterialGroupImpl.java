package com.jozufozu.flywheel.backend.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.jozufozu.flywheel.backend.instancing.InstanceData;
import com.jozufozu.flywheel.core.shader.WorldProgram;
import com.jozufozu.flywheel.util.TextureBinder;
import com.mojang.math.Matrix4f;

import net.minecraft.client.renderer.RenderType;

/**
 * A group of materials all rendered with the same GL state.
 *
 * The children of a material group will all be rendered at the same time.
 * No guarantees are made about the order of draw calls.
 */
public class MaterialGroupImpl<P extends WorldProgram> implements MaterialGroup {

	protected final MaterialManagerImpl<P> owner;

	protected final ArrayList<MaterialRenderer<P>> renderers = new ArrayList<>();

	private final Map<MaterialSpec<?>, MaterialImpl<?>> materials = new HashMap<>();

	public MaterialGroupImpl(MaterialManagerImpl<P> owner) {
		this.owner = owner;
	}

	/**
	 * Get the material as defined by the given {@link MaterialSpec spec}.
	 * @param spec The material you want to create instances with.
	 * @param <D> The type representing the per instance data.
	 * @return A
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <D extends InstanceData> MaterialImpl<D> material(MaterialSpec<D> spec) {
		return (MaterialImpl<D>) materials.computeIfAbsent(spec, this::createInstanceMaterial);
	}

	public void render(RenderType type, Matrix4f viewProjection, double camX, double camY, double camZ) {
		type.setupRenderState();
		TextureBinder.bindActiveTextures();
		for (MaterialRenderer<P> renderer : renderers) {
			renderer.render(viewProjection, camX, camY, camZ);
		}
		type.clearRenderState();
	}

	public void setup(P program) {

	}

	public void clear() {
		materials.values().forEach(MaterialImpl::clear);
	}

	public void delete() {
		materials.values()
				.forEach(MaterialImpl::delete);

		materials.clear();
		renderers.clear();
	}

	private MaterialImpl<?> createInstanceMaterial(MaterialSpec<?> type) {
		MaterialImpl<?> material = new MaterialImpl<>(type);

		this.renderers.add(new MaterialRenderer<>(owner.getProgram(type.getProgramName()), material, this::setup));

		return material;
	}
}