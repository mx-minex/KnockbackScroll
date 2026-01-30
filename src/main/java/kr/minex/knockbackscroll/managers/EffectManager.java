package kr.minex.knockbackscroll.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import kr.minex.knockbackscroll.KnockbackScroll;
import kr.minex.knockbackscroll.utils.TimeUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 넉백저항 효과 관리 클래스
 * 효과가 활성화된 플레이어 추적 및 만료 처리
 */
public class EffectManager {

    private final KnockbackScroll plugin;

    // UUID -> 효과 종료 시각 (밀리초)
    private final Map<UUID, Long> activeEffects = new ConcurrentHashMap<>();

    // 효과 만료 체크용 스케줄러 태스크
    private BukkitTask expirationTask;

    public EffectManager(KnockbackScroll plugin) {
        this.plugin = plugin;
    }

    /**
     * 넉백저항 효과 활성화
     * @param player 대상 플레이어
     */
    public void activateEffect(Player player) {
        int durationSeconds = plugin.getConfigManager().getDurationSeconds();
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        activeEffects.put(player.getUniqueId(), endTime);

        // 넉백 저항 속성 적용
        plugin.getKnockbackListener().applyKnockbackResistance(player);
    }

    /**
     * 효과가 활성화되어 있는지 확인
     * @param player 확인할 플레이어
     * @return 효과 활성화 중이면 true
     */
    public boolean hasActiveEffect(Player player) {
        Long endTime = activeEffects.get(player.getUniqueId());
        if (endTime == null) {
            return false;
        }
        if (System.currentTimeMillis() >= endTime) {
            activeEffects.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    /**
     * 효과 비활성화
     * @param player 대상 플레이어
     */
    public void deactivateEffect(Player player) {
        activeEffects.remove(player.getUniqueId());
        plugin.getKnockbackListener().removeKnockbackResistance(player);
    }

    /**
     * 남은 효과 시간 반환 (초 단위, 올림)
     * @param player 확인할 플레이어
     * @return 남은 시간 (초), 효과가 없으면 0
     */
    public long getRemainingEffectTime(Player player) {
        Long endTime = activeEffects.get(player.getUniqueId());
        if (endTime == null) {
            return 0;
        }
        return TimeUtils.getRemainingSeconds(endTime);
    }

    /**
     * 효과 만료 체크 스케줄러 시작
     * 1초마다 실행하여 만료된 효과 정리 및 알림
     */
    public void startExpirationChecker() {
        expirationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            Iterator<Map.Entry<UUID, Long>> iterator = activeEffects.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Long> entry = iterator.next();

                if (now >= entry.getValue()) {
                    iterator.remove();

                    // 효과 종료 알림 및 넉백 저항 제거
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        plugin.getKnockbackListener().removeKnockbackResistance(player);
                        plugin.getMessageManager().send(player, "effect.expired");
                    }
                }
            }
        }, 20L, 20L); // 1초마다 실행
    }

    /**
     * 스케줄러 정지 및 정리
     */
    public void shutdown() {
        if (expirationTask != null) {
            expirationTask.cancel();
            expirationTask = null;
        }
        activeEffects.clear();
    }

    /**
     * 플레이어 퇴장 시 데이터 정리
     * @param uuid 플레이어 UUID
     */
    public void cleanup(UUID uuid) {
        activeEffects.remove(uuid);
    }

    /**
     * 모든 효과 데이터 정리
     */
    public void clear() {
        activeEffects.clear();
    }
}
