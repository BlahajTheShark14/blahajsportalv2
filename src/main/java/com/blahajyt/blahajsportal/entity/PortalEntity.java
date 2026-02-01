package com.blahajyt.blahajsportal.entity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class PortalEntity extends Entity {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> FACING = SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.DIRECTION);

    public PortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // Megkeressük a játékosokat a portál közvetlen közelében
            AABB detectionBox = this.getBoundingBox().inflate(0.2);
            List<Player> players = this.level().getEntitiesOfClass(Player.class, detectionBox);

            for (Player player : players) {
                if (player.invulnerableTime <= 0) {
                    teleportPlayer(player);
                }
            }
        }
    }

    private void teleportPlayer(Player player) {
        int targetColor = (this.getPortalColor() == 0) ? 1 : 0;
        List<PortalEntity> portals = this.level().getEntitiesOfClass(PortalEntity.class, new AABB(-30000, -64, -30000, 30000, 320, 30000));

        for (PortalEntity other : portals) {
            if (other != this && other.getPortalColor() == targetColor) {
                Direction outFace = other.getFacing();

                // Kiszámoljuk a kijárati pontot a portál elé
                double spawnX = other.getX() + outFace.getStepX() * 0.8;
                double spawnY = other.getY();
                double spawnZ = other.getZ() + outFace.getStepZ() * 0.8;

                float yaw = outFace.toYRot();

                // Teleportáljuk a játékost
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.teleportTo(serverPlayer.serverLevel(), spawnX, spawnY, spawnZ, yaw, player.getXRot());
                } else {
                    player.absMoveTo(spawnX, spawnY, spawnZ, yaw, player.getXRot());
                }

                player.setYHeadRot(yaw);
                player.invulnerableTime = 20; // Megakadályozza a visszateleportálást azonnal
                return;
            }
        }
    }

    @Override protected void defineSynchedData() {
        this.entityData.define(COLOR, 0);
        this.entityData.define(FACING, Direction.NORTH);
    }

    public void setPortalColor(int color) { this.entityData.set(COLOR, color); }
    public int getPortalColor() { return this.entityData.get(COLOR); }
    public void setFacing(Direction direction) { this.entityData.set(FACING, direction); }
    public Direction getFacing() { return this.entityData.get(FACING); }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        setPortalColor(tag.getInt("PortalColor"));
        setFacing(Direction.from3DDataValue(tag.getInt("Facing")));
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("PortalColor", getPortalColor());
        tag.putInt("Facing", getFacing().get3DDataValue());
    }
}