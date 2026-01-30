package kr.minex.knockbackscroll.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import kr.minex.knockbackscroll.KnockbackScroll;
import kr.minex.knockbackscroll.models.ScrollType;

import java.util.ArrayList;
import java.util.List;

/**
 * /넉백저항주문서 명령어 핸들러
 */
public class ScrollCommand implements CommandExecutor, TabCompleter {

    private final KnockbackScroll plugin;

    public ScrollCommand(KnockbackScroll plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 인자가 없으면 도움말 표시
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "생성", "create", "give" -> handleCreate(sender, args);
            case "지정", "set", "convert" -> handleConvert(sender, args);
            case "리로드", "reload" -> handleReload(sender);
            case "도움말", "help" -> sendHelpMessage(sender);
            default -> {
                plugin.getMessageManager().send(sender, "command.usage");
            }
        }

        return true;
    }

    /**
     * 주문서 생성 명령어 처리
     */
    private void handleCreate(CommandSender sender, String[] args) {
        // 플레이어 전용 명령어
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "command.player-only");
            return;
        }

        // 권한 체크
        if (!player.hasPermission("knockbackscroll.create")) {
            plugin.getMessageManager().send(player, "command.no-permission");
            return;
        }

        // 타입 인자 확인
        if (args.length < 2) {
            plugin.getMessageManager().send(player, "command.usage");
            return;
        }

        // 타입 파싱
        String typeArg = args[1];
        ScrollType type = parseScrollType(typeArg);

        if (type == null) {
            plugin.getMessageManager().send(player, "command.invalid-type");
            return;
        }

        // 주문서 생성 및 지급
        ItemStack scroll = plugin.getScrollManager().createScroll(type);

        // 인벤토리에 추가 (가득 차면 발밑에 드롭)
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), scroll);
        } else {
            player.getInventory().addItem(scroll);
        }

        plugin.getMessageManager().send(player, "command.scroll-given", "type", type.getDisplayName());
    }

    /**
     * 손에 든 아이템을 주문서로 변환 명령어 처리
     */
    private void handleConvert(CommandSender sender, String[] args) {
        // 플레이어 전용 명령어
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "command.player-only");
            return;
        }

        // 권한 체크
        if (!player.hasPermission("knockbackscroll.create")) {
            plugin.getMessageManager().send(player, "command.no-permission");
            return;
        }

        // 타입 인자 확인
        if (args.length < 2) {
            plugin.getMessageManager().send(player, "command.usage");
            return;
        }

        // 타입 파싱
        String typeArg = args[1];
        ScrollType type = parseScrollType(typeArg);

        if (type == null) {
            plugin.getMessageManager().send(player, "command.invalid-type");
            return;
        }

        // 손에 든 아이템 확인
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType().isAir()) {
            plugin.getMessageManager().send(player, "command.no-item-in-hand");
            return;
        }

        // 주문서로 변환
        if (!plugin.getScrollManager().convertToScroll(handItem, type)) {
            plugin.getMessageManager().send(player, "command.convert-failed");
            return;
        }

        plugin.getMessageManager().send(player, "command.scroll-converted", "type", type.getDisplayName());
    }

    /**
     * 설정 리로드 명령어 처리
     */
    private void handleReload(CommandSender sender) {
        // 권한 체크
        if (!sender.hasPermission("knockbackscroll.reload")) {
            plugin.getMessageManager().send(sender, "command.no-permission");
            return;
        }

        // 설정 리로드
        plugin.getConfigManager().reload();
        plugin.getMessageManager().reload();

        plugin.getMessageManager().send(sender, "command.reload-success");
    }

    /**
     * 도움말 메시지 전송
     */
    private void sendHelpMessage(CommandSender sender) {
        plugin.getMessageManager().sendRaw(sender, "command.help.header");
        plugin.getMessageManager().sendRaw(sender, "command.help.create");
        plugin.getMessageManager().sendRaw(sender, "command.help.convert");
        plugin.getMessageManager().sendRaw(sender, "command.help.reload");
    }

    /**
     * 타입 문자열 파싱
     */
    private ScrollType parseScrollType(String typeArg) {
        // 한국어 입력 지원
        if (typeArg.equals("1회용") || typeArg.equalsIgnoreCase("single")) {
            return ScrollType.SINGLE_USE;
        }
        if (typeArg.equals("다회용") || typeArg.equalsIgnoreCase("unlimited") || typeArg.equalsIgnoreCase("infinite")) {
            return ScrollType.UNLIMITED;
        }

        // enum 기본 파싱
        return ScrollType.fromDisplayName(typeArg);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 첫 번째 인자: 하위 명령어
            List<String> subCommands = List.of("생성", "지정", "리로드", "도움말");
            String input = args[0].toLowerCase();

            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            // 두 번째 인자: 주문서 타입 (생성/지정 명령어인 경우)
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("생성") || subCommand.equals("create") || subCommand.equals("give") ||
                subCommand.equals("지정") || subCommand.equals("set") || subCommand.equals("convert")) {
                List<String> types = List.of("1회용", "다회용");
                String input = args[1].toLowerCase();

                for (String type : types) {
                    if (type.startsWith(input)) {
                        completions.add(type);
                    }
                }
            }
        }

        return completions;
    }
}
