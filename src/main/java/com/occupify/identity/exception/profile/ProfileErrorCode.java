package com.occupify.identity.exception.profile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProfileErrorCode {

    EXPERIENCE_NOT_FOUND("EXP_404", "Experience record not found", HttpStatus.NOT_FOUND),
    EDUCATION_NOT_FOUND("EDU_404", "Education record not found", HttpStatus.NOT_FOUND),
    QUALIFICATION_NOT_FOUND("QUAL_404", "Qualification record not found", HttpStatus.NOT_FOUND),
    SKILL_NOT_FOUND("SKILL_404", "Skill not found in system catalog", HttpStatus.NOT_FOUND),
    USER_SKILL_NOT_FOUND("USKILL_404", "User skill association not found", HttpStatus.NOT_FOUND),
    SKILL_ALREADY_EXISTS_ON_PROFILE("USKILL_409", "Skill already added to user profile", HttpStatus.CONFLICT),
    INVALID_DATE_RANGE("DATE_400", "End date cannot be earlier than start date", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
