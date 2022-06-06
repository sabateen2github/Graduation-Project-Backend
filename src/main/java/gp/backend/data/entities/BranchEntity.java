package gp.backend.data.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
public class BranchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    private String name;
    private String phone;
    @ManyToOne(optional = false)
    @JoinColumn(name = "institute_id")
    private InstituteEntity institute;
    private double latitude;
    private double longitude;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "branch", cascade = CascadeType.ALL)
    private List<QueueEntity> queues;

    @ElementCollection
    private Set<WorkingDay> workingDays;

    public enum Day {
        Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
    }

    @Getter
    @Setter
    @Embeddable
    public static class WorkingDay {
        private Day day;
        private int hour;
        private int minute;
        private int periodInMinutes;


        @Override
        public int hashCode() {
            return day.name().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof WorkingDay && ((WorkingDay) obj).day.equals(day);
        }
    }

}
