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
            if (world.isClientSide) return;

            PortalGunItem gun = (PortalGunItem) event.getItemStack().getItem();
            BlockHitResult hit = gun.shootRaycast(world, event.getEntity());

            if (hit.getType() == HitResult.Type.BLOCK) {
                Direction face = hit.getDirection();
                if (face.getAxis().isHorizontal()) {
                    BlockPos hitPos = hit.getBlockPos();

                    // 1. Alappozíció
                    double px = hit.getLocation().x + face.getStepX() * 0.05;
                    double py = hit.getLocation().y - 1.0;
                    double pz = hit.getLocation().z + face.getStepZ() * 0.05;

                    // 2. SZIGORÚ FÜGGŐLEGES HATÁROK
                    // Megkeressük a fal legalját és legtetejét
                    int minY = hitPos.getY();
                    while (world.getBlockState(new BlockPos(hitPos.getX(), minY - 1, hitPos.getZ())).isFaceSturdy(world, new BlockPos(hitPos.getX(), minY - 1, hitPos.getZ()), face)) {
                        minY--;
                    }
                    int maxY = hitPos.getY() + 1;
                    while (world.getBlockState(new BlockPos(hitPos.getX(), maxY, hitPos.getZ())).isFaceSturdy(world, new BlockPos(hitPos.getX(), maxY, hitPos.getZ()), face)) {
                        maxY++;
                    }

                    // Kényszerítés (Clamp):
                    // Ha py kisebb, mint a fal alja, legyen pontosan a fal alja.
                    if (py < (double)minY) py = (double)minY;
                    // Ha a teteje kilógna, legyen pontosan a fal teteje mínusz 2 blokk.
                    if (py + 2.0 > (double)maxY) py = (double)maxY - 2.0;

                    // Extra biztonság: Ha nagyon közel van a határhoz, kerekítsük rá
                    if (Math.abs(py - minY) < 0.01) py = minY;
                    if (Math.abs((py + 2.0) - maxY) < 0.01) py = maxY - 2.0;

                    // 3. VÍZSZINTES SAROK-VÉDELEM
                    Direction side = face.getClockWise();
                    double margin = 0.45;

                    // Csak akkor tologatjuk, ha nem 1 blokk széles oszlop
                    boolean leftSolid = world.getBlockState(hitPos.relative(face.getCounterClockWise())).isFaceSturdy(world, hitPos.relative(face.getCounterClockWise()), face);
                    boolean rightSolid = world.getBlockState(hitPos.relative(face.getClockWise())).isFaceSturdy(world, hitPos.relative(face.getClockWise()), face);

                    if (!leftSolid && !rightSolid) {
                        // Oszlop: Középre igazít
                        px = hitPos.getX() + 0.5 + face.getStepX() * 0.51;
                        pz = hitPos.getZ() + 0.5 + face.getStepZ() * 0.51;
                    } else {
                        // Sarok védelem: Megnézzük a széleket
                        if (!world.getBlockState(hitPos.relative(side)).isFaceSturdy(world, hitPos.relative(side), face)) {
                            // Ha a "jobb" oldalán levegő van, limitáljuk a mozgást abba az irányba
                            if (face.getAxis() == Direction.Axis.Z) px = Math.min(px, hitPos.getX() + 1.0 - margin);
                            else pz = Math.min(pz, hitPos.getZ() + 1.0 - margin);
                        }
                        if (!world.getBlockState(hitPos.relative(side.getOpposite())).isFaceSturdy(world, hitPos.relative(side.getOpposite()), face)) {
                            // Ha a "bal" oldalán levegő van
                            if (face.getAxis() == Direction.Axis.Z) px = Math.max(px, hitPos.getX() + margin);
                            else pz = Math.max(pz, hitPos.getZ() + margin);
                        }
                    }

                    // 4. EGYMÁSBA CSÚSZÁS TILTÁSA (Szigorú AABB)
                    AABB myBox = new AABB(px - 0.4, py, pz - 0.4, px + 0.4, py + 2.0, pz + 0.4);
                    if (world.getEntitiesOfClass(PortalEntity.class, myBox.inflate(0.05)).stream().anyMatch(p -> p.getPortalColor() != colorCode)) {
                        return; // Ütközés van, nem rakjuk le
                    }

                    // 5. LERAKÁS
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

    private static void removeOldPortal(Level level, int color) {
        level.getEntitiesOfClass(PortalEntity.class, new AABB(-30000, -64, -30000, 30000, 320, 30000))
                .stream().filter(p -> p.getPortalColor() == color).forEach(p -> p.discard());
    }
}