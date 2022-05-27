package gp.backend.service;

import gp.backend.service.beans.QueueLockKey;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
public class SingleInstanceLockService implements LockService {

    private List<Lock> locks;
    private List<Integer> counters;
    private boolean isUpdating;

    /**
     * We start with 8 locks initially and, then, we multiply by 2 each time we want to increase the number of locks.
     * The reason we multiply by 2 is for hashing/indexing purposes.
     */
    @PostConstruct
    public void init() {
        setLocks(8);
    }

    private void setLocks(int size) {
        locks = new ArrayList<>();
        counters = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            locks.add(new ReentrantLock());
            counters.add(0);
        }
    }

    public void acquireLock(QueueLockKey key) {
        while (isUpdating) ;
        int index = getIndex(key.hashCode());
        locks.get(index).lock();
        counters.set(index, counters.get(index) + 1);
    }

    public void releaseLock(QueueLockKey key) {
        int index = getIndex(key.hashCode());
        locks.get(index).unlock();
    }

    private int getIndex(int hashCode) {
        return (hashCode ^ (hashCode >> 16)) & (locks.size() - 1);
    }

    @Scheduled(fixedRate = 1800000) // every 30 mins
    public void checkLocksPerformance() {

        OptionalDouble averageOptional = counters.stream().mapToInt(Integer::intValue).average();
        averageOptional.ifPresent(average -> {
            if (average > 1000) {
                int multiplier = (int) Math.pow(2, Math.ceil(Math.log((float) average / 1000) / Math.log(2)));
                isUpdating = true;
                locks.forEach(Lock::lock);
                setLocks(locks.size() * multiplier);
                isUpdating = false;
            }
        });
    }

}
