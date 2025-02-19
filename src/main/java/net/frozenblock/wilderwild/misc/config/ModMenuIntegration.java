package net.frozenblock.wilderwild.misc.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.wilderwild.WilderWild;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        if (WilderWild.hasCloth) {
            return ClientOnlyConfigInteractionHandler.buildScreen();
        }
        return (screen -> null);
    }

}
