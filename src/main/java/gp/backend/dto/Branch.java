package gp.backend.dto;

import gp.backend.data.entities.BranchEntity;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Data
public class Branch {
    private String name;
    private String id;
    private String phone;
    private String instituteId;
    private LatLng location;
    private Set<WorkingDay> workingDays;

    @Getter
    @Setter
    public static class WorkingDay {
        private BranchEntity.Day day;
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

