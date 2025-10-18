package lommie.playerloginlogger.client.yaclcontroller;

import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.string.IStringController;
import dev.isxander.yacl3.gui.controllers.string.StringControllerElement;
import lommie.playerloginlogger.client.ModMenuIntegration;
import lommie.playerloginlogger.client.PlayerloginloggerClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.Objects;
import java.util.function.Supplier;

public class FormattedStringControllerElement extends StringControllerElement {
    Supplier<String> textColor;
    public FormattedStringControllerElement(IStringController<?> control, YACLScreen screen, Dimension<Integer> dim, boolean instantApply, Supplier<String> textColor) {
        super(control, screen, dim, instantApply);
        this.textColor = textColor;
    }

    @Override
    protected void drawValueText(DrawContext graphics, int mouseX, int mouseY, float delta) {
        // modified code
        Text valueText = this.getValueText();
        if (!this.isHovered()) {
            valueText = PlayerloginloggerClient.addFormatting(valueText.getString(),textColor.get(), ModMenuIntegration.formatting_prefix);
            /*int maxLen = GuiUtils.shortenString(valueText.getString(), this.textRenderer, this.getMaxUnwrapLength(),"").length();
            int currentLen = 0;
            MutableText newText = Text.empty().setStyle(valueText.getStyle());
            for (int i = 0; i < valueText.getSiblings().size(); i++) {
                Text siblingOfValueText = valueText.getSiblings().get(i);
                if (currentLen+siblingOfValueText.getString().length() < maxLen){
                    newText.append(siblingOfValueText);
                    currentLen += siblingOfValueText.getString().length();
                } else {
                    MutableText splitSiblingOfValueText = Text.literal(siblingOfValueText.getString().substring(0,maxLen-currentLen)).setStyle(siblingOfValueText.getStyle());
                    newText.append(splitSiblingOfValueText);
                    break;
                }
            }
            newText.append(Text.literal("..."));
            valueText = newText;*/
        } else {
            valueText = Text.literal(valueText.getString()).setStyle(Objects.equals(valueText.getString(), textColor.get()) ?Style.EMPTY.withColor(TextColor.parse(textColor.get()).result().orElseGet(() -> TextColor.fromFormatting(Formatting.WHITE))):Style.EMPTY);
        }

        // random junk
        int textX = (Integer)this.getDimension().xLimit() - this.textRenderer.getWidth((StringVisitable)valueText) + this.renderOffset - this.getXPadding();
        graphics.enableScissor((Integer)this.inputFieldBounds.x(), (Integer)this.inputFieldBounds.y() - 2, (Integer)this.inputFieldBounds.xLimit() + 1, (Integer)this.inputFieldBounds.yLimit() + 4);
        graphics.drawText(this.textRenderer, (Text)valueText, textX, this.getTextY(), this.getValueColor(), true);
        if (this.isHovered()) {
            this.ticks += delta;
            String text = this.getValueText().getString();
            graphics.fill((Integer)this.inputFieldBounds.x(), (Integer)this.inputFieldBounds.yLimit(), (Integer)this.inputFieldBounds.xLimit(), (Integer)this.inputFieldBounds.yLimit() + 1, -1);
            graphics.fill((Integer)this.inputFieldBounds.x() + 1, (Integer)this.inputFieldBounds.yLimit() + 1, (Integer)this.inputFieldBounds.xLimit() + 1, (Integer)this.inputFieldBounds.yLimit() + 2, -12566464);
            if (this.inputFieldFocused || this.focused) {
                if (this.caretPos > text.length()) {
                    this.caretPos = text.length();
                }

                int caretX = textX + this.textRenderer.getWidth(text.substring(0, this.caretPos));
                if (text.isEmpty()) {
                    caretX = (Integer)this.inputFieldBounds.x() + (Integer)this.inputFieldBounds.width() / 2;
                }

                if (this.selectionLength != 0) {
                    int selectionX = textX + this.textRenderer.getWidth(text.substring(0, this.caretPos + this.selectionLength));
                    graphics.fill(caretX, (Integer)this.inputFieldBounds.y() - 2, selectionX, (Integer)this.inputFieldBounds.yLimit() - 1, -2144325377);
                }

                if (this.caretPos != this.previousCaretPos) {
                    this.previousCaretPos = this.caretPos;
                    this.caretTicks = 0.0F;
                }

                if ((this.caretTicks += delta) % 20.0F <= 10.0F) {
                    graphics.fill(caretX, (Integer)this.inputFieldBounds.y() - 2, caretX + 1, (Integer)this.inputFieldBounds.yLimit() - 1, -1);
                }
            }
        }

        graphics.disableScissor();
    }
}
