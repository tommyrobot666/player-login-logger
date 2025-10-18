package lommie.playerloginlogger.client.yaclcontroller;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

public interface FormattedStringControllerBuilder extends ControllerBuilder<String> {
    static FormattedStringControllerBuilder create(Option<String> option) {
        return new FormattedStringControllerBuilderImpl(option);
    }
}