package lommie.playerloginlogger.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.text.Text;

import java.io.IOException;

public class ModMenuIntegration implements ModMenuApi {
    public static String self_first_time_message = "";
    public static String self_first_time_message_color = "";
    public static String self_welcome_back_message = "";
    public static String self_welcome_back_message_color = "";

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent -> YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Configuration for Player Login Logger"))
                .screenInit((screen) -> {
                    PlayerloginloggerClient.getConfigOrLoad();
                    ModMenuIntegration.self_first_time_message = PlayerloginloggerClient.loadedConfig.self_first_time_message.text;
                    ModMenuIntegration.self_first_time_message_color = PlayerloginloggerClient.loadedConfig.self_first_time_message.textColor;
                    ModMenuIntegration.self_welcome_back_message = PlayerloginloggerClient.loadedConfig.self_welcome_back_message.text;
                    ModMenuIntegration.self_welcome_back_message_color = PlayerloginloggerClient.loadedConfig.self_welcome_back_message.textColor;
                })
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Messages"))
                        .tooltip(Text.literal("No, this is not a tooltip"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("First join (self)"))
                                .description(OptionDescription.createBuilder()
                                        .text(Text.literal("Shown when you join a server for the first time"))
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Text.literal("Text"))
                                        .description(OptionDescription.createBuilder().build())
                                        .binding(
                                                ModMenuIntegration.self_first_time_message,
                                                () -> ModMenuIntegration.self_first_time_message,
                                                (value) -> ModMenuIntegration.self_first_time_message = value
                                        )
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Text.literal("Color"))
                                        .description(OptionDescription.createBuilder().build())
                                        .binding(
                                                ModMenuIntegration.self_first_time_message_color,
                                                () -> ModMenuIntegration.self_first_time_message_color,
                                                (value) -> ModMenuIntegration.self_first_time_message_color = value
                                        )
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Welcome back (self)"))
                                .description(OptionDescription.createBuilder()
                                        .text(Text.literal("Shown when you join a server aegain"))
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Text.literal("Text"))
                                        .description(OptionDescription.createBuilder().build())
                                        .binding(
                                                ModMenuIntegration.self_welcome_back_message,//PlayerloginloggerClient.loadedConfig.self_welcome_back_message.text,
                                                () -> ModMenuIntegration.self_welcome_back_message,
                                                (value) -> ModMenuIntegration.self_welcome_back_message = value
                                        )
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .option(Option.<String>createBuilder()
                                        .name(Text.literal("Color"))
                                        .description(OptionDescription.createBuilder().build())
                                        .binding(
                                                ModMenuIntegration.self_welcome_back_message_color,
                                                () -> ModMenuIntegration.self_welcome_back_message_color,
                                                (value) -> ModMenuIntegration.self_welcome_back_message_color = value
                                        )
                                        .controller(StringControllerBuilder::create)
                                        .build())
                                .build())
                        .build())
                .save(() -> {
                    PlayerloginloggerClient.loadedConfig = new PlayerloginloggerClient.MessageConfig(
                            new PlayerloginloggerClient.MessageConfig.MessageEntry(self_first_time_message,self_first_time_message_color),
                            new PlayerloginloggerClient.MessageConfig.MessageEntry(self_welcome_back_message,self_welcome_back_message_color),
                            null,
                            null,
                            null,
                            '$'
                    );
                    try {
                        PlayerloginloggerClient.saveConfig(PlayerloginloggerClient.loadedConfig);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .build().generateScreen(parent));
    }
}
