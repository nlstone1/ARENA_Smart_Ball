package arena.arenasmartball.views;

import android.view.View;

/**
 * Thread for regulating updates of a View.
 *
 * Created by Nathaniel on 4/6/2016.
 */
public class ViewUpdater extends Thread
{
    // Lock for sleeping on
    private final Object LOCK;

    // Whether the thread should die
    private boolean isDead;

    // Whether the view should be invalidated
    private boolean shouldUpdate;

    // The View to update
    private final View VIEW;

    // Runnable for posting invalidate notices
    private final Runnable RUNNABLE;

    // The minimum time between updates
    private long sleepTime;

    // The default sleep time
    private static final long DEFAULT_SLEEP_TIME = 16L;

    /**
     * Default Constructor.
     */
    public ViewUpdater(View view)
    {
        LOCK = new Object();
        VIEW = view;
        sleepTime = DEFAULT_SLEEP_TIME;

        RUNNABLE = new Runnable()
        {
            @Override
            public void run()
            {
                VIEW.invalidate();
            }
        };
    }

    @Override
    public void run()
    {
        isDead = false;
        shouldUpdate = false;

        try
        {
            while (!isDead)
            {
                // Sleep if nothing to do
                if (!shouldUpdate)
                {
                    synchronized (LOCK)
                    {
                        LOCK.wait();
                    }
                }

                if (!isDead)
                {
                    // Otherwise invalidate the View and continue
                    VIEW.post(RUNNABLE);
                    shouldUpdate = false;

                    Thread.sleep(sleepTime);
                }
            }
        }
        catch (InterruptedException ignore) {}
    }

    /**
     * Notifies the View that it should be redrawn.
     * @param notify Whether to notify the LOCK
     */
    public void redraw(boolean notify)
    {
        shouldUpdate |= true;

        if (notify)
        {
            synchronized (LOCK)
            {
                LOCK.notify();
            }
        }
    }

    /**
     * Notifies the Mutex lock.
     */
    public void notifyLock()
    {
        if (shouldUpdate)
        {
            synchronized (LOCK)
            {
                LOCK.notify();
            }
        }
    }

    /**
     * Kills this ViewUpdater.
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
