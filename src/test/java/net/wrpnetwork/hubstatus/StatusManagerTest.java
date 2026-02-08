package net.wrpnetwork.hubstatus;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import java.util.Collections;

public class StatusManagerTest {

    private WRPHubStatus plugin;
    private FileConfiguration config;
    private StatusManager statusManager;

    @Before
    public void setUp() {
        plugin = Mockito.mock(WRPHubStatus.class);
        config = Mockito.mock(FileConfiguration.class);
        ConfigurationSection serversSection = Mockito.mock(ConfigurationSection.class);

        Mockito.when(plugin.getConfig()).thenReturn(config);
        Mockito.when(config.getConfigurationSection("servers")).thenReturn(serversSection);
        Mockito.when(serversSection.getKeys(false)).thenReturn(Collections.singleton("survival"));

        ConfigurationSection survivalSection = Mockito.mock(ConfigurationSection.class);
        Mockito.when(serversSection.getConfigurationSection("survival")).thenReturn(survivalSection);
        Mockito.when(survivalSection.getString("display-name", "survival")).thenReturn("Survival");
        Mockito.when(survivalSection.getString("address", "127.0.0.1:25565")).thenReturn("127.0.0.1:25565");
        Mockito.when(survivalSection.getInt("max-players", 100)).thenReturn(100);
        Mockito.when(survivalSection.getBoolean("auto-status", true)).thenReturn(true);
        Mockito.when(survivalSection.getString("default-state", "OFFLINE")).thenReturn("OFFLINE");

        statusManager = new StatusManager(plugin);
    }

    @Test
    public void testServerLoading() {
        StatusManager.ServerData data = statusManager.getServerData("survival");
        Assert.assertNotNull(data);
        Assert.assertEquals("Survival", data.getDisplayName());
        Assert.assertEquals("OFFLINE", data.getCurrentState());
    }

    @Test
    public void testStateColors() {
        Assert.assertEquals("&a", statusManager.getStateColor("ONLINE"));
        Assert.assertEquals("&e", statusManager.getStateColor("MANTENIMIENTO"));
        Assert.assertEquals("&c", statusManager.getStateColor("OFFLINE"));
    }
}
