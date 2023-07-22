package io.github.apace100.origins.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.origin.Impact;
import io.github.apace100.origins.registry.ModItems;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.data.PartialOrigin;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.C2SChooseOrigin;
import io.github.edwinmindcraft.origins.common.network.C2SChooseRandomOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ChooseOriginScreen extends OriginDisplayScreen {

	private static final Comparator<Origin> COMPARATOR = Comparator.comparingInt((Origin a) -> a.getImpact().getImpactValue()).thenComparingInt(Origin::getOrder);

	private final List<OriginLayer> layerList;
	private final int currentLayerIndex;
	private int currentOrigin = 0;
	private final List<Origin> originSelection;
	private int maxSelection;

	private Origin randomOrigin;

	public ChooseOriginScreen(List<OriginLayer> layerList, int currentLayerIndex, boolean showDirtBackground) {
		super(new TranslatableComponent(Origins.MODID + ".screen.choose_origin"), showDirtBackground);
		this.layerList = layerList;
		this.currentLayerIndex = currentLayerIndex;
		this.originSelection = new ArrayList<>(10);
		Player player = Minecraft.getInstance().player;
		OriginLayer currentLayer = layerList.get(currentLayerIndex);
		Registry<Origin> originsRegistry = OriginsAPI.getOriginsRegistry();
		currentLayer.origins(Objects.requireNonNull(player)).forEach(originId -> {
			Origin origin = originsRegistry.get(originId);
			if (origin != null && origin.isChoosable()) {
				ItemStack displayItem = origin.getIcon();
				if (displayItem.getItem() == Items.PLAYER_HEAD) {
					if (!displayItem.hasTag() || !Objects.requireNonNull(displayItem.getTag()).contains("SkullOwner")) {
						displayItem.getOrCreateTag().putString("SkullOwner", player.getDisplayName().getString());
					}
				}
				this.originSelection.add(origin);
			}
		});
		this.originSelection.sort(COMPARATOR);
		this.maxSelection = this.originSelection.size();
		if (currentLayer.allowRandom() && currentLayer.randomOrigins(player).size() > 0) {
			this.maxSelection += 1;
		}
		if (this.maxSelection == 0) {
			this.openNextLayerScreen();
		}
		Origin newOrigin = this.getCurrentOriginInternal();
		this.showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin == this.randomOrigin);
	}

	private void openNextLayerScreen() {
		Minecraft.getInstance().setScreen(new WaitForNextLayerScreen(this.layerList, this.currentLayerIndex, this.showDirtBackground));
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (this.width - windowWidth) / 2;
		this.guiTop = (this.height - windowHeight) / 2;
		if (this.maxSelection > 1) {
			this.addRenderableWidget(new Button(this.guiLeft - 40, this.height / 2 - 10, 20, 20, new TextComponent("<"), b -> {
				this.currentOrigin = (this.currentOrigin - 1 + this.maxSelection) % this.maxSelection;
				Origin newOrigin = this.getCurrentOriginInternal();
				this.showOrigin(newOrigin, this.layerList.get(this.currentLayerIndex), newOrigin == this.randomOrigin);
			}));
			this.addRenderableWidget(new Button(this.guiLeft + windowWidth + 20, this.height / 2 - 10, 20, 20, new TextComponent(">"), b -> {
				this.currentOrigin = (this.currentOrigin + 1) % this.maxSelection;
				Origin newOrigin = this.getCurrentOriginInternal();
				this.showOrigin(newOrigin, this.layerList.get(this.currentLayerIndex), newOrigin == this.randomOrigin);
			}));
		}
		this.addRenderableWidget(new Button(this.guiLeft + windowWidth / 2 - 50, this.guiTop + windowHeight + 5, 100, 20, new TranslatableComponent(Origins.MODID + ".gui.select"), b -> {
			ResourceLocation layer = OriginsAPI.getLayersRegistry().getKey(this.layerList.get(this.currentLayerIndex));
			this.openNextLayerScreen();
			if (this.currentOrigin == this.originSelection.size())
				OriginsCommon.CHANNEL.send(PacketDistributor.SERVER.noArg(), new C2SChooseRandomOrigin(layer));
			else
				OriginsCommon.CHANNEL.send(PacketDistributor.SERVER.noArg(), new C2SChooseOrigin(layer, this.getCurrentOrigin().getRegistryName()));
		}));
	}

	@Override
	protected Component getTitleText() {
		if (this.getCurrentLayer().title().choose() != null)
			return this.getCurrentLayer().title().choose();
		return new TranslatableComponent(Origins.MODID + ".gui.choose_origin.title", this.getCurrentLayer().name());
	}

	private Origin getCurrentOriginInternal() {
		if (this.currentOrigin == this.originSelection.size()) {
			if (this.randomOrigin == null) {
				this.initRandomOrigin();
			}
			return this.randomOrigin;
		}
		return this.originSelection.get(this.currentOrigin);
	}

	private void initRandomOrigin() {
		Registry<Origin> registry = OriginsAPI.getOriginsRegistry();
		this.randomOrigin = PartialOrigin.builder().icon(new ItemStack(ModItems.ORB_OF_ORIGIN.get())).impact(Impact.NONE).order(Integer.MAX_VALUE).loadingOrder(Integer.MAX_VALUE).build().create(Origins.identifier("random"));
		MutableComponent text = new TextComponent("");
		List<Origin> randoms = this.layerList.get(this.currentLayerIndex).randomOrigins(Objects.requireNonNull(Minecraft.getInstance().player)).stream().map(registry::get).filter(Objects::nonNull).sorted(COMPARATOR).toList();
		randoms.forEach(x -> text.append(x.getName()).append("\n"));
		this.setRandomOriginText(text);
	}

	@Override
	public void renderBackground(@NotNull PoseStack matrices, int vOffset) {
		if (this.showDirtBackground) {
			super.renderDirtBackground(vOffset);
		} else {
			super.renderBackground(matrices, vOffset);
		}
	}

	@Override
	public void render(@NotNull PoseStack matrices, int mouseX, int mouseY, float delta) {
		if (this.maxSelection == 0) {
			this.openNextLayerScreen();
			return;
		}
		super.render(matrices, mouseX, mouseY, delta);
	}
}
