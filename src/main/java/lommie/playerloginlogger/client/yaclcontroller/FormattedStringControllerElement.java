package lommie.playerloginlogger.client.yaclcontroller;

import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.string.IStringController;
import dev.isxander.yacl3.gui.controllers.string.StringControllerElement;
import lommie.playerloginlogger.client.ModMenuIntegration;
import lommie.playerloginlogger.client.PlayerloginloggerClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Objects;
import java.util.function.Supplier;

public class FormattedStringControllerElement extends StringControllerElement {
    Supplier<String> textColor;
    public FormattedStringControllerElement(IStringController<?> control, YACLScreen screen, Dimension<Integer> dim, boolean instantApply, Supplier<String> textColor) {
        super(control, screen, dim, instantApply);
        this.textColor = textColor;
    }

    @Override
    protected void extractValueText(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Component valueText = getValueText();
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
            valueText = Component.literal(valueText.getString()).setStyle(Objects.equals(valueText.getString(), textColor.get()) ? Style.EMPTY.withColor(TextColor.parseColor(textColor.get()).result().orElseGet(() -> TextColor.fromLegacyFormat(ChatFormatting.WHITE))):Style.EMPTY);
        }

        int textX = getDimension().xLimit() - textRenderer.width(valueText) + renderOffset - getXPadding();
        graphics.enableScissor(inputFieldBounds.x(), inputFieldBounds.y() - 2, inputFieldBounds.xLimit() + 1, inputFieldBounds.yLimit() + 4);
        graphics.text(textRenderer, valueText, textX, getTextY(), getValueColor(), true);

        if (isHovered()) {
            ticks += a;

            String text = getValueText().getString();

            graphics.fill(inputFieldBounds.x(), inputFieldBounds.yLimit(), inputFieldBounds.xLimit(), inputFieldBounds.yLimit() + 1, -1);
            graphics.fill(inputFieldBounds.x() + 1, inputFieldBounds.yLimit() + 1, inputFieldBounds.xLimit() + 1, inputFieldBounds.yLimit() + 2, 0xFF404040);

            if (inputFieldFocused || focused) {
                if (caretPos > text.length())
                    caretPos = text.length();

                int caretX = textX + textRenderer.width(text.substring(0, caretPos));
                if (text.isEmpty())
                    caretX = inputFieldBounds.x() + inputFieldBounds.width() / 2;

                if (selectionLength != 0) {
                    int selectionX = textX + textRenderer.width(text.substring(0, caretPos + selectionLength));
                    graphics.fill(caretX, inputFieldBounds.y() - 2, selectionX, inputFieldBounds.yLimit() - 1, 0x803030FF);
                }

                if(caretPos != previousCaretPos) {
                    previousCaretPos = caretPos;
                    caretTicks = 0;
                }

                if ((caretTicks += a) % 20 <= 10)
                    graphics.fill(caretX, inputFieldBounds.y() - 2, caretX + 1, inputFieldBounds.yLimit() - 1, -1);
            }
        }
        graphics.disableScissor();
    }
}
