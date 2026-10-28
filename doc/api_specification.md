# Occupify Identity - API Specification

> [!NOTE]
> **Base Port & Gateway Routing:**
> - Direct Service Port: `8181` (Internal)
> - Gateway Port: `8180` (External ingress entry point)
> - Gateway Routes: `/auth/**`, `/account/**`, `/users/**`, `/user/**`
> - Database Port: PostgreSQL `5530` (`occupify_identity`), Redis `6379`, RabbitMQ `5672`

---

## Standard Response Envelopes

Every API endpoint returns a standardized response envelope:

### Single Data Response (`SingleResponse<T>`)
```json
{
  "statusCode": 200,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

### Resource Created Response (`CreatedResponse<T>`)
```json
{
  "statusCode": 201,
  "message": "Resource created successfully",
  "data": { ... }
}
```

### Generic Action Success Response (`SuccessResponse`)
```json
{
  "statusCode": 200,
  "message": "Action completed successfully"
}
```

### Error Response (`ErrorResponse`)
```json
{
  "statusCode": 400,
  "errorCode": "AUTH_001",
  "message": "Email address is already in use",
  "timestamp": "2026-10-18T22:30:00Z"
}
```

---

## 1. Authentication Endpoints (`/auth`)

### 1.1 Register Account
- **Endpoint:** `POST /auth/register`
- **Description:** Registers a new user account with email and password. Generates a registration OTP and publishes `UserRegisteredEvent` to RabbitMQ (`occupify.notification.exchange` with routing key `user.registered`).
- **Authorization:** Public
- **Headers:** `Content-Type: application/json`

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `email` | String | Yes | Valid email format, not blank | User registration email |
| `password` | String | Yes | Min 8 chars, not blank | Secure user password |
| `institutionId` | UUID | No | Valid institution UUID | Optional educational institution |

#### Request Body Example
```json
{
  "email": "candidate@occupify.work",
  "password": "SecurePassword123!",
  "institutionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

#### Response Example (`201 CREATED`)
```json
{
  "statusCode": 201,
  "message": "Account registered successfully. Verification OTP has been sent to your email."
}
```

---

### 1.2 Resend Registration OTP
- **Endpoint:** `POST /auth/resend-otp`
- **Description:** Generates and dispatches a fresh 6-digit OTP if the previous one expired or was not received. Rate-limited to prevent abuse (`AUTH_007`).
- **Authorization:** Public
- **Headers:** `Content-Type: application/json`

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `email` | String | Yes | Valid email, not blank | User registered email |
| `type` | String (Enum) | No | `REGISTER`, `FORGOT_PASSWORD` | Optional OTP type (auto-detected if omitted) |

#### Request Body Example
```json
{
  "email": "candidate@occupify.work",
  "type": "REGISTER"
}
```

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "OTP resent to your email successfully"
}
```

---

### 1.3 Verify Registration / Reset OTP
- **Endpoint:** `POST /auth/verify-otp`
- **Description:** Validates 6-digit verification OTP. For `REGISTER`, it activates the user account (`status = ACTIVE`) and issues tokens (`AuthResponse`). For `FORGOT_PASSWORD`, it returns a temporary password reset token (`VerifyOtpResponse`).
- **Authorization:** Public
- **Headers:** `Content-Type: application/json`

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `email` | String | Yes | Valid email, not blank | User registered email |
| `otpCode` | String | Yes | Exact 6-digit number (regex `^\d{6}$`). Aliases: `code`, `otp` | Verification OTP code |
| `type` | String (Enum) | No | `REGISTER`, `FORGOT_PASSWORD` | Optional OTP type |

#### Request Body Example
```json
{
  "email": "candidate@occupify.work",
  "otpCode": "123456",
  "type": "REGISTER"
}
```

#### Response Example for Registration (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "OTP verified successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "7c8e9f10-1234-5678-abcd-ef0123456789",
    "user": {
      "id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
      "email": "candidate@occupify.work",
      "role": "USER",
      "status": "ACTIVE",
      "createdAt": "2026-10-18T10:00:00Z",
      "institutionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "institutionName": "University of Information Technology"
    }
  }
}
```

#### Response Example for Forgot Password (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "OTP verified successfully",
  "data": {
    "resetToken": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

### 1.4 User Login
- **Endpoint:** `POST /auth/login`
- **Description:** Authenticates user via email and password. Returns JWT access token and long-lived refresh token.
- **Authorization:** Public
- **Headers:** `Content-Type: application/json`

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `email` | String | Yes | Valid email, not blank | User email |
| `password` | String | Yes | Not blank | User password |

#### Request Body Example
```json
{
  "email": "candidate@occupify.work",
  "password": "SecurePassword123!"
}
```

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "7c8e9f10-1234-5678-abcd-ef0123456789",
    "user": {
      "id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
      "email": "candidate@occupify.work",
      "role": "USER",
      "status": "ACTIVE",
      "createdAt": "2026-10-18T10:00:00Z",
      "institutionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "institutionName": "University of Information Technology"
    }
  }
}
```

---

### 1.5 Refresh Access Token
- **Endpoint:** `POST /auth/refresh-token`
- **Description:** Obtains a new JWT access token using a valid, non-revoked refresh token. The token can be supplied in the request body, in the `X-Refresh-Token` header, or in the `Authorization: Bearer <refresh_token>` header.
- **Authorization:** Public
- **Headers:** `Content-Type: application/json` (optional `X-Refresh-Token` header)

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `refreshToken` | String | Conditional | Valid refresh token UUID string | Required if not passed in header |

#### Request Body Example
```json
{
  "refreshToken": "7c8e9f10-1234-5678-abcd-ef0123456789"
}
```

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.newAccessToken...",
    "refreshToken": "7c8e9f10-1234-5678-abcd-ef0123456789",
    "user": {
      "id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
      "email": "candidate@occupify.work",
      "role": "USER",
      "status": "ACTIVE",
      "createdAt": "2026-10-18T10:00:00Z"
    }
  }
}
```

---

### 1.6 Sign Out / Revoke Session
- **Endpoint:** `POST /auth/sign-out`
- **Description:** Invalidates active session, blacklisting JWT and revoking the refresh token in Redis. Can be supplied via body, `X-Refresh-Token` header, or `Authorization` header.
- **Authorization:** Public / Authenticated

#### Request Body Example
```json
{
  "refreshToken": "7c8e9f10-1234-5678-abcd-ef0123456789"
}
```

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Signed out successfully"
}
```

---

### 1.7 Forgot Password Request
- **Endpoint:** `POST /auth/forgot-password`
- **Description:** Initiates password reset by publishing `PasswordResetRequestedEvent` to RabbitMQ to send a 6-digit reset OTP to the user's email.
- **Authorization:** Public

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `email` | String | Yes | Valid email format, not blank | Registered user email |

#### Request Body Example
```json
{
  "email": "candidate@occupify.work"
}
```

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Password reset OTP sent to your email"
}
```

---

### 1.8 Reset Password With Token
- **Endpoint:** `POST /auth/reset-password`
- **Description:** Resets user password using the `resetToken` received from `/auth/verify-otp`. Token can be in body, `X-Reset-Token` header, or `Authorization` header.
- **Authorization:** Public

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `newPassword` | String | Yes | Min 8 chars, not blank | New password |
| `confirmPassword` | String | Yes | Must match `newPassword` | Password confirmation |
| `resetToken` | String | Conditional | Valid reset token | Optional if sent via header |

#### Request Body Example
```json
{
  "newPassword": "BrandNewPassword2026!",
  "confirmPassword": "BrandNewPassword2026!",
  "resetToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Password reset successfully. Please sign in with your new password."
}
```

---

### 1.9 Change Password (Authenticated)
- **Endpoint:** `POST /auth/change-password`
- **Description:** Allows an authenticated user to change their password by validating their current password.
- **Authorization:** Authenticated (`Authorization: Bearer <token>` or `X-User-Email` header)

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `currentPassword` | String | Yes | Not blank | Existing password |
| `newPassword` | String | Yes | Min 8 chars, not blank, not identical to current | New password |

#### Request Body Example
```json
{
  "currentPassword": "SecurePassword123!",
  "newPassword": "UpdatedPassword456!"
}
```

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Password changed successfully"
}
```

---

## 2. User Account Management (`/users`)

All endpoints in this section require authentication via `Authorization: Bearer <jwt_access_token>` or Gateway header injection (`X-User-Id: <uuid>`).

### 2.1 Get Current User Basic Profile
- **Endpoint:** `GET /users/me`
- **Description:** Retrieves basic profile and account status of the authenticated user.
- **Authorization:** Authenticated

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "User profile retrieved successfully",
  "data": {
    "id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
    "email": "candidate@occupify.work",
    "fullName": "Alex Mercer",
    "headline": "Senior Full-Stack Engineer & Cloud Architect",
    "avatarUrl": "https://storage.occupify.work/avatars/c1a2b3c4-avatar.jpg",
    "bio": "Specialized in microservices, Spring Boot, and scalable cloud applications.",
    "phoneNumber": "+84987654321",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-10-18T10:00:00Z",
    "updatedAt": "2026-10-18T12:00:00Z",
    "institutionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "institutionName": "University of Information Technology"
  }
}
```

---

### 2.2 Get Current User Full 360-Degree Profile Detail
- **Endpoint:** `GET /users/me/detail`
- **Description:** Retrieves comprehensive user profile including work experiences, educations, qualifications, and skills.
- **Authorization:** Authenticated

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "User full profile detail retrieved successfully",
  "data": {
    "user": {
      "id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
      "email": "candidate@occupify.work",
      "fullName": "Alex Mercer",
      "headline": "Senior Full-Stack Engineer",
      "avatarUrl": "https://storage.occupify.work/avatars/c1a2b3c4-avatar.jpg",
      "bio": "Experienced backend developer passionate about distributed architectures.",
      "phoneNumber": "+84987654321",
      "role": "USER",
      "status": "ACTIVE",
      "createdAt": "2026-10-18T10:00:00Z",
      "updatedAt": "2026-10-18T12:00:00Z",
      "institutionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "institutionName": "University of Information Technology"
    },
    "experiences": [
      {
        "id": "11111111-2222-3333-4444-555555555555",
        "title": "Lead Software Engineer",
        "organization": "TechCorp Global",
        "employmentType": "FULL_TIME",
        "location": "Ho Chi Minh City",
        "locationType": "HYBRID",
        "startDate": "2023-01-01",
        "endDate": null,
        "isCurrent": true,
        "description": "Led development of core payment microservice."
      }
    ],
    "educations": [
      {
        "id": "22222222-3333-4444-5555-666666666666",
        "institutionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "institutionName": "University of Information Technology",
        "institutionLogoUrl": "https://storage.occupify.work/institutions/uit.png",
        "fieldOfStudy": "Software Engineering",
        "degree": "Bachelor of Science",
        "grade": "3.8/4.0",
        "description": "Graduated with High Honors.",
        "startDate": "2019-09-01",
        "endDate": "2023-06-30",
        "documentUrl": "https://storage.occupify.work/documents/diploma.pdf"
      }
    ],
    "qualifications": [
      {
        "id": "33333333-4444-5555-6666-777777777777",
        "title": "AWS Certified Solutions Architect - Associate",
        "organization": "Amazon Web Services",
        "issueDate": "2024-05-15",
        "expirationDate": "2027-05-15",
        "credentialId": "AWS-PSA-123456",
        "credentialUrl": "https://www.credly.com/badges/sample-id",
        "documentUrl": "https://storage.occupify.work/documents/cert.pdf"
      }
    ],
    "skills": [
      {
        "skillId": "44444444-5555-6666-7777-888888888888",
        "skillName": "Java",
        "category": "Backend",
        "isTopSkill": true,
        "proficiencyLevel": "EXPERT",
        "yearsOfExperience": 5
      }
    ]
  }
}
```

---

### 2.3 Get Public User Profile Detail
- **Endpoint:** `GET /users/{id}/detail`
- **Description:** Retrieves public profile detail for candidate discovery and hiring review.
- **Authorization:** Public
- **Path Parameter:** `id` (UUID of candidate)

#### Response Example (`200 OK`)
Returns `SingleResponse<UserDetailResponse>` with identical structure to `/users/me/detail`.

---

### 2.4 Update Current User Profile
- **Endpoint:** `PUT /users/me`
- **Description:** Updates personal details (full name, headline, bio, phone number).
- **Authorization:** Authenticated

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `fullName` | String | Yes | Max 150 chars, not blank | Full name |
| `headline` | String | No | Max 255 chars | Professional title / headline |
| `bio` | String | No | Unbounded text | About me biography |
| `phoneNumber` | String | No | Regex `^[+0-9\-\s()]{7,30}$` | Contact phone number |

#### Request Body Example
```json
{
  "fullName": "Alex Mercer",
  "headline": "Lead Backend Architect & Distributed Systems Engineer",
  "bio": "Over 7 years building resilient cloud-native systems with Spring Boot, RabbitMQ, and PostgreSQL.",
  "phoneNumber": "+84987654321"
}
```

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "User profile updated successfully",
  "data": {
    "id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
    "email": "candidate@occupify.work",
    "fullName": "Alex Mercer",
    "headline": "Lead Backend Architect & Distributed Systems Engineer",
    "avatarUrl": "https://storage.occupify.work/avatars/c1a2b3c4-avatar.jpg",
    "bio": "Over 7 years building resilient cloud-native systems with Spring Boot, RabbitMQ, and PostgreSQL.",
    "phoneNumber": "+84987654321",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-10-18T10:00:00Z",
    "updatedAt": "2026-10-18T12:30:00Z"
  }
}
```

---

### 2.5 Delete User Account (Soft Delete)
- **Endpoint:** `DELETE /users/me`
- **Description:** Soft-deletes user account by setting `status = DELETED` and invalidating sessions.
- **Authorization:** Authenticated

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "User account deleted successfully"
}
```

---

### 2.6 Upload Profile Avatar
- **Endpoint:** `POST /users/me/avatar`
- **Consumes:** `multipart/form-data`
- **Description:** Uploads a profile avatar to S3/OCI storage.
- **Constraints:** Allowed types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`. Max file size: 5MB.
- **Authorization:** Authenticated

#### Multipart Form Parameters
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `file` | Binary (File) | Yes | Image file (JPEG, PNG, WEBP, GIF $\le$ 5MB) |

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Avatar uploaded successfully",
  "data": {
    "avatarUrl": "https://storage.occupify.work/avatars/c1a2b3c4-updated.png"
  }
}
```

---

### 2.7 Delete Profile Avatar
- **Endpoint:** `DELETE /users/me/avatar`
- **Description:** Deletes active avatar file from cloud storage and resets `avatarUrl` to null.
- **Authorization:** Authenticated

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Avatar deleted successfully"
}
```

---

## 3. Career Profile Sub-Resources (`/users/me/*`)

All endpoints in this section operate on career sub-collections for the authenticated user (`@Authenticated UUID userId`).

### 3.1 Work Experience

#### Add Work Experience
- **Endpoint:** `POST /users/me/experiences`
- **Authorization:** Authenticated

##### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `title` | String | Yes | Max 150 chars, not blank | Job title |
| `organization` | String | Yes | Max 200 chars, not blank | Company / employer name |
| `employmentType` | String (Enum) | No | `FULL_TIME`, `PART_TIME`, `SELF_EMPLOYED`, `FREELANCE`, `CONTRACT`, `INTERNSHIP`, `APPRENTICESHIP`, `SEASONAL` | Employment arrangement |
| `location` | String | No | Max 150 chars | City / Region |
| `locationType` | String (Enum) | No | `ON_SITE`, `HYBRID`, `REMOTE` | Workplace modality |
| `startDate` | String (Date) | Yes | ISO format (`YYYY-MM-DD`) | Employment start date |
| `endDate` | String (Date) | No | ISO format (`YYYY-MM-DD`) | End date (null if current) |
| `isCurrent` | Boolean | No | Default `false` | True if currently working here |
| `description` | String | No | Unbounded | Responsibilities and accomplishments |

##### Request Body Example
```json
{
  "title": "Senior Backend Developer",
  "organization": "FinTech Innovators Ltd",
  "employmentType": "FULL_TIME",
  "location": "Ho Chi Minh City, Vietnam",
  "locationType": "HYBRID",
  "startDate": "2023-02-01",
  "endDate": null,
  "isCurrent": true,
  "description": "Architected low-latency transaction processing engine handling 10k RPS."
}
```

##### Response Example (`201 CREATED`)
```json
{
  "statusCode": 201,
  "message": "Work experience created successfully",
  "data": {
    "id": "11111111-2222-3333-4444-555555555555",
    "title": "Senior Backend Developer",
    "organization": "FinTech Innovators Ltd",
    "employmentType": "FULL_TIME",
    "location": "Ho Chi Minh City, Vietnam",
    "locationType": "HYBRID",
    "startDate": "2023-02-01",
    "endDate": null,
    "isCurrent": true,
    "description": "Architected low-latency transaction processing engine handling 10k RPS."
  }
}
```

#### Update Work Experience
- **Endpoint:** `PUT /users/me/experiences/{id}`
- **Authorization:** Authenticated
- **Path Parameter:** `id` (UUID of experience entry)
- **Request Body:** Same schema as `POST /users/me/experiences`.
- **Response:** `SingleResponse<ExperienceItemResponse>`.

#### Delete Work Experience
- **Endpoint:** `DELETE /users/me/experiences/{id}`
- **Authorization:** Authenticated
- **Path Parameter:** `id` (UUID of experience entry)
- **Response Example (`200 OK`):**
```json
{
  "statusCode": 200,
  "message": "Work experience deleted successfully"
}
```

---

### 3.2 Education Records

#### Add Education Record
- **Endpoint:** `POST /users/me/educations`
- **Consumes:** `multipart/form-data`
- **Authorization:** Authenticated

##### Form Parameters
| Parameter | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `institutionId` | UUID | Yes | Must exist in institutions catalog | Educational institution ID |
| `fieldOfStudy` | String | Yes | Max 150 chars, not blank | Major / Academic field |
| `degree` | String | No | Max 150 chars | Degree earned (e.g., Bachelor) |
| `grade` | String | No | Max 50 chars | GPA or final classification |
| `description` | String | No | Unbounded | Activities, thesis, or coursework |
| `startDate` | String (Date) | Yes | ISO format (`YYYY-MM-DD`) | Study commencement date |
| `endDate` | String (Date) | No | ISO format (`YYYY-MM-DD`) | Graduation date |
| `document` | Binary (File) | No | PDF, JPEG, PNG, WEBP $\le$ 10MB | Degree certificate or diploma |

##### Multipart Form-Data Example (cURL)
```bash
curl -X POST "http://localhost:8180/users/me/educations" \
  -H "Authorization: Bearer <token>" \
  -F "institutionId=3fa85f64-5717-4562-b3fc-2c963f66afa6" \
  -F "fieldOfStudy=Computer Science" \
  -F "degree=Bachelor of Engineering" \
  -F "grade=3.85 / 4.0" \
  -F "description=Valedictorian, Specialization in Cloud Computing" \
  -F "startDate=2019-09-01" \
  -F "endDate=2023-06-30" \
  -F "document=@/path/to/diploma.pdf;type=application/pdf"
```

##### Response Example (`201 CREATED`)
```json
{
  "statusCode": 201,
  "message": "Education record created successfully",
  "data": {
    "id": "22222222-3333-4444-5555-666666666666",
    "institutionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "institutionName": "University of Information Technology",
    "institutionLogoUrl": "https://storage.occupify.work/institutions/uit.png",
    "fieldOfStudy": "Computer Science",
    "degree": "Bachelor of Engineering",
    "grade": "3.85 / 4.0",
    "description": "Valedictorian, Specialization in Cloud Computing",
    "startDate": "2019-09-01",
    "endDate": "2023-06-30",
    "documentUrl": "https://storage.occupify.work/documents/diploma-abc.pdf"
  }
}
```

#### Update Education Record
- **Endpoint:** `PUT /users/me/educations/{id}`
- **Consumes:** `multipart/form-data`
- **Path Parameter:** `id` (UUID of education entry)
- **Form Parameters:** Same as create, with optional `removeDocument` (Boolean) to delete existing proof document.
- **Response:** `SingleResponse<EducationItemResponse>`.

#### Delete Education Record
- **Endpoint:** `DELETE /users/me/educations/{id}`
- **Path Parameter:** `id` (UUID of education entry)
- **Response:** `SuccessResponse` ("Education record deleted successfully").

---

### 3.3 Qualifications / Certifications

#### Add Qualification Record
- **Endpoint:** `POST /users/me/qualifications`
- **Consumes:** `multipart/form-data`
- **Authorization:** Authenticated

##### Form Parameters
| Parameter | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `title` | String | Yes | Max 200 chars, not blank | Certification title |
| `organization` | String | Yes | Max 200 chars, not blank | Issuing authority |
| `issueDate` | String (Date) | Yes | ISO format (`YYYY-MM-DD`) | Date earned |
| `expirationDate` | String (Date) | No | ISO format (`YYYY-MM-DD`) | Expiration date |
| `credentialId` | String | No | Max 100 chars | License or credential number |
| `credentialUrl` | String | No | Max 500 chars | Online verification URL |
| `document` | Binary (File) | No | PDF, JPEG, PNG, WEBP $\le$ 10MB | Certificate document |

##### Multipart Form-Data Example (cURL)
```bash
curl -X POST "http://localhost:8180/users/me/qualifications" \
  -H "Authorization: Bearer <token>" \
  -F "title=Certified Kubernetes Administrator (CKA)" \
  -F "organization=Cloud Native Computing Foundation (CNCF)" \
  -F "issueDate=2024-03-15" \
  -F "expirationDate=2027-03-15" \
  -F "credentialId=CKA-2400-00123" \
  -F "credentialUrl=https://www.cncf.io/certification/verify/CKA-2400-00123" \
  -F "document=@/path/to/cka_cert.pdf;type=application/pdf"
```

##### Response Example (`201 CREATED`)
```json
{
  "statusCode": 201,
  "message": "Qualification record created successfully",
  "data": {
    "id": "33333333-4444-5555-6666-777777777777",
    "title": "Certified Kubernetes Administrator (CKA)",
    "organization": "Cloud Native Computing Foundation (CNCF)",
    "issueDate": "2024-03-15",
    "expirationDate": "2027-03-15",
    "credentialId": "CKA-2400-00123",
    "credentialUrl": "https://www.cncf.io/certification/verify/CKA-2400-00123",
    "documentUrl": "https://storage.occupify.work/documents/cka-cert.pdf"
  }
}
```

#### Update Qualification Record
- **Endpoint:** `PUT /users/me/qualifications/{id}`
- **Consumes:** `multipart/form-data`
- **Path Parameter:** `id` (UUID of qualification entry)
- **Form Parameters:** Same as create, plus optional `removeDocument` (Boolean).
- **Response:** `SingleResponse<QualificationItemResponse>`.

#### Delete Qualification Record
- **Endpoint:** `DELETE /users/me/qualifications/{id}`
- **Path Parameter:** `id` (UUID of qualification entry)
- **Response:** `SuccessResponse` ("Qualification record deleted successfully").

---

### 3.4 User Skills

#### Search Skills Catalog
- **Endpoint:** `GET /users/me/skills`
- **Description:** Searches system skills catalog by name substring, or returns full catalog if `keyword` is omitted.
- **Authorization:** Public / Authenticated
- **Query Parameter:** `keyword` (String, optional)

##### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Skills retrieved successfully",
  "data": [
    {
      "id": "44444444-5555-6666-7777-888888888888",
      "name": "Java",
      "category": "Backend"
    },
    {
      "id": "55555555-6666-7777-8888-999999999999",
      "name": "Spring Boot",
      "category": "Backend"
    }
  ]
}
```

#### Attach Skill to Profile
- **Endpoint:** `POST /users/me/skills`
- **Authorization:** Authenticated

##### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `skillId` | UUID | Conditional | Must exist in catalog if provided | ID of existing catalog skill |
| `skillName` | String | Conditional | Required if `skillId` is null | Custom skill name |
| `category` | String | No | Freeform | Skill category (e.g. Backend) |
| `isTopSkill` | Boolean | No | Default `false` | Highlighted on profile |
| `proficiencyLevel` | String (Enum) | No | `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT` | Level of mastery |
| `yearsOfExperience`| Integer | No | Must be $\ge 0$ | Total experience years |

##### Request Body Example
```json
{
  "skillId": "44444444-5555-6666-7777-888888888888",
  "isTopSkill": true,
  "proficiencyLevel": "EXPERT",
  "yearsOfExperience": 5
}
```

##### Response Example (`201 CREATED`)
```json
{
  "statusCode": 201,
  "message": "Skill attached to profile successfully",
  "data": {
    "skillId": "44444444-5555-6666-7777-888888888888",
    "skillName": "Java",
    "category": "Backend",
    "isTopSkill": true,
    "proficiencyLevel": "EXPERT",
    "yearsOfExperience": 5
  }
}
```

#### Update Skill Attributes
- **Endpoint:** `PUT /users/me/skills/{skillId}`
- **Authorization:** Authenticated
- **Path Parameter:** `skillId` (UUID of attached skill)

##### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `isTopSkill` | Boolean | Yes | Default `false` | Highlighted skill flag |
| `proficiencyLevel` | String (Enum) | No | `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT` | Proficiency level |
| `yearsOfExperience`| Integer | No | Must be $\ge 0$ | Experience years |

##### Request Body Example
```json
{
  "isTopSkill": true,
  "proficiencyLevel": "EXPERT",
  "yearsOfExperience": 6
}
```

##### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Skill updated successfully",
  "data": {
    "skillId": "44444444-5555-6666-7777-888888888888",
    "skillName": "Java",
    "category": "Backend",
    "isTopSkill": true,
    "proficiencyLevel": "EXPERT",
    "yearsOfExperience": 6
  }
}
```

#### Remove Skill from Profile
- **Endpoint:** `DELETE /users/me/skills/{skillId}`
- **Authorization:** Authenticated
- **Path Parameter:** `skillId` (UUID of skill to detach)
- **Response:** `SuccessResponse` ("Skill removed from profile successfully").

---

## 4. Institutions Catalog (`/user/institutions`, `/users/institutions`)

### 4.1 Search Educational Institutions Catalog
- **Endpoint:** `GET /user/institutions` or `GET /users/institutions`
- **Description:** Searches the recognized institutions catalog by name or code substring. When `keyword` is omitted or empty, retrieves top institutions.
- **Authorization:** Public
- **Query Parameter:** `keyword` (String, optional)

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Institutions retrieved successfully",
  "data": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "name": "University of Information Technology",
      "code": "UIT",
      "country": "Vietnam",
      "city": "Ho Chi Minh City",
      "website": "https://www.uit.edu.vn",
      "logoUrl": "https://storage.occupify.work/institutions/uit.png"
    },
    {
      "id": "7bb7e292-1234-5678-90ab-cdef12345678",
      "name": "Ho Chi Minh City University of Technology",
      "code": "HCMUT",
      "country": "Vietnam",
      "city": "Ho Chi Minh City",
      "website": "https://www.hcmut.edu.vn",
      "logoUrl": "https://storage.occupify.work/institutions/hcmut.png"
    }
  ]
}
```

---

## 5. Asynchronous Event Publishing (RabbitMQ)

`occupify-identity` acts as the event publisher for identity lifecycle changes across the microservices ecosystem.

```
[occupify-identity]
        |
        | (topic: occupify.notification.exchange, routingKey: user.registered)
        +-----> [occupify.notification.user.queue] -> occupify-noti (Send OTP Email)
        |
        | (topic: occupify.user.exchange, routingKey: user.activated)
        +-----> [occupify.notification.user.queue] -> occupify-noti (Sync User in DB)
        |
        | (topic: occupify.notification.exchange, routingKey: user.password-reset)
        +-----> [occupify.notification.user.queue] -> occupify-noti (Send Reset OTP Email)
```

| Event Record | Target Exchange | Routing Key | Key Payload Fields | Downstream Action |
| :--- | :--- | :--- | :--- | :--- |
| `UserRegisteredEvent` | `occupify.notification.exchange` | `user.registered` | `userId`, `email`, `otpCode`, `occurredAt` | `occupify-noti` dispatches registration OTP email. |
| `UserActivatedEvent` | `occupify.user.exchange` | `user.activated` | `userId`, `email`, `fullName`, `role`, `status`, `occurredAt` | `occupify-noti` synchronizes user record in notification database. |
| `PasswordResetRequestedEvent` | `occupify.notification.exchange` | `user.password-reset` | `userId`, `email`, `otpCode`, `occurredAt` | `occupify-noti` dispatches password reset OTP email. |
