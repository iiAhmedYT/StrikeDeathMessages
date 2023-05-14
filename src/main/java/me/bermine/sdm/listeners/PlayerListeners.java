package me.bermine.sdm.listeners;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.api.StrikePracticeAPI;
import ga.strikepractice.fights.BotFight;
import ga.strikepractice.fights.Fight;
import ga.strikepractice.fights.duel.BestOfFight;
import lombok.RequiredArgsConstructor;
import me.bermine.sdm.StrikeDeathMessages;
import me.bermine.sdm.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;
import java.util.Optional;

/**
 * @author Bermine
 */
@RequiredArgsConstructor
public class PlayerListeners implements Listener {

    private final StrikeDeathMessages plugin;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player) || !plugin.getConfig().getBoolean("death.enabled")) return;
        StrikePracticeAPI api = StrikePractice.getAPI();
        Player damaged = (Player) e.getEntity();
        if (round(damaged.getHealth()) > 4.1) return;
        if (e.getDamage() <= 1.5 && round(damaged.getHealth()) >= 1.0) return;
        Fight fight = api.getFight(damaged);
        if (!(fight instanceof BestOfFight) || fight instanceof BotFight) return;

        List<Player> playersInFight = fight.getPlayersInFight();
        Optional<Player> optionalPlayer = playersInFight.stream().filter(p -> !p.getName().equals(damaged.getName())).findFirst();
        if (!optionalPlayer.isPresent()) return;
        Player opponent = optionalPlayer.get();

        if (fight.getKit().isBedwars() || fight.getKit().isBridges() && !fight.hasEnded()) {
            if (damaged.getLastDamageCause().getCause() == DamageCause.ENTITY_ATTACK) {
                playersInFight.forEach(player ->
                    player.sendMessage(CC.translate(plugin.getConfig().getString("death.message"))
                        .replace("<looser>", damaged.getName())
                        .replace("<winner>", opponent.getName()))
                );
                fight.getSpectators().forEach(spectator ->
                    spectator.sendMessage(CC.translate(plugin.getConfig().getString("death.message"))
                        .replace("<looser>", damaged.getName())
                        .replace("<winner>", opponent.getName()))
                );
            }
            if (damaged.getLastDamageCause().getCause() == DamageCause.FALL) {
                playersInFight.forEach(player ->
                    player.sendMessage(CC.translate(plugin.getConfig().getString("death.message_no_player")).replace("<player>", damaged.getName()).replace("<opponent>", opponent.getName()))
                );
                fight.getSpectators().forEach(spectator ->
                    spectator.sendMessage(CC.translate(plugin.getConfig().getString("death.message_no_player")).replace("<player>", damaged.getName()).replace("<opponent>", opponent.getName()))
                );
            }
        }
    }

    boolean teleporting = false;

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!plugin.getConfig().getBoolean("death.enabled")) return;
        if (e.getFrom().getBlockY() == -1.0 || e.getFrom().getBlockY() == -2.0) {
            execute(e.getPlayer());
        }
    }

    void execute(Player dead) {
        if (teleporting) return;
        teleporting = true;
        StrikePracticeAPI api = StrikePractice.getAPI();
        Fight fight = api.getFight(dead);
        if (fight == null) return;

        List<Player> playersInFight = fight.getPlayersInFight();
        Optional<Player> optionalPlayer = playersInFight.stream().filter(p -> !p.getName().equals(dead.getName())).findAny();
        if (!optionalPlayer.isPresent()) return;
        Player opponent = optionalPlayer.get();

        String message = CC.translate(plugin.getConfig().getString("death.message_no_player")).replace("<player>", dead.getName()).replace("<opponent>", opponent.getName())
            .replace("<player>", dead.getName())
            .replace("<opponent>", opponent.getName());
        fight.getPlayersInFight().forEach(player -> player.sendMessage(message));
        fight.getSpectators().forEach(spectator -> spectator.sendMessage(message));
        // Prevent sending multiple messages when both -1.0 and -2.0 Y-cords are present
        Bukkit.getScheduler().runTaskLater(plugin, () -> teleporting = false, 5L);
    }

    double round(double value) {
        int scale = (int) Math.pow(10, 1);
        return (double) Math.round(value * scale) / scale;
    }
}