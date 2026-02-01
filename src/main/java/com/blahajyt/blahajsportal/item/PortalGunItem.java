package com.blahajyt.blahajsportal.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class PortalGunItem extends Item {
    public PortalGunItem(Properties properties) {
        super(properties);
    }

    public BlockHitResult shootRaycast(Level level, Player player) {
        double reach = 50.0;
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.x * reach, look.y * reach, look.z * reach);

        // A COLLIDER helyett érdemesebb lehet a BLOCK-ot nézni, hogy pontos legyen
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }
}