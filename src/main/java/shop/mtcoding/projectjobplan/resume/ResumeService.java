package shop.mtcoding.projectjobplan.resume;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.mtcoding.projectjobplan._core.errors.exception.Exception403;
import shop.mtcoding.projectjobplan._core.errors.exception.Exception404;
import shop.mtcoding.projectjobplan._core.errors.exception.Exception500;
import shop.mtcoding.projectjobplan.rating.Rating;
import shop.mtcoding.projectjobplan.rating.RatingJpaRepository;
import shop.mtcoding.projectjobplan.subscribe.Subscribe;
import shop.mtcoding.projectjobplan.subscribe.SubscribeJpaRepository;
import shop.mtcoding.projectjobplan.user.SessionUser;
import shop.mtcoding.projectjobplan.user.User;
import shop.mtcoding.projectjobplan.user.UserJpaRepository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ResumeService {
    private final UserJpaRepository userJpaRepository;
    private final ResumeJpaRepository resumeJpaRepository;
    private final SubscribeJpaRepository subscribeJpaRepository;
    private final RatingJpaRepository ratingJpaRepository;

    @Transactional // 이력서 작성
    public ResumeResponse.SaveDTO createResume(ResumeRequest.PostDTO requestDTO, SessionUser sessionUser) {
        User user = userJpaRepository.findById(sessionUser.getId()).orElseThrow(() -> new Exception404("조회된 데이터가 없습니다."));
        Resume resume = new Resume(requestDTO, user);
        resumeJpaRepository.save(resume);
        return new ResumeResponse.SaveDTO(resume);
    }

    @Transactional(readOnly = true) // 이력서 상세보기
    public ResumeResponse.DetailDTO getResumeInDetail(int resumeId, Integer sessionUserId) {
        Resume resume = resumeJpaRepository.findById(resumeId).orElseThrow(() -> new Exception404("조회된 이력서가 없습니다."));
        Double rating = ratingJpaRepository.findRatingAvgByUserId(resume.getUser().getId()).orElse(0.0);

        boolean isResumeOwner = false;
        boolean hasSubscribed = false;
        boolean hasRated = false;
        if (sessionUserId != null) {
            isResumeOwner = resume.getUser().getId() == sessionUserId ? true : false;
            Optional<Subscribe> optionalSubscribe = subscribeJpaRepository.findByResumeIdAndUserId(resume.getId(), sessionUserId);
            hasSubscribed = optionalSubscribe.isPresent() ? true : false;
            Optional<Rating> optionalRating = ratingJpaRepository.findByRaterIdAndSubjectId(sessionUserId, resume.getUser().getId());
            hasRated = optionalRating.isPresent() ? true : false;
        }
        // DTO가 완성되는 시점까지 DB 연결 유지
        return new ResumeResponse.DetailDTO(resume, rating, isResumeOwner, hasSubscribed, hasRated);
    }

    @Transactional(readOnly = true) // 이력서 목록보기
    public ResumeResponse.ListingsDTO getAllResume(Pageable pageable, Integer userId, String skill, String address, String keyword) {
        List<Resume> resumeList; // 이력서..
        if (userId != null) { // 작성자 id 검색
            resumeList = resumeJpaRepository.findByUserId(userId).orElseThrow(() -> new Exception404("조회된 이력서가 없습니다."));
        } else if (skill != null) { // 스킬 검색
            resumeList = resumeJpaRepository.findAllJoinUserWithSkill(skill).orElseThrow(() -> new Exception404("조회된 이력서가 없습니다."));
        } else if (address != null) { // 지역 검색
            resumeList = resumeJpaRepository.findAllJoinUserWithAddress(address).orElseThrow(() -> new Exception404("조회된 이력서가 없습니다."));
        } else if (keyword != null) { // 키워드 검색
            resumeList = resumeJpaRepository.findAllJoinUserWithKeyword(keyword).orElseThrow(() -> new Exception404("조회된 이력서가 없습니다."));
        } else { // 전체 보기
            resumeList = resumeJpaRepository.findAllJoinUser().orElseThrow(() -> new Exception404("조회된 이력서가 없습니다."));
        }
        List<User> userList; // 추천 인재 (스킬 기반 매칭) (빈 컬랙션 DTO에서 처리)
        userList = userJpaRepository.findUsersWithMostSkills()
                .orElseThrow(() -> new Exception500("DB 조회 불가"));
        // DTO가 완성되는 시점까지 DB 연결 유지
        return new ResumeResponse.ListingsDTO(pageable, resumeList, userList, skill, address, keyword);
    }

    // 이력서 수정 폼
    public ResumeResponse.UpdateFormDTO getResume(int id, User sessionUser) {
        // 조회 및 예외처리
        Resume resume = resumeJpaRepository.findById(id)
                .orElseThrow(() -> new Exception404("해당 이력서를 찾을 수 없습니다."));

        // 권한 처리
        if (sessionUser.getId() != resume.getUser().getId()) {
            throw new Exception403("해당 이력서의 수정페이지로 이동할 권한이 없습니다.");
        }

        return new ResumeResponse.UpdateFormDTO(resumeJpaRepository.findById(id).get());
    }

    @Transactional // 이력서 수정
    public ResumeResponse.UpdateDTO setResume(int id, ResumeRequest.UpdateDTO requestDTO, SessionUser sessionUser) {
        // 조회 및 예외처리
        Resume resume = resumeJpaRepository.findById(id)
                .orElseThrow(() -> new Exception404("해당 이력서를 찾을 수 없습니다."));

        // 권한 처리
        if (sessionUser.getId() != resume.getUser().getId()) {
            throw new Exception403("해당 이력서를 수정할 권한이 없습니다.");
        }

        resume.update(requestDTO);

        return new ResumeResponse.UpdateDTO(resume);
    }

    @Transactional // 이력서 삭제
    public void removeResume(int id, SessionUser sessionUser) {
        // 조회 및 예외처리
        Resume resume = resumeJpaRepository.findById(id)
                .orElseThrow(() -> new Exception404("해당 이력서를 찾을 수 없습니다."));

        // 권한 처리
        if (sessionUser.getId() != resume.getUser().getId()) {
            throw new Exception403("해당 이력서를 삭제할 권한이 없습니다.");
        }

        resumeJpaRepository.delete(resume);
    }
}
