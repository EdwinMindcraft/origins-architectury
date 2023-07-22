package io.github.apace100.origins.screen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.apace100.origins.Origins;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

public class ViewOriginScreen extends OriginDisplayScreen {

	private final ArrayList<Tuple<OriginLayer, Origin>> originLayers;
	private int currentLayer = 0;
	private Button chooseOriginButton;

	public ViewOriginScreen() {
		super(new TranslatableComponent(Origins.MODID + ".screen.view_origin"), false);
		Player player = Objects.requireNonNull(Minecraft.getInstance().player);
		Map<OriginLayer, Origin> origins = IOriginContainer.get(player).map(IOriginContainer::getOrigins).orElseGet(ImmutableMap::of);
		this.originLayers = new ArrayList<>(origins.size());

		origins.forEach((layer, origin) -> {
			ItemStack displayItem = origin.getIcon();
			if (displayItem.getItem() == Items.PLAYER_HEAD) {
				if (!displayItem.hasTag() || !Objects.requireNonNull(displayItem.getTag()).contains("SkullOwner")) {
					displayItem.getOrCreateTag().putString("SkullOwner", player.getDisplayName().getString());
				}
			}
			if ((origin != Origin.EMPTY || layer.getOriginOptionCount(player) > 0) && !layer.hidden()) {
				this.originLayers.add(new Tuple<>(layer, origin));
			}
		});
		this.originLayers.sort(Comparator.comparing(Tuple::getA));
		if (this.originLayers.size() > 0) {
			Tuple<OriginLayer, Origin> current = this.originLayers.get(this.currentLayer);
			this.showOrigin(current.getB(), current.getA(), false);
		} else {
			this.showOrigin(null, null, false);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (this.width - windowWidth) / 2;
		this.guiTop = (this.height - windowHeight) / 2;
		if (this.originLayers.size() > 0) {
			this.addRenderableWidget(this.chooseOriginButton = new Button(this.guiLeft + windowWidth / 2 - 50, this.guiTop + windowHeight - 40, 100, 20, new TranslatableComponent(Origins.MODID + ".gui.choose"), b ->
					Minecraft.getInstance().setScreen(new ChooseOriginScreen(Lists.newArrayList(this.originLayers.get(this.currentLayer).getA()), 0, false))));
			Player player = Objects.requireNonNull(Minecraft.getInstance().player);
			this.chooseOriginButton.active = this.chooseOriginButton.visible = this.originLayers.get(this.currentLayer).getB() == Origin.EMPTY && this.originLayers.get(this.currentLayer).getA().getOriginOptionCount(player) > 0;
			if (this.originLayers.size() > 1) {
				this.addRenderableWidget(new Button(this.guiLeft - 40, this.height / 2 - 10, 20, 20, new TextComponent("<"), b -> {
					this.currentLayer = (this.currentLayer - 1 + this.originLayers.size()) % this.originLayers.size();
					this.switchLayer(player);
				}));
				this.addRenderableWidget(new Button(this.guiLeft + windowWidth + 20, this.height / 2 - 10, 20, 20, new TextComponent(">"), b -> {
					this.currentLayer = (this.currentLayer + 1) % this.originLayers.size();
					this.switchLayer(player);
				}));
			}
		}
		this.addRenderableWidget(new Button(this.guiLeft + windowWidth / 2 - 50, this.guiTop + windowHeight + 5, 100, 20, new TranslatableComponent(Origins.MODID + ".gui.close"), b -> Minecraft.getInstance().setScreen(null)));
	}

	private void switchLayer(Player player) {
		Tuple<OriginLayer, Origin> current = this.originLayers.get(this.currentLayer);
		this.showOrigin(current.getB(), current.getA(), false);
		this.chooseOriginButton.active = this.chooseOriginButton.visible = current.getB() == Origin.EMPTY && current.getA().getOriginOptionCount(player) > 0;
	}

	@Override
	public void render(@NotNull PoseStack matrices, int mouseX, int mouseY, float delta) {
		super.render(matrices, mouseX, mouseY, delta);
		if (this.originLayers.size() == 0) {
			//if(OriginsClient.isServerRunningOrigins) {
			drawCenteredString(matrices, this.font, new TranslatableComponent(Origins.MODID + ".gui.view_origin.empty").getString(), this.width / 2, this.guiTop + 48, 0xFFFFFF);
			//} else {
			//	drawCenteredText(matrices, this.textRenderer, new TranslatableText(Origins.MODID + ".gui.view_origin.not_installed").getString(), width / 2, guiTop + 48, 0xFFFFFF);
			//}
		}
	}

	@Override
	protected Component getTitleText() {
		if (this.getCurrentLayer().title().view() != null)
			return this.getCurrentLayer().title().view();
		return new TranslatableComponent(Origins.MODID + ".gui.view_origin.title", this.getCurrentLayer().name());
	}
}
