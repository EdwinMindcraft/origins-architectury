package io.github.apace100.origins.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.registry.ModComponents;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;

public class WaitForNextLayerScreen extends Screen {

    private final ArrayList<OriginLayer> layerList;
    private final int currentLayerIndex;
    private final boolean showDirtBackground;
    private final int maxSelection;

    protected WaitForNextLayerScreen(ArrayList<OriginLayer> layerList, int currentLayerIndex, boolean showDirtBackground) {
        super(TextComponent.EMPTY);
        this.layerList = layerList;
        this.currentLayerIndex = currentLayerIndex;
        this.showDirtBackground = showDirtBackground;
        Player player = Minecraft.getInstance().player;
        OriginLayer currentLayer = layerList.get(currentLayerIndex);
        maxSelection = currentLayer.getOriginOptionCount(player);
    }

    public void openSelection() {
        int index = currentLayerIndex + 1;
        Player player = Minecraft.getInstance().player;
        OriginComponent component = ModComponents.ORIGIN.get(player);
        while(index < layerList.size()) {
            if(!component.hasOrigin(layerList.get(index)) && layerList.get(index).getOrigins(player).size() > 0) {
                Minecraft.getInstance().setScreen(new ChooseOriginScreen(layerList, index, showDirtBackground));
                return;
            }
            index++;
        }
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        if(maxSelection == 0) {
            openSelection();
            return;
        }
        this.renderBackground(matrices);
    }

    @Override
    public void renderBackground(PoseStack matrices, int vOffset) {
        if(showDirtBackground) {
            super.renderDirtBackground(vOffset);
        } else {
            super.renderBackground(matrices, vOffset);
        }
    }
}
