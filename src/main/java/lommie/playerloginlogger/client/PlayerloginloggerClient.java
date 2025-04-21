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
import net.minecraft.text.Text;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;

public class PlayerloginloggerClient implements ClientModInitializer {
    final String[] defaultConfig = {"Player $(player) joined at $(date) $(time), last seen $(since) ago.\n($(rawTime), $(rawSince))","Player $(player) left at $(date) $(time), last seen $(since) ago.\n($(rawTime), $(rawSince))","red"};
    private static final File SAVE_FILE = new File("player_login_logger_logs.dat");
    private static final File CONFIG_FILE = new File("config/player_login_logger/messages.json");
    Set<UUID> lastPlayers = new HashSet<>();

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
        });
    }

    private void joinMessage(UUID id, MinecraftClient c) {
        String message = loadConfig()[0];
        c.player.sendMessage(replaceMessage(id, c, message),false);
    }

    private void leaveMessage(UUID id, MinecraftClient c) {
        String message = loadConfig()[1];
        c.player.sendMessage(replaceMessage(id, c, message),false);
    }

    private MutableText replaceMessage(UUID id, MinecraftClient c, String message) {
        LocalDateTime leftDate = loadLeftDate(id, c.getCurrentServerEntry().address);
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
            text.append(Text.literal(splitMessage[i]));//.setStyle(Style.EMPTY.withColor(TextColor.parse(loadConfig()[2]))));
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

    private String[] loadConfig() {
        if (!CONFIG_FILE.exists()) {
            try {
                Files.createDirectories(CONFIG_FILE.getParentFile().toPath());
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(defaultConfig);
                Files.writeString(CONFIG_FILE.toPath(), json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            return new Gson().fromJson(reader, String[].class);
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
