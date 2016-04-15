package arena.arenasmartball;

/**
 * A Thread to control periodic updating.
 * Created by Theodore on 4/14/2016.
 */
public class PeriodicUpdateThread extends Thread
{
    // Lock for sleeping on
    private final Object LOCK;

    // The Runnable to run every update sequence
    private final Runnable ACTION;

    // The amount to wait between updates
    private final long UPDATE_DELAY;

    // Whether the thread should die
    private boolean isDead;

    // Whether or not this thread should block
    private boolean isBlocked;

    /**
     * Default Constructor.
     * @param delay The amount to wait between updates
     */
    public PeriodicUpdateThread(long delay)
    {
        this (delay, null);
    }

    /**
     * Constructor.
     * @param delay The amount to wait between updates
     * @param action An optional Runnable to run every update sequence
     */
    public PeriodicUpdateThread(long delay, Runnable action)
    {
        UPDATE_DELAY = delay;
        ACTION = action;
        LOCK = new Object();
    }

    /**
     * Resets the delay timer and blocked status (to unblocked) of this Thread.
     */
    public void reset()
    {
        unblock();
        interrupt();
        unblock();
    }

    /**
     * Called when this Thread is updates.
     */
    public void onUpdate()
    {    }

    @Override
    public void run()
    {
        isDead = false;
        isBlocked = true;

        while (!isDead)
        {
            // Block if needed
            if (isBlocked)
            {
                synchronized (LOCK)
                {
                    try
                    {
                        LOCK.wait();
                    }
                    catch (InterruptedException ignore) {}
                }
            }

            if (!isDead && !isBlocked)
            {
                // Otherwise do work and continue
                onUpdate();

                if (ACTION != null)
                    ACTION.run();

                try
                {
                    Thread.sleep(UPDATE_DELAY);
                }
                catch (InterruptedException ignore) {}
            }
        }
    }

    /**
     * Unblocks this UpdaterThread if needed.
     */
    public void unblock()
    {
        if (!isBlocked)
            return;

        isBlocked = false;

        // Wake up
        synchronized (LOCK)
        {
            LOCK.notify();
        }
    }

    /**
     * Blocks this Thread.
     */
    public void block()
    {
        isBlocked = true;
    }

    /**
     * Kills this Thread.
     */
    public void kill()
    {
        isDead = false;

        synchronized (LOCK)
        {
            LOCK.notify();
        }
    }
}