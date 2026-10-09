package com.joplayx.client.state;

/** Counts active game ticks only. Pausing, retrying and finding a trade never reset it. */
public final class SessionStats {
    private int attempts;
    private long activeTicks;
    public void tick() { activeTicks++; }
    public void attempt() { attempts++; }
    public int attempts() { return attempts; }
    public long seconds() { return activeTicks / 20; }
    public void reset() { attempts = 0; activeTicks = 0; }
}
