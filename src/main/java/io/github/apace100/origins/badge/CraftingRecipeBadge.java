package io.github.apace100.origins.badge;

import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.screen.tooltip.CraftingRecipeTooltipComponent;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

public record CraftingRecipeBadge(ResourceLocation spriteId,
								  RecipeHolder<Recipe<CraftingInput>> recipe,
								  @Nullable Component prefix,
								  @Nullable Component suffix) implements Badge {

	public CraftingRecipeBadge(SerializableData.Instance instance) {
		this(instance.getId("sprite"),
				instance.get("recipe"),
				instance.get("prefix"),
				instance.get("suffix"));
	}

	@Override
	public boolean hasTooltip() {
		return true;
	}

	public NonNullList<ItemStack> peekInputs(float time) {
		int seed = Mth.floor(time / 30);
		NonNullList<ItemStack> inputs = NonNullList.withSize(9, ItemStack.EMPTY);
		List<Ingredient> ingredients = this.recipe.value().getIngredients();
		for (int index = 0; index < ingredients.size(); ++index) {
			ItemStack[] stacks = ingredients.get(index).getItems();
			if (stacks.length > 0) inputs.set(index, stacks[seed % stacks.length].copy());
		}
		return inputs;
	}

	@Override
	public List<ClientTooltipComponent> getTooltipComponents(ConfiguredPower<?, ?> powerType, int widthLimit, float time, Font textRenderer) {
		List<ClientTooltipComponent> tooltips = new LinkedList<>();
        if (Minecraft.getInstance().level == null) {
            Origins.LOGGER.warn("Could not construct crafting recipe badge, because world was null");
            return tooltips;
        }
		if (Minecraft.getInstance().options.advancedItemTooltips) {
			Component recipeIdText = Component.literal(this.recipe.id().toString()).withStyle(ChatFormatting.DARK_GRAY);
			widthLimit = Math.max(130, textRenderer.width(recipeIdText));
			if (this.prefix != null) TooltipBadge.addLines(tooltips, this.prefix, textRenderer, widthLimit);
			tooltips.add(new CraftingRecipeTooltipComponent(this.peekInputs(time), this.recipe.value().getResultItem(Minecraft.getInstance().level.registryAccess()).copy()));
			if (this.suffix != null) TooltipBadge.addLines(tooltips, this.suffix, textRenderer, widthLimit);
			TooltipBadge.addLines(tooltips, recipeIdText, textRenderer, widthLimit);
		} else {
			widthLimit = 130;
			if (this.prefix != null) TooltipBadge.addLines(tooltips, this.prefix, textRenderer, widthLimit);
			tooltips.add(new CraftingRecipeTooltipComponent(this.peekInputs(time), this.recipe.value().getResultItem(Minecraft.getInstance().level.registryAccess()).copy()));
			if (this.suffix != null) TooltipBadge.addLines(tooltips, this.suffix, textRenderer, widthLimit);
		}
		return tooltips;
	}

	@Override
	public SerializableData.Instance toData(SerializableData.Instance instance) {
		instance.set("sprite", this.spriteId);
		instance.set("recipe", this.recipe);
		instance.set("prefix", this.prefix);
		instance.set("suffix", this.suffix);
		return instance;
	}

	@Override
	public BadgeFactory getBadgeFactory() {
		return BadgeFactories.CRAFTING_RECIPE.get();
	}
}
