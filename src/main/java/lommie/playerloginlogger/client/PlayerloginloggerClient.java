package lommie.playerloginlogger.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;

public class PlayerloginloggerClient implements ClientModInitializer {
    final Set<String> noPrefixFormatting;
    {
        ArrayList<String> noPrefixFormattingChars = new ArrayList<>();
        for (Formatting formatting : Formatting.values()){
            noPrefixFormattingChars.add(formatting.getCode() + "");
        }
        noPrefixFormattingChars.addAll(List.of(
                "(endOfTEXT)",
                "(player)",
                "(month)",
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
                "(raw-since)"));
        noPrefixFormatting = Set.copyOf(noPrefixFormattingChars);
    }
    final private String configComment = "";
    final MessageConfig defaultConfig = new MessageConfig(
            new MessageConfig.MessageEntry("$(player) joined at $(date) $(time),\nlast seen $(since) ago.","#004f00"),
            new MessageConfig.MessageEntry("$(player) seen for the first time at $(date) $(time)","#00ff00"),
            new MessageConfig.MessageEntry("Last joined this server $(since) ago","#ffffff")
    );
    private static final File SAVE_FILE = new File("player_login_logger_logs.dat");
    private static final File CONFIG_FILE = new File("config/player_login_logger/messages.json");
    private static Set<UUID> lastPlayers = new HashSet<>();

    // Config class for messages with text and color
    private static class MessageConfig {
        MessageConfig(){
        }

        MessageConfig(MessageEntry join_message, MessageEntry first_time_message, MessageEntry welcome_back_message, Optional<MessageEntry> leave_message, char formattingPrefix){
            this.join_message = join_message;
            this.first_time_message = first_time_message;
            this.leave_message = leave_message;
            this.welcome_back_message = welcome_back_message;
            this.formattingPrefix = formattingPrefix;
        }

        MessageConfig(MessageEntry join_message, MessageEntry first_time_message, MessageEntry welcome_back_message, Optional<MessageEntry> leave_message){
            this.join_message = join_message;
            this.first_time_message = first_time_message;
            this.leave_message = leave_message;
            this.welcome_back_message = welcome_back_message;
        }

        MessageConfig(MessageEntry join_message, MessageEntry welcome_back_message, MessageEntry first_time_message){
            this(join_message, first_time_message, welcome_back_message, Optional.empty());
        }

        char formattingPrefix = '$';
        MessageEntry join_message = new MessageEntry("","");
        MessageEntry first_time_message = new MessageEntry("","");
        Optional<MessageEntry> leave_message = Optional.empty();
        MessageEntry welcome_back_message = new MessageEntry("","");

        static class MessageEntry {
            String text;
            String color;

            public MessageEntry(String text, String color) {
                this.text = text;
                this.color = color;
            }
        }

        static class AbleToBeTurnedIntoJson{
            char formattingPrefix = '$';
            private MessageEntry join_message = new MessageEntry("","");
            private MessageEntry first_time_message = new MessageEntry("","");
            private MessageEntry leave_message = new MessageEntry("","");
            private boolean no_leave_message = false;
            private MessageEntry welcome_back_message = new MessageEntry("","");

            AbleToBeTurnedIntoJson(MessageEntry join_message, MessageEntry first_time_message, MessageEntry welcome_back_message, Optional<MessageEntry> leave_message){
                this.join_message = join_message;
                this.first_time_message = first_time_message;
                this.welcome_back_message = welcome_back_message;
                if (leave_message.isPresent()){
                    this.leave_message = leave_message.get();
                } else {
                    this.no_leave_message = true;
                }
            }

            AbleToBeTurnedIntoJson(MessageConfig config){
                this(config.join_message,config.first_time_message,config.welcome_back_message,config.leave_message);
            }

            MessageConfig toNormalConfig(){
                return new MessageConfig(join_message,first_time_message,welcome_back_message,
                        no_leave_message?Optional.empty(): Optional.ofNullable(leave_message), formattingPrefix);
            }
        }
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c ->{
            if (c.world == null) return;
            if (c.getCurrentServerEntry() == null) return;
            Set<UUID> leftPlayers = new HashSet<>();
            Set<UUID> joinedPlayers = new HashSet<>();
            Set<UUID> currentPlayers = new HashSet<>();
            c.world.getPlayers().forEach(
                    i -> currentPlayers.add(i.getUuid())
            );
            if (!lastPlayers.contains(c.player.getUuid())){
                lastPlayers = currentPlayers;
                for (UUID id : currentPlayers){
                    joinMessage(id, c);
                }
            }
            for (UUID id : lastPlayers){
                if (!currentPlayers.contains(id)){
                    leftPlayers.add(id);
                }
            }
            for (UUID id : currentPlayers){
                if (!lastPlayers.contains(id)){
                    joinedPlayers.add(id);
                }
            }
            for (UUID id : leftPlayers){
                saveLeftDate(id, c.getCurrentServerEntry().address, LocalDateTime.now());
                leaveMessage(id, c);
            }
            for (UUID id : joinedPlayers){
                joinMessage(id,c);
            }
            lastPlayers = currentPlayers;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((p,c) -> {
            if (c.getCurrentServerEntry() == null) return;
            Set<UUID> currentPlayers = new HashSet<>();
            c.world.getPlayers().forEach(
                    i -> currentPlayers.add(i.getUuid())
            );
            for (UUID id : currentPlayers){
                saveLeftDate(id, c.getCurrentServerEntry().address, LocalDateTime.now());
                leaveMessage(id, c);
            }
            lastPlayers = new HashSet<>();
        });
    }

    private void joinMessage(UUID id, MinecraftClient c) {
        MessageConfig config = loadConfig();
        MessageConfig.MessageEntry message = id == c.player.getUuid() ? config.welcome_back_message : loadLeftDate(id, c.getCurrentServerEntry().address) == null ? config.first_time_message : config.join_message;
        c.player.sendMessage(replaceMessage(id, c, message, config.formattingPrefix),false);
    }

    private void leaveMessage(UUID id, MinecraftClient c) {
        MessageConfig config = loadConfig();
        Optional<MessageConfig.MessageEntry> message = config.leave_message;
        message.ifPresent(messageEntry -> c.player.sendMessage(replaceMessage(id, c, messageEntry, config.formattingPrefix), false));
    }

    private MutableText replaceMessage(UUID id, MinecraftClient c, MessageConfig.MessageEntry messageEntry, char formattingCharPrefix) {
        LocalDateTime leftDate = loadLeftDate(id, c.getCurrentServerEntry().address);
        String message = Objects.requireNonNullElse(messageEntry.text, "$cERROR: PlayerLoginLoader");
        Duration since =  leftDate == null ? Duration.ZERO :  Duration.between(leftDate,LocalDateTime.now());

        MutableText[] texts = mapMessageToFormatting(id,c,leftDate,since,message, noPrefixFormatting ,formattingCharPrefix);
        MutableText text = texts[0];
        for (int i = 1; i < texts.length; i++) {
            text.append(texts[i]);
        }
        return text;
    }

    MutableText[] mapMessageToFormatting(UUID playerId,MinecraftClient client,LocalDateTime leftDate, Duration since,String message, Set<String> noPrefixFormattingChars, char formattingCharPrefix){
        // formattingCharPrefix = $
        String[] formattingCharsNotSet = new String[noPrefixFormattingChars.size()];
        for (int j = 0; j < noPrefixFormattingChars.size(); j++) {
            formattingCharsNotSet[j] = formattingCharPrefix + noPrefixFormattingChars.toArray(String[]::new)[j];
        }
        Set<String> formattingChars = Set.copyOf(List.of(formattingCharsNotSet));

        ArrayList<MutableText> texts = new ArrayList<>();

        ArrayList<String> lastUsedFormattingChar = new ArrayList<>();
        String nextText = "";
        String nextFormatting = "";
        int charsToGoBackIfNotFormating = 0;
        boolean addedToNextFormatting = false;
        for (int i = 0; i < message.length(); i++) {
            if (formattingChars.contains(nextFormatting)){
                lastUsedFormattingChar.add(nextFormatting);
                charsToGoBackIfNotFormating = 0;
                nextFormatting = "";
            }

            char c = message.toCharArray()[i];
            for (String formatting : formattingChars){
                if (formatting.length() > charsToGoBackIfNotFormating){
                    nextFormatting += c;
                    charsToGoBackIfNotFormating++;
                    addedToNextFormatting = true;
                }
            }
            if (!addedToNextFormatting){
                if (charsToGoBackIfNotFormating > 0) {
                    //just finished adding a formatting
                    //only had the start of a formatting char
                    //add formatting to that
                    nextText += nextFormatting;
                    nextFormatting = "";
                    charsToGoBackIfNotFormating = 0;


                    // adding a new text
                    lastUsedFormattingChar = removeIncompatibleFormattingChars(lastUsedFormattingChar);

                    MutableText newText = ConstructNewFormattedText(playerId, client, leftDate, since, nextText, lastUsedFormattingChar);

                    nextText = "";

                    texts.add(newText);

                    lastUsedFormattingChar = new ArrayList<>(lastUsedFormattingChar.stream().filter(charInLastUsedFormattingChar -> charInLastUsedFormattingChar.length() == 1).toList());
                    lastUsedFormattingChar.replaceAll( charInLastUsedFormattingChar -> formattingCharPrefix + charInLastUsedFormattingChar);

                }

                nextText += c;
            }

            addedToNextFormatting = false;
        }
        return texts.toArray(MutableText[]::new);
    }

    private static MutableText ConstructNewFormattedText(UUID playerId, MinecraftClient client, LocalDateTime leftDate, Duration since, String nextText, ArrayList<String> lastUsedFormattingChar) {
        MutableText newText = Text.literal(nextText);
        ArrayList<Formatting> realFormatting = new ArrayList<>();
        String replacementFormatting = "";
        for (String prefixRemovedItem : lastUsedFormattingChar){
            if (prefixRemovedItem.length() > 1){
                replacementFormatting = prefixRemovedItem;
            } else {
                realFormatting.add(Formatting.byCode(prefixRemovedItem.charAt(0)));
            }
        }

        for (Formatting realFormattingItem : realFormatting){
            newText = newText.formatted(realFormattingItem);
        }

        Text addOn = switch (replacementFormatting){
            case "(player)" -> client.world.getPlayerByUuid(playerId) == null ? Text.literal("{"+ playerId.toString()+"}").setStyle(newText.getStyle()) : client.world.getPlayerByUuid(playerId).getDisplayName();
            case "(month)" -> leftDate == null ? Text.literal("{null}").setStyle(newText.getStyle()) : Text.literal(leftDate.getMonthValue()+"").setStyle(newText.getStyle());
            case "(day)" -> leftDate == null ? Text.literal("{null}").setStyle(newText.getStyle()) : Text.literal(leftDate.getDayOfMonth()+"").setStyle(newText.getStyle());
            case "(year)" -> leftDate == null ? Text.literal("{null}").setStyle(newText.getStyle()) : Text.literal(leftDate.getYear()+"").setStyle(newText.getStyle());
            case "(minute)" -> leftDate == null ? Text.literal("{null}").setStyle(newText.getStyle()) : Text.literal(leftDate.getMinute()+"").setStyle(newText.getStyle());
            case "(hour)" -> leftDate == null ? Text.literal("{null}").setStyle(newText.getStyle()) : Text.literal(leftDate.getHour()+"").setStyle(newText.getStyle());
            case "(second)" -> leftDate == null ? Text.literal("{null}").setStyle(newText.getStyle()) : Text.literal(leftDate.getSecond()+"").setStyle(newText.getStyle());
            case "(raw-time)" -> leftDate == null ? Text.literal("{null}").setStyle(newText.getStyle()) : Text.literal(leftDate.toString()).setStyle(newText.getStyle());
            case "(since-day)" -> Text.literal(((int) since.toDaysPart())+"").setStyle(newText.getStyle());
            case "(since-minute)" -> Text.literal(since.toMinutesPart()+"").setStyle(newText.getStyle());
            case "(since-hour)" -> Text.literal(since.toHoursPart()+"").setStyle(newText.getStyle());
            case "(since-second)" -> Text.literal(since.toSecondsPart()+"").setStyle(newText.getStyle());
            case "(raw-since)" -> Text.literal(since.toString()).setStyle(newText.getStyle());
            default -> null;
        };

        if (addOn != null){
            newText.append(addOn);
        }
        return newText;
    }

    private ArrayList<String> removeIncompatibleFormattingChars(ArrayList<String> lastUsedFormattingChar) {
        ArrayList<String> prefixRemoved = new ArrayList<>();
        for (String prefixedFormattingChar : lastUsedFormattingChar){
            prefixRemoved.add(prefixedFormattingChar.substring(1));
        }
        ArrayList<Formatting> realFormatting = new ArrayList<>();
        String replacementFormatting = "";
        for (String prefixRemovedItem : prefixRemoved){
            if (prefixRemovedItem.length() > 1){
                replacementFormatting = prefixRemovedItem;
            } else {
                realFormatting.add(Formatting.byCode(prefixRemovedItem.charAt(0)));
            }
        }
        Collections.reverse(realFormatting);
        ArrayList<Formatting> addedRealFormatting = new ArrayList<>();
        for (Formatting theFormatting : realFormatting){
            if (theFormatting == Formatting.RESET) {
                break;
            }
            if (theFormatting.isColor()) {
                boolean addedColor = false;
                for (Formatting addedRealFormattingItem : addedRealFormatting){
                    if (addedRealFormattingItem.isColor()){
                        addedColor = true;
                    }
                }
                if (!addedColor){
                    addedRealFormatting.add(theFormatting);
                }
            } else if (theFormatting.isModifier() && !addedRealFormatting.contains(theFormatting)) {
                addedRealFormatting.add(theFormatting);
            }
        }
        ArrayList<String> butArentIGoingToTurnThisBackIntoFormattingSoonQuestionMark = new ArrayList<>();
        for (Formatting addedRealFormattingItem : addedRealFormatting){
            butArentIGoingToTurnThisBackIntoFormattingSoonQuestionMark.add(addedRealFormattingItem.getCode() + "");
        }
        butArentIGoingToTurnThisBackIntoFormattingSoonQuestionMark.add(replacementFormatting);
        return butArentIGoingToTurnThisBackIntoFormattingSoonQuestionMark;
    }

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

    private MessageConfig loadConfig() {
        if (!CONFIG_FILE.exists()) {
            try {
                Files.createDirectories(CONFIG_FILE.getParentFile().toPath());
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(new MessageConfig.AbleToBeTurnedIntoJson(defaultConfig));
                Files.writeString(CONFIG_FILE.toPath(), configComment + "\n$" + json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            int c;
            while ((c = reader.read()) != -1 && c != '$') {}
            if (c == -1) {
                throw new IllegalArgumentException("No '$' found in the input");
            }

            return (new Gson().fromJson(reader, MessageConfig.AbleToBeTurnedIntoJson.class)).toNormalConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return defaultConfig;
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
