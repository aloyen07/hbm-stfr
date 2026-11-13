package fun.aloyenz.stfr.client.render;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.physics.StfrMagneticCollapse;
import fun.aloyenz.stfr.reactor.physics.StfrStartupFailureLight;

/** Local point-light volume. A voxel texture supplies both view occlusion and source shadows. */
final class StfrVolumetricLight {

	private static final int SIZE = 37, HALF = 18, BOXES = 8;
	private static final Map<Long, Volume> VOLUMES = new HashMap<>();
	private static World currentWorld;
	private static int program, compositeProgram, depthTexture;
	private static Framebuffer lowResolution;
	private static boolean attempted;

	private StfrVolumetricLight() {}

	static void render(
			World world,
			BlockPos center,
			double cx,
			double cy,
			double cz,
			double time,
			boolean meltdown) {
		render(
				world,
				center,
				cx,
				cy,
				cz,
				time,
				meltdown ? 1F : .57F,
				meltdown ? 1F : .09F,
				1F,
				meltdown ? 2.8F : .8F,
				18F);
	}

	static void startupFailure(World world,BlockPos center,double cx,double cy,double cz,double time,double ticks,double recovery) {
		render(world,center,cx,cy,cz,time,.57F,.09F,1F,
				StfrStartupFailureLight.volume(ticks,recovery),18F);
	}

	static void stable(
			World world,
			BlockPos center,
			double cx,
			double cy,
			double cz,
			double time,
			double formation,
			int destabilizerStage,
			double destabilizerTicks) {
		if (formation <= 0) return;
		float fade = (float) (formation * formation * (3D - 2D * formation));
		float intensity = 1.2F * fade, radius = 4F;
		if (destabilizerStage == 5) {
			float progress = (float) Math.min(1D, Math.max(0D, destabilizerTicks / 460D));
			intensity += 1.5F * progress;
			radius += 4F * progress;
		} else if (destabilizerStage == 6 && destabilizerTicks < 40D) {
			float flash = (float) Math.max(0D, 1D - destabilizerTicks / 40D);
			intensity += 2.2F * flash;
			radius += 6F * flash;
		}
		render(world, center, cx, cy, cz, time, 1F, 1F, 1F, intensity, radius);
	}

	static void contingency(
			World world,
			BlockPos center,
			double cx,
			double cy,
			double cz,
			double time,
			StfrReactor.RunState state,
			double tick,
			double recovery) {
		float red = 1, green = .22F, blue = .06F, intensity = 1.6F, radius = 6;
		if (state == StfrReactor.RunState.REACTION_LOSS
				|| state == StfrReactor.RunState.REIGNITION) {
			red = .12F;
			green = .85F;
			blue = 1;
			intensity = .8F;
		}
        if(state==StfrReactor.RunState.REACTION_LOSS || state==StfrReactor.RunState.UNDERLOAD) {
            red=state==StfrReactor.RunState.UNDERLOAD?1F:.25F;green=state==StfrReactor.RunState.UNDERLOAD?1F:.85F;blue=1F;
            intensity=(float)StfrMagneticCollapse.light(state,tick);
            radius=state==StfrReactor.RunState.UNDERLOAD && tick>=3400 ? 8 : 4;
        }
		if (state == StfrReactor.RunState.SHUTDOWN_FAILURE) {
			green = .08F;
			blue = .02F;
			radius = 8;
			intensity *= Math.min(1, Math.max(0, (tick - 300) / 200D));
			if (recovery >= 990) return;
		}
		if (state == StfrReactor.RunState.SELF_DESTRUCT) return;
		render(world, center, cx, cy, cz, time, red, green, blue, intensity, radius);
	}

	private static void render(
			World world,
			BlockPos center,
			double cx,
			double cy,
			double cz,
			double time,
			float red,
			float green,
			float blue,
			float intensity,
			float radius) {
		if (!GLContext.getCapabilities().OpenGL20 || !OpenGlHelper.isFramebufferEnabled()) return;
		if (!attempted) {
			attempted = true;
			program = createProgram("reactor_volume.frag");
			compositeProgram = createProgram("reactor_volume_composite.frag");
		}
		if (program == 0 || compositeProgram == 0) return;
		if (currentWorld != world) {
			for (Volume volume : VOLUMES.values()) volume.delete();
			VOLUMES.clear();
			currentWorld = world;
		}
		VOLUMES.entrySet()
				.removeIf(
						entry -> {
							if (world.getTotalWorldTime() - entry.getValue().used > 100) {
								entry.getValue().delete();
								return true;
							}
							return false;
						});
		Volume volume = VOLUMES.computeIfAbsent(center.toLong(), ignored -> new Volume());
		volume.used = world.getTotalWorldTime();
		Minecraft mc = Minecraft.getMinecraft();
		int sourceFramebuffer =
				GL11.glGetInteger(0x8CA6); // GL_FRAMEBUFFER_BINDING (core and EXT share the enum).
		int width = Math.max(1, mc.displayWidth / 3), height = Math.max(1, mc.displayHeight / 3);
		if (lowResolution == null
				|| lowResolution.framebufferWidth != width
				|| lowResolution.framebufferHeight != height) {
			if (lowResolution != null) lowResolution.deleteFramebuffer();
			lowResolution = new Framebuffer(width, height, false);
			lowResolution.setFramebufferColor(0, 0, 0, 0);
			lowResolution.setFramebufferFilter(GL11.GL_LINEAR);
			OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, sourceFramebuffer);
			GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
		}
		int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		boolean previousAlpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		int previous2D = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		if (depthTexture == 0) depthTexture = GL11.glGenTextures();
		GlStateManager.bindTexture(depthTexture);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glCopyTexImage2D(
				GL11.GL_TEXTURE_2D,
				0,
				GL11.GL_DEPTH_COMPONENT,
				0,
				0,
				mc.displayWidth,
				mc.displayHeight,
				0);
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + 1);
		int previous3D = GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
		GL11.glBindTexture(GL12.GL_TEXTURE_3D, volume.minimum);
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + 2);
		int previousBoxes = GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
		GL11.glBindTexture(GL12.GL_TEXTURE_3D, volume.maximum);
		if (volume.updated < 0 || world.getTotalWorldTime() - volume.updated >= 10) {
			ByteBuffer minima = BufferUtils.createByteBuffer(SIZE * SIZE * SIZE * BOXES * 4);
			ByteBuffer maxima = BufferUtils.createByteBuffer(minima.capacity());
			int[] counts = new int[SIZE * SIZE * SIZE];
			BlockPos base = center.add(-HALF, -HALF, -HALF);
			AxisAlignedBB region = new AxisAlignedBB(base, base.add(SIZE, SIZE, SIZE));
			List<AxisAlignedBB> boxes = new ArrayList<>();
			// Include a halo: fences and other shapes can extend beyond their own cell.
			for (int z = -HALF - 1; z <= HALF + 1; z++)
				for (int y = -HALF - 1; y <= HALF + 1; y++)
					for (int x = -HALF - 1; x <= HALF + 1; x++) {
						BlockPos at = center.add(x, y, z);
						boxes.clear();
						if (!world.isBlockLoaded(at)) boxes.add(new AxisAlignedBB(at));
						else {
							IBlockState state = world.getBlockState(at);
							if (!StfrEmergencyLight.transparent(state))
								state.addCollisionBoxToList(world, at, region, boxes, null, false);
						}
						for (AxisAlignedBB box : boxes) pack(box, base, counts, minima, maxima);
					}
			upload(maxima);
			GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + 1);
			upload(minima);
			volume.updated = world.getTotalWorldTime();
		}
		GL20.glUseProgram(program);
		GL20.glUniform1i(GL20.glGetUniformLocation(program, "sceneDepth"), 0);
		GL20.glUniform1i(GL20.glGetUniformLocation(program, "boxMin"), 1);
		GL20.glUniform1i(GL20.glGetUniformLocation(program, "boxMax"), 2);
		GL20.glUniform2f(GL20.glGetUniformLocation(program, "viewport"), width, height);
		GL20.glUniform3f(
				GL20.glGetUniformLocation(program, "core"),
				(float) (center.getX() + .5 - cx),
				(float) (center.getY() + .5 - cy),
				(float) (center.getZ() + .5 - cz));
		GL20.glUniform3f(GL20.glGetUniformLocation(program, "tint"), red, green, blue);
		GL20.glUniform1f(GL20.glGetUniformLocation(program, "intensity"), intensity);
		GL20.glUniform1f(GL20.glGetUniformLocation(program, "radius"), radius);
		GL20.glUniform1f(GL20.glGetUniformLocation(program, "time"), (float) time);
		lowResolution.bindFramebuffer(true);
		GlStateManager.clearColor(0, 0, 0, 0);
		GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.disableCull();
		GlStateManager.disableAlpha();
		GlStateManager.disableLighting();
		GlStateManager.disableBlend();
		fullscreen();
		OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, sourceFramebuffer);
		GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GlStateManager.bindTexture(lowResolution.framebufferTexture);
		GL20.glUseProgram(compositeProgram);
		GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "lightBuffer"), 0);
		GL20.glUniform2f(
				GL20.glGetUniformLocation(compositeProgram, "viewport"),
				mc.displayWidth,
				mc.displayHeight);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
		fullscreen();
		GL20.glUseProgram(previousProgram);
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + 2);
		GL11.glBindTexture(GL12.GL_TEXTURE_3D, previousBoxes);
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + 1);
		GL11.glBindTexture(GL12.GL_TEXTURE_3D, previous3D);
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GlStateManager.bindTexture(previous2D);
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.enableCull();
		if (previousAlpha) GlStateManager.enableAlpha();
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.color(1, 1, 1, 1);
	}

	private static void pack(
			AxisAlignedBB box, BlockPos base, int[] counts, ByteBuffer minima, ByteBuffer maxima) {
		int x0 = Math.max(0, (int) Math.floor(box.minX) - base.getX()),
				x1 = Math.min(SIZE - 1, (int) Math.ceil(box.maxX) - 1 - base.getX());
		int y0 = Math.max(0, (int) Math.floor(box.minY) - base.getY()),
				y1 = Math.min(SIZE - 1, (int) Math.ceil(box.maxY) - 1 - base.getY());
		int z0 = Math.max(0, (int) Math.floor(box.minZ) - base.getZ()),
				z1 = Math.min(SIZE - 1, (int) Math.ceil(box.maxZ) - 1 - base.getZ());
		for (int z = z0; z <= z1; z++)
			for (int y = y0; y <= y1; y++)
				for (int x = x0; x <= x1; x++) {
					int cell = (z * SIZE + y) * SIZE + x, slot = Math.min(counts[cell], BOXES - 1);
					int offset = (((slot * SIZE + z) * SIZE + y) * SIZE + x) * 4;
					double[] lo = {
						box.minX - base.getX() - x,
						box.minY - base.getY() - y,
						box.minZ - base.getZ() - z
					};
					double[] hi = {
						box.maxX - base.getX() - x,
						box.maxY - base.getY() - y,
						box.maxZ - base.getZ() - z
					};
					for (int axis = 0; axis < 3; axis++) {
						int low = (int) Math.floor(Math.max(0, lo[axis]) * 255),
								high = (int) Math.ceil(Math.min(1, hi[axis]) * 255);
						// Rare shapes with more than eight boxes conservatively merge the overflow.
						if (counts[cell] >= BOXES) {
							low = Math.min(low, minima.get(offset + axis) & 255);
							high = Math.max(high, maxima.get(offset + axis) & 255);
						}
						minima.put(offset + axis, (byte) low);
						maxima.put(offset + axis, (byte) high);
					}
					minima.put(offset + 3, (byte) 255);
					counts[cell]++;
				}
	}

	private static void upload(ByteBuffer data) {
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
		GL12.glTexImage3D(
				GL12.GL_TEXTURE_3D,
				0,
				GL11.GL_RGBA,
				SIZE,
				SIZE,
				SIZE * BOXES,
				0,
				GL11.GL_RGBA,
				GL11.GL_UNSIGNED_BYTE,
				data);
	}

	private static void fullscreen() {
		BufferBuilder b = Tessellator.getInstance().getBuffer();
		b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
		b.pos(-1, -1, 0).endVertex();
		b.pos(1, -1, 0).endVertex();
		b.pos(1, 1, 0).endVertex();
		b.pos(-1, 1, 0).endVertex();
		Tessellator.getInstance().draw();
	}

	private static int createProgram(String fragmentName) {
		int vertex = 0, fragment = 0, linked = 0;
		try {
			vertex = compile(GL20.GL_VERTEX_SHADER, "reactor_volume.vert");
			fragment = compile(GL20.GL_FRAGMENT_SHADER, fragmentName);
			linked = GL20.glCreateProgram();
			GL20.glAttachShader(linked, vertex);
			GL20.glAttachShader(linked, fragment);
			GL20.glLinkProgram(linked);
			if (GL20.glGetProgrami(linked, GL20.GL_LINK_STATUS) == 0)
				throw new IllegalStateException(GL20.glGetProgramInfoLog(linked, 4096));
			return linked;
		} catch (Exception error) {
			LogManager.getLogger("hbmstfr")
					.error("Cannot compile reactor volumetric light", error);
			if (linked != 0) GL20.glDeleteProgram(linked);
			return 0;
		} finally {
			if (vertex != 0) GL20.glDeleteShader(vertex);
			if (fragment != 0) GL20.glDeleteShader(fragment);
		}
	}

	private static int compile(int type, String name) throws Exception {
		String source;
		try (InputStream stream =
				StfrVolumetricLight.class.getResourceAsStream("/assets/hbmstfr/shaders/" + name)) {
			if (stream == null) throw new IllegalStateException("Missing shader " + name);
			source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
		int shader = GL20.glCreateShader(type);
		GL20.glShaderSource(shader, source);
		GL20.glCompileShader(shader);
		if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
			String error = GL20.glGetShaderInfoLog(shader, 4096);
			GL20.glDeleteShader(shader);
			throw new IllegalStateException(error);
		}
		return shader;
	}

	private static final class Volume {

		final int minimum = GL11.glGenTextures(), maximum = GL11.glGenTextures();
		long updated = -1, used;

		void delete() {
			GL11.glDeleteTextures(minimum);
			GL11.glDeleteTextures(maximum);
		}
	}
}
