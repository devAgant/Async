package com.axalotl.async.mixin.world;

import com.axalotl.async.parallelised.fastutil.Int2ObjectConcurrentHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ThreadedAnvilChunkStorage.class, priority = 1500)
public abstract class ThreadedAnvilChunkStorageMixin {
    @Shadow @Final @Mutable
    private Int2ObjectMap<?> entityTrackers;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        this.entityTrackers = new Int2ObjectConcurrentHashMap<>();
    }
}
