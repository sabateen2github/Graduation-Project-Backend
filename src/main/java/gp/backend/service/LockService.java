package gp.backend.service;

import gp.backend.service.beans.QueueLockKey;

/**
 * This interface can be implemented for universal locking service across clusters of spring boot instances.
 */
public interface LockService {

    public void acquireLock(QueueLockKey key);

    public void releaseLock(QueueLockKey key);
}
