package kr.minex.knockbackscroll.managers;

import org.bukkit.entity.Player;
import kr.minex.knockbackscroll.KnockbackScroll;
import kr.minex.knockbackscroll.utils.TimeUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 플레이어별 쿨타임 관리 클래스
 * 메모리 기반으로 관리 (서버 재시작 시 초기화)
 */
public class CooldownManager {

    private final KnockbackScroll plugin;

    // UUID -> 쿨타임 종료 시각 (밀리초)
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public CooldownManager(KnockbackScroll plugin) {
        this.plugin = plugin;
    }

    /**
     * 쿨타임 설정
     * @param player 대상 플레이어
     */
    public void setCooldown(Player player) {
        int cooldownSeconds = plugin.getConfigManager().getCooldownSeconds();
        long endTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        cooldowns.put(player.getUniqueId(), endTime);
    }

    /**
     * 쿨타임 중인지 확인
     * @param player 확인할 플레이어
     * @return 쿨타임 중이면 true
     */
    public boolean isOnCooldown(Player player) {
        Long endTime = cooldowns.get(player.getUniqueId());
        if (endTime == null) {
            return false;
        }
        if (System.currentTimeMillis() >= endTime) {
            cooldowns.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    /**
     * 남은 쿨타임 반환 (초 단위, 올림)
     * @param player 확인할 플레이어
     * @return 남은 시간 (초), 쿨타임이 아니면 0
     */
    public long getRemainingCooldown(Player player) {
        Long endTime = cooldowns.get(player.getUniqueId());
        if (endTime == null) {
            return 0;
        }
        return TimeUtils.getRemainingSeconds(endTime);
    }

    /**
     * 쿨타임 해제
     * @param player 대상 플레이어
     */
    public void removeCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    /**
     * 플레이어 퇴장 시 데이터 정리
     * @param uuid 플레이어 UUID
     */
    public void cleanup(UUID uuid) {
        cooldowns.remove(uuid);
    }

    /**
     * 모든 쿨타임 데이터 정리
     */
    public void clear() {
        cooldowns.clear();
    }
}
