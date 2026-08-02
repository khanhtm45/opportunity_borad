package com.opportunityboard.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunityboard.application.dto.student.StudentProfileResponse;
import com.opportunityboard.application.dto.student.StudentProfileUpdateRequest;
import com.opportunityboard.common.exception.BadRequestException;
import com.opportunityboard.common.exception.ForbiddenException;
import com.opportunityboard.domain.entity.StudentProfile;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.UserRole;
import com.opportunityboard.infrastructure.repository.StudentProfileRepository;
import com.opportunityboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;

    public StudentProfileResponse getMine() {
        User user = requireStudent();
        StudentProfile profile = studentProfileRepository.findByUserUserId(user.getUserId())
                .orElse(null);
        return toResponse(user, profile);
    }

    @Transactional
    public StudentProfileResponse updateMine(StudentProfileUpdateRequest req) {
        User user = requireStudent();
        StudentProfile profile = studentProfileRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> StudentProfile.builder().user(user).build());

        if (req.major() != null) profile.setMajor(blankToNull(req.major()));
        if (req.university() != null) profile.setUniversity(blankToNull(req.university()));
        if (req.universityYear() != null) profile.setUniversityYear(req.universityYear());
        if (req.bio() != null) profile.setBio(blankToNull(req.bio()));
        if (req.cvUrl() != null) {
            String cv = blankToNull(req.cvUrl());
            if (cv != null) validateCvUrl(cv);
            profile.setCvUrl(cv);
        }
        if (req.skills() != null) {
            profile.setSkills(writeSkills(req.skills()));
        }
        profile.setUpdatedAt(Instant.now());
        profile = studentProfileRepository.save(profile);
        return toResponse(user, profile);
    }

    /** CV đã lưu trên hồ sơ — dùng khi apply không gửi cvFile. */
    public String resolveCvUrlForCurrentStudent() {
        User user = currentUser.get();
        if (user.getRole() != UserRole.STUDENT) return null;
        return studentProfileRepository.findByUserUserId(user.getUserId())
                .map(StudentProfile::getCvUrl)
                .filter(u -> u != null && !u.isBlank())
                .orElse(null);
    }

    private User requireStudent() {
        User user = currentUser.get();
        if (user.getRole() != UserRole.STUDENT) {
            throw new ForbiddenException("Chỉ sinh viên quản lý hồ sơ & CV");
        }
        return user;
    }

    private static void validateCvUrl(String cv) {
        String lower = cv.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("ob-s3://"))) {
            throw new BadRequestException("CV phải là https:// hoặc ob-s3:// (upload S3 private)");
        }
    }

    private StudentProfileResponse toResponse(User user, StudentProfile profile) {
        String cv = profile != null ? profile.getCvUrl() : null;
        return new StudentProfileResponse(
                profile != null ? profile.getProfileId() : null,
                user.getFullName(),
                user.getEmail(),
                profile != null ? profile.getMajor() : null,
                profile != null ? profile.getUniversity() : null,
                profile != null ? profile.getUniversityYear() : null,
                cv,
                cv != null && !cv.isBlank(),
                profile != null ? readSkills(profile.getSkills()) : List.of(),
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getUpdatedAt() : null
        );
    }

    private List<String> readSkills(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("skills JSON parse: {}", ex.getMessage());
            return List.of();
        }
    }

    private String writeSkills(List<String> skills) {
        List<String> cleaned = new ArrayList<>();
        if (skills != null) {
            for (String s : skills) {
                if (s != null && !s.isBlank()) cleaned.add(s.trim());
            }
        }
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (Exception ex) {
            throw new BadRequestException("skills không hợp lệ");
        }
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
