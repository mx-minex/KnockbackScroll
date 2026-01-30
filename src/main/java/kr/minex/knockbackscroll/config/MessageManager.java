package kr.minex.knockbackscroll.config;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import kr.minex.knockbackscroll.KnockbackScroll;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;

/**
 * 메시지 관리 클래스
 * messages.yml 로드 및 메시지 전송
 */
public class MessageManager {

    private final KnockbackScroll plugin;
    private FileConfiguration messages;
    private String prefix;

    public MessageManager(KnockbackScroll plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    /**
     * 메시지 파일 로드
     */
    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // 파일이 없으면 리소스에서 복사
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // 기본값과 병합 (새로운 키 추가 대응)
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaultMessages);
        }

        // 접두사 캐싱
        prefix = colorize(messages.getString("messages.prefix", "&8[&6넉백저항&8] &r"));
    }

    /**
     * 메시지 리로드
     */
    public void reload() {
        loadMessages();
    }

    /**
     * 메시지 전송 (접두사 포함)
     * @param sender 수신자
     * @param key 메시지 키 (messages. 이후 경로)
     * @param placeholders 플레이스홀더 (키, 값 쌍)
     */
    public void send(CommandSender sender, String key, Object... placeholders) {
        String message = getMessage(key, placeholders);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(prefix + message);
        }
    }

    /**
     * 메시지 전송 (접두사 없음)
     * @param sender 수신자
     * @param key 메시지 키
     * @param placeholders 플레이스홀더
     */
    public void sendRaw(CommandSender sender, String key, Object... placeholders) {
        String message = getMessage(key, placeholders);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    /**
     * 액션바 메시지 전송
     * @param player 대상 플레이어
     * @param key 메시지 키
     * @param placeholders 플레이스홀더
     */
    public void sendActionBar(Player player, String key, Object... placeholders) {
        String message = getMessage(key, placeholders);
        if (message != null && !message.isEmpty()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }

    /**
     * 원본 메시지 반환 (포맷팅 적용)
     * @param key 메시지 키
     * @param placeholders 플레이스홀더 (키, 값 쌍)
     * @return 포맷팅된 메시지
     */
    public String getMessage(String key, Object... placeholders) {
        String message = messages.getString("messages." + key);

        if (message == null) {
            plugin.getLogger().warning("메시지 키를 찾을 수 없음: " + key);
            return null;
        }

        // 플레이스홀더 치환
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = "{" + placeholders[i] + "}";
            String value = formatValue(placeholders[i + 1]);
            message = message.replace(placeholder, value);
        }

        return colorize(message);
    }

    /**
     * 접두사 반환
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 값 포맷팅 (숫자는 천단위 구분)
     */
    private String formatValue(Object value) {
        if (value instanceof Number) {
            return NumberFormat.getInstance().format(value);
        }
        return String.valueOf(value);
    }

    /**
     * 색상 코드 변환
     */
    private String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
