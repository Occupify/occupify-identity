package com.occupify.identity.service.institution.impl;

import com.occupify.identity.dto.response.profile.InstitutionResponse;
import com.occupify.identity.mapper.UserProfileMapper;
import com.occupify.identity.repository.InstitutionRepository;
import com.occupify.identity.service.institution.InstitutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class InstitutionServiceImpl implements InstitutionService {

    private final InstitutionRepository institutionRepository;
    private final UserProfileMapper userProfileMapper;

    public InstitutionServiceImpl(InstitutionRepository institutionRepository, UserProfileMapper userProfileMapper) {
        this.institutionRepository = institutionRepository;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public List<InstitutionResponse> searchInstitutions(String keyword) {
        log.debug("Searching institutions catalog with keyword: {}", keyword);
        if (keyword != null && !keyword.trim().isBlank()) {
            return institutionRepository.searchByNameOrCode(keyword.trim())
                    .stream()
                    .map(userProfileMapper::toInstitutionResponse)
                    .toList();
        }
        return institutionRepository.findTop20ByOrderByNameAsc()
                .stream()
                .map(userProfileMapper::toInstitutionResponse)
                .toList();
    }
}
