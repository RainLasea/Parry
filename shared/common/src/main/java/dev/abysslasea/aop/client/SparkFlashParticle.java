package dev.abysslasea.aop.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Keeps the contact flash fully lit so it remains visible in dark environments. */
final class SparkFlashParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float originalSize;
    private int previousAge;
    private SparkFlashParticle(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,SpriteSet sprites) {
        super(level,x,y,z);
        this.sprites=sprites;
        friction=0.96F;
        xd=vx*0.5+(random.nextDouble()*3-1.5)*0.07;
        yd=vy*0.5+(random.nextDouble()*3-1.5)*0.07;
        zd=vz*0.5+(random.nextDouble()*3-1.5)*0.07;
        lifetime=3+random.nextInt(4);
        quadSize=0.125F;
        originalSize=quadSize;
        setSpriteFromAge(sprites);
    }
    @Override public void tick() {
        previousAge=age;
        super.tick();
        if(!removed) setSpriteFromAge(sprites);
    }
    @Override public int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public void render(VertexConsumer buffer,Camera camera,float partialTick) {
        quadSize=originalSize*(0.5F+(1F-Mth.lerp(partialTick,previousAge,age)/(float)lifetime)*0.5F);
        super.render(buffer,camera,partialTick);
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_OPAQUE; }
    static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        Provider(SpriteSet sprites) {this.sprites=sprites;}
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double vx,double vy,double vz) {
            return new SparkFlashParticle(level,x,y,z,vx,vy,vz,sprites);
        }
    }
}
