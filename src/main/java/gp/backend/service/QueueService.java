package gp.backend.service;

import gp.backend.data.BookedTurnQueueDAO;
import gp.backend.data.QueueDAO;
import gp.backend.data.entities.BookedTurnQueueEntity;
import gp.backend.data.entities.QueueEntity;
import gp.backend.dto.*;
import gp.backend.security.DurationServiceException;
import gp.backend.service.beans.QueueLockKey;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueueService {


    private final QueueDAO queueDAO;
    private final BookedTurnQueueDAO bookedTurnQueueDAO;
    private final BranchService branchService;
    private final DistanceService distanceService;
    private final LockService locksService;


    public List<BookedTurnQueue> getActiveQueues(String userId) {
        return bookedTurnQueueDAO.findAllById_UuidAndState(userId, BookedTurnQueue.QueueState.ACTIVE).stream().map(this::mapToBookedQueue).collect(Collectors.toList());
    }

    public List<BookedTurnQueue> getArchivedQueues(String userId) {
        return bookedTurnQueueDAO.findAllById_UuidAndStateIsNot(userId, BookedTurnQueue.QueueState.ACTIVE).stream().map(this::mapToBookedQueue).collect(Collectors.toList());
    }

    public Queue getQueue(String instituteId, String branchId, String id) {
        QueueEntity queueEntity = queueDAO.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mapToQueue(queueEntity);
    }


    @Transactional
    public void resetQueue(String instituteId, String branchId, String id) {
        QueueLockKey queueLockKey = QueueLockKey.builder().queueId(id).branchId(branchId).instituteId(instituteId).build();
        executeWithLock(queueLockKey, () -> {
            QueueEntity queueEntity = queueDAO.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            List<BookedTurnQueueEntity> allQueues = bookedTurnQueueDAO.findAllById_QueueIdAndState(id, BookedTurnQueue.QueueState.ACTIVE);

            allQueues.forEach(item -> {
                item.setState(BookedTurnQueue.QueueState.CANCELLED);
            });

            bookedTurnQueueDAO.saveAll(allQueues);
            queueEntity.setQueueSize(0);
            queueEntity.setCurrentTurnId(null);
            queueEntity.setPhysicalSize(0);
            queueEntity.setRemoteSize(0);
            queueDAO.save(queueEntity);
        });
    }

    @Transactional
    public void advanceQueue(String instituteId, String branchId, String id) {
        QueueLockKey queueLockKey = QueueLockKey.builder().queueId(id).branchId(branchId).instituteId(instituteId).build();
        executeWithLock(queueLockKey, () -> {
            QueueEntity queueEntity = queueDAO.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            queueEntity.setPhysicalSize(queueEntity.getPhysicalSize() - 1);
            queueEntity.setQueueSize(queueEntity.getQueueSize() - 1);

            BookedTurnQueueEntity bookedEntity = bookedTurnQueueDAO.findById_QueueIdAndTurnId(id, queueEntity.getCurrentTurnId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            bookedEntity.setState(BookedTurnQueue.QueueState.COMPLETED);

            List<BookedTurnQueueEntity> nextTurns = bookedTurnQueueDAO.findAllById_QueueIdAndPositionGreaterThan(id, bookedEntity.getPosition());
            Optional<BookedTurnQueueEntity> next = nextTurns.stream().peek(turn -> turn.setPosition(turn.getPosition() - 1)).min(Comparator.comparingInt(BookedTurnQueueEntity::getPosition));
            if (next.isPresent()) {
                queueEntity.setCurrentTurnId(next.get().getTurnId());
            } else {
                queueEntity.setCurrentTurnId(null);
            }

            bookedTurnQueueDAO.saveAll(nextTurns);
            queueDAO.save(queueEntity);
            bookedTurnQueueDAO.save(bookedEntity);
        });
    }

    @Transactional
    public void bookQueue(String userId, String branchId, String queueId, LatLng location) {
        Branch branch = branchService.getBranch(branchId);
        QueueLockKey queueLockKey = QueueLockKey.builder().queueId(queueId).branchId(branchId).instituteId(branch.getInstituteId()).build();
        executeWithLock(queueLockKey, () -> {
            QueueEntity queueEntity = queueDAO.findById(queueId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            try {
                long duration = distanceService.getDurationInSeconds(location, branch.getLocation());
                long allowedDuration = (long) queueEntity.getPhysicalSize() * queueEntity.getAverageTime() * 60;
                if (duration * 0.9f > allowedDuration)
                    throw new ResponseStatusException(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);

                queueEntity.setRemoteSize(queueEntity.getRemoteSize() + 1);
                queueEntity.setQueueSize(queueEntity.getQueueSize() + 1);
                BookedTurnQueueEntity bookedTurnQueue = new BookedTurnQueueEntity();
                bookedTurnQueue.setQueue(queueEntity);
                BookedTurnQueueEntity.CompositeId compositeId = new BookedTurnQueueEntity.CompositeId();
                compositeId.setQueueId(queueId);
                compositeId.setUuid(userId);
                bookedTurnQueue.setId(compositeId);
                bookedTurnQueue.setLogoUrl(queueEntity.getInstitute().getLogoUrl());
                bookedTurnQueue.setPosition(queueEntity.getQueueSize());
                bookedTurnQueue.setState(BookedTurnQueue.QueueState.ACTIVE);

                bookedTurnQueueDAO.save(bookedTurnQueue);
                queueDAO.save(queueEntity);
            } catch (DurationServiceException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    @Transactional
    public void cancelTurn(String userId, String branchId, String queueId) {
        Branch branch = branchService.getBranch(branchId);
        QueueLockKey queueLockKey = QueueLockKey.builder().queueId(queueId).branchId(branchId).instituteId(branch.getInstituteId()).build();
        executeWithLock(queueLockKey, () -> {

            BookedTurnQueueEntity.CompositeId compositeId = new BookedTurnQueueEntity.CompositeId();
            compositeId.setUuid(userId);
            compositeId.setQueueId(queueId);

            BookedTurnQueueEntity bookedTurnQueue = bookedTurnQueueDAO.findByIdAndState(compositeId, BookedTurnQueue.QueueState.ACTIVE).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            QueueEntity queueEntity = queueDAO.findById(queueId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            if (bookedTurnQueue.isPhysical())
                queueEntity.setPhysicalSize(queueEntity.getRemoteSize() - 1);
            else queueEntity.setRemoteSize(queueEntity.getRemoteSize() - 1);
            queueEntity.setQueueSize(queueEntity.getQueueSize() - 1);

            List<BookedTurnQueueEntity> nextTurns = bookedTurnQueueDAO.findAllById_QueueIdAndPositionGreaterThan(queueId, bookedTurnQueue.getPosition());
            Optional<BookedTurnQueueEntity> next = nextTurns.stream().peek(turn -> turn.setPosition(turn.getPosition() - 1)).min(Comparator.comparingInt(BookedTurnQueueEntity::getPosition));

            if (bookedTurnQueue.getTurnId().equals(queueEntity.getCurrentTurnId())) {
                if (next.isPresent()) {
                    queueEntity.setCurrentTurnId(next.get().getTurnId());
                } else {
                    queueEntity.setCurrentTurnId(null);
                }
            }

            bookedTurnQueueDAO.saveAll(nextTurns);
            bookedTurnQueueDAO.save(bookedTurnQueue);
            queueDAO.save(queueEntity);
        });
    }

    public void editQueueSpec(String instituteId, QueueSpec queueSpec) {
        QueueEntity queueEntity = queueDAO.findById(queueSpec.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        queueEntity.setName(queueSpec.getName());
        queueDAO.save(queueEntity);
    }

    public void toggleQueueMode(String instituteId, String userId, String branchId, String queueId) {

        BookedTurnQueueEntity.CompositeId compositeId = new BookedTurnQueueEntity.CompositeId();
        compositeId.setUuid(userId);
        compositeId.setQueueId(queueId);
        BookedTurnQueueEntity turn = bookedTurnQueueDAO.findByIdAndState(compositeId, BookedTurnQueue.QueueState.ACTIVE).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        turn.setPhysical(!turn.isPhysical());
        bookedTurnQueueDAO.save(turn);

    }

    public void deleteQueue(String instituteId, String branchId, String id) {
        // TODO: check if related bookedTurnQueueEntities are also deleted
        queueDAO.deleteById(id);
    }

    public List<QueueSpec> getAllQueueSpecs(String instituteId, String branchId) {
        return queueDAO.findAllByInstitute_IdAndBranch_Id(instituteId, branchId).stream().map(this::mapToQueueSpec).collect(Collectors.toList());
    }

    public List<Queue> getAllQueues(String instituteId, String branchId) {
        return queueDAO.findAllByInstitute_IdAndBranch_Id(instituteId, branchId).stream().map(this::mapToQueue).collect(Collectors.toList());
    }

    private Queue mapToQueue(QueueEntity queueEntity) {
        Queue queue = new Queue();
        queue.setQueueSize(queueEntity.getQueueSize());
        queue.setPhysicalSize(queueEntity.getPhysicalSize());
        queue.setRemoteSize(queueEntity.getRemoteSize());
        queue.setCurrentTurnId(queueEntity.getCurrentTurnId());
        queue.setAverageTime(queueEntity.getAverageTime());
        queue.setQueueSpec(mapToQueueSpec(queueEntity));
        return queue;
    }

    private QueueSpec mapToQueueSpec(QueueEntity queueEntity) {
        QueueSpec queueSpec = new QueueSpec();
        queueSpec.setBranchId(queueEntity.getBranch().getId());
        queueSpec.setId(queueEntity.getId());
        queueSpec.setName(queueEntity.getName());
        return queueSpec;
    }


    private void executeWithLock(QueueLockKey key, Runnable function) {
        locksService.acquireLock(key);
        try {
            function.run();
        } catch (Throwable e) {
            locksService.releaseLock(key);
            throw e;
        }
    }

    private BookedTurnQueue mapToBookedQueue(BookedTurnQueueEntity item) {
        BookedTurnQueue bookedTurnQueue = new BookedTurnQueue();
        bookedTurnQueue.setQueue(mapToQueue(item.getQueue()));
        bookedTurnQueue.setTurnId(item.getTurnId());
        bookedTurnQueue.setLogoUrl(item.getLogoUrl());
        bookedTurnQueue.setPosition(item.getPosition());
        bookedTurnQueue.setState(item.getState());
        return bookedTurnQueue;
    }


}
