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
    static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
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
    final private String configComment =
            """
            "
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
            -> Normal minecraft formatting (check <insert-website-here>)
            
            If you need to use any of these placeholders/formatting as plain text in your messages change formattingPrefix to a different character.
            If the text after the prefix is invalid placeholder/formatting, it will not be converted to placeholder/formatting.
            "
            """;
    final MessageConfig defaultConfig = new MessageConfig(
            new MessageConfig.MessageEntry("$(player) last seen $l$(since-day)$r $lda$(end)$lys, $l$(since-hour) hours, $l$(since-minute) minutes, and $l$(since-second) seconds$r ago.","#006f00"),
            new MessageConfig.MessageEntry("$(player) seen for the $(end)$nfirst time","#00ff00"),
            new MessageConfig.MessageEntry("Last joined this server in $(year) on $(day) of $(month-name) at $(hour):$(minute)","#eeeeee")
    );
    private static final File SAVE_FILE = new File("player_login_logger_logs.dat");
    private static final File CONFIG_FILE = new File("config/player_login_logger/messages.json");
    private static Set<UUID> lastPlayers = new HashSet<>();
    static MessageConfig loadedConfig = null;

    // Config class for messages with text and textColor
    private static class MessageConfig {
        MessageConfig(MessageEntry join_message, MessageEntry first_time_message, MessageEntry welcome_back_message, @Nullable MessageEntry leave_message, char formattingPrefix){
            this.join_message = join_message;
            this.first_time_message = first_time_message;
            if (leave_message != null){
                this.leave_message = leave_message;
                no_leave_message = false;
            } else {
                this.leave_message = null;
            }

            this.welcome_back_message = welcome_back_message;
            this.formattingPrefix = formattingPrefix;
        }

        MessageConfig(MessageEntry join_message, MessageEntry first_time_message, MessageEntry welcome_back_message, @Nullable MessageEntry leave_message){
            this(join_message, welcome_back_message, first_time_message,leave_message, '$');
        }

        MessageConfig(MessageEntry join_message, MessageEntry welcome_back_message, MessageEntry first_time_message){
            this(join_message, first_time_message, welcome_back_message, null);
        }

        char formattingPrefix;
        MessageEntry join_message;
        MessageEntry first_time_message;
        MessageEntry leave_message;
        boolean no_leave_message = true;
        MessageEntry welcome_back_message;

        static class MessageEntry {
            String text;
            String textColor;

            public MessageEntry(String text, String textColor) {
                this.text = text;
                this.textColor = textColor;
            }
        }

        public Optional<MessageEntry> getLeave_message() {
            return leave_message==null?Optional.empty():Optional.of(leave_message);
        }
    }

    @Override
    public void onInitializeClient() {
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
                        config.welcome_back_message :
                        // else, is first time seen?
                        leftDate == null ?
                                // do first join
                                config.first_time_message :
                                // else, normal message
                                config.join_message;
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
        player.sendMessage(Text.literal(replacePlaceholders(joinedPlayer, client, message, placeholderFormattingPrefix,leftDate,since)),false);
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
                matchingPlaceholders.removeIf(placeholder -> placeholder.charAt(finalPlaceholderIndex) != currentChar);
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
                    String placeholder = matchingPlaceholders.stream().toList().getFirst();
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
        finalText.append(currentSection);
        return finalText.toString();
    }

    String getPlaceholderValue(String placeholder, MinecraftClient client, UUID playerId, LocalDateTime leftDate, Duration since) {
        assert client.world != null;
        return switch (placeholder.substring(1)) {
            case "(player)" ->
                    client.world.getPlayerByUuid(playerId) == null ? "{" + playerId.toString() + "}" : Objects.requireNonNull(Objects.requireNonNull(client.world.getPlayerByUuid(playerId)).getDisplayName()).copy().toString();
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


/*
    private MutableText replaceMessage(UUID id, MinecraftClient c, MessageConfig.MessageEntry messageEntry, char formattingCharPrefix) {
        LocalDateTime leftDate = loadLeftDate(id, c.getCurrentServerEntry().address);
        String message = Objects.requireNonNullElse(messageEntry.text,   formattingCharPrefix + "cERROR: PlayerLoginLoader") + formattingCharPrefix + "(end)";
        Duration since =  leftDate == null ? Duration.ZERO :  Duration.between(leftDate,LocalDateTime.now());

        MutableText[] texts = mapMessageToFormatting(id,c,leftDate,since,message, noPrefixFormatting ,formattingCharPrefix, messageEntry.textColor);
        MutableText text = texts[0];
        for (int i = 1; i < texts.length; i++) {
            text.append(texts[i]);
        }
        return text;
    }

    MutableText[] mapMessageToFormatting(UUID playerId,MinecraftClient client,LocalDateTime leftDate, Duration since,String message, Set<String> formattingChars, char formattingCharPrefix, String color){
        // formattingCharPrefix = $
        ArrayList<MutableText> texts = new ArrayList<>();

        LinkedHashSet<String> currentFormattingChars = new LinkedHashSet<>();
        StringBuilder nextText = new StringBuilder();
        String nextFormatting = "";
        int charsAddedToNextFormatting = 0;
        boolean addedACharToNextFormatting = false;
        for (int i = 0; i < message.length(); i++) {
            if (formattingChars.contains(nextFormatting)){
                int result = formattingCharToReplace(currentFormattingChars, nextFormatting);
                if (result == -2){
                    currentFormattingChars.clear();
                } else if (result == -1) {
                    currentFormattingChars.add(nextFormatting);
                } else {
                    ArrayList<String> list = new ArrayList<>(currentFormattingChars.stream().toList());
                    list.add(result,nextFormatting);
                    currentFormattingChars = new LinkedHashSet<>(list);
                }
                charsAddedToNextFormatting = 0;
                nextFormatting = "";

                // adding a new text
                texts.addAll(constructNewFormattedText(playerId, client, leftDate, since, nextText.toString(), currentFormattingChars, color));
                nextText = new StringBuilder();
                currentFormattingChars = removeFakeFormatting(currentFormattingChars);
            }

            char c = message.charAt(i);
            if (c == formattingCharPrefix) {
                int j = 1;
                String nextNextFormatting = "";
                ArrayList<String> formattingCharsLeft = new ArrayList<>(formattingChars);
                while (formattingCharsLeft.size() > 1) {
                    nextNextFormatting += message.charAt(i+j);
                    j++;

                    ArrayList<String> tempFormattingCharsLeft = (ArrayList<String>) formattingCharsLeft.clone();
                    formattingCharsLeft = new ArrayList<>();
                    for (String formatting : tempFormattingCharsLeft){
                        if (formatting.startsWith(nextNextFormatting)){
                            formattingCharsLeft.add(formatting);
                        }
                    }
                }
                if (formattingCharsLeft.size() == 1){
                    nextFormatting = formattingCharsLeft.get(0);
                    charsAddedToNextFormatting = formattingCharsLeft.get(0).length();
                    i += charsAddedToNextFormatting;
                    addedACharToNextFormatting = true;
                }
            }
            if (!addedACharToNextFormatting){
                if (charsAddedToNextFormatting > 0) {
                    //just finished adding a formatting
                    //only had the start of a formatting char
                    //add formatting to that
                    nextText.append(nextFormatting);
                    nextFormatting = "";
                    charsAddedToNextFormatting = 0;
                }

                nextText.append(c);
            }

            addedACharToNextFormatting = false;
        }
        if (nextText.length() > 0) {
            texts.addAll(constructNewFormattedText(playerId, client, leftDate, since, nextText.toString(), currentFormattingChars, color));
        }
        return texts.toArray(MutableText[]::new);
    }

    private static List<MutableText> constructNewFormattedText(UUID playerId, MinecraftClient client, LocalDateTime leftDate, Duration since, String nextText, Set<String> lastUsedFormattingChar, String color) {
        MutableText newText = Text.literal(nextText).styled(s -> s.withColor(TextColor.parse(color).getOrThrow()));
        ArrayList<Formatting> realFormatting = new ArrayList<>();
        String replacementFormatting = "";
        for (String prefixRemovedItem : lastUsedFormattingChar){
            if (prefixRemovedItem.length() == 1){
                realFormatting.add(Formatting.byCode(prefixRemovedItem.charAt(0)));
            } else {
                replacementFormatting = prefixRemovedItem;
            }
        }

        for (Formatting realFormattingItem : realFormatting){
            newText = newText.formatted(realFormattingItem);
        }

        final MutableText finalNewText = newText;
        MutableText addOn = switch (replacementFormatting){
            case "(player)" -> client.world.getPlayerByUuid(playerId) == null ? Text.literal("{"+ playerId.toString()+"}").styled(s -> finalNewText.getStyle()) : client.world.getPlayerByUuid(playerId).getDisplayName().copy();
            case "(month-name)" -> leftDate == null ? Text.literal("{null}").styled(s -> finalNewText.getStyle()) : Text.literal(leftDate.getMonth().getDisplayName(TextStyle.FULL,Locale.ENGLISH)).styled(s -> finalNewText.getStyle());
            case "(month)" -> leftDate == null ? Text.literal("{null}").styled(s -> finalNewText.getStyle()) : Text.literal(leftDate.getMonthValue()+"").styled(s -> finalNewText.getStyle());
            case "(day)" -> leftDate == null ? Text.literal("{null}").styled(s -> finalNewText.getStyle()) : Text.literal(leftDate.getDayOfMonth()+"").styled(s -> finalNewText.getStyle());
            case "(year)" -> leftDate == null ? Text.literal("{null}").styled(s -> finalNewText.getStyle()) : Text.literal(leftDate.getYear()+"").styled(s -> finalNewText.getStyle());
            case "(minute)" -> leftDate == null ? Text.literal("{null}").styled(s -> finalNewText.getStyle()) : Text.literal(leftDate.getMinute()+"").styled(s -> finalNewText.getStyle());
            case "(hour)" -> leftDate == null ? Text.literal("{null}").styled(s -> finalNewText.getStyle()) : Text.literal(leftDate.getHour()+"").styled(s -> finalNewText.getStyle());
            case "(second)" -> leftDate == null ? Text.literal("{null}").styled(s -> finalNewText.getStyle()) : Text.literal(leftDate.getSecond()+"").styled(s -> finalNewText.getStyle());
            case "(raw-time)" -> leftDate == null ? Text.literal("{null}").styled(s -> finalNewText.getStyle()) : Text.literal(leftDate.toString()).styled(s -> finalNewText.getStyle());
            case "(since-day)" -> Text.literal(((int) since.toDaysPart())+"").styled(s -> finalNewText.getStyle());
            case "(since-minute)" -> Text.literal(since.toMinutesPart()+"").styled(s -> finalNewText.getStyle());
            case "(since-hour)" -> Text.literal(since.toHoursPart()+"").styled(s -> finalNewText.getStyle());
            case "(since-second)" -> Text.literal(since.toSecondsPart()+"").styled(s -> finalNewText.getStyle());
            case "(raw-since)" -> Text.literal(since.toString()).styled(s -> finalNewText.getStyle());
            default -> null;
        };

        if (addOn != null){
            return List.of(newText, addOn);
        }
        return List.of(newText);
    }

    private int formattingCharToReplace(Set<String> currentFormattingChars, String theChar) {
        Set<String> prefixRemoved = new HashSet<>();
        for (String prefixedFormattingChar : currentFormattingChars){
            prefixRemoved.add(prefixedFormattingChar.substring(1));
        }

        if (theChar.substring(1).length() != 1){
            String[] currentFormattingCharArray = currentFormattingChars.toArray(String[]::new);
            int toReplace = -1; //add to list
            for (int i = 0; i < currentFormattingCharArray.length; i++) {
                if (currentFormattingCharArray[i].length() != 1){
                    toReplace = i;
                }
            }
            return toReplace;
        }

        Set<Formatting> realFormatting = new HashSet<>();
        for (String prefixRemovedItem : prefixRemoved){
            if (prefixRemovedItem.length() == 1){
                realFormatting.add(Formatting.byCode(prefixRemovedItem.charAt(0)));
            }
        }

        Formatting theFormatting = Formatting.byCode(theChar.charAt(1));
        if (theFormatting == null){
            return -1;
        }
        if (theFormatting == Formatting.RESET) {
            return -2; //clear list
        }
        if (theFormatting.isColor()) {
            for (int i = 0; i < realFormatting.size(); i++) {
                Formatting realFormattingItem = realFormatting.toArray(Formatting[]::new)[i];
                if (realFormattingItem.isColor()){
                    return i;
                }
            }
        } else if (theFormatting.isModifier() && !realFormatting.contains(theFormatting)) {
            return -1; //add it
        }

        return -1;
    }

    private LinkedHashSet<String> removeFakeFormatting(LinkedHashSet<String> currentFormattingChars){
        LinkedHashSet<String> kept = new LinkedHashSet<>();
        for (String i : currentFormattingChars){
            if (i.length() < 3){
                kept.add(i);
            }
        }
        return kept;
    }*/

    private void saveLeftDate(UUID id,  String address, LocalDateTime date) {
        NbtCompound root = null;
        if (!SAVE_FILE.exists()) {
            try {
                if (SAVE_FILE.getParentFile() != null) {
                    Files.createDirectories(SAVE_FILE.getParentFile().toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            root = new NbtCompound();
        } else {
            try {
                root = readNbtCompound(SAVE_FILE);
            } catch (IOException e) {
                e.printStackTrace();
                root = new NbtCompound();
            }
        }

        NbtCompound idsAndDates = root.getCompound(address);

        idsAndDates.putString(id.toString(),date.toString());

        root.put(address,idsAndDates);

        try {
            writeNbtCompound(root,SAVE_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LocalDateTime loadLeftDate(UUID id, String address) {
        if (!SAVE_FILE.exists()) return null;

        try {
            NbtCompound root = readNbtCompound(SAVE_FILE);
            if(!root.contains(address)){
                return null;
            }
            NbtCompound idsAndDates = root.getCompound(address);
            String time = idsAndDates.getString(id.toString());
            if (!time.isEmpty()){
                return LocalDateTime.parse(time);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull MessageConfig getConfigOrLoad(){
        if (loadedConfig == null){
            try {
                loadedConfig = loadConfig();
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.error("Failed to load config");
                for (StackTraceElement element : e.getStackTrace()) {
                    LOGGER.error(element.toString());
                }
                return defaultConfig;
            }
            createPlaceholdersWithPrefix();
        }
        return loadedConfig;
    }

    private MessageConfig loadConfig() throws IOException, IllegalArgumentException {
        if (!CONFIG_FILE.exists()) {
            // generate config file
            Files.createDirectories(CONFIG_FILE.getParentFile().toPath());
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(defaultConfig);
            Files.writeString(CONFIG_FILE.toPath(), configComment + "\n|\n" + json);
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

    void createPlaceholdersWithPrefix(){
        placeholdersWithPrefix = placeholdersWithoutPrefix.stream().map(string -> loadedConfig.formattingPrefix+string).collect(Collectors.toSet());
    }

    private NbtCompound readNbtCompound(File file) throws IOException {
        try {
            return (NbtCompound) NbtIo.class
                    .getMethod("read", Path.class)
                    .invoke(null, file.toPath());
        } catch (NoSuchMethodException e) {
            try {
                return (NbtCompound) NbtIo.class
                        .getMethod("read", File.class)
                        .invoke(null, file);
            } catch (Exception inner) {
                try {
                        return NbtIo.read(SAVE_FILE.toPath());
                } catch (IOException ex) {
                    throw new RuntimeException("The file will never be read" ,ex);
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to call NbtIo.read(Path)", e);
        }
    }

    private void writeNbtCompound(NbtCompound compound ,File file) throws IOException {
        NbtIo.write(compound, new DataOutputStream(new FileOutputStream(file)));
    }

}
