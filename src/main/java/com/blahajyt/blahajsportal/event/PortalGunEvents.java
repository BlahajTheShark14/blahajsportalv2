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

                        // 1. OSZLOP-MÁGNES (Vízszintes középre igazítás)
                        boolean isNarrow = world.isEmptyBlock(hitPos.relative(face.getCounterClockWise())) &&
                                world.isEmptyBlock(hitPos.relative(face.getClockWise()));
                        if (isNarrow) {
                            px = hitPos.getX() + 0.5 + face.getStepX() * 0.51;
                            pz = hitPos.getZ() + 0.5 + face.getStepZ() * 0.51;
                        }

                        // 2. ÚJ FÜGGŐLEGES FAL-SZKENNER ÉS CLAMP
                        // A fal síkjában keressük a határokat
                        BlockPos wallCheckStart = BlockPos.containing(px - face.getStepX()*0.2, hit.getLocation().y, pz - face.getStepZ()*0.2);

                        // Lefelé keressük az utolsó szilárd blokkot
                        BlockPos currentPos = wallCheckStart;
                        while (!world.isEmptyBlock(currentPos.below()) && currentPos.getY() > world.getMinBuildHeight()) {
                            currentPos = currentPos.below();
                        }
                        double minY = currentPos.getY(); // A legalsó szilárd blokk alja

                        // Felfelé keressük az utolsó szilárd blokkot
                        currentPos = wallCheckStart;
                        while (!world.isEmptyBlock(currentPos.above()) && currentPos.getY() < world.getMaxBuildHeight()) {
                            currentPos = currentPos.above();
                        }
                        double maxY = currentPos.getY() + 1.0; // A legfelső szilárd blokk teteje

                        // Szigorú clamp a fal határain belülre
                        if (py < minY) py = minY;
                        if (py + 2.0 > maxY) py = maxY - 2.0;

                        // 3. VÍZSZINTES CLAMP (Sarokvédelem széles falon)
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

                        // 4. ÜTKÖZÉSVIZSGÁLAT (Egymásra csúszás tiltása)
                        // A portál tervezett doboza (szélesség +/- 0.45, magasság 2.0)
                        AABB proposedBox = new AABB(px - 0.45, py, pz - 0.45, px + 0.45, py + 2.0, pz + 0.45);
                        // Egy picit kisebb dobozzal nézzük a fedést, hogy az érintkezést engedje
                        AABB checkOverlapBox = proposedBox.deflate(0.001);

                        boolean overlaps = world.getEntitiesOfClass(PortalEntity.class, checkOverlapBox)
                                .stream()
                                .anyMatch(p -> p.getPortalColor() != colorCode); // Csak a másik színnel nem fedhet

                        if (overlaps) {
                            return; // Ha fedésben van, nem rakjuk le
                        }

                        // 5. LERAKÁS (Végső 2x1 ellenőrzéssel)
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