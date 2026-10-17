package com.occupify.identity.service.institution;

import com.occupify.identity.dto.response.profile.InstitutionResponse;

import java.util.List;

public interface InstitutionService {

    List<InstitutionResponse> searchInstitutions(String keyword);
}
