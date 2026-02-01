package com.blahajyt.blahajsportal.event;

import com.blahajyt.blahajsportal.BlahajsPortalMod;
import com.blahajyt.blahajsportal.entity.ModEntities;
import com.blahajyt.blahajsportal.entity.PortalEntity;
import com.blahajyt.blahajsportal.item.ModItems;
import com.blahajyt.blahajsportal.item.PortalGunItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlahajsPortalMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PortalGunEvents {

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) { handleShoot(event, 0); }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) { handleShoot(event, 1); }

    private static void handleShoot(PlayerInteractEvent event, int colorCode) {
        if (event.getItemStack().is(ModItems.PORTAL_GUN.get())) {
            event.setCanceled(true);
            Level world = event.getLevel();
            if (!world.isClientSide) {
                PortalGunItem gun = (PortalGunItem) event.getItemStack().getItem();
                BlockHitResult hit = gun.shootRaycast(world, event.getEntity());

                if (hit.getType() == HitResult.Type.BLOCK) {
                    Direction face = hit.getDirection();
                    if (face.getAxis().isHorizontal()) {
                        BlockPos hitPos = hit.getBlockPos();
                        double px = hit.getLocation().x + face.getStepX() * 0.05;
                        double py = hit.getLocation().y - 1.0;
                        double pz = hit.getLocation().z + face.getStepZ() * 0.05;

                        // OSZLOP ELLENŐRZÉS (Ha csak 1 széles a fal, rácuppan)
                        boolean isNarrow = world.isEmptyBlock(hitPos.relative(face.getCounterClockWise())) &&
                                world.isEmptyBlock(hitPos.relative(face.getClockWise()));
                        if (isNarrow) {
                            px = hitPos.getX() + 0.5 + face.getStepX() * 0.51;
                            pz = hitPos.getZ() + 0.5 + face.getStepZ() * 0.51;
                            py = Math.floor(py + 0.5);
                        }

                        // ALAPSZABÁLY: Kell a 2 blokk magas fal
                        BlockPos wallBase = BlockPos.containing(px - face.getStepX()*0.2, py + 0.5, pz - face.getStepZ()*0.2);
                        if (world.isEmptyBlock(wallBase) || world.isEmptyBlock(wallBase.above())) {
                            return;
                        }

                        // FÖLDBE LÓGÁS ELLENI VÉDELEM (isFaceSturdy javítva)
                        if (world.getBlockState(wallBase.below()).isFaceSturdy(world, wallBase.below(), Direction.UP)) {
                            if (py < wallBase.getY()) py = wallBase.getY();
                        }
                        // PLAFON ELLENI VÉDELEM
                        if (world.getBlockState(wallBase.above(2)).isFaceSturdy(world, wallBase.above(2), Direction.DOWN)) {
                            if (py + 1.9 > wallBase.getY() + 1.0) py = wallBase.getY() + 1.0 - 0.9;
                        }

                        // LERAKÁS
                        removeOldPortal(world, colorCode);
                        PortalEntity portal = new PortalEntity(ModEntities.PORTAL.get(), world);
                        portal.setPos(px, py, pz);
                        portal.setFacing(face);
                        portal.setPortalColor(colorCode);
                        world.addFreshEntity(portal);
                    }
                }
            }
        }
    }

    private static void removeOldPortal(Level level, int color) {
        level.getEntitiesOfClass(PortalEntity.class, new AABB(-30000, -64, -30000, 30000, 320, 30000))
                .stream().filter(p -> p.getPortalColor() == color).forEach(p -> p.discard());
    }
}