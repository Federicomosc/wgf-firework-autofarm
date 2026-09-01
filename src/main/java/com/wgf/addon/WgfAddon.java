package com.wgf.addon;

import com.wgf.addon.modules.FireworkAutofarm;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class WgfAddon extends MeteorAddon {
    public static final Category CATEGORY = new Category("WGF");

    @Override
    public void onInitialize() {
        Modules.get().add(new FireworkAutofarm());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.wgf.addon";
    }
}
