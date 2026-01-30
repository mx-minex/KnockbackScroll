package kr.minex.knockbackscroll.config;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import kr.minex.knockbackscroll.KnockbackScroll;
import kr.minex.knockbackscroll.models.ScrollType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 설정 파일 관리 클래스
 * config.yml 로드 및 설정값 캐싱
 */
public class ConfigManager {

    private final KnockbackScroll plugin;
    private FileConfiguration config;

    // 캐시된 설정값
    private int cooldownSeconds;
    private int durationSeconds;

    // 소리 설정
    private Sound activateSound;
    private float activateSoundVolume;
    private float activateSoundPitch;

    public ConfigManager(KnockbackScroll plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * 설정 파일 로드 및 값 캐싱
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        cacheValues();
    }

    /**
     * 설정 리로드
     */
    public void reload() {
        loadConfig();
    }

    /**
     * 설정값 캐싱
     */
    private void cacheValues() {
        // 기본 설정
        cooldownSeconds = config.getInt("settings.cooldown", 60);
        durationSeconds = config.getInt("settings.duration", 10);

        // 소리 설정
        String soundName = config.getString("sounds.activate.type", "BLOCK_ENCHANTMENT_TABLE_USE");
        try {
            activateSound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("알 수 없는 소리 타입: " + soundName + ", 기본값 사용");
            activateSound = Sound.BLOCK_ENCHANTMENT_TABLE_USE;
        }
        activateSoundVolume = (float) config.getDouble("sounds.activate.volume", 1.0);
        activateSoundPitch = (float) config.getDouble("sounds.activate.pitch", 1.0);
    }

    /**
     * 쿨타임 반환 (초)
     */
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * 지속시간 반환 (초)
     */
    public int getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * 활성화 소리 반환
     */
    public Sound getActivateSound() {
        return activateSound;
    }

    /**
     * 활성화 소리 볼륨 반환
     */
    public float getActivateSoundVolume() {
        return activateSoundVolume;
    }

    /**
     * 활성화 소리 피치 반환
     */
    public float getActivateSoundPitch() {
        return activateSoundPitch;
    }

    /**
     * 주문서 표시명 반환
     * @param type 주문서 타입
     * @return 색상 코드가 적용된 표시명
     */
    public String getScrollDisplayName(ScrollType type) {
        String path = "item." + type.getId() + ".display-name";
        String name = config.getString(path, "&e넉백저항 주문서");
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    /**
     * 주문서 설명 반환
     * @param type 주문서 타입
     * @return 색상 코드가 적용된 설명 리스트
     */
    public List<String> getScrollLore(ScrollType type) {
        String path = "item." + type.getId() + ".lore";
        List<String> lore = config.getStringList(path);

        return lore.stream()
                .map(line -> line
                        .replace("{duration}", String.valueOf(durationSeconds))
                        .replace("{cooldown}", String.valueOf(cooldownSeconds)))
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }
}
