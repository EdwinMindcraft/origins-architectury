package io.github.apace100.origins.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

public class LayerArgumentType implements ArgumentType<ResourceLocation> {
   public static final DynamicCommandExceptionType LAYER_NOT_FOUND = new DynamicCommandExceptionType((p_208663_0_) -> new TranslatableComponent("commands.origin.layer_not_found", p_208663_0_));

   public static LayerArgumentType layer() {
      return new LayerArgumentType();
   }

   public ResourceLocation parse(StringReader p_parse_1_) throws CommandSyntaxException {
      return ResourceLocation.read(p_parse_1_);
   }

   public static OriginLayer getLayer(CommandContext<CommandSourceStack> context, String argumentName) throws CommandSyntaxException {
      ResourceLocation id = context.getArgument(argumentName, ResourceLocation.class);
      try {
         return OriginLayers.getLayer(id);
      } catch(IllegalArgumentException e) {
         throw LAYER_NOT_FOUND.create(id);
      }
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggestResource(OriginLayers.getLayers().stream().map(OriginLayer::getIdentifier), builder);
   }
}