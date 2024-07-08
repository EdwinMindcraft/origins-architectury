package io.github.edwinmindcraft.origins.api.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import io.github.edwinmindcraft.calio.api.network.OptionalFuncs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jetbrains.annotations.Nullable;

public record GuiTitle(@Nullable Component view, @Nullable Component choose) {
	public static final GuiTitle DEFAULT = new GuiTitle(null, null);

	public static final Codec<GuiTitle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ComponentSerialization.CODEC.optionalFieldOf("view_origin").forGetter(OptionalFuncs.opt(GuiTitle::view)),
			ComponentSerialization.CODEC.optionalFieldOf("choose_origin").forGetter(OptionalFuncs.opt(GuiTitle::choose))
	).apply(instance, OptionalFuncs.of(GuiTitle::new)));
}
