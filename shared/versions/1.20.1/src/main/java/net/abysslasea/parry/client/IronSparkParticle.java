package net.abysslasea.parry.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.abysslasea.parry.ParryMod;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Uses crossed ribbons so a moving spark stays visible from different viewing angles. */
final class IronSparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private boolean enteredWater;
    private double prevPrevX, prevPrevY, prevPrevZ;
    private final float crossX, crossZ;

    private IronSparkParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.prevPrevX = x; this.prevPrevY = y; this.prevPrevZ = z;
        this.xd = vx * 1.0 + (random.nextDouble() * 3 - 1.5) * 0.05;
        this.yd = vy * 1.0 + (random.nextDouble() * 3 - 1.5) * 0.05;
        this.zd = vz * 1.0 + (random.nextDouble() * 3 - 1.5) * 0.05;
        this.gravity = 0.25F + random.nextFloat() * 0.05F;
        this.friction = 1.0F;
        this.lifetime = 4 + random.nextInt(9);
        this.quadSize = random.nextBoolean() ? 0.025F : 0.03F;
        this.crossX = 0.4F + random.nextFloat() * 0.7F;
        this.crossZ = 0.4F + random.nextFloat() * 0.7F;
        this.setSize(quadSize, quadSize);
        setSpriteFromAge(sprites);
    }

    @Override public void tick() {
        prevPrevX = xo; prevPrevY = yo; prevPrevZ = zo;
        super.tick();
        if (removed) return;
        if (level.getFluidState(BlockPos.containing(x,y,z)).is(FluidTags.WATER)) {
            enteredWater = true;
            xd *= 0.93; yd *= 0.93; zd *= 0.93;
        }
        setSpriteFromAge(sprites);
        float progress = (float) age / lifetime;
        if (random.nextFloat() > progress + 0.8F || (random.nextFloat() < 0.01F && xd*xd+yd*yd+zd*zd > 0.001)) {
            level.addParticle(ParryMod.SPARK_FLASH.get(), prevPrevX, prevPrevY, prevPrevZ, 0, 0, 0);
        }
    }

    @Override public void setSpriteFromAge(SpriteSet set) {
        if (!removed) setSprite(set.get(Mth.clamp(age * (enteredWater ? 3 : 1), 0, lifetime), lifetime));
    }

    @Override public void move(double dx, double dy, double dz) {
        Vec3 actual = Entity.collideBoundingBox(null, new Vec3(dx,dy,dz), getBoundingBox(), level, List.of());
        setBoundingBox(getBoundingBox().move(actual));
        setLocationFromBoundingbox();
        if (Math.abs(actual.x-dx)>1e-7) xd = -dx * 0.8;
        if (Math.abs(actual.y-dy)>1e-7) yd = -dy * 0.8;
        if (Math.abs(actual.z-dz)>1e-7) zd = -dz * 0.8;
        // Vanilla stops grounded particles; keep ticking after the collision response to allow bounces.
        onGround = false;
    }

    @Override public int getLightColor(float partialTick) {
        int adjusted = Mth.clamp(age * (enteredWater ? 3 : 1), 0, lifetime);
        int glow = Mth.clamp((int)((1F - (float)adjusted / lifetime) * 15), 0, 15);
        BlockPos pos = BlockPos.containing(x,y,z);
        int block = Math.max(level.getBrightness(LightLayer.BLOCK, pos), glow);
        int sky = level.getBrightness(LightLayer.SKY, pos);
        return (sky << 20) | (block << 4);
    }

    @Override public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 now = new Vec3(Mth.lerp(partialTick,xo,x), Mth.lerp(partialTick,yo,y), Mth.lerp(partialTick,zo,z));
        Vec3 old = new Vec3(Mth.lerp(partialTick,prevPrevX,xo), Mth.lerp(partialTick,prevPrevY,yo), Mth.lerp(partialTick,prevPrevZ,zo));
        Vec3 delta = now.subtract(old);
        Vec3 axis = delta.lengthSqr()>1e-9 ? delta.normalize() : new Vec3(0,1,0);
        Vec3 center = now.add(old).scale(0.5).subtract(camera.getPosition());
        double halfLength = Math.max(quadSize * 0.5, delta.length() * 0.5);
        Vec3 end = axis.scale(halfLength);
        Vec3 side1 = axis.cross(new Vec3(0,1,0));
        // Vertical trails need another reference axis to avoid a zero-width ribbon.
        if (side1.lengthSqr()<1e-8) side1 = axis.cross(new Vec3(1,0,0));
        side1 = side1.normalize().scale(quadSize * crossX);
        Vec3 side2 = axis.cross(side1).normalize().scale(quadSize * crossZ);
        drawQuad(buffer,center,end,side1,partialTick);
        drawQuad(buffer,center,end,side2,partialTick);
    }

    private void drawQuad(VertexConsumer b, Vec3 c, Vec3 end, Vec3 side, float partialTick) {
        Vec3 a=c.add(end).add(side), d=c.add(end).subtract(side);
        Vec3 e=c.subtract(end).subtract(side), f=c.subtract(end).add(side);
        int light=getLightColor(partialTick);
        vertex(b,a,getU0(),getV0(),light);
        vertex(b,d,getU1(),getV0(),light);
        vertex(b,e,getU1(),getV1(),light);
        vertex(b,f,getU0(),getV1(),light);
    }
    private void vertex(VertexConsumer b, Vec3 p, float u, float v, int light) {
        b.vertex(p.x,p.y,p.z).uv(u,v).color(1F,1F,1F,1F).uv2(light).endVertex();
    }
    public AABB getRenderBoundingBox(float partialTick) {
        return new AABB(prevPrevX,prevPrevY,prevPrevZ,x,y,z).inflate(1);
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_OPAQUE; }
    static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        Provider(SpriteSet sprites) { this.sprites=sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x,double y,double z,double vx,double vy,double vz) {
            return new IronSparkParticle(level,x,y,z,vx,vy,vz,sprites);
        }
    }
}
