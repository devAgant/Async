package com.axalotl.async.mixin.world;

import com.axalotl.async.ParallelProcessor;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@Mixin(value = ServerChunkManager.class, priority = 1500)
public abstract class ServerChunkManagerMixin extends ChunkManager {
    @Shadow
    @Final
    Thread serverThread;

    @Shadow
    public abstract @Nullable ChunkHolder getChunkHolder(long pos);

    @Shadow
    @Final
    public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

    @Inject(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;", at = @At("HEAD"), cancellable = true)
    private void shortcutGetChunk(int x, int z, ChunkStatus status, boolean create, CallbackInfoReturnable<Chunk> cir) {
        if (Thread.currentThread() != this.serverThread) {
            ChunkHolder holder = this.getChunkHolder(ChunkPos.toLong(x, z));
            if (holder != null) {
                Chunk chunk = holder.getWorldChunk();
                if (chunk != null && chunk.getStatus().isAtLeast(status)) {
                    cir.setReturnValue(chunk);
                } else {
                    cir.setReturnValue(null);
                }
            } else {
                cir.setReturnValue(null);
            }
        }
    }

    @Inject(method = "getWorldChunk", at = @At("HEAD"), cancellable = true)
    private void shortcutGetWorldChunk(int chunkX, int chunkZ, CallbackInfoReturnable<WorldChunk> cir) {
        if (Thread.currentThread() != this.serverThread) {
            ChunkHolder holder = this.getChunkHolder(ChunkPos.toLong(chunkX, chunkZ));
            if (holder != null) {
                WorldChunk chunk = holder.getWorldChunk();
                if (chunk != null) {
                    cir.setReturnValue(chunk);
                } else {
                    cir.setReturnValue(null);
                }
            } else {
                cir.setReturnValue(null);
            }
        }
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/SpawnHelper;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/SpawnHelper$Info;ZZZ)V"))
    private void tickChunks(ServerWorld world, WorldChunk worldChunk, SpawnHelper.Info info, boolean bl, boolean bl2, boolean bl3) {
        ParallelProcessor.asyncSpawn(world, worldChunk, info);
    }

    @WrapMethod(method = "putInCache")
    private synchronized void syncPutInCache(long pos, Chunk chunk, ChunkStatus status, Operation<Void> original) {
        original.call(pos, chunk, status);
    }
}
