package com.occupify.identity.controller;

import com.occupify.identity.constant.SecurityConstants;
import com.occupify.identity.dto.base.CreatedResponse;
import com.occupify.identity.dto.base.ErrorResponse;
import com.occupify.identity.dto.base.PageResponse;
import com.occupify.identity.dto.base.SingleResponse;
import com.occupify.identity.dto.base.SuccessResponse;
import com.occupify.identity.exception.auth.AuthErrorCode;
import com.occupify.identity.exception.auth.AuthException;
import com.occupify.identity.security.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public abstract class AbstractBaseController {

    @Autowired(required = false)
    protected ResponseFactory responseFactory = new ResponseFactory();

    protected <T> SingleResponse<T> successSingle(T data, String message) {
        return responseFactory.createSingleResponse(HttpStatus.OK, message, data);
    }

    protected SuccessResponse success(String message) {
        return responseFactory.createSuccessResponse(HttpStatus.OK, message);
    }

    protected <T> CreatedResponse<T> created(T data, String message) {
        return responseFactory.createCreatedResponse(HttpStatus.CREATED, message, data);
    }

    protected <T> PageResponse<T> paging(List<T> data, int page, int size, long totalElements, int totalPages,
            String message) {
        return responseFactory.createPageResponse(HttpStatus.OK, message, data, page, size, totalElements, totalPages);
    }

    protected <T> ResponseEntity<SingleResponse<T>> successSingleEntity(T data, String message) {
        return responseFactory.successSingle(data, message);
    }

    protected ResponseEntity<SuccessResponse> successEntity(String message) {
        return responseFactory.success(message);
    }

    protected <T> ResponseEntity<CreatedResponse<T>> createdEntity(T data, String message) {
        return responseFactory.created(data, message);
    }

    protected ResponseEntity<ErrorResponse> errorEntity(HttpStatus status, String message, String errorCode) {
        return responseFactory.error(status, message, errorCode);
    }

    protected UUID resolveUserId(String headerUserId, HttpServletRequest request,
            JwtUtils jwtUtils) {
        if (headerUserId != null && !headerUserId.isBlank()) {
            try {
                return UUID.fromString(headerUserId.trim());
            } catch (IllegalArgumentException e) {
                throw new AuthException(
                        AuthErrorCode.AUTH_003,
                        "Invalid user ID format in X-User-Id header");
            }
        }
        if (request != null && jwtUtils != null) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null
                    && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
                String token = authHeader
                        .substring(SecurityConstants.BEARER_PREFIX.length()).trim();
                if (jwtUtils.validateToken(token)) {
                    String uid = jwtUtils.extractUserId(token);
                    if (uid != null) {
                        try {
                            return UUID.fromString(uid);
                        } catch (IllegalArgumentException e) {
                            throw new AuthException(
                                    AuthErrorCode.AUTH_003,
                                    "Invalid user ID encoded in JWT token");
                        }
                    }
                }
            }
        }
        throw new AuthException(
                AuthErrorCode.AUTH_003,
                "Authentication required: missing X-User-Id header or valid Bearer token");
    }
}
