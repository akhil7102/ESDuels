package fun.endcore.esduels.arena;

/**
 * Lifecycle states of a duels arena.
 */
public enum ArenaState {
    /**
     * Missing one or more required configurations or snapshot.
     */
    INCOMPLETE,

    /**
     * Configured, saved, restored, and ready to host a duel.
     */
    IDLE,

    /**
     * Players have teleported, pre-duel countdown is ticking.
     */
    COUNTDOWN,

    /**
     * Active combat phase.
     */
    ACTIVE,

    /**
     * Combat ended; winner is collecting loot drops.
     */
    LOOTING,

    /**
     * Snapshot blocks are actively regenerating.
     */
    RESTORING
}
