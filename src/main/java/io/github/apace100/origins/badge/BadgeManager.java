package io.github.apace100.origins.badge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonElement;
import io.github.apace100.apoli.integration.PowerLoadEvent;
import io.github.apace100.calio.registry.DataObjectRegistry;
import io.github.apace100.origins.Origins;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.configuration.FieldConfiguration;
import io.github.edwinmindcraft.apoli.api.power.IActivePower;
import io.github.edwinmindcraft.apoli.api.power.ITogglePower;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.apoli.common.power.RecipePower;
import io.github.edwinmindcraft.calio.api.event.CalioDynamicRegistryEvent;
import io.github.edwinmindcraft.origins.api.event.AutoBadgeEvent;
import io.github.edwinmindcraft.origins.api.registry.OriginsBuiltinRegistries;
import io.github.edwinmindcraft.origins.common.network.S2CSynchronizeBadges;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedList;
import java.util.List;

public final class BadgeManager {
	public static final DataObjectRegistry<Badge> REGISTRY = new DataObjectRegistry.Builder<>(Origins.identifier("badge"), Badge.class)
			.readFromData("badges", true)
			.dataErrorHandler((id, exception) -> Origins.LOGGER.error("Failed to read badge " + id + ", caused by", exception))
			.defaultFactory(BadgeFactories.KEYBIND)
			.buildAndRegister();
	private static final Multimap<ResourceLocation, Badge> BADGES = LinkedListMultimap.create();

	private static final ResourceLocation TOGGLE_BADGE_SPRITE = Origins.identifier("textures/gui/badge/toggle.png");
	private static final ResourceLocation ACTIVE_BADGE_SPRITE = Origins.identifier("textures/gui/badge/active.png");
	private static final ResourceLocation RECIPE_BADGE_SPRITE = Origins.identifier("textures/gui/badge/recipe.png");

	private static final ResourceLocation TOGGLE_BADGE_ID = Origins.identifier("toggle");
	private static final ResourceLocation ACTIVE_BADGE_ID = Origins.identifier("active");

	public static void init() {
		//Mark factories as forge registry backed.
		REGISTRY.setRegistryAccess(OriginsBuiltinRegistries.BADGE_FACTORIES);
		//register callbacks
		NeoForge.EVENT_BUS.addListener((CalioDynamicRegistryEvent.Reload event) -> BadgeManager.clear());
		NeoForge.EVENT_BUS.addListener(BadgeManager::createAutoBadges);
		//These events are fired on a non-standard priority to allow for better integration.
		//Custom badges on HIGH, which means NORMAL will have the custom badges loaded.
		//Auto badges on LOW which means that it'll only fire after NORMAL events perform their actions.
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, BadgeManager::readCustomBadges);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOW, BadgeManager::readAutoBadges);
		ApoliAPI.addAdditionalDataField("badges");
	}

	public static void register(BadgeFactory factory) {
		REGISTRY.registerFactory(factory.id(), factory);
	}

	public static void putPowerBadge(ResourceLocation powerId, Badge badge) {
		BADGES.put(powerId, badge);
	}

	public static List<Badge> getPowerBadges(ResourceLocation powerId) {
		return ImmutableList.copyOf(BADGES.get(powerId));
	}

	public static void clear() {
		BADGES.clear();
	}

	public static S2CSynchronizeBadges createPacket() {
		return new S2CSynchronizeBadges(Multimaps.unmodifiableMultimap(BADGES));
	}

	public static void sync(ServerPlayer player) {
		REGISTRY.sync(player);
		PacketDistributor.sendToPlayer(player, createPacket());
	}

	public static void readCustomBadges(PowerLoadEvent.Post event) {
		if (event.getAdditionalData("badges").isJsonNull())
			return; //No data, clear errors.
		ConfiguredPower<?, ?> powerType = event.getPower();
		ResourceLocation powerId = event.getId();
		JsonElement data = event.getAdditionalData("badges");
		if (!(powerType.getData().hidden())) { //Forge: Subpowers are hidden by default.
			if (data.isJsonArray()) {
				for (JsonElement badgeJson : data.getAsJsonArray()) {
					if (badgeJson.isJsonPrimitive()) {
						ResourceLocation badgeId = ResourceLocation.tryParse(badgeJson.getAsString());
						if (badgeId != null) {
							Badge badge = REGISTRY.get(badgeId);
							if (badge != null) {
								putPowerBadge(powerId, badge);
							} else {
								Origins.LOGGER.error("\"badges\" field in power \"{}\" is referring to an undefined badge \"{}\"!", powerId, badgeId);
							}
						} else {
							Origins.LOGGER.error("\"badges\" field in power \"{}\" is not a valid identifier!", powerId);
						}
					} else if (badgeJson.isJsonObject()) {
						try {
							putPowerBadge(powerId, REGISTRY.readDataObject(badgeJson));
						} catch (Exception exception) {
							Origins.LOGGER.error("\"badges\" field in power \"{}\" contained an JSON object entry that cannot be resolved!", powerId, exception);
						}
					} else {
						Origins.LOGGER.error("\"badges\" field in power \"{}\" contained an entry that was a JSON array, which is not allowed!", powerId);
					}
				}
			} else {
				Origins.LOGGER.error("\"badges\" field in power \"{}\" should be an array.", powerId);
			}
		}
	}

	public static void readAutoBadges(PowerLoadEvent.Post event) {
		ConfiguredPower<?, ?> powerType = event.getPower();
		ResourceLocation powerId = event.getId();
		if (BADGES.containsKey(powerId) || powerType.getData().hidden()) {
			//FORGE: sub-powers are hidden no matter what.
			// No auto-badges should be created if:
			// - The power has custom badges defined in the data
			// - The power is hidden
			// - The power is a sub-power
			return;
		}
		List<Badge> badgeList = new LinkedList<>();
		for (Holder<ConfiguredPower<?, ?>> value : powerType.getContainedPowers().values()) {
			if (value.isBound())
				NeoForge.EVENT_BUS.post(new AutoBadgeEvent(powerId, value.value(), badgeList));
		}
		NeoForge.EVENT_BUS.post(new AutoBadgeEvent(powerId, powerType, badgeList));
		for (Badge badge : badgeList)
			putPowerBadge(powerId, badge);
	}

	@SuppressWarnings("unchecked")
	public static void createAutoBadges(AutoBadgeEvent event) {
		PowerFactory<?> factory = event.getPower().getFactory();
		if (factory instanceof IActivePower<?>) {
			boolean toggle = factory instanceof ITogglePower<?>;
			ResourceLocation autoBadgeId = toggle ? TOGGLE_BADGE_ID : ACTIVE_BADGE_ID;
			if (REGISTRY.containsId(autoBadgeId)) {
				event.getBadges().add(REGISTRY.get(autoBadgeId));
			} else {
				event.getBadges().add(new KeybindBadge(toggle ? TOGGLE_BADGE_SPRITE : ACTIVE_BADGE_SPRITE,
						toggle ? "origins.gui.badge.toggle"
								: "origins.gui.badge.active"
				));
			}
		} else if (factory instanceof RecipePower) {
			FieldConfiguration<RecipeHolder<Recipe<CraftingInput>>> config = (FieldConfiguration<RecipeHolder<Recipe<CraftingInput>>>) event.getPower().getConfiguration();
			RecipeHolder<Recipe<CraftingInput>> recipe = config.value();
			String type = recipe.value() instanceof ShapedRecipe ? "shaped" : "shapeless";
			event.getBadges().add(new CraftingRecipeBadge(RECIPE_BADGE_SPRITE, recipe,
					Component.translatable("origins.gui.badge.recipe.crafting." + type), null
			));
		}
	}
}
