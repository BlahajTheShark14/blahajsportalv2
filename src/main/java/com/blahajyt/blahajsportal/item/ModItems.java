package com.blahajyt.blahajsportal.item;

import com.blahajyt.blahajsportal.BlahajsPortalMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BlahajsPortalMod.MODID);

    public static final RegistryObject<Item> PORTAL_GUN = ITEMS.register("portal_gun",
            () -> new PortalGunItem(new Item.Properties().stacksTo(1)));
}