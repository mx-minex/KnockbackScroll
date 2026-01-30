package kr.minex.knockbackscroll;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("효과 만료 체커 일관성 테스트")
class EffectExpirationCheckerConsistencyTest {

    private ServerMock server;
    private KnockbackScroll plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(KnockbackScroll.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("만료된 효과가 hasActiveEffect 호출로 false가 되어도 Attribute는 만료 체커로 정리되어야 한다")
    void 만료_효과_체커로_속성_정리_테스트() {
        PlayerMock player = server.addPlayer("TestPlayer");

        // 지속시간을 0초로 설정해, 활성화 직후 즉시 만료된 상태를 만든다.
        plugin.getConfig().set("settings.duration", 0);
        plugin.saveConfig();
        plugin.getConfigManager().reload();

        plugin.getEffectManager().activateEffect(player);

        // 즉시 만료되므로 활성 효과는 아니어야 한다.
        assertFalse(plugin.getEffectManager().hasActiveEffect(player));

        // 하지만 activateEffect는 AttributeModifier를 적용하므로, 이 시점에서는 속성이 남아있을 수 있다.
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        assertNotNull(attribute);
        assertTrue(attribute.getModifiers().stream().anyMatch(mod -> "knockback_scroll_resistance".equals(mod.getName())));

        // 만료 체커가 1초(20틱)마다 정리하므로, 20틱을 진행해 태스크를 실행시킨다.
        server.getScheduler().performTicks(20);

        assertFalse(attribute.getModifiers().stream().anyMatch(mod -> "knockback_scroll_resistance".equals(mod.getName())),
                "만료 체커 실행 후 넉백 저항 모디파이어가 제거되어야 한다");
    }
}

