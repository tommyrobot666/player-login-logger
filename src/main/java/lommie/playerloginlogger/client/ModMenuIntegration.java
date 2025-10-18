package lommie.playerloginlogger.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import lommie.playerloginlogger.client.yaclcontroller.FormattedStringControllerBuilder;
import net.minecraft.text.Text;

import java.awt.*;
import java.io.IOException;

public class ModMenuIntegration implements ModMenuApi {
    public static String self_first_time_message = "";
    public static String self_first_time_message_color = "";
    public static String self_welcome_back_message = "";
    public static String self_welcome_back_message_color = "";
    public static String other_first_time_message = "";
    public static String other_first_time_message_color = "";
    public static String other_welcome_back_message = "";
    public static String other_welcome_back_message_color = "";
    public static boolean has_leave_message = false;
    public static String leave_message = "";
    public static String leave_message_color = "";

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent -> {
            PlayerloginloggerClient.getConfigOrLoad();
            setValuesToLoadedConfig();
            return YetAnotherConfigLib.createBuilder()
                    .title(Text.literal("Configuration for Player Login Logger"))
                    .category(ConfigCategory.createBuilder()
                            .name(Text.literal("Messages"))
                            .tooltip(Text.literal("No, this is not a tooltip"))
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("First join"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Shown when someone joins for the first time"))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Text"))
                                            .description(OptionDescription.createBuilder().build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.other_first_time_message.text,
                                                    () -> ModMenuIntegration.other_first_time_message,
                                                    (value) -> ModMenuIntegration.other_first_time_message = value
                                            )
                                            .controller((option) -> FormattedStringControllerBuilder.create(option,() -> ModMenuIntegration.other_first_time_message_color))
                                            .build())
                                    .option(Option.<Color>createBuilder()
                                            .name(Text.literal("Color"))
                                            .description(OptionDescription.createBuilder().build())
                                            .binding(
                                                    Color.decode(PlayerloginloggerClient.loadedConfig.other_first_time_message.textColor),
                                                    () -> Color.decode(ModMenuIntegration.other_first_time_message_color),
                                                    (value) -> ModMenuIntegration.other_first_time_message_color = colorToString(value)
                                            )
                                            .controller(ColorControllerBuilder::create)
                                            .instant(true)
                                            .build())
                                    .build())
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("Welcome back"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Shown when someone is seen again"))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Text"))
                                            .description(OptionDescription.createBuilder().build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.other_welcome_back_message.text,
                                                    () -> ModMenuIntegration.other_welcome_back_message,
                                                    (value) -> ModMenuIntegration.other_welcome_back_message = value
                                            )
                                            .controller(StringControllerBuilder::create)
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Color"))
                                            .description(OptionDescription.createBuilder().build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.other_welcome_back_message.textColor,
                                                    () -> ModMenuIntegration.other_welcome_back_message_color,
                                                    (value) -> ModMenuIntegration.other_welcome_back_message_color = value
                                            )
                                            .controller(StringControllerBuilder::create)
                                            .build())
                                    .build())
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("First join (self)"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Shown when you join a server for the first time"))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Text"))
                                            .description(OptionDescription.createBuilder().build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.self_first_time_message.text,
                                                    () -> ModMenuIntegration.self_first_time_message,
                                                    (value) -> ModMenuIntegration.self_first_time_message = value
                                            )
                                            .controller(StringControllerBuilder::create)
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Color"))
                                            .description(OptionDescription.createBuilder().build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.self_first_time_message.textColor,
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
                                                    PlayerloginloggerClient.loadedConfig.self_welcome_back_message.text,
                                                    () -> ModMenuIntegration.self_welcome_back_message,
                                                    (value) -> ModMenuIntegration.self_welcome_back_message = value
                                            )
                                            .controller(StringControllerBuilder::create)
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Color"))
                                            .description(OptionDescription.createBuilder().build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.self_welcome_back_message.textColor,
                                                    () -> ModMenuIntegration.self_welcome_back_message_color,
                                                    (value) -> ModMenuIntegration.self_welcome_back_message_color = value
                                            )
                                            .controller(StringControllerBuilder::create)
                                            .build())
                                    .build())
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("Leave"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Shown when someone leaves"))
                                            .build())
                                    .option(Option.<Boolean>createBuilder()
                                            .name(Text.literal("Text"))
                                            .description(OptionDescription.createBuilder().build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.getLeave_message().isPresent(),
                                                    () -> ModMenuIntegration.has_leave_message,
                                                    (value) -> ModMenuIntegration.has_leave_message = value
                                            )
                                            .controller(TickBoxControllerBuilder::create)
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Text"))
                                            .description(OptionDescription.createBuilder().build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.getLeave_message().orElse(new PlayerloginloggerClient.MessageConfig.MessageEntry("", "")).text,
                                                    () -> ModMenuIntegration.leave_message,
                                                    (value) -> ModMenuIntegration.leave_message = value
                                            )
                                            .controller(StringControllerBuilder::create)
                                            .addListener(((option, event) -> {
                                                option.setAvailable(ModMenuIntegration.has_leave_message);
                                            }))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Color"))
                                            .description(OptionDescription.createBuilder().build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.getLeave_message().orElse(new PlayerloginloggerClient.MessageConfig.MessageEntry("", "")).textColor,
                                                    () -> ModMenuIntegration.leave_message_color,
                                                    (value) -> ModMenuIntegration.leave_message_color = value
                                            )
                                            .controller(StringControllerBuilder::create)
                                            .build())
                                    .build())
                            .build())
                    .save(() -> {
                        PlayerloginloggerClient.MessageConfig.MessageEntry leave_message_entry = null;
                        if (ModMenuIntegration.has_leave_message) {
                            leave_message_entry = new PlayerloginloggerClient.MessageConfig.MessageEntry(leave_message, leave_message_color);
                        }
                        PlayerloginloggerClient.loadedConfig = new PlayerloginloggerClient.MessageConfig(
                                new PlayerloginloggerClient.MessageConfig.MessageEntry(self_first_time_message, self_first_time_message_color),
                                new PlayerloginloggerClient.MessageConfig.MessageEntry(self_welcome_back_message, self_welcome_back_message_color),
                                new PlayerloginloggerClient.MessageConfig.MessageEntry(other_first_time_message, other_first_time_message_color),
                                new PlayerloginloggerClient.MessageConfig.MessageEntry(other_welcome_back_message, other_welcome_back_message_color),
                                leave_message_entry,
                                '$'
                        );
                        try {
                            PlayerloginloggerClient.saveConfig(PlayerloginloggerClient.loadedConfig);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    })
                    .build().generateScreen(parent);
        });
    }

    public static void setValuesToLoadedConfig(){
        ModMenuIntegration.self_first_time_message = PlayerloginloggerClient.loadedConfig.self_first_time_message.text;
        ModMenuIntegration.self_first_time_message_color = PlayerloginloggerClient.loadedConfig.self_first_time_message.textColor;
        ModMenuIntegration.self_welcome_back_message = PlayerloginloggerClient.loadedConfig.self_welcome_back_message.text;
        ModMenuIntegration.self_welcome_back_message_color = PlayerloginloggerClient.loadedConfig.self_welcome_back_message.textColor;
        ModMenuIntegration.other_first_time_message = PlayerloginloggerClient.loadedConfig.other_first_time_message.text;
        ModMenuIntegration.other_first_time_message_color = PlayerloginloggerClient.loadedConfig.other_first_time_message.textColor;
        ModMenuIntegration.other_welcome_back_message = PlayerloginloggerClient.loadedConfig.other_welcome_back_message.text;
        ModMenuIntegration.other_welcome_back_message_color = PlayerloginloggerClient.loadedConfig.other_welcome_back_message.textColor;
        PlayerloginloggerClient.loadedConfig.getLeave_message().ifPresentOrElse(
                (entry) -> {
                    ModMenuIntegration.leave_message = PlayerloginloggerClient.loadedConfig.leave_message.text;
                    ModMenuIntegration.leave_message_color = PlayerloginloggerClient.loadedConfig.leave_message.textColor;
                    has_leave_message = true;
                },
                () -> {has_leave_message=false;}
        );
    }

    public static String colorToString(Color color){
        return String.format("#{}{}{}",color.getRed(),color.getGreen(),color.getBlue());
    }
}
