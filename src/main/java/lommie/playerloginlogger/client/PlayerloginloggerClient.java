package lommie.playerloginlogger.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.time.*;
import java.util.*;

public class PlayerloginloggerClient implements ClientModInitializer {
    private static final File SAVE_FILE = new File("player_login_logger_logs.dat");
    private static final File CONFIG_FILE = new File("config/player_login_logger/messages.json");
    ArrayList<UUID> lastPlayers = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c ->{
            ArrayList<UUID> leftPlayers = new ArrayList<>();
            ArrayList<UUID> joinedPlayers = new ArrayList<>();
            ArrayList<UUID> currentPlayers = new ArrayList<>();
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
            ArrayList<UUID> currentPlayers = new ArrayList<>();
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
        message = message.replace("$(date)",leftDate.getDayOfMonth()+"/"+leftDate.getMonth()+"/"+leftDate.getYear());
        message = message.replace("$(time)",leftDate.getMinute()+" minutes "+leftDate.getHour()+" hours");
        message = message.replace("$(player)", c.world.getPlayerByUuid(id).getDisplayName().getLiteralString());
        Duration since = Duration.between(leftDate,LocalDateTime.now());
        message = message.replace("$(since)",((int) since.toDaysPart())+" days "+since.toHoursPart()+" hours "+since.toMinutesPart()+" minutes");
        return Text.literal(message);
    }

    private void saveLeftDate(UUID id,  String address, LocalDateTime date) {
        NbtCompound root = null;
        if (!SAVE_FILE.exists()) {
            try {
                Files.createDirectories(SAVE_FILE.getParentFile().toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            root = new NbtCompound();
        } else {
            try {
                root = NbtIo.read(SAVE_FILE.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Optional<NbtCompound> idsAndDates = root.getCompound(address);
        if (idsAndDates.isEmpty()){
            root.put(address,new NbtCompound());
            idsAndDates = root.getCompound(address);
        }

        idsAndDates.get().putString(id.toString(),date.toString());


        try {
            NbtIo.write(root,SAVE_FILE.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LocalDateTime loadLeftDate(UUID id, String address) {
        if (!SAVE_FILE.exists()) return LocalDateTime.now();

        try {
            NbtCompound root = NbtIo.read(SAVE_FILE.toPath());
            if(!root.contains(address)){
                return LocalDateTime.now();
            }
            NbtCompound idsAndDates = ((NbtCompound) root.get(address)).getCompound(id.toString()).get();
            String time = idsAndDates.getString(id.toString(),"NOPE");
            if (time.equals("NOPE")){
                return LocalDateTime.now();
            }
            return LocalDateTime.parse(time);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return LocalDateTime.now();
    }

    private String[] loadConfig() {
        String[] defaultConfig = {"Player $(player) joined at $(date) $(time), last seen $(since) ago.","Player $(player) left at $(date) $(time), last seen $(since) ago."};
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

}
