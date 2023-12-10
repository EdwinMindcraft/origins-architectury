package io.github.edwinmindcraft.origins.api.origin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.origins.data.OriginsDataTypes;
import io.github.apace100.origins.origin.Impact;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.registry.ApoliDynamicRegistries;
import io.github.edwinmindcraft.apoli.common.ApoliEventHandler;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import io.github.edwinmindcraft.calio.api.network.CodecSet;
import io.github.edwinmindcraft.calio.api.registry.ICalioDynamicRegistryManager;
import io.github.edwinmindcraft.origins.api.registry.OriginsBuiltinRegistries;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ObjectHolder;

import java.util.*;
import java.util.stream.Stream;

public class Origin {
    @ObjectHolder(value = "origins:empty", registryName = "origins:origins")
    public static final Origin EMPTY = new Origin(ImmutableSet.of(), ItemStack.EMPTY, true, -1, Impact.NONE, Component.literal(""), Component.literal(""), ImmutableSet.of(), true);

    private final List<HolderSet<ConfiguredPower<?, ?>>> powers;
    private final ItemStack icon;
    private final boolean unchoosable;
    private final int order;
    private final Impact impact;
    private final Component name;
    private final Component description;
    private final Set<OriginUpgrade> upgrades;
    private final boolean special;

    public static final Codec<Holder<Origin>> HOLDER_REFERENCE = CalioCodecHelper.holderRef(OriginsDynamicRegistries.ORIGINS_REGISTRY, SerializableDataTypes.IDENTIFIER);

    public static final Codec<Origin> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfiguredPower.CODEC_SET.set().fieldOf("powers").forGetter(Origin::getPowers),
            CalioCodecHelper.optionalField(ItemStack.CODEC, "icon", ItemStack.EMPTY).forGetter(Origin::getIcon),
            CalioCodecHelper.optionalField(CalioCodecHelper.BOOL, "unchoosable", false).forGetter(Origin::isUnchoosable),
            CalioCodecHelper.optionalField(CalioCodecHelper.INT, "order", Integer.MAX_VALUE).forGetter(Origin::getOrder),
            CalioCodecHelper.optionalField(OriginsDataTypes.IMPACT, "impact", Impact.NONE).forGetter(Origin::getImpact),
            CalioCodecHelper.COMPONENT_CODEC.fieldOf("name").forGetter(Origin::getName),
            CalioCodecHelper.COMPONENT_CODEC.fieldOf("description").forGetter(Origin::getDescription),
            CalioCodecHelper.setOf(OriginUpgrade.CODEC).fieldOf("upgrades").forGetter(Origin::getUpgrades),
            CalioCodecHelper.optionalField(CalioCodecHelper.BOOL, "special", false).forGetter(Origin::isSpecial)
    ).apply(instance, Origin::new));

    public static final CodecSet<Origin> CODEC_SET = CalioCodecHelper.forDynamicRegistry(OriginsDynamicRegistries.ORIGINS_REGISTRY, SerializableDataTypes.IDENTIFIER, CODEC);

    public static MapCodec<Holder<Origin>> optional(String name) {
        return CalioCodecHelper.registryDefaultedField(HOLDER_REFERENCE, name, OriginsDynamicRegistries.ORIGINS_REGISTRY, OriginsBuiltinRegistries.ORIGINS);
    }

    public Origin(Collection<HolderSet<ConfiguredPower<?, ?>>> powers, ItemStack icon, boolean unchoosable, int order, Impact impact,
                  Component name, Component description, Set<OriginUpgrade> upgrades, boolean special) {
        this.powers = ImmutableList.copyOf(powers);
        this.icon = icon;
        this.unchoosable = unchoosable;
        this.order = order;
        this.impact = impact;
        this.name = name;
        this.description = description;
        this.upgrades = ImmutableSet.copyOf(upgrades);
        this.special = special;
    }

    public Origin(Collection<HolderSet<ConfiguredPower<?, ?>>> powers, ItemStack icon, boolean unchoosable, int order, Impact impact, Component name, Component description, Set<OriginUpgrade> upgrades) {
        this(powers, icon, unchoosable, order, impact, name, description, upgrades, false);
    }

    public Origin cleanup(ICalioDynamicRegistryManager manager) {
        Registry<ConfiguredPower<?, ?>> powers = manager.get(ApoliDynamicRegistries.CONFIGURED_POWER_KEY);
        ImmutableList.Builder<Holder<ConfiguredPower<?, ?>>> direct = ImmutableList.builder();
        ImmutableList.Builder<HolderSet<ConfiguredPower<?, ?>>> sets = ImmutableList.builder();
        for (HolderSet<ConfiguredPower<?, ?>> holderSet : this.getPowers()) {
            Either<TagKey<ConfiguredPower<?, ?>>, List<Holder<ConfiguredPower<?, ?>>>> unwrap = holderSet.unwrap();
            unwrap.ifLeft(tagKey -> powers.getTag(tagKey).ifPresent(x -> sets.add(holderSet)));
            unwrap.ifRight(holders -> {
                for (Holder<ConfiguredPower<?, ?>> holder : holders) {
                    if (holder.isBound())
                        direct.add(holder);
                }
            });
        }
        ImmutableList<Holder<ConfiguredPower<?, ?>>> build = direct.build();
        if (build.size() > 0)
            sets.add(HolderSet.direct(build));
        return new Origin(
                sets.build(),
                this.getIcon(),
                this.isUnchoosable(),
                this.getOrder(),
                this.getImpact(),
                this.getName(),
                this.getDescription(),
                this.getUpgrades(),
                this.isSpecial());
    }

    public List<HolderSet<ConfiguredPower<?, ?>>> getPowers() {
        return this.powers;
    }

    public int getPowerAmount() {
        return (int) this.powers.stream().flatMap(HolderSet::stream).count();
    }

    public Stream<Holder<ConfiguredPower<?, ?>>> getValidPowers() {
        return this.powers.stream().flatMap(HolderSet::stream);
    }

    public ItemStack getIcon() {
        return this.icon;
    }

    public boolean isUnchoosable() {
        return this.unchoosable;
    }

    public int getOrder() {
        return this.order;
    }

    public Impact getImpact() {
        return this.impact;
    }

    public Component getName() {
        return this.name;
    }

    public Component getDescription() {
        return this.description;
    }

    public Set<OriginUpgrade> getUpgrades() {
        return this.upgrades;
    }

    public boolean isChoosable() {
        return !this.isUnchoosable();
    }

    public boolean isSpecial() {
        return this.special;
    }

    public Optional<OriginUpgrade> findUpgrade(ResourceLocation advancement) {
        return this.getUpgrades().stream().filter(x -> Objects.equals(x.advancement(), advancement)).findFirst();
    }

    @Override
    public String toString() {
        ResourceLocation name = CalioAPI.getDynamicRegistries()
                .getOrEmpty(OriginsDynamicRegistries.ORIGINS_REGISTRY)
                .flatMap(x -> x.getResourceKey(this))
                .map(ResourceKey::location).orElse(null);
        StringBuilder builder = new StringBuilder("Origin(").append(name).append(")[");
        boolean first = true;
        for (Holder<ConfiguredPower<?, ?>> power : this.getValidPowers().toList()) {
            if (!power.isBound())
                continue;
            if (first)
                first = false;
            else
                builder.append(',');
            builder.append(power);
        }
        return builder.append(']').toString();
    }
}
