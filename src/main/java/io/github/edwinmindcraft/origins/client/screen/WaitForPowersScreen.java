package io.github.edwinmindcraft.origins.client.screen;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.origins.api.origin.IOriginCallbackPower;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.C2SFinalizeNowReadyPowers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class WaitForPowersScreen extends OriginDisplayScreen {

    private static final ResourceLocation WAITING_WINDOW = new ResourceLocation(Origins.MODID, "textures/gui/wait_for_powers.png");
    private final Set<ResourceKey<ConfiguredPower<?, ?>>> waitingFor = Sets.newHashSet();
    private final boolean wasOrb;

    public WaitForPowersScreen(boolean showDirtBackground, Set<ResourceKey<ConfiguredPower<?, ?>>> waitingFor, boolean wasOrb) {
        super(Component.empty(), showDirtBackground);
        this.waitingFor.addAll(waitingFor);
        this.wasOrb = wasOrb;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.time += delta;
        this.renderBackground(graphics);
        graphics.blit(WAITING_WINDOW, this.guiLeft, this.guiTop, 0, 0, windowWidth, windowHeight);
        this.renderWaitingText(graphics);
    }

    @Override
    public void tick() {
        if (this.areAllPowersReadyToGo()) {
            OriginsCommon.CHANNEL.sendToServer(new C2SFinalizeNowReadyPowers(this.waitingFor, this.wasOrb));
            this.onClose();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean areAllPowersReadyToGo() {
        return waitingFor.stream().map(key -> ApoliAPI.getPowers().get(key)).allMatch(p -> p == null || !(p.getFactory() instanceof IOriginCallbackPower callbackPower) || callbackPower.isReady(p, this.minecraft.player, this.wasOrb));
    }

    private void renderWaitingText(@NotNull GuiGraphics graphics) {
        int textWidth = windowWidth - 48;
        int dotAmount = (int) (this.time / 10 % 4);
        MutableComponent component = Component.translatable("origins.gui.waiting_for_powers");
        for (int i = 0; i < dotAmount; ++i) {
            component.append(".");
        }
        List<FormattedCharSequence> components = this.font.split(component, textWidth);
        int x = this.guiLeft + (windowWidth / 2);
        int y = this.guiTop + 50;
        for (FormattedCharSequence c : components) {
            graphics.drawCenteredString(this.font, c, x, y, 0xffffff);
            y += 12;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
