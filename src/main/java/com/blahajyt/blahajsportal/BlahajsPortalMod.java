package com.blahajyt.blahajsportal;

import com.blahajyt.blahajsportal.entity.ModEntities;
import com.blahajyt.blahajsportal.client.renderer.PortalRenderer;
import com.blahajyt.blahajsportal.item.ModItems;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BlahajsPortalMod.MODID)
public class BlahajsPortalMod {
    public static final String MODID = "blahajsportal";

    public BlahajsPortalMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);

        modEventBus.addListener(this::registerRenderers);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PORTAL.get(), PortalRenderer::new);
    }
}