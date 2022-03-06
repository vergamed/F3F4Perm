package de.redgames.f3nperm.provider;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.redgames.f3nperm.F3NPermPlugin;
import de.redgames.f3nperm.OpPermissionLevel;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class ProtocolLibProvider implements Provider {
    private F3NPermPlugin plugin;
    private ProtocolManager manager;

    @Override
    public void register(F3NPermPlugin plugin) {
        this.plugin = plugin;
        this.manager = ProtocolLibrary.getProtocolManager();

        manager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.ENTITY_STATUS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacketType() != PacketType.Play.Server.ENTITY_STATUS) {
                    return;
                }

                PacketContainer packet = event.getPacket();
                int playerId = packet.getIntegers().read(0);

                if (playerId != event.getPlayer().getEntityId()) {
                    return;
                }

                byte currentStatus = packet.getBytes().read(0);
                F3NPermPlugin plugin = ProtocolLibProvider.this.plugin;

                if (OpPermissionLevel.fromStatusByte(plugin.getServerVersion(), currentStatus) == null) {
                    return;
                }

                OpPermissionLevel targetLevel = plugin.getF3NPermPermissionLevel(event.getPlayer());

                packet.getBytes().write(0, targetLevel.toStatusByte(plugin.getServerVersion()));
            }
        });
    }

    @Override
    public void unregister(F3NPermPlugin plugin) {
        manager.removePacketListeners(plugin);
    }

    @Override
    public void update(Player player) {
        OpPermissionLevel level = plugin.getF3NPermPermissionLevel(player);
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_STATUS);

        packet.getIntegers().write(0, player.getEntityId());
        packet.getBytes().write(0, level.toStatusByte(plugin.getServerVersion()));

        try {
            manager.sendServerPacket(player, packet, false);
        } catch (InvocationTargetException e) {
            throw new ProviderException("Could not send status packet!", e);
        }
    }
}
