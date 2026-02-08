package net.wrpnetwork.hubstatus;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.bukkit.configuration.file.FileConfiguration;

public class StatusManagerTest {

    private WRPHubStatus plugin;
    private FileConfiguration config;
    private StatusManager statusManager;

    @Before
    public void setUp() {
        plugin = Mockito.mock(WRPHubStatus.class);
        config = Mockito.mock(FileConfiguration.class);
        Mockito.when(plugin.getConfig()).thenReturn(config);
        Mockito.when(config.getString("server.default-state", "OFFLINE")).thenReturn("OFFLINE");

        statusManager = new StatusManager(plugin);
    }

    @Test
    public void testDefaultState() {
        Assert.assertEquals("OFFLINE", statusManager.getCurrentState());
    }

    @Test
    public void testSetState() {
        statusManager.setCurrentState("ONLINE");
        Assert.assertEquals("ONLINE", statusManager.getCurrentState());
        Assert.assertEquals("&a", statusManager.getStateColor());
    }

    @Test
    public void testMantenimientoState() {
        statusManager.setCurrentState("MANTENIMIENTO");
        Assert.assertEquals("MANTENIMIENTO", statusManager.getCurrentState());
        Assert.assertEquals("&e", statusManager.getStateColor());
    }
}
