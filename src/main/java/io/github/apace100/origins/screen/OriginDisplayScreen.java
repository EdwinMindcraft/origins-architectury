package io.github.apace100.origins.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.badge.Badge;
import io.github.apace100.origins.badge.BadgeManager;
import io.github.apace100.origins.mixin.ScreenAccessor;
import io.github.apace100.origins.origin.Impact;
import io.github.apace100.origins.util.PowerKeyManager;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Registry;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class OriginDisplayScreen extends Screen {

	private static final ResourceLocation WINDOW = new ResourceLocation(Origins.MODID, "textures/gui/choose_origin.png");

	private Origin origin;
	private OriginLayer layer;
	private boolean isOriginRandom;
	private Component randomOriginText;

	protected static final int windowWidth = 176;
	protected static final int windowHeight = 182;
	protected int scrollPos = 0;
	private int currentMaxScroll = 0;
    private float time = 0;

	protected int guiTop, guiLeft;

	protected final boolean showDirtBackground;

    private final LinkedList<RenderedBadge> renderedBadges = new LinkedList<>();

	public OriginDisplayScreen(Component title, boolean showDirtBackground) {
		super(title);
		this.showDirtBackground = showDirtBackground;
	}

	public void showOrigin(Origin origin, OriginLayer layer, boolean isRandom) {
		this.origin = origin;
		this.layer = layer;
		this.isOriginRandom = isRandom;
		this.scrollPos = 0;
        this.time = 0;
	}

	public void setRandomOriginText(Component text) {
		this.randomOriginText = text;
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (this.width - windowWidth) / 2;
		this.guiTop = (this.height - windowHeight) / 2;
	}

	public Origin getCurrentOrigin() {
		return this.origin;
	}

	public OriginLayer getCurrentLayer() {
		return this.layer;
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
		this.renderedBadges.clear();
        this.time += delta;
		this.renderBackground(matrices);
		this.renderOriginWindow(matrices, mouseX, mouseY);
		super.render(matrices, mouseX, mouseY, delta);
		if (this.origin != null) {
			this.renderScrollbar(matrices, mouseX, mouseY);
			this.renderBadgeTooltip(matrices, mouseX, mouseY);
		}
	}

	private void renderScrollbar(PoseStack matrices, int mouseX, int mouseY) {
		if (!this.canScroll()) {
			return;
		}
		RenderSystem.setShaderTexture(0, WINDOW);
		this.blit(matrices, this.guiLeft + 155, this.guiTop + 35, 188, 24, 8, 134);
		int scrollbarY = 36;
		int maxScrollbarOffset = 141;
		int u = 176;
		float part = this.scrollPos / (float) this.currentMaxScroll;
		scrollbarY += (maxScrollbarOffset - scrollbarY) * part;
		if (this.scrolling) {
			u += 6;
		} else if (mouseX >= this.guiLeft + 156 && mouseX < this.guiLeft + 156 + 6) {
			if (mouseY >= this.guiTop + scrollbarY && mouseY < this.guiTop + scrollbarY + 27) {
				u += 6;
			}
		}
		this.blit(matrices, this.guiLeft + 156, this.guiTop + scrollbarY, u, 24, 6, 27);
	}

	private boolean scrolling = false;
	private int scrollDragStart = 0;
	private double mouseDragStart = 0;

	private boolean canScroll() {
		return this.origin != null && this.currentMaxScroll > 0;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		this.scrolling = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.canScroll()) {
			this.scrolling = false;
			int scrollbarY = 36;
			int maxScrollbarOffset = 141;
			float part = this.scrollPos / (float) this.currentMaxScroll;
			scrollbarY += (maxScrollbarOffset - scrollbarY) * part;
			if (mouseX >= this.guiLeft + 156 && mouseX < this.guiLeft + 156 + 6) {
				if (mouseY >= this.guiTop + scrollbarY && mouseY < this.guiTop + scrollbarY + 27) {
					this.scrolling = true;
					this.scrollDragStart = scrollbarY;
					this.mouseDragStart = mouseY;
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.scrolling) {
			int delta = (int) (mouseY - this.mouseDragStart);
			int newScrollPos = Math.max(36, Math.min(141, this.scrollDragStart + delta));
			float part = (newScrollPos - 36) / (float) (141 - 36);
			this.scrollPos = (int) (part * this.currentMaxScroll);
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

    private void renderBadgeTooltip(PoseStack matrices, int mouseX, int mouseY) {
        for(RenderedBadge rb : this.renderedBadges) {
            if(mouseX >= rb.x &&
               mouseX < rb.x + 9 &&
               mouseY >= rb.y &&
               mouseY < rb.y + 9 &&
               rb.hasTooltip()) {
                int widthLimit = this.width - mouseX - 24;
                ((ScreenAccessor)this).invokeRenderTooltipFromComponents(matrices, rb.getTooltipComponents(this.font, widthLimit), mouseX, mouseY);
            }
        }
    }

	protected Component getTitleText() {
		return new TextComponent("Origins");
	}

	private void renderOriginWindow(PoseStack matrices, int mouseX, int mouseY) {
		RenderSystem.enableBlend();
		this.renderWindowBackground(matrices, 16, 0);
		if (this.origin != null) {
			this.renderOriginContent(matrices, mouseX, mouseY);
		}
		RenderSystem.setShaderTexture(0, WINDOW);
		this.blit(matrices, this.guiLeft, this.guiTop, 0, 0, windowWidth, windowHeight);
		if (this.origin != null) {
			this.renderOriginName(matrices);
			RenderSystem.setShaderTexture(0, WINDOW);
			this.renderOriginImpact(matrices, mouseX, mouseY);
			Component title = this.getTitleText();
			drawCenteredString(matrices, this.font, title.getString(), this.width / 2, this.guiTop - 15, 0xFFFFFF);
		}
		RenderSystem.disableBlend();
	}

	private void renderOriginImpact(PoseStack matrices, int mouseX, int mouseY) {
		Impact impact = this.getCurrentOrigin().getImpact();
		int impactValue = impact.getImpactValue();
		int wOffset = impactValue * 8;
		for (int i = 0; i < 3; i++) {
			if (i < impactValue) {
				this.blit(matrices, this.guiLeft + 128 + i * 10, this.guiTop + 19, windowWidth + wOffset, 16, 8, 8);
			} else {
				this.blit(matrices, this.guiLeft + 128 + i * 10, this.guiTop + 19, windowWidth, 16, 8, 8);
			}
		}
		if (mouseX >= this.guiLeft + 128 && mouseX <= this.guiLeft + 158
			&& mouseY >= this.guiTop + 19 && mouseY <= this.guiTop + 27) {
			Component ttc = new TranslatableComponent(Origins.MODID + ".gui.impact.impact").append(": ").append(impact.getTextComponent());
			this.renderTooltip(matrices, ttc, mouseX, mouseY);
		}
	}

	private void renderOriginName(PoseStack matrices) {
		FormattedText originName = this.font.substrByWidth(this.getCurrentOrigin().getName(), windowWidth - 36);
		drawString(matrices, this.font, originName.getString(), this.guiLeft + 39, this.guiTop + 19, 0xFFFFFF);
		ItemStack is = this.getCurrentOrigin().getIcon();
		this.itemRenderer.renderGuiItem(is, this.guiLeft + 15, this.guiTop + 15);
	}

	private void renderWindowBackground(PoseStack matrices, int offsetYStart, int offsetYEnd) {
		int border = 13;
		int endX = this.guiLeft + windowWidth - border;
		int endY = this.guiTop + windowHeight - border;
		RenderSystem.setShaderTexture(0, WINDOW);
		for (int x = this.guiLeft; x < endX; x += 16) {
			for (int y = this.guiTop + offsetYStart; y < endY + offsetYEnd; y += 16) {
				this.blit(matrices, x, y, windowWidth, 0, Math.max(16, endX - x), Math.max(16, endY + offsetYEnd - y));
			}
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double z) {
		boolean retValue = super.mouseScrolled(x, y, z);
		int np = this.scrollPos - (int) z * 4;
		this.scrollPos = np < 0 ? 0 : Math.min(np, this.currentMaxScroll);
		return retValue;
	}

	private void renderOriginContent(PoseStack matrices, int mouseX, int mouseY) {

		int textWidth = windowWidth - 48;
		// Without this code, the text may not cover the whole width of the window
		// if the scrollbar isn't shown. However, with this code, you'll see 1 frame
		// of misaligned text because the text length (and whether scrolling is enabled)
		// is only evaluated on first render. :(
        /*if(!canScroll()) {
            textWidth += 12;
        }*/

		Origin origin = this.getCurrentOrigin();
		int x = this.guiLeft + 18;
		int y = this.guiTop + 50;
		int startY = y;
		int endY = y - 72 + windowHeight;
		y -= this.scrollPos;

		Component orgDesc = origin.getDescription();
		List<FormattedCharSequence> descLines = this.font.split(orgDesc, textWidth);
		for (FormattedCharSequence line : descLines) {
			if (y >= startY - 18 && y <= endY + 12) {
				this.font.draw(matrices, line, x + 2, y - 6, 0xCCCCCC);
			}
			y += 12;
		}

		if (this.isOriginRandom) {
			List<FormattedCharSequence> drawLines = this.font.split(this.randomOriginText, textWidth);
			for (FormattedCharSequence line : drawLines) {
				y += 12;
				if (y >= startY - 24 && y <= endY + 12) {
					this.font.draw(matrices, line, x + 2, y, 0xCCCCCC);
				}
			}
			y += 14;
		} else {
			Registry<ConfiguredPower<?, ?>> powers = ApoliAPI.getPowers();
			for (ResourceLocation id : origin.getPowers()) {
				ConfiguredPower<?, ?> p = powers.get(id);
				if (p == null || p.getData().hidden()) {
					continue;
				}
				FormattedCharSequence name = Language.getInstance().getVisualOrder(this.font.substrByWidth(p.getData().getName().withStyle(ChatFormatting.UNDERLINE), textWidth));
				Component desc = p.getData().getDescription();
				List<FormattedCharSequence> drawLines = this.font.split(desc, textWidth);
				if (y >= startY - 24 && y <= endY + 12) {
					this.font.draw(matrices, name, x, y, 0xFFFFFF);
					int tw = this.font.width(name);
					Collection<Badge> badges = BadgeManager.getPowerBadges(id);
					int xStart = x + tw + 4;
					int bi = 0;
                    for(Badge badge : badges) {
                        RenderedBadge renderedBadge = new RenderedBadge(p, badge,xStart + 10 * bi, y - 1);
                        this.renderedBadges.add(renderedBadge);
                        RenderSystem.setShaderTexture(0, badge.spriteId());
                        blit(matrices, xStart + 10 * bi, y - 1, 0, 0, 9, 9, 9, 9);
                        bi++;
                    }
				}
				for (FormattedCharSequence line : drawLines) {
					y += 12;
					if (y >= startY - 24 && y <= endY + 12) {
						this.font.draw(matrices, line, x + 2, y, 0xCCCCCC);
					}
				}

				y += 14;
			}
		}
		y += this.scrollPos;
		this.currentMaxScroll = y - 14 - (this.guiTop + 158);
		if (this.currentMaxScroll < 0) {
			this.currentMaxScroll = 0;
		}
	}

    private class RenderedBadge {
        private final ConfiguredPower<?, ?> powerType;
        private final Badge badge;
        private final int x;
        private final int y;

        public RenderedBadge(ConfiguredPower<?, ?> powerType, Badge badge, int x, int y) {
            this.powerType = powerType;
            this.badge = badge;
            this.x = x;
            this.y = y;
        }

        public boolean hasTooltip() {
            return this.badge.hasTooltip();
        }

        public List<ClientTooltipComponent> getTooltipComponents(Font textRenderer, int widthLimit) {
            return this.badge.getTooltipComponents(this.powerType, widthLimit, OriginDisplayScreen.this.time, textRenderer);
        }

    }

}
