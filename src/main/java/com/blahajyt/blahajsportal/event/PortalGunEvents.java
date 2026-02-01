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

import java.util.List;

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
                        double pz = hit.getLocation().z + face.getStepZ() * 0.05;
                        double py = hit.getLocation().y - 1.0;

                        // 1. FÜGGŐLEGES KORLÁTOZÁS (Padló/Plafon)
                        if (py < hitPos.getY()) py = hitPos.getY();
                        if (py + 1.9 > hitPos.getY() + 1.0) {
                            // Ha csak 2 blokk magas falunk van, rácuppanunk az aljára
                            if (!world.isEmptyBlock(hitPos.above())) {
                                py = hitPos.getY() + 1.0 - 1.9;
                            }
                        }

                        // 2. VÍZSZINTES IGAZÍTÁS (Ne lógjon le a falról)
                        // Megnézzük a szomszédos blokkokat a fal síkjában
                        Direction leftDir = face.getCounterClockWise();
                        BlockPos side1 = hitPos.relative(leftDir);
                        BlockPos side2 = hitPos.relative(leftDir.getOpposite());

                        boolean hasSide1 = !world.isEmptyBlock(side1) && !world.isEmptyBlock(side1.above());
                        boolean hasSide2 = !world.isEmptyBlock(side2) && !world.isEmptyBlock(side2.above());

                        if (!hasSide1 && !hasSide2) {
                            // 1 blokk széles oszlop: kényszerítjük a közepet
                            px = hitPos.getX() + 0.5 + face.getStepX() * 0.05;
                            pz = hitPos.getZ() + 0.5 + face.getStepZ() * 0.05;
                        } else {
                            // Ha csak az egyik oldalon van fal, korlátozzuk a mozgást a szélénél
                            double offset = 0.4; // Portál fél-szélessége
                            if (!hasSide1) { /* Itt lehetne finomítani a clamp-et, de az oszlop-fix a legfontosabb */ }
                        }

                        // 3. ÜTKÖZÉSVIZSGÁLAT ÉS LERAKÁS
                        AABB checkArea = new AABB(px-0.3, py, pz-0.3, px+0.3, py+1.9, pz+0.3);
                        List<PortalEntity> existing = world.getEntitiesOfClass(PortalEntity.class, checkArea);

                        if (existing.isEmpty() || (existing.size() == 1 && existing.get(0).getPortalColor() == colorCode)) {
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