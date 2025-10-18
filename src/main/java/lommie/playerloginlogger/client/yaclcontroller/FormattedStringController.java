package lommie.playerloginlogger.client.yaclcontroller;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.string.StringController;

import java.util.function.Supplier;

public class FormattedStringController extends StringController {
    Supplier<String> textColor;
    public FormattedStringController(Option<String> option, Supplier<String> textColor) {
        super(option);
        this.textColor = textColor;
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new FormattedStringControllerElement(this, screen, widgetDimension, true,textColor);
    }
}
