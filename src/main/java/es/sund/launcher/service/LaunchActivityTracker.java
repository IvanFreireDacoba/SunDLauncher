package es.sund.launcher.service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cuenta cuántas instancias tienen ahora mismo una partida de Minecraft
 * abierta. GameLaunchCoordinator lo usa para coordinar el minimizado del
 * launcher (ver LauncherSettings.isMinimizeDuringGameEnabled()) cuando hay
 * varias instancias jugando en paralelo, cada una en su propio hilo (ver
 * PlayOrInstallAction):
 *
 * - Solo se minimiza el launcher cuando la PRIMERA partida arranca (si ya
 *   había otra en marcha, no tiene sentido volver a minimizar algo que ya
 *   está minimizado).
 * - Solo se restaura cuando la ÚLTIMA partida activa termina (si queda
 *   alguna otra instancia jugando, el launcher se queda minimizado por ella).
 */
public final class LaunchActivityTracker {

    private static final AtomicInteger ACTIVE = new AtomicInteger(0);

    /** @return true si esta era la única partida activa (había 0 antes de contar esta). */
    public static boolean beginAndWasFirst() {
        return ACTIVE.getAndIncrement() == 0;
    }

    /** @return true si, tras terminar esta partida, no queda ninguna otra activa. */
    public static boolean endAndWasLast() {
        return ACTIVE.decrementAndGet() <= 0;
    }

    private LaunchActivityTracker() {}
}
