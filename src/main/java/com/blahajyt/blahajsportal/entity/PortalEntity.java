package com.blahajyt.blahajsportal.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
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
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(0.9f, 2.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            checkWallIntegrity();

            Direction f = getFacing();
            AABB triggerBox = this.getBoundingBox().deflate(
                    f.getAxis() == Direction.Axis.X ? 0.4 : 0.05,
                    0.1,
                    f.getAxis() == Direction.Axis.Z ? 0.4 : 0.05
            );

            List<Player> players = this.level().getEntitiesOfClass(Player.class, triggerBox);
            for (Player player : players) {
                if (player.invulnerableTime <= 0) {
                    teleportPlayer(player);
                }
            }
        }
    }

    private void checkWallIntegrity() {
        Direction f = getFacing();
        BlockPos centerWall = BlockPos.containing(this.getX() - f.getStepX() * 0.2, this.getY() + 1.0, this.getZ() - f.getStepZ() * 0.2);
        if (this.level().isEmptyBlock(centerWall) && this.level().isEmptyBlock(centerWall.below())) {
            this.discard();
        }
    }

    private void teleportPlayer(Player player) {
        PortalEntity pair = getPair();
        if (pair != null) {
            Direction outFace = pair.getFacing();
            double spawnX = pair.getX() + outFace.getStepX() * 0.75;
            double spawnY = pair.getY();
            double spawnZ = pair.getZ() + outFace.getStepZ() * 0.75;
            float yaw = outFace.toYRot();

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.teleportTo(serverPlayer.serverLevel(), spawnX, spawnY, spawnZ, yaw, player.getXRot());
            } else {
                player.absMoveTo(spawnX, spawnY, spawnZ, yaw, player.getXRot());
            }

            player.setYHeadRot(yaw);
            player.invulnerableTime = 15;
        }
    }

    // Ezt használd a Rendererben az átlátáshoz!
    public PortalEntity getPair() {
        int targetColor = (this.getPortalColor() == 0) ? 1 : 0;
        List<PortalEntity> portals = this.level().getEntitiesOfClass(PortalEntity.class, this.getBoundingBox().inflate(256));
        for (PortalEntity other : portals) {
            if (other != this && other.getPortalColor() == targetColor) {
                return other;
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR, 0);
        this.entityData.define(FACING, Direction.NORTH);
    }

    public void setPortalColor(int color) { this.entityData.set(COLOR, color); }
    public int getPortalColor() { return this.entityData.get(COLOR); }
    public void setFacing(Direction direction) { this.entityData.set(FACING, direction); }
    public Direction getFacing() { return this.entityData.get(FACING); }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setPortalColor(tag.getInt("PortalColor"));
        if (tag.contains("Facing")) {
            setFacing(Direction.from3DDataValue(tag.getInt("Facing")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("PortalColor", getPortalColor());
        tag.putInt("Facing", getFacing().get3DDataValue());
    }
}