package locked_villager_trades.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import locked_villager_trades.Locked_villager_trades;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and saves mod configuration from config/locked_villager_trades.json.
 */
public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MIN_TRADE_SET_COUNT = 1;
    private static final int MAX_TRADE_SET_COUNT = 20;
    private static final int DEFAULT_TRADE_SET_COUNT = 4;

    private int tradeSetCount = DEFAULT_TRADE_SET_COUNT;

    public int getTradeSetCount() {
        return tradeSetCount;
    }

    public void setTradeSetCount(int value) {
        this.tradeSetCount = Math.max(MIN_TRADE_SET_COUNT, Math.min(MAX_TRADE_SET_COUNT, value));
    }

    public static ModConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("locked_villager_trades.json");
        ModConfig config = new ModConfig();

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                ConfigData data = GSON.fromJson(json, ConfigData.class);
                if (data != null && data.trade_set_count != null) {
                    config.setTradeSetCount(data.trade_set_count);
                }
            } catch (IOException e) {
                Locked_villager_trades.LOGGER.warn("Failed to load config, using defaults: {}", e.getMessage());
            }
        } else {
            config.save();
        }

        return config;
    }

    public void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("locked_villager_trades.json");
        try {
            Files.createDirectories(configPath.getParent());
            ConfigData data = new ConfigData(tradeSetCount);
            Files.writeString(configPath, GSON.toJson(data));
        } catch (IOException e) {
            Locked_villager_trades.LOGGER.warn("Failed to save config: {}", e.getMessage());
        }
    }

    private static class ConfigData {
        Integer trade_set_count;

        ConfigData() {}

        ConfigData(int tradeSetCount) {
            this.trade_set_count = tradeSetCount;
        }
    }
}
