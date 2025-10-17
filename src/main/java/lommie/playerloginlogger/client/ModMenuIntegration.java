package lommie.playerloginlogger.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class ModMenuIntegration implements ModMenuApi {
    public static String self_first_time_message = "";
    private @NotNull Boolean myBooleanOption = false;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent -> YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Used for narration. Could be used to render a title in the future."))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Name of the category"))
                        .tooltip(Text.literal("This text will appear as a tooltip when you hover or focus the button with Tab. There is no need to add \n to wrap as YACL will do it for you."))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Name of the group"))
                                .description(OptionDescription.of(Text.literal("This text will appear when you hover over the name or focus on the collapse button with Tab.")))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Boolean Option"))
                                        .description(OptionDescription.of(Text.literal("This text will appear as a tooltip when you hover over the option.")))
                                        .binding(true, () -> this.myBooleanOption, newVal -> this.myBooleanOption = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .build()
                .generateScreen(parent)/*YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Configuration for Player Login Logger"))
//                .screenInit((screen) -> {
//                    ModMenuIntegration.self_first_time_message = PlayerloginloggerClient.loadedConfig.self_first_time_message.text;
//                })
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Messages"))
                        .tooltip(Text.literal("No, this is not a tooltip"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("First join (self)"))
//                                .description(OptionDescription.createBuilder()
//                                        .text(Text.literal("Shown when you join a server for the first time"))
//                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Text.literal("Text"))
                                        .description(OptionDescription.createBuilder().build())
                                        .binding(
                                                "",//ModMenuIntegration.self_first_time_message,
                                                () -> ModMenuIntegration.self_first_time_message,
                                                (value) -> ModMenuIntegration.self_first_time_message = value
                                        )
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
//                .save(() -> {
//                    try {
//                        PlayerloginloggerClient.saveConfig(
//                                new PlayerloginloggerClient.MessageConfig(
//                                        new PlayerloginloggerClient.MessageConfig.MessageEntry(self_first_time_message,""),
//                                        null,
//                                        null,
//                                        null,
//                                        null,
//                                        '$'
//                                )
//                        );
//                        PlayerloginloggerClient.loadedConfig = PlayerloginloggerClient.loadConfig();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                })
                .build().generateScreen(parent)*/);
    }
}
