package lommie.playerloginlogger.client.yaclcontroller;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.string.IStringController;

import java.util.function.Supplier;

public record FormattedStringController(Option<String> option, Supplier<String> textColor) implements IStringController<String> {

    @Override
    public String getString() {
        return option().pendingValue();
    }

    @Override
    public void setFromString(String value) {
        option().requestSet(value);
    }

    @Override
    public Option<String> option() {
        return option;
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new FormattedStringControllerElement(this, screen, widgetDimension, true,textColor);
    }
}
