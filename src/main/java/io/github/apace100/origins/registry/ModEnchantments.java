package io.github.apace100.origins.registry;

import io.github.apace100.origins.enchantment.WaterProtectionEnchantment;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import java.util.function.Supplier;

public class ModEnchantments {

	public static final Supplier<Enchantment> WATER_PROTECTION = OriginRegisters.ENCHANTMENTS.register("water_protection", () -> new WaterProtectionEnchantment(Enchantment.Rarity.RARE, EnchantmentCategory.ARMOR, new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}));

	public static void register() {}
}
