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

                        // 1. OSZLOP-MÁGNES (Csak vízszintesen snapped, függőlegesen pixelpontos)
                        boolean isNarrow = world.isEmptyBlock(hitPos.relative(face.getCounterClockWise())) &&
                                world.isEmptyBlock(hitPos.relative(face.getClockWise()));
                        if (isNarrow) {
                            px = hitPos.getX() + 0.5 + face.getStepX() * 0.51;
                            pz = hitPos.getZ() + 0.5 + face.getStepZ() * 0.51;
                        }

                        // 2. FÜGGŐLEGES PADLÓ/PLAFON VÉDELEM (Pixelpontos clamp)
                        // Megkeressük a fal határait (meddig tart a szilárd rész függőlegesen)
                        BlockPos wallCheckPos = BlockPos.containing(px - face.getStepX()*0.2, hit.getLocation().y, pz - face.getStepZ()*0.2);

                        double minY = wallCheckPos.getY();
                        while (!world.isEmptyBlock(BlockPos.containing(px - face.getStepX()*0.2, minY - 1, pz - face.getStepZ()*0.2))) {
                            minY--;
                            if (minY < world.getMinBuildHeight()) break;
                        }

                        double maxY = wallCheckPos.getY();
                        while (!world.isEmptyBlock(BlockPos.containing(px - face.getStepX()*0.2, maxY + 1, pz - face.getStepZ()*0.2))) {
                            maxY++;
                            if (maxY > world.getMaxBuildHeight()) break;
                        }

                        // A portál magassága 2.0 blokk. Clamp-eljük a py-t a falon belülre.
                        if (py < minY) py = minY;
                        if (py + 2.0 > maxY + 1.0) py = (maxY + 1.0) - 2.0;

                        // 3. VÍZSZINTES SAROK-ILLÉSZTÉS (Csak ha nem oszlop)
                        if (!isNarrow) {
                            double margin = 0.45;
                            if (face.getAxis() == Direction.Axis.Z) {
                                if (world.isEmptyBlock(hitPos.west())) px = Math.max(px, hitPos.getX() + margin);
                                if (world.isEmptyBlock(hitPos.east())) px = Math.min(px, hitPos.getX() + 1.0 - margin);
                            } else {
                                if (world.isEmptyBlock(hitPos.north())) pz = Math.max(pz, hitPos.getZ() + margin);
                                if (world.isEmptyBlock(hitPos.south())) pz = Math.min(pz, hitPos.getZ() + 1.0 - margin);
                            }
                        }

                        // 4. LERAKÁS
                        BlockPos finalCheckPos = BlockPos.containing(px - face.getStepX()*0.2, py + 0.5, pz - face.getStepZ()*0.2);
                        if (!world.isEmptyBlock(finalCheckPos) && !world.isEmptyBlock(finalCheckPos.above())) {
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
    }

    private static void removeOldPortal(Level level, int color) {
        level.getEntitiesOfClass(PortalEntity.class, new AABB(-30000, -64, -30000, 30000, 320, 30000))
                .stream().filter(p -> p.getPortalColor() == color).forEach(p -> p.discard());
    }
}