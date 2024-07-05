package io.github.edwinmindcraft.origins.api.data;

import com.google.gson.*;
import io.github.edwinmindcraft.origins.api.origin.GuiTitle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public record PartialGuiTitle(@Nullable String view,
							  @Nullable String choose) {

	@NotNull
	public GuiTitle create(@NotNull ResourceLocation identifier) {
		return new GuiTitle(
				this.view() != null ? Component.translatable(!this.view().isEmpty() ? this.view() : "layer.%s.%s.view_origin.name".formatted(identifier.getNamespace(), identifier.getPath())) : null,
				this.choose() != null ? Component.translatable(!this.choose().isEmpty() ? this.choose() : "layer.%s.%s.choose_origin.name".formatted(identifier.getNamespace(), identifier.getPath())) : null
		);
	}

	@Nullable
	@Contract("null, null -> null; null, _ -> param2; _, null -> param1")
	public static PartialGuiTitle merge(@Nullable PartialGuiTitle self, @Nullable PartialGuiTitle other) {
		if (other == null) return self;
		if (self == null) return other;
		return new PartialGuiTitle(
				other.view() != null ? other.view() : self.view(),
				other.choose() != null ? other.choose() : self.choose()
		);
	}

	public enum Serializer implements JsonSerializer<PartialGuiTitle>, JsonDeserializer<PartialGuiTitle> {
		INSTANCE;

		@Override
		public PartialGuiTitle deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if (!json.isJsonObject())
				return new PartialGuiTitle(null, null);
			JsonObject guiTitleObj = json.getAsJsonObject();
			String view = null;
			String choose = null;
			if (guiTitleObj.has("view_origin")) {
				view = GsonHelper.getAsString(guiTitleObj, "view_origin", "");
			}
			if (guiTitleObj.has("choose_origin")) {
				choose = GsonHelper.getAsString(guiTitleObj, "choose_origin", "");
			}
			return new PartialGuiTitle(view, choose);
		}

		@Override
		public JsonElement serialize(PartialGuiTitle src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject object = new JsonObject();
			if (src.view() != null) object.addProperty("view_origin", src.view());
			if (src.choose() != null) object.addProperty("choose_origin", src.choose());
			return object;
		}
	}
}
