package com.occupify.identity.controller.institution;

import com.occupify.identity.controller.AbstractBaseController;
import com.occupify.identity.dto.base.SingleResponse;
import com.occupify.identity.dto.response.profile.InstitutionResponse;
import com.occupify.identity.service.institution.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({ "/user/institutions", "/users/institutions" })
@Tag(name = "Institutions", description = "Endpoints for searching educational institutions catalog")
public class InstitutionController extends AbstractBaseController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    @Operation(summary = "Search educational institutions catalog", description = "Searches institutions catalog by name or code substring, or retrieves top institutions when keyword is empty")
    public SingleResponse<List<InstitutionResponse>> searchInstitutions(
            @Parameter(description = "Keyword to filter institution catalog by name or code") @RequestParam(value = "keyword", required = false) String keyword) {
        log.info("[GET /user/institutions] Searching institutions catalog with keyword: {}", keyword);
        List<InstitutionResponse> response = institutionService.searchInstitutions(keyword);
        return successSingle(response, "Institutions retrieved successfully");
    }
}
