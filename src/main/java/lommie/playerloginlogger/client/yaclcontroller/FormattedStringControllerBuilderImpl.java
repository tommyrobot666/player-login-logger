package lommie.playerloginlogger.client.yaclcontroller;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.impl.controller.AbstractControllerBuilderImpl;

import java.util.function.Supplier;

public class FormattedStringControllerBuilderImpl extends AbstractControllerBuilderImpl<String> implements FormattedStringControllerBuilder {
    Supplier<String> textColor;
    public FormattedStringControllerBuilderImpl(Option<String> option, Supplier<String> textColor) {
        super(option);
        this.textColor = textColor;
    }

    @Override
    public Controller<String> build() {
        return new FormattedStringController(this.option, this.textColor);
    }
}
