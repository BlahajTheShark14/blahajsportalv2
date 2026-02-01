package com.blahajyt.blahajsportal.event;

import com.blahajyt.blahajsportal.entity.PortalEntity;
import com.blahajyt.blahajsportal.entity.ModEntities;
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

@Mod.EventBusSubscriber(modid = "blahajsportal", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PortalGunEvents {

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) { handleShoot(event, 0); }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) { handleShoot(event, 1); }

    private static void handleShoot(PlayerInteractEvent event, int colorCode) {
        if (!event.getItemStack().is(ModItems.PORTAL_GUN.get())) return;
        event.setCanceled(true);
        Level world = event.getLevel();
        if (world.isClientSide) return;

        PortalGunItem gun = (PortalGunItem) event.getItemStack().getItem();
        BlockHitResult hit = gun.shootRaycast(world, event.getEntity());

        if (hit.getType() == HitResult.Type.BLOCK) {
            Direction face = hit.getDirection();
            if (!face.getAxis().isHorizontal()) return;

            BlockPos hitPos = hit.getBlockPos();

            // 1. Alappozíció (fal előtt 0.1-el, kurzor magasságában)
            double px = hit.getLocation().x + face.getStepX() * 0.1;
            double py = hit.getLocation().y - 1.0;
            double pz = hit.getLocation().z + face.getStepZ() * 0.1;

            // 2. FÜGGŐLEGES HATÁROK KERESÉSE
            double wallMinY = hitPos.getY();
            while (isWall(world, BlockPos.containing(px - face.getStepX()*0.2, wallMinY - 1, pz - face.getStepZ()*0.2), face)) wallMinY--;
            double wallMaxY = hitPos.getY() + 1;
            while (isWall(world, BlockPos.containing(px - face.getStepX()*0.2, wallMaxY, pz - face.getStepZ()*0.2), face)) wallMaxY++;

            // Ha kisebb a fal mint 2 blokk, cancel
            if (wallMaxY - wallMinY < 2.0) return;

            // CLAMP Függőlegesen
            if (py < wallMinY) py = wallMinY;
            if (py + 2.0 > wallMaxY) py = wallMaxY - 2.0;

            // 3. VÍZSZINTES HATÁROK KERESÉSE (Itt dől el minden)
            Direction side = face.getClockWise();
            double margin = 0.45; // Portál sugara

            if (face.getAxis() == Direction.Axis.Z) {
                double wallMinX = hitPos.getX();
                while (isWall(world, BlockPos.containing(wallMinX - 1, py + 0.5, hitPos.getZ()), face)) wallMinX--;
                double wallMaxX = hitPos.getX() + 1;
                while (isWall(world, BlockPos.containing(wallMaxX, py + 0.5, hitPos.getZ()), face)) wallMaxX++;

                // Kényszerítés (Clamp) X tengelyen
                if (px - margin < wallMinX) px = wallMinX + margin;
                if (px + margin > wallMaxX) px = wallMaxX - margin;
            } else {
                double wallMinZ = hitPos.getZ();
                while (isWall(world, BlockPos.containing(hitPos.getX(), py + 0.5, wallMinZ - 1), face)) wallMinZ--;
                double wallMaxZ = hitPos.getZ() + 1;
                while (isWall(world, BlockPos.containing(hitPos.getX(), py + 0.5, wallMaxZ), face)) wallMaxZ++;

                // Kényszerítés (Clamp) Z tengelyen
                if (pz - margin < wallMinZ) pz = wallMinZ + margin;
                if (pz + margin > wallMaxZ) pz = wallMaxZ - margin;
            }

            // 4. MÁSIK PORTÁL ÜTKÖZÉS
            AABB finalBox = new AABB(px - 0.4, py, pz - 0.4, px + 0.4, py + 2.0, pz + 0.4);
            if (!world.getEntitiesOfClass(PortalEntity.class, finalBox.inflate(0.01)).isEmpty()) {
                // Ha ütközik a másikkal, megnézzük a színt
                if (world.getEntitiesOfClass(PortalEntity.class, finalBox.inflate(0.01)).stream()
                        .anyMatch(p -> p.getPortalColor() != colorCode)) return;
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

    private static boolean isWall(Level world, BlockPos pos, Direction face) {
        // Fontos: csak azt tekintjük falnak, ami felénk néző szilárd felület!
        return world.getBlockState(pos).isFaceSturdy(world, pos, face);
    }

    private static void removeOldPortal(Level level, int color) {
        level.getEntitiesOfClass(PortalEntity.class, new AABB(-30000, -64, -30000, 30000, 320, 30000))
                .stream().filter(p -> p.getPortalColor() == color).forEach(p -> p.discard());
    }
}