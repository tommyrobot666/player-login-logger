package lommie.playerloginlogger.client;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerloginloggerClient implements ClientModInitializer {
    static final String MOD_ID = "playerloginlogger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    final static Set<String> placeholdersWithoutPrefix;
    final static Set<String> formattingWithoutPrefix;
    static {
        placeholdersWithoutPrefix = Set.of(
                "(player)",
                "(month)",
                "(month-name)",
                "(day)",
                "(year)",
                "(minute)",
                "(hour)",
                "(second)",
                "(raw-time)",
                "(since-day)",
                "(since-minute)",
                "(since-hour)",
                "(since-second)",
                "(raw-since)");
        List<String> allNormalMinecraftFormatting = Arrays.stream(Formatting.values()).flatMap(formatting -> Stream.of(formatting.getCode()+"")).toList();
        formattingWithoutPrefix = Set.copyOf(Lists.asList("(reset)",allNormalMinecraftFormatting.toArray(new String[0])));
    }
    static Set<String> placeholdersWithPrefix;
    static Set<String> formattingWithPrefix;
    static final public String configComment =
            """
            Supports all of these placeholders:
            $(player) -> The player's name,
            $(month) -> Last seen on month,
            $(month-name) -> Name of last seen on month,
            $(day) -> Last seen on day,
            $(year) -> Last seen on year,
            $(minute) -> Last seen on minute,
            $(hour) -> Last seen on hour,
            $(second) -> Last seen on second,
            $(raw-time) -> LocalDateTime.toString();,
            $(since-day) -> Days since last seen,
            $(since-minute) -> Minutes since last seen,
            $(since-hour) -> Hours since last seen,
            $(since-second) -> Seconds since last seen,
            $(raw-since) -> Duration.toString();
            
            Supports all of these formatting:
            $(reset) -> Reset formatting and set color back to textColor,
            $1,$2,$3,$4,$5,$6,$7,$8,$9,$a,$b,$c,$d,$e,$f,$k,$l,$m,$n,$o,$r
            -> Normal minecraft formatting (check https://minecraft.wiki/w/Formatting_codes)
            
            If you need to use any of these placeholders/formatting as plain text in your messages change formattingPrefix to a different character.
            If the text after the prefix is invalid placeholder/formatting, it will not be converted to placeholder/formatting.""";
    static final MessageConfig defaultConfig = new MessageConfig(
            new MessageConfig.MessageEntry("$kaaa$(reset) Joined this server for the first time $kaaa","#eede11"),
            new MessageConfig.MessageEntry("Last joined this server in $o$(year)$(reset) on $o$(day)$(reset) of $o$(month-name)$(reset) at $n$(hour):$(minute)","#555555"),
            new MessageConfig.MessageEntry("$kaaa$(reset) $(player) seen for the $nfirst time$(reset) $kaaa","#00ff00"),
            new MessageConfig.MessageEntry("$(player) last seen $l$(since-day)$(reset) days, $l$(since-hour)$(reset) hours, $l$(since-minute)$(reset) minutes, and $l$(since-second)$(reset) seconds ago.","#00aff0"),
            null,
            '$'

    );
    private static final File SAVE_FILE = new File("player_login_logger_logs.dat");
    private static final Path SAVE_LOCATION = Path.of("player_login_logger");
    private static final File CONFIG_FILE = new File("config/player_login_logger/messages.json");
    private static Set<UUID> lastPlayers = new HashSet<>();
    public static MessageConfig loadedConfig = null;

    // Config class for messages with text and textColor
    public static class MessageConfig {
        MessageConfig(MessageEntry self_first_time_message, MessageEntry self_welcome_back_message, MessageEntry other_first_time_message, MessageEntry other_welcome_back_message ,@Nullable MessageEntry leave_message, char formattingPrefix){
            this.self_welcome_back_message = self_welcome_back_message;
            this.self_first_time_message = self_first_time_message;
            if (leave_message != null){
                this.leave_message = leave_message;
                no_leave_message = false;
            } else {
                this.leave_message = null;
            }

            this.other_welcome_back_message = other_welcome_back_message;
            this.other_first_time_message = other_first_time_message;
            this.formattingPrefix = formattingPrefix;
        }

        public char formattingPrefix;
        MessageEntry other_first_time_message;
        MessageEntry leave_message;
        boolean no_leave_message = true;
        MessageEntry other_welcome_back_message;
        MessageEntry self_first_time_message;
        MessageEntry self_welcome_back_message;

        static class MessageEntry {
            String text;
            String textColor;

            public MessageEntry(String text, String textColor) {
                this.text = text;
                this.textColor = textColor;
            }

            @Override
            public String toString() {
                return "MessageEntry{" +
                        "text='" + text + '\'' +
                        ", textColor='" + textColor + '\'' +
                        '}';
            }
        }

        public Optional<MessageEntry> getLeave_message() {
            return leave_message==null?Optional.empty():Optional.of(leave_message);
        }
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("PlayerLoginLogger loading");
        ClientTickEvents.END_CLIENT_TICK.register(c ->{
            if (c.world == null) return;
            if (c.getCurrentServerEntry() == null) return;
            assert c.player != null;
            Set<UUID> leftPlayers = new HashSet<>();
            Set<UUID> joinedPlayers = new HashSet<>();
            Set<UUID> currentPlayers = new HashSet<>();
            c.world.getPlayers().forEach(
                    i -> currentPlayers.add(i.getUuid())
            );
            // player just joined
            if (!lastPlayers.contains(c.player.getUuid())){
                lastPlayers = currentPlayers;
                for (UUID id : currentPlayers){
                    joinMessage(id, c);
                }
            }
            // calculate players who left
            for (UUID id : lastPlayers){
                if (!currentPlayers.contains(id)){
                    leftPlayers.add(id);
                }
            }
            // calculate players who joined
            for (UUID id : currentPlayers){
                if (!lastPlayers.contains(id)){
                    joinedPlayers.add(id);
                }
            }
            // save left players and show message
            for (UUID id : leftPlayers){
                saveLeftDate(id, c.getCurrentServerEntry().address, LocalDateTime.now());
                leaveMessage(id, c);
            }
            // don't save join date, and show message
            for (UUID id : joinedPlayers){
                joinMessage(id,c);
            }
            lastPlayers = currentPlayers;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((p,c) -> {
            if (c.getCurrentServerEntry() == null) return;
            assert c.world != null;
            // get all players
            Set<UUID> currentPlayers = new HashSet<>();
            c.world.getPlayers().forEach(
                    i -> currentPlayers.add(i.getUuid())
            );
            // save leave dates (including self)
            for (UUID id : currentPlayers){
                saveLeftDate(id, c.getCurrentServerEntry().address, LocalDateTime.now());
            }
            // reset global var
            lastPlayers = new HashSet<>();
        });

//        getConfigOrLoad();
//        ModMenuIntegration.setValuesToLoadedConfig();
    }

    private void joinMessage(UUID id, MinecraftClient c) {
        assert c.player != null;
        MessageConfig config = getConfigOrLoad();
        LocalDateTime leftDate = loadLeftDate(id, Objects.requireNonNull(c.getCurrentServerEntry()).address);
        Duration since = leftDate==null ? Duration.ZERO : Duration.between(leftDate,LocalDateTime.now());
        MessageConfig.MessageEntry message =
                // if self
                id == c.player.getUuid() ?
                        // do welcome
                        leftDate == null ?
                                config.self_first_time_message :
                                config.self_welcome_back_message
                        :
                        // else, is first time seen?
                        leftDate == null ?
                                // do first join
                                config.other_first_time_message :
                                // else, normal message
                                config.other_welcome_back_message;
        sendMessage(c.player,id,message,c,config.formattingPrefix,leftDate,since);
    }

    private void leaveMessage(UUID id, MinecraftClient c) {
        MessageConfig config = getConfigOrLoad();
        LocalDateTime leftDate = loadLeftDate(id, Objects.requireNonNull(c.getCurrentServerEntry()).address);
        Duration since = leftDate==null ? Duration.ZERO : Duration.between(leftDate,LocalDateTime.now());
        Optional<MessageConfig.MessageEntry> message = config.getLeave_message();
        message.ifPresent(messageEntry -> {
            assert c.player != null;
            sendMessage(c.player,id,messageEntry,c,config.formattingPrefix,leftDate,since);
        });
    }

    private void sendMessage(ClientPlayerEntity player, UUID joinedPlayer, MessageConfig.MessageEntry message, MinecraftClient client, char placeholderFormattingPrefix,  LocalDateTime leftDate, Duration since){
        player.sendMessage(addFormatting(replacePlaceholders(joinedPlayer, client, message, placeholderFormattingPrefix,leftDate,since),message.textColor,placeholderFormattingPrefix),false);
    }

    private String replacePlaceholders(UUID joinedPlayer, MinecraftClient client, MessageConfig.MessageEntry message, char placeholderFormattingPrefix, LocalDateTime leftDate, Duration since) {
        StringBuilder finalText = new StringBuilder();
        StringBuilder currentSection = new StringBuilder();
        boolean foundPrefix = false;
        HashSet<String> matchingPlaceholders = new HashSet<>(placeholdersWithPrefix.size());
        int i = 0;
        int placeholderIndex = 0;

        while (i < message.text.length()){
            char currentChar = message.text.charAt(i);
            // processing placeholder
            if (foundPrefix){
                int finalPlaceholderIndex = placeholderIndex;
                matchingPlaceholders.removeIf(placeholder -> {
                    if (placeholder.length() <= finalPlaceholderIndex) return false;
                    return placeholder.charAt(finalPlaceholderIndex) != currentChar;
                });
                // no valid placeholder
                if (matchingPlaceholders.isEmpty()){
                    // add section
                    finalText.append(currentSection);
                    currentSection = new StringBuilder();
                    // setup continue
                    foundPrefix = false;
                }
                // one placeholder left and at end of it
                else if (matchingPlaceholders.size() == 1) {
                    String placeholder = matchingPlaceholders.stream().toList().get(0);
                    if (placeholder.length() == currentSection.length()) {
                        // add placeholder
                        finalText.append(getPlaceholderValue(placeholder, client, joinedPlayer, leftDate, since));
                        // setup continue
                        currentSection = new StringBuilder();
                        foundPrefix = false;
                        matchingPlaceholders.clear();
                    }
                }
            }
            // check for placeholder
            else if (currentChar == placeholderFormattingPrefix){
                // add section
                finalText.append(currentSection);
                currentSection = new StringBuilder();
                // setup to add placeholder
                foundPrefix = true;
                matchingPlaceholders.addAll(placeholdersWithPrefix);
                placeholderIndex = 0;
            }
            currentSection.append(currentChar);
            i++;
            placeholderIndex++;
        }
        if (foundPrefix){
            String placeholder = matchingPlaceholders.stream().toList().get(0);
            if (placeholder.length() == currentSection.length()) {
                // add placeholder
                finalText.append(getPlaceholderValue(placeholder, client, joinedPlayer, leftDate, since));
            }
        } else {
            finalText.append(currentSection);
        }
        return finalText.toString();
    }

    String getPlaceholderValue(String placeholder, MinecraftClient client, UUID playerId, LocalDateTime leftDate, Duration since) {
        assert client.world != null;
        return switch (placeholder.substring(1)) {
            case "(player)" ->
                    client.world.getPlayerByUuid(playerId) == null ? "{" + playerId.toString() + "}" : Objects.requireNonNull(client.world.getPlayerByUuid(playerId)).getName().getLiteralString();
            case "(month-name)" ->
                    leftDate == null ? "{null}" : leftDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            case "(month)" ->
                    leftDate == null ? "{null}" : leftDate.getMonthValue() + "";
            case "(day)" ->
                    leftDate == null ? "{null}" : leftDate.getDayOfMonth() + "";
            case "(year)" ->
                    leftDate == null ? "{null}" : leftDate.getYear() + "";
            case "(minute)" ->
                    leftDate == null ? "{null}" : leftDate.getMinute() + "";
            case "(hour)" ->
                    leftDate == null ? "{null}" : leftDate.getHour() + "";
            case "(second)" ->
                    leftDate == null ? "{null}" : leftDate.getSecond() + "";
            case "(raw-time)" ->
                    leftDate == null ? "{null}" : leftDate.toString();
            case "(since-day)" -> ((int) since.toDaysPart()) + "";
            case "(since-minute)" -> since.toMinutesPart() + "";
            case "(since-hour)" -> since.toHoursPart() + "";
            case "(since-second)" -> since.toSecondsPart() + "";
            case "(raw-since)" -> since.toString();
            default -> "{not-exist}";
        };
    }

    public static Text addFormatting(String message, String color, char placeholderFormattingPrefix) {
        MutableText finalText = Text.empty();
        Style style = Style.EMPTY.withColor(TextColor.parse(color).result().orElseGet(() -> TextColor.fromFormatting(Formatting.WHITE)));
        finalText.setStyle(style);
        StringBuilder currentSection = new StringBuilder();
        boolean foundPrefix = false;
        HashSet<String> matchingFormatting = new HashSet<>(formattingWithPrefix.size());
        int i = 0;
        int formattingIndex = 0;

        while (i < message.length()){
            char currentChar = message.charAt(i);
            // processing formating
            if (foundPrefix){
                int finalFormattingIndex = formattingIndex;
                matchingFormatting.removeIf(formatting -> {
                    if (formatting.length() <= finalFormattingIndex) return false;
                    return formatting.charAt(finalFormattingIndex) != currentChar;
                });
                // no valid formating
                if (matchingFormatting.isEmpty()){
                    // add section
                    MutableText newText = Text.literal(currentSection.toString());
                    newText.setStyle(style);
                    finalText.append(newText);
                    // setup continue
                    currentSection = new StringBuilder();
                    foundPrefix = false;
                }
                // one formating left and at end of it
                else if (matchingFormatting.size() == 1) {
                    String formatting = matchingFormatting.stream().toList().get(0);
                    if (formatting.length() == currentSection.length()) {
                        // add formating
                        style = applyFormatting(style,currentSection.toString(),color);
                        // setup continue
                        currentSection = new StringBuilder();
                        foundPrefix = false;
                        matchingFormatting.clear();
                    }
                }
            }
            // check for formatting
            else if (currentChar == placeholderFormattingPrefix){
                // add section
                MutableText newText = Text.literal(currentSection.toString());
                newText.setStyle(style);
                finalText.append(newText);
                currentSection = new StringBuilder();
                // setup to add formatting
                foundPrefix = true;
                matchingFormatting.addAll(formattingWithPrefix);
                formattingIndex = 0;
            }
            currentSection.append(currentChar);
            i++;
            formattingIndex++;
        }
        MutableText newText = Text.literal(currentSection.toString());
        newText.setStyle(style);
        finalText.append(newText);
        return finalText;
    }

    private static Style applyFormatting(Style style, String formatting, String color) {
        if (formatting.length() == 2){
            return style.withFormatting(Formatting.byCode(formatting.charAt(1)));
        } else if (formatting.substring(1).equals("(reset)")) {
            return Style.EMPTY.withColor((TextColor.parse(color).result().orElseGet(() -> TextColor.fromFormatting(Formatting.WHITE))));
        }
        return Style.EMPTY;
    }

    private void saveLeftDate(UUID id, String address, LocalDateTime date) {
        try {
            Files.createDirectories(SAVE_LOCATION);
            NbtCompound root = new NbtCompound();
            Path idSaveLocation = SAVE_LOCATION.resolve(id.toString() + ".dat");
            if (Files.exists(idSaveLocation)) {
                root = readNbtCompound(idSaveLocation);
            }
            root.putString(address, date.toString());
            writeNbtCompound(root, idSaveLocation);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                LOGGER.debug(element.toString());
            }
        }
    }

    private LocalDateTime loadLeftDate(UUID id, String address) {
        Path idSaveLocation = SAVE_LOCATION.resolve(id.toString() + ".dat");
        if (!Files.exists(idSaveLocation)) return null;
        NbtCompound root;
        try {
            root = readNbtCompound(idSaveLocation);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                LOGGER.debug(element.toString());
            }
            root = new NbtCompound();
        }
        if (!root.contains(address)) return null;
        return LocalDateTime.parse(root.getString(address));
    }

    public static @NotNull MessageConfig getConfigOrLoad(){
        if (loadedConfig == null){
            try {
                LOGGER.info("Loading config");
                loadedConfig = loadConfig();
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.error("Failed to load config");
                for (StackTraceElement element : e.getStackTrace()) {
                    LOGGER.error(element.toString());
                }
                return defaultConfig;
            }
            LOGGER.debug("self_first_time_message:{}\nself_welcome_back_message:{}\nother_first_time_message{}\nother_welcome_back_message{}",loadedConfig.self_first_time_message,loadedConfig.self_welcome_back_message,loadedConfig.other_first_time_message,loadedConfig.other_welcome_back_message);
            createPlaceholdersWithPrefix();
            createFormattingWithPrefix();
        }
        return loadedConfig;
    }

    public static MessageConfig loadConfig() throws IOException, IllegalArgumentException {
        if (!CONFIG_FILE.exists()) {
            // generate config file
            LOGGER.debug("No config found ,Creating config file");
            saveConfig(defaultConfig);
            return defaultConfig;
        } else {
            // read existing file
            Reader reader = new FileReader(CONFIG_FILE);
            // offset reader to start at json data
            int c = 0;
            while (c != -1 && c != '|') {
                c = reader.read();
            }
            if (c == -1) {
                throw new IllegalArgumentException("No '|' found in the input");
            }

            return new Gson().fromJson(reader, MessageConfig.class);
        }
    }

    public static void saveConfig(MessageConfig config) throws IOException {
        LOGGER.info("Saving config");
        Files.createDirectories(CONFIG_FILE.getParentFile().toPath());
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(config);
        Files.writeString(CONFIG_FILE.toPath(), configComment + "\n|\n" + json);
    }

    static void createPlaceholdersWithPrefix(){
        placeholdersWithPrefix = placeholdersWithoutPrefix.stream().map(string -> loadedConfig.formattingPrefix+string).collect(Collectors.toSet());
    }

    static void createFormattingWithPrefix(){
        formattingWithPrefix = formattingWithoutPrefix.stream().map(string -> loadedConfig.formattingPrefix+string).collect(Collectors.toSet());
    }

    private NbtCompound readNbtCompound(Path path) throws IOException {
        return NbtIo.read(path);
    }

    private void writeNbtCompound(NbtCompound compound ,Path path) throws IOException {
        NbtIo.write(compound,path);
    }

}
