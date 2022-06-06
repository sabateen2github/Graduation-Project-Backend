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

import java.util.*;
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
        QueueEntity queueEntity = queueDAO.findById(Long.valueOf(id)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mapToQueue(queueEntity);
    }


    @Transactional
    public void resetQueue(String instituteId, String branchId, String id) {
        QueueLockKey queueLockKey = QueueLockKey.builder().queueId(id).branchId(branchId).instituteId(instituteId).build();
        InstituteEntity instituteEntity = institutesDAO.findById(Long.valueOf(instituteId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        executeWithLock(queueLockKey, () -> {
            QueueEntity queueEntity = queueDAO.findById(Long.valueOf(id)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!instituteId.equals(queueEntity.getInstitute().getId()) && !instituteEntity.isAdmin())
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            List<BookedTurnQueueEntity> allQueues = bookedTurnQueueDAO.findAllById_QueueIdAndState(Long.valueOf(id), BookedTurnQueue.QueueState.ACTIVE);
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
        InstituteEntity instituteEntity = institutesDAO.findById(Long.valueOf(instituteId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        executeWithLock(queueLockKey, () -> {
            QueueEntity queueEntity = queueDAO.findById(Long.valueOf(id)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!instituteId.equals(queueEntity.getInstitute().getId()) && !instituteEntity.isAdmin())
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

            queueEntity.setPhysicalSize(queueEntity.getPhysicalSize() - 1);
            queueEntity.setQueueSize(queueEntity.getQueueSize() - 1);

            updateAverageTime(queueEntity);

            BookedTurnQueueEntity bookedEntity = bookedTurnQueueDAO.findById_QueueIdAndTurnId(Long.valueOf(id), queueEntity.getCurrentTurnId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            bookedEntity.setState(BookedTurnQueue.QueueState.COMPLETED);

            List<BookedTurnQueueEntity> nextTurns = bookedTurnQueueDAO.findAllById_QueueIdAndPositionGreaterThan(Long.valueOf(id), bookedEntity.getPosition()).stream().sorted(Comparator.comparingInt(BookedTurnQueueEntity::getPosition)).map(it -> {
                it.setPosition(it.getPosition() - 1);
                return it;
            }).collect(Collectors.toList());

            Optional<BookedTurnQueueEntity> next = Optional.empty();

            if (nextTurns.size() > 0) {
                if (nextTurns.get(0).isPhysical()) {
                    next = Optional.of(nextTurns.get(0));
                } else {
                    nextTurns.get(0).setState(BookedTurnQueue.QueueState.CANCELLED);
                    next = nextTurns.stream().skip(1).filter(BookedTurnQueueEntity::isPhysical).findFirst();
                }
            }

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
        BookedTurnQueueEntity.CompositeId compositeId = new BookedTurnQueueEntity.CompositeId();
        compositeId.setQueueId(Long.valueOf(queueId));
        compositeId.setUuid(userId);
        Optional<BookedTurnQueueEntity> bookedTurnQueueEntity = bookedTurnQueueDAO.findById(compositeId);
        if (bookedTurnQueueEntity.isPresent() && bookedTurnQueueEntity.get().getState() == BookedTurnQueue.QueueState.ACTIVE)
            return;

        BranchEntity branch = branchDAO.findById(Long.valueOf(branchId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        QueueLockKey queueLockKey = QueueLockKey.builder().queueId(queueId).branchId(branchId).instituteId(String.valueOf(branch.getInstitute().getId())).build();
        executeWithLock(queueLockKey, () -> {
            QueueEntity queueEntity = queueDAO.findById(Long.valueOf(queueId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            try {

                LatLng latLng = new LatLng();
                latLng.setLng(branch.getLongitude());
                latLng.setLat(branch.getLatitude());

                long duration = distanceService.getDurationInSeconds(location, latLng);
                long allowedDuration = (long) queueEntity.getPhysicalSize() * queueEntity.getAverageTime() * 60;
                //if (duration * 0.9f > allowedDuration)
                //   throw new ResponseStatusException(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);

                queueEntity.setRemoteSize(queueEntity.getRemoteSize() + 1);
                queueEntity.setQueueSize(queueEntity.getQueueSize() + 1);
                BookedTurnQueueEntity bookedTurnQueue = new BookedTurnQueueEntity();
                bookedTurnQueue.setQueue(queueEntity);
                bookedTurnQueue.setId(compositeId);
                bookedTurnQueue.setLogoUrl(queueEntity.getInstitute().getLogoUrl());
                bookedTurnQueue.setPosition(queueEntity.getQueueSize());
                bookedTurnQueue.setState(BookedTurnQueue.QueueState.ACTIVE);
                bookedTurnQueue.setTurnId((long) (bookedTurnQueue.getId().hashCode() + bookedTurnQueue.getPosition()));

                bookedTurnQueue = bookedTurnQueueDAO.save(bookedTurnQueue);

                if (queueEntity.getCurrentTurnId() == null) {
                    queueEntity.setCurrentTurnId(bookedTurnQueue.getTurnId());
                }

                queueDAO.save(queueEntity);
            } catch (DurationServiceException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    @Transactional
    public void cancelTurn(String userId, String branchId, String queueId) {
        BranchEntity branch = branchDAO.findById(Long.valueOf(branchId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        QueueLockKey queueLockKey = QueueLockKey.builder().queueId(queueId).branchId(branchId).instituteId(String.valueOf(branch.getInstitute().getId())).build();
        executeWithLock(queueLockKey, () -> {

            BookedTurnQueueEntity.CompositeId compositeId = new BookedTurnQueueEntity.CompositeId();
            compositeId.setUuid(userId);
            compositeId.setQueueId(Long.valueOf(queueId));

            BookedTurnQueueEntity bookedTurnQueue = bookedTurnQueueDAO.findByIdAndState(compositeId, BookedTurnQueue.QueueState.ACTIVE).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            QueueEntity queueEntity = queueDAO.findById(Long.valueOf(queueId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            if (bookedTurnQueue.isPhysical())
                queueEntity.setPhysicalSize(queueEntity.getRemoteSize() - 1);
            else queueEntity.setRemoteSize(queueEntity.getRemoteSize() - 1);
            queueEntity.setQueueSize(queueEntity.getQueueSize() - 1);

            List<BookedTurnQueueEntity> nextTurns = bookedTurnQueueDAO.findAllById_QueueIdAndPositionGreaterThan(Long.valueOf(queueId), bookedTurnQueue.getPosition());
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
        QueueEntity queueEntity = queueDAO.findById(Long.valueOf(queueSpec.getId())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        InstituteEntity instituteEntity = institutesDAO.findById(Long.valueOf(instituteId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!queueEntity.getInstitute().getId().equals(instituteId) && !instituteEntity.isAdmin())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        queueEntity.setName(queueSpec.getName());
        queueDAO.save(queueEntity);
    }


    public void createQueueSpec(String instituteId, QueueSpec queueSpec) {

        InstituteEntity instituteEntity = institutesDAO.findById(Long.valueOf(instituteId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        BranchEntity branchEntity = branchDAO.findById(Long.valueOf(queueSpec.getBranchId())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!branchEntity.getInstitute().getId().equals(instituteId) && !instituteEntity.isAdmin())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        QueueEntity queueEntity = new QueueEntity();
        queueEntity.setName(queueSpec.getName());
        queueEntity.setInstitute(branchEntity.getInstitute());
        queueEntity.setBranch(branchEntity);
        queueEntity.setAverageTime(5);
        queueDAO.save(queueEntity);

    }

    public void toggleQueueMode(String instituteId, String userId, String branchId, String queueId) {

        BookedTurnQueueEntity.CompositeId compositeId = new BookedTurnQueueEntity.CompositeId();
        compositeId.setUuid(userId);
        compositeId.setQueueId(Long.valueOf(queueId));
        BookedTurnQueueEntity turn = bookedTurnQueueDAO.findByIdAndState(compositeId, BookedTurnQueue.QueueState.ACTIVE).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        turn.setPhysical(!turn.isPhysical());
        bookedTurnQueueDAO.save(turn);

    }

    public void deleteQueue(String instituteId, String branchId, String id) {

        QueueEntity queueEntity = queueDAO.findById(Long.valueOf(id)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        InstituteEntity instituteEntity = institutesDAO.findById(Long.valueOf(instituteId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!instituteEntity.getId().equals(queueEntity.getInstitute().getId()) && !instituteEntity.isAdmin())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        queueDAO.deleteById(Long.valueOf(id));
    }

    public List<QueueSpec> getAllQueueSpecs(String instituteId, String branchId) {

        if (isClosed(branchDAO.findById(Long.valueOf(branchId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)))) {
            return Arrays.asList();
        }

        return queueDAO.findAllByBranch_Id(Long.valueOf(branchId)).stream().map(this::mapToQueueSpec).collect(Collectors.toList());
    }

    public List<Queue> getAllQueues(String instituteId, String branchId) {
        if (isClosed(branchDAO.findById(Long.valueOf(branchId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)))) {
            return Arrays.asList();
        }
        return queueDAO.findAllByBranch_Id(Long.valueOf(branchId)).stream().map(this::mapToQueue).collect(Collectors.toList());
    }

    private Queue mapToQueue(QueueEntity queueEntity) {
        Queue queue = new Queue();
        queue.setQueueSize(queueEntity.getQueueSize());
        queue.setPhysicalSize(queueEntity.getPhysicalSize());
        queue.setRemoteSize(queueEntity.getRemoteSize());
        queue.setCurrentTurnId(String.valueOf(queueEntity.getCurrentTurnId()));
        queue.setAverageTime(queueEntity.getAverageTime());
        queue.setQueueSpec(mapToQueueSpec(queueEntity));
        return queue;
    }

    private QueueSpec mapToQueueSpec(QueueEntity queueEntity) {
        QueueSpec queueSpec = new QueueSpec();
        queueSpec.setBranchId(String.valueOf(queueEntity.getBranch().getId()));
        queueSpec.setId(String.valueOf(queueEntity.getId()));
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
        bookedTurnQueue.setTurnId(String.valueOf(item.getTurnId()));
        bookedTurnQueue.setLogoUrl(item.getLogoUrl());
        bookedTurnQueue.setPosition(item.getPosition());
        bookedTurnQueue.setState(item.getState());
        return bookedTurnQueue;
    }


    private boolean isClosed(BranchEntity branch) {
        Optional<BranchEntity.WorkingDay> workingDay = branch.getWorkingDays().stream().filter(it -> it.getDay().equals(getCurrentDay())).findAny();
        if (workingDay.isPresent()) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int minutes = hour * 60 + minute;

            int hourBranch = workingDay.get().getHour();
            int minuteBranch = workingDay.get().getMinute();
            int minutesBranch = hourBranch * 60 + minuteBranch;
            int period = workingDay.get().getPeriodInMinutes();

            if (minutesBranch + period >= 24 * 60)
                period = 24 * 60 - minutesBranch - 1;

            if (minutesBranch <= minutes && minutes <= minuteBranch + period) {
                return true;
            }
            return false;
        }
        return false;
    }


    private BranchEntity.Day getCurrentDay() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return BranchEntity.Day.Sunday;
            case Calendar.MONDAY:
                return BranchEntity.Day.Monday;
            case Calendar.TUESDAY:
                return BranchEntity.Day.Tuesday;
            case Calendar.WEDNESDAY:
                return BranchEntity.Day.Wednesday;
            case Calendar.THURSDAY:
                return BranchEntity.Day.Thursday;
            case Calendar.FRIDAY:
                return BranchEntity.Day.Friday;
            case Calendar.SATURDAY:
                return BranchEntity.Day.Saturday;
            default:
                throw new IllegalStateException("should not be reached!");
        }

    }


}
