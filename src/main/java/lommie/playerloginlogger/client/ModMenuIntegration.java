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
    public static char formatting_prefix = '$';

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent -> {
            PlayerloginloggerClient.getConfigOrLoad();
            setValuesToLoadedConfig();
            return YetAnotherConfigLib.createBuilder()
                    .title(Text.literal("Configuration for Player Login Logger"))
                    /*.category(ConfigCategory.createBuilder()
                            .name(Text.literal("Help"))
                            .tooltip(Text.literal("Read this page first"))
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("Help"))
                                    .collapsed(false)
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Some info"))
                                            .build())
                                    .option(LabelOption.create(Text.literal(PlayerloginloggerClient.configComment)))
                                    .option(LabelOption.create(Text.literal("""
                                            If you change only the text color of a message(s), then you must press the "Force save" button.
                                            This is because YACL doesn't track whether "instantly applied" options are changed
                                            """)))
                                    .build())
                            .build())*/
                    .category(ConfigCategory.createBuilder()
                            .name(Text.literal("Messages"))
                            .tooltip(Text.literal("No, this is not a tooltip"))
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("Help"))
                                    .collapsed(true)
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Some info"))
                                            .build())
                                    .option(LabelOption.create(Text.literal(PlayerloginloggerClient.configComment)))
                                    .option(LabelOption.create(Text.literal("""
                                            If you change only the text color of a message(s), then you must press the "Force save" button.
                                            This is because YACL doesn't track whether "instantly applied" options are changed
                                            """)))
                                    .build())
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("First join"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Shown when someone joins for the first time"))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Text"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Shown when someone joins for the first time"))
                                                    .build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.other_first_time_message.text,
                                                    () -> ModMenuIntegration.other_first_time_message,
                                                    (value) -> ModMenuIntegration.other_first_time_message = value
                                            )
                                            .controller((option) -> FormattedStringControllerBuilder.create(option,() -> ModMenuIntegration.other_first_time_message_color))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Color"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Shown when someone joins for the first time"))
                                                    .build())
                                            .stateManager(StateManager.createInstant(
                                                    PlayerloginloggerClient.loadedConfig.other_first_time_message.textColor,
                                                    () -> ModMenuIntegration.other_first_time_message_color,
                                                    (value) -> ModMenuIntegration.other_first_time_message_color = value
                                            ))
                                            /*.binding(
                                                    PlayerloginloggerClient.loadedConfig.other_first_time_message.textColor,
                                                    () -> ModMenuIntegration.other_first_time_message_color,
                                                    (value) -> ModMenuIntegration.other_first_time_message_color = value
                                            )*/
                                            .controller((option) -> FormattedStringControllerBuilder.create(option,() -> ModMenuIntegration.other_first_time_message_color))
                                            .build())
                                    .build())
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("Welcome back"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Shown when someone is seen again"))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Text"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Shown when someone is seen again"))
                                                    .build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.other_welcome_back_message.text,
                                                    () -> ModMenuIntegration.other_welcome_back_message,
                                                    (value) -> ModMenuIntegration.other_welcome_back_message = value
                                            )
                                            .controller((option) -> FormattedStringControllerBuilder.create(option,() -> ModMenuIntegration.other_welcome_back_message_color))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Color"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Shown when someone is seen again"))
                                                    .build())
                                            .stateManager(StateManager.createInstant(
                                                    PlayerloginloggerClient.loadedConfig.other_welcome_back_message.textColor,
                                                    () -> ModMenuIntegration.other_welcome_back_message_color,
                                                    (value) -> ModMenuIntegration.other_welcome_back_message_color = value
                                            ))
                                            /*.binding(
                                                    PlayerloginloggerClient.loadedConfig.other_welcome_back_message.textColor,
                                                    () -> ModMenuIntegration.other_welcome_back_message_color,
                                                    (value) -> ModMenuIntegration.other_welcome_back_message_color = value
                                            )*/
                                            .controller((option) -> FormattedStringControllerBuilder.create(option,() -> ModMenuIntegration.other_welcome_back_message_color))
                                            .build())
                                    .build())
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("First join (self)"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Shown when you join a server for the first time"))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Text"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Shown when you join a server for the first time"))
                                                    .build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.self_first_time_message.text,
                                                    () -> ModMenuIntegration.self_first_time_message,
                                                    (value) -> ModMenuIntegration.self_first_time_message = value
                                            )
                                            .controller((option) -> FormattedStringControllerBuilder.create(option,() -> ModMenuIntegration.self_first_time_message_color))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Color"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Shown when you join a server for the first time"))
                                                    .build())
                                            .stateManager(StateManager.createInstant(
                                                    PlayerloginloggerClient.loadedConfig.self_first_time_message.textColor,
                                                    () -> ModMenuIntegration.self_first_time_message_color,
                                                    (value) -> ModMenuIntegration.self_first_time_message_color = value
                                            ))
                                            /*.binding(
                                                    PlayerloginloggerClient.loadedConfig.self_first_time_message.textColor,
                                                    () -> ModMenuIntegration.self_first_time_message_color,
                                                    (value) -> ModMenuIntegration.self_first_time_message_color = value
                                            )*/
                                            .controller((option) -> FormattedStringControllerBuilder.create(option,() -> ModMenuIntegration.self_first_time_message_color))
                                            .build())
                                    .build())
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("Welcome back (self)"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Shown when you join a server aegain"))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Text"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Shown when you join a server again"))
                                                    .build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.self_welcome_back_message.text,
                                                    () -> ModMenuIntegration.self_welcome_back_message,
                                                    (value) -> ModMenuIntegration.self_welcome_back_message = value
                                            )
                                            .controller((option) -> FormattedStringControllerBuilder.create(option,() -> ModMenuIntegration.self_welcome_back_message_color))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Color"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Shown when you join a server again"))
                                                    .build())
                                            .stateManager(StateManager.createInstant(
                                                    PlayerloginloggerClient.loadedConfig.self_welcome_back_message.textColor,
                                                    () -> ModMenuIntegration.self_welcome_back_message_color,
                                                    (value) -> ModMenuIntegration.self_welcome_back_message_color = value
                                            ))
                                            /*.binding(
                                                    PlayerloginloggerClient.loadedConfig.self_welcome_back_message.textColor,
                                                    () -> ModMenuIntegration.self_welcome_back_message_color,
                                                    (value) -> ModMenuIntegration.self_welcome_back_message_color = value
                                            )*/
                                            .controller((option) -> FormattedStringControllerBuilder.create(option,() -> ModMenuIntegration.self_welcome_back_message_color))
                                            .build())
                                    .build())
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("Leave"))
                                    .collapsed(!ModMenuIntegration.has_leave_message)
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Shown when someone leaves"))
                                            .build())
                                    .option(Option.<Boolean>createBuilder()
                                            .name(Text.literal("Show leave message"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Shown when someone leaves"))
                                                    .build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.getLeave_message().isPresent(),
                                                    () -> ModMenuIntegration.has_leave_message,
                                                    (value) -> ModMenuIntegration.has_leave_message = value
                                            )
                                            .controller(TickBoxControllerBuilder::create)
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Text"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Shown when someone leaves"))
                                                    .build())
                                            .binding(
                                                    PlayerloginloggerClient.loadedConfig.getLeave_message().orElse(new PlayerloginloggerClient.MessageConfig.MessageEntry("", "")).text,
                                                    () -> ModMenuIntegration.leave_message,
                                                    (value) -> ModMenuIntegration.leave_message = value
                                            )
                                            .controller((option) -> FormattedStringControllerBuilder.create(option,() -> ModMenuIntegration.leave_message_color))
//                                            .addListener(((option, event) -> option.setAvailable(ModMenuIntegration.has_leave_message)))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Color"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Shown when someone leaves"))
                                                    .build())
                                            .stateManager(StateManager.createInstant(
                                                    PlayerloginloggerClient.loadedConfig.getLeave_message().orElse(new PlayerloginloggerClient.MessageConfig.MessageEntry("", "")).textColor,
                                                    () -> ModMenuIntegration.leave_message_color,
                                                    (value) -> ModMenuIntegration.leave_message_color = value
                                            ))
                                            /*.binding(
                                                    PlayerloginloggerClient.loadedConfig.getLeave_message().orElse(new PlayerloginloggerClient.MessageConfig.MessageEntry("", "")).textColor,
                                                    () -> ModMenuIntegration.leave_message_color,
                                                    (value) -> ModMenuIntegration.leave_message_color = value
                                            )*/
                                            .controller((option) -> FormattedStringControllerBuilder.create(option,() -> ModMenuIntegration.leave_message_color))
//                                            .addListener(((option, event) -> option.setAvailable(ModMenuIntegration.has_leave_message)))
                                            .build())
                                    .build())
                            .group(OptionGroup.createBuilder()
                                    .name(Text.literal("Other"))
                                    .collapsed(true)
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Other options"))
                                            .build())
                                    .option(Option.<String>createBuilder()
                                            .name(Text.literal("Formatting Prefix"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("Prefix used to detect placeholders and formatting (usually '$')"))
                                                    .build())
                                            .binding(
                                                    String.valueOf(PlayerloginloggerClient.loadedConfig.formattingPrefix),
                                                    () -> String.valueOf(ModMenuIntegration.formatting_prefix),
                                                    (value) -> ModMenuIntegration.formatting_prefix = value.isEmpty()?'$':value.charAt(0)
                                            )
                                            .controller(StringControllerBuilder::create)
                                            .build())
                                    .option(ButtonOption.createBuilder()
                                            .name(Text.literal("Force save"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("\"Why doesn't it notice that I changed the text's color?\" -You")).build())
                                            .text(Text.literal("Save"))
                                            .action(((yaclScreen, buttonOption) -> save()))
                                            .build())
                                    .option(ButtonOption.createBuilder()
                                            .name(Text.literal("Reset settings"))
                                            .description(OptionDescription.createBuilder()
                                                    .text(Text.literal("\"AAAAHHHH! -The Settings\"")).build())
                                            .text(Text.literal("Reset all"))
                                            .action(((yaclScreen, buttonOption) -> {
                                                PlayerloginloggerClient.loadedConfig = PlayerloginloggerClient.defaultConfig;
                                                try {
                                                    PlayerloginloggerClient.saveConfig(PlayerloginloggerClient.defaultConfig);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                yaclScreen.finishOrSave();
                                                yaclScreen.close();
                                            }))
                                            .build())
                                    .build())
                            .build())
                    .save(() -> save())
                    .build().generateScreen(parent);
        });
    }

    static void save(){
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
                formatting_prefix
        );
        try {
            PlayerloginloggerClient.saveConfig(PlayerloginloggerClient.loadedConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
