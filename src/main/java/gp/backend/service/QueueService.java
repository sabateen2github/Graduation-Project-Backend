package gp.backend.service;

import gp.backend.data.BookedTurnQueueDAO;
import gp.backend.data.BranchDAO;
import gp.backend.data.InstitutesDAO;
import gp.backend.data.QueueDAO;
import gp.backend.data.entities.BookedTurnQueueEntity;
import gp.backend.data.entities.BranchEntity;
import gp.backend.data.entities.InstituteEntity;
import gp.backend.data.entities.QueueEntity;
import gp.backend.dto.BookedTurnQueue;
import gp.backend.dto.LatLng;
import gp.backend.dto.Queue;
import gp.backend.dto.QueueSpec;
import gp.backend.security.DurationServiceException;
import gp.backend.service.beans.QueueLockKey;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueueService {


    private final QueueDAO queueDAO;
    private final BookedTurnQueueDAO bookedTurnQueueDAO;
    private final BranchDAO branchDAO;
    private final InstitutesDAO institutesDAO;
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
            if (!instituteId.equals(queueEntity.getInstitute().getId()))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
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
            if (!instituteId.equals(queueEntity.getInstitute().getId()))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

            queueEntity.setPhysicalSize(queueEntity.getPhysicalSize() - 1);
            queueEntity.setQueueSize(queueEntity.getQueueSize() - 1);

            updateAverageTime(queueEntity);

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

    private void updateAverageTime(QueueEntity queueEntity) {
        if (queueEntity.getStart() == null) {
            queueEntity.setStart(new Date());
        } else {
            long durationMins = ((new Date()).getTime() - queueEntity.getStart().getTime()) / 1000;
            queueEntity.setStart(new Date());
            if (durationMins < 60 * 2) {
                queueEntity.setAverageTime((int) ((durationMins + queueEntity.getCount() * queueEntity.getAverageTime()) / (queueEntity.getCount() + 1)));
            }
            if (queueEntity.getCount() > 1000) queueEntity.setCount(1);
        }
    }

    @Transactional
    public void bookQueue(String userId, String branchId, String queueId, LatLng location) {
        BranchEntity branch = branchDAO.findById(branchId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        QueueLockKey queueLockKey = QueueLockKey.builder().queueId(queueId).branchId(branchId).instituteId(branch.getInstitute().getId()).build();
        executeWithLock(queueLockKey, () -> {
            QueueEntity queueEntity = queueDAO.findById(queueId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            try {

                LatLng latLng = new LatLng();
                latLng.setLng(branch.getLongitude());
                latLng.setLat(branch.getLatitude());

                long duration = distanceService.getDurationInSeconds(location, latLng);
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
        BranchEntity branch = branchDAO.findById(branchId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        QueueLockKey queueLockKey = QueueLockKey.builder().queueId(queueId).branchId(branchId).instituteId(branch.getInstitute().getId()).build();
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
        if (!queueEntity.getInstitute().getId().equals(instituteId))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        queueEntity.setName(queueSpec.getName());
        queueDAO.save(queueEntity);
    }


    public void createQueueSpec(String instituteId, QueueSpec queueSpec) {

        InstituteEntity instituteEntity = institutesDAO.findById(instituteId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        BranchEntity branchEntity = branchDAO.findById(queueSpec.getBranchId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        QueueEntity queueEntity = new QueueEntity();
        queueEntity.setName(queueSpec.getName());
        queueEntity.setInstitute(instituteEntity);
        queueEntity.setBranch(branchEntity);
        queueEntity.setAverageTime(5);
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
        QueueEntity queueEntity = queueDAO.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!instituteId.equals(queueEntity.getInstitute().getId()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
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
