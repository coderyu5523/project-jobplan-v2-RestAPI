package shop.mtcoding.projectjobplan.rating;

import lombok.Data;

import java.sql.Timestamp;

public class RatingResponse {

    @Data // 평가 결과 returnDTO
    public static class DTO {
        private Integer id;
        private Integer raterId; // 평가자 id
        private Integer subjectId; // 피평가자 id
        private Double rating;
        private Timestamp createdAt;

        public DTO(Rating rating) {
            this.id = rating.getId();
            this.raterId = rating.getRater().getId();
            this.subjectId = rating.getSubject().getId();
            this.rating = rating.getRating();
            this.createdAt = rating.getCreatedAt();
        }
    }
}
