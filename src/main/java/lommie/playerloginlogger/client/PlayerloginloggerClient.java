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

import java.io.*;
import java.nio.file.Files;
import java.time.*;
import java.util.*;

public class PlayerloginloggerClient implements ClientModInitializer {
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

        MessageConfig(MessageEntry join_message, MessageEntry first_time_message, MessageEntry welcome_back_message, Optional<MessageEntry> leave_message){
            this.join_message = join_message;
            this.first_time_message = first_time_message;
            this.leave_message = leave_message;
        }

        MessageConfig(MessageEntry join_message, MessageEntry welcome_back_message, MessageEntry first_time_message){
            this(join_message, first_time_message, welcome_back_message, Optional.empty());
        }

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
                        no_leave_message?Optional.empty(): Optional.ofNullable(leave_message));
            }
        }
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c ->{
            if (c.world == null) return;
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
        MessageConfig.MessageEntry message = id == c.player.getUuid() ? loadConfig().welcome_back_message : loadLeftDate(id, c.getCurrentServerEntry().address) == null ? loadConfig().first_time_message : loadConfig().join_message;
        c.player.sendMessage(replaceMessage(id, c, message),false);
    }

    private void leaveMessage(UUID id, MinecraftClient c) {
        Optional<MessageConfig.MessageEntry> message = loadConfig().leave_message;
        message.ifPresent(messageEntry -> c.player.sendMessage(replaceMessage(id, c, messageEntry), false));
    }

    private MutableText replaceMessage(UUID id, MinecraftClient c, MessageConfig.MessageEntry messageEntry) {
        LocalDateTime leftDate = loadLeftDate(id, c.getCurrentServerEntry().address);
        String message;
        message = Objects.requireNonNullElse(messageEntry.text, "ERROR: PlayerLoginLoader");
        message = message.replace("$(date)", leftDate == null ? "{null}" : leftDate.getDayOfMonth()+"/"+leftDate.getMonthValue()+"/"+leftDate.getYear());
        message = message.replace("$(time)", leftDate == null ? "{null}" : leftDate.getMinute()+" minutes "+leftDate.getHour()+" hours");
        message = message.replace("$(rawTime)", leftDate == null ? "{null}" : leftDate.toString());
        Duration since =  leftDate == null ? Duration.ZERO :  Duration.between(leftDate,LocalDateTime.now());
        message = message.replace("$(since)",((int) since.toDaysPart())+" days "+since.toHoursPart()+" hours "+since.toMinutesPart()+" minutes "+since.toSecondsPart()+" seconds");
        message = message.replace("$(rawSince)", since.toString());
        String[] splitMessage = message.split("\\$\\(player\\)", -1);
        Text playerName = null;
        if (c.world.getPlayerByUuid(id) != null) {
            playerName = c.world.getPlayerByUuid(id).getDisplayName();
        } else {
            playerName = Text.of("{"+id.toString()+"}");
        }
        MutableText text = Text.empty();
        for (int i = 0; i < splitMessage.length; i++) {
            try {
                text.append(Text.literal(splitMessage[i]).setStyle(Style.EMPTY.withColor(TextColor.parse(messageEntry.color).getOrThrow())));
            } catch (IllegalStateException e) {
                text.append(Text.literal(splitMessage[i]));
            }
            if (i < splitMessage.length - 1) {
                text.append(playerName.copy());
            }
        }
        return text;
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
                Files.writeString(CONFIG_FILE.toPath(), json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            return (new Gson().fromJson(reader, MessageConfig.AbleToBeTurnedIntoJson.class)).toNormalConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return defaultConfig;
    }

    private NbtCompound readNbtCompound(File file) throws IOException {
        try {
            return (NbtCompound) NbtIo.class
                    .getMethod("read", java.nio.file.Path.class)
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
