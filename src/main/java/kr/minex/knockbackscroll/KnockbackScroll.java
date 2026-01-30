package kr.minex.knockbackscroll;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import kr.minex.knockbackscroll.commands.ScrollCommand;
import kr.minex.knockbackscroll.config.ConfigManager;
import kr.minex.knockbackscroll.config.MessageManager;
import kr.minex.knockbackscroll.listeners.KnockbackListener;
import kr.minex.knockbackscroll.listeners.ScrollUseListener;
import kr.minex.knockbackscroll.managers.CooldownManager;
import kr.minex.knockbackscroll.managers.EffectManager;
import kr.minex.knockbackscroll.managers.ScrollManager;

/**
 * 넉백저항 주문서 플러그인 메인 클래스
 * 플러그인의 생명주기를 관리하고 각 컴포넌트를 초기화합니다.
 */
public class KnockbackScroll extends JavaPlugin {

    private static KnockbackScroll instance;

    // 설정 관리
    private ConfigManager configManager;
    private MessageManager messageManager;

    // 핵심 매니저
    private ScrollManager scrollManager;
    private CooldownManager cooldownManager;
    private EffectManager effectManager;

    // 리스너
    private KnockbackListener knockbackListener;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 설정 파일 로드
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);

        // 2. 매니저 초기화
        scrollManager = new ScrollManager(this);
        cooldownManager = new CooldownManager(this);
        effectManager = new EffectManager(this);

        // 3. 효과 만료 체크 스케줄러 시작
        effectManager.startExpirationChecker();

        // 4. 이벤트 리스너 등록
        registerListeners();

        // 5. 명령어 등록
        registerCommands();

        // 6. 리로드 감지 - 이미 접속 중인 플레이어 처리
        if (Bukkit.getOnlinePlayers().size() > 0) {
            getLogger().info("플러그인 리로드 감지됨. 기존 플레이어 데이터는 초기화됩니다.");
            cleanupOnlinePlayers();
        }

        // 시작 메시지 출력
        printStartupMessage();
    }

    /**
     * 플러그인 시작 메시지 출력
     */
    private void printStartupMessage() {
        getLogger().info("========================================");
        getLogger().info("  KnockbackScroll Plugin v" + getDescription().getVersion());
        getLogger().info("  Created by Junseo5");
        getLogger().info("  Bug reports: Discord - Junseo5#3213");
        getLogger().info("========================================");
        getLogger().info("Plugin has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // 온라인 플레이어에 남아있는 AttributeModifier 제거 (리로드/플러그인 제거 대비)
        cleanupOnlinePlayers();

        // 1. 스케줄러 정리
        Bukkit.getScheduler().cancelTasks(this);

        // 2. 이벤트 리스너 해제
        HandlerList.unregisterAll(this);

        // 3. 매니저 및 리스너 정리
        if (effectManager != null) {
            effectManager.shutdown();
        }
        if (cooldownManager != null) {
            cooldownManager.clear();
        }
        if (knockbackListener != null) {
            knockbackListener.clear();
        }

        // 4. static 참조 제거 (메모리 누수 방지)
        instance = null;

        getLogger().info("넉백저항 주문서 플러그인이 비활성화되었습니다.");
    }

    /**
     * 리로드/비정상 종료 등으로 남아있는 넉백 저항 속성을 정리합니다.
     *
     * 주의: Attribute 조작은 메인 스레드에서 수행되어야 합니다.
     */
    private void cleanupOnlinePlayers() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(this, this::cleanupOnlinePlayers);
            return;
        }

        if (knockbackListener == null) {
            return;
        }

        Bukkit.getOnlinePlayers().forEach(player -> {
            try {
                knockbackListener.removeKnockbackResistance(player);
            } catch (Exception e) {
                getLogger().warning("플레이어 넉백 저항 정리 실패: " + player.getName() + " - " + e.getMessage());
            }
        });
    }

    /**
     * 이벤트 리스너 등록
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ScrollUseListener(this), this);
        knockbackListener = new KnockbackListener(this);
        getServer().getPluginManager().registerEvents(knockbackListener, this);
    }

    /**
     * 명령어 등록
     */
    private void registerCommands() {
        PluginCommand command = getCommand("넉백저항주문서");
        if (command != null) {
            ScrollCommand scrollCommand = new ScrollCommand(this);
            command.setExecutor(scrollCommand);
            command.setTabCompleter(scrollCommand);
        } else {
            getLogger().severe("명령어 '넉백저항주문서'를 등록할 수 없습니다. plugin.yml을 확인해주세요.");
        }
    }

    /**
     * 플러그인 인스턴스 반환
     */
    public static KnockbackScroll getInstance() {
        return instance;
    }

    /**
     * 설정 관리자 반환
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * 메시지 관리자 반환
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * 주문서 관리자 반환
     */
    public ScrollManager getScrollManager() {
        return scrollManager;
    }

    /**
     * 쿨타임 관리자 반환
     */
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    /**
     * 효과 관리자 반환
     */
    public EffectManager getEffectManager() {
        return effectManager;
    }

    /**
     * 넉백 리스너 반환
     */
    public KnockbackListener getKnockbackListener() {
        return knockbackListener;
    }
}
