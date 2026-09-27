package pl.kiosel.rosacore.nms.api.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerTpsTest {

    @Test
    void prefersPaperApiOverLegacyField() {
        assertArrayEquals(new double[]{19.8D, 19.7D, 19.6D},
                ServerTps.read(new PaperServer(), new LegacyMinecraftServer()));
    }

    @Test
    void usesSpigotFacadeWhenAvailable() {
        assertArrayEquals(new double[]{19.5D, 19.4D, 19.3D},
                ServerTps.read(new SpigotServer(), new Object()));
    }

    @Test
    void fallsBackToLegacyField() {
        assertArrayEquals(new double[]{18.0D, 17.0D, 16.0D},
                ServerTps.read(new Object(), new LegacyMinecraftServer()));
    }

    @Test
    void reportsUnavailableTpsInsteadOfInventingIt() {
        assertThrows(IllegalStateException.class,
                () -> ServerTps.read(new Object(), new Object()));
    }

    public static final class PaperServer {
        public double[] getTPS() {
            return new double[]{19.8D, 19.7D, 19.6D};
        }
    }

    public static final class SpigotServer {
        public SpigotFacade spigot() {
            return new SpigotFacade();
        }
    }

    public static final class SpigotFacade {
        public double[] getTPS() {
            return new double[]{19.5D, 19.4D, 19.3D};
        }
    }

    public static final class LegacyMinecraftServer {
        public final double[] recentTps = {18.0D, 17.0D, 16.0D};
    }
}
