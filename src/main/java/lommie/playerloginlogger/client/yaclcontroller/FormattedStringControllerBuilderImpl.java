package lommie.playerloginlogger.client.yaclcontroller;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.impl.controller.AbstractControllerBuilderImpl;

public class FormattedStringControllerBuilderImpl extends AbstractControllerBuilderImpl<String> implements FormattedStringControllerBuilder {
    public FormattedStringControllerBuilderImpl(Option<String> option) {
        super(option);
    }

    @Override
    public Controller<String> build() {
        return new FormattedStringController(this.option);
    }
}
