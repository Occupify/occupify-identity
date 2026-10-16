package com.occupify.identity.repository;

import com.occupify.identity.entity.Contract;
import com.occupify.identity.entity.Education;
import com.occupify.identity.entity.Experience;
import com.occupify.identity.entity.Institution;
import com.occupify.identity.entity.Qualification;
import com.occupify.identity.entity.Review;
import com.occupify.identity.entity.Skill;
import com.occupify.identity.entity.SkillUser;
import com.occupify.identity.entity.SkillUserId;
import com.occupify.identity.entity.User;
import com.occupify.identity.enums.EmploymentType;
import com.occupify.identity.enums.LocationType;
import com.occupify.identity.enums.ProficiencyLevel;
import com.occupify.identity.enums.UserRole;
import com.occupify.identity.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ==============================================================================================
 * Test Suite: DatabaseSchemaMappingTest (Data JPA Slice Tests)
 * ==============================================================================================
 *
 * <p><b>Mục đích kiểm thử tổng quan:</b>
 * Xác thực toàn diện cơ chế ánh xạ ORM (Object-Relational Mapping) của Spring Data JPA và Hibernate
 * đối với 8 bảng thực thể trong thiết kế ERD đã chuẩn hóa của dịch vụ <code>occupify-identity</code>:
 * <ul>
 *   <li><b>1. users:</b> Quản lý danh tính cốt lõi, vai trò (EMPLOYEE, EMPLOYER, ADMIN), trạng thái (ACTIVE, INACTIVE...).</li>
 *   <li><b>2. experiences:</b> Kinh nghiệm làm việc gồm organization, employment_type, location_type, is_current...</li>
 *   <li><b>3. educations:</b> Học vấn gồm institution, field_of_study, degree, grade, description, mốc thời gian...</li>
 *   <li><b>4. qualifications:</b> Chứng chỉ gồm title, organization, issue_date, expiration_date, credential_id, credential_url...</li>
 *   <li><b>5. skills:</b> Danh mục kỹ năng chuyên môn toàn hệ thống (tên duy nhất - UNIQUE).</li>
 *   <li><b>6. skills_users:</b> Quan hệ N-N với Composite PK {@link SkillUserId}, thâm niên, trình độ, top skill.</li>
 *   <li><b>7. contracts:</b> Hợp đồng giao dịch giữa Employer và Employee (UUID tham chiếu ngoài sang project_role_id).</li>
 *   <li><b>8. reviews:</b> Đánh giá nghiệm thu dự án hai chiều giữa Employer và Employee, kèm rating (1-5 sao).</li>
 * </ul>
 *
 * <p><b>Môi trường thực thi:</b>
 * H2 In-Memory Database kích hoạt profile <code>test</code> với PostgreSQL mode emulation.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseSchemaMappingTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExperienceRepository experienceRepository;

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private QualificationRepository qualificationRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private SkillUserRepository skillUserRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    // ==========================================================================================
    // HELPER METHODS
    // ==========================================================================================

    private User createTestUser(String email, UserRole role, String fullName) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashedPasswordSecret123");
        user.setFullName(fullName);
        user.setHeadline("Software Engineer");
        user.setRole(role.name());
        user.setStatus(UserStatus.ACTIVE.name());
        return userRepository.save(user);
    }

    private Institution createTestInstitution(String name) {
        Institution institution = new Institution(
                null,
                name,
                "INST",
                "Vietnam",
                "Danang",
                "https://institution.edu.vn",
                null,
                true
        );
        return institutionRepository.save(institution);
    }

    // ==========================================================================================
    // TEST CASES WITH DETAILED BRIEF INFORMATION
    // ==========================================================================================

    /**
     * ------------------------------------------------------------------------------------------
     * TEST CASE 01: Core User Entity Persistence & Retrieval
     * ------------------------------------------------------------------------------------------
     * <b>Mục tiêu kiểm thử:</b>
     * - Kiểm tra ánh xạ bảng `users`.
     * - Xác nhận tự động sinh UUID Primary Key khi lưu entity.
     * - Xác nhận các trường Enum string (Role: EMPLOYEE / EMPLOYER, Status: ACTIVE) được lưu chuẩn xác.
     * - Xác minh phương thức truy vấn JPA tìm kiếm theo email (findByEmail).
     */
    @Test
    @Order(1)
    @DisplayName("TC01 - User: Lưu trữ tài khoản, tự sinh UUID và truy vấn theo Email thành công")
    void testUserPersistenceAndQuery() {
        // [ARRANGE - Chuẩn bị dữ liệu]
        User candidate = new User();
        candidate.setEmail("freelancer.dev@occupify.com");
        candidate.setPassword("bcryptSecureHash$2a$12$xyz");
        candidate.setFullName("Nguyen Van A");
        candidate.setHeadline("Senior Fullstack Developer");
        candidate.setBio("Passionate about distributed microservices and clean code.");
        candidate.setRole(UserRole.EMPLOYEE.name());
        candidate.setStatus(UserStatus.ACTIVE.name());

        // [ACT - Thực thi hành động lưu qua JPA]
        User savedUser = userRepository.save(candidate);

        // [ASSERT - Kiểm tra kết quả và ràng buộc]
        assertThat(savedUser.getId())
                .as("Khóa chính UUID phải được sinh tự động bởi Hibernate/DB")
                .isNotNull();

        Optional<User> queriedUser = userRepository.findByEmail("freelancer.dev@occupify.com");
        assertThat(queriedUser)
                .as("Phải tìm thấy người dùng qua Email")
                .isPresent();
        assertThat(queriedUser.get().getFullName()).isEqualTo("Nguyen Van A");
        assertThat(queriedUser.get().getRole()).isEqualTo(UserRole.EMPLOYEE.name());
        assertThat(queriedUser.get().getStatus()).isEqualTo(UserStatus.ACTIVE.name());
    }

    /**
     * ------------------------------------------------------------------------------------------
     * TEST CASE 02: User Profile Extensions (Experiences, Educations, Qualifications)
     * ------------------------------------------------------------------------------------------
     * <b>Mục tiêu kiểm thử:</b>
     * - Kiểm tra 3 bảng mở rộng hồ sơ Freelancer đã được nâng cấp theo chuẩn LinkedIn:
     *   + `experiences`: organization, employment_type, location_type, is_current, start_date.
     *   + `educations`: institution, field_of_study, degree, grade, description, start_date, end_date.
     *   + `qualifications`: title, organization, issue_date, expiration_date, credential_id, credential_url.
     * - Ràng buộc Khóa ngoại (Foreign Key): Tất cả đều tham chiếu đúng đến `users.id`.
     */
    @Test
    @Order(2)
    @DisplayName("TC02 - Profile Extensions: Lưu và truy vấn kinh nghiệm, học vấn, chứng chỉ qua User FK")
    void testProfileExtensionsPersistence() {
        // [ARRANGE - Tạo User Freelancer làm chủ thể hồ sơ]
        User freelancer = createTestUser("candidate.profile@occupify.com", UserRole.EMPLOYEE, "Le Thi B");

        // [ACT 1 - Thêm Kinh nghiệm làm việc (Experience)]
        Experience experience = new Experience(
                null,
                freelancer,
                "Backend Lead Engineer",
                "Occupify Inc",
                EmploymentType.FULL_TIME.name(),
                "Da Nang City, Vietnam",
                LocationType.REMOTE.name(),
                LocalDate.of(2022, 1, 15),
                null,
                true,
                "Architecting high-throughput Spring Boot event-driven services."
        );
        experience = experienceRepository.save(experience);

        // [ACT 2 - Thêm Học vấn (Education)]
        Institution dutInstitution = createTestInstitution("Danang University of Technology");
        Education education = new Education(
                null,
                freelancer,
                dutInstitution,
                "Software Engineering",
                "Bachelor of Science",
                "3.85 / 4.0",
                "Thesis: Building resilient event-driven microservices with RabbitMQ.",
                LocalDate.of(2017, 9, 1),
                LocalDate.of(2021, 6, 30),
                "https://s3.../diploma.pdf"
        );
        education = educationRepository.save(education);

        // [ACT 3 - Thêm Bằng cấp / Chứng chỉ (Qualification)]
        Qualification qualification = new Qualification(
                null,
                freelancer,
                "AWS Certified Solutions Architect",
                "Amazon Web Services",
                LocalDate.of(2023, 4, 10),
                LocalDate.of(2026, 4, 10),
                "AWS-SAA-12345678",
                "https://www.credly.com/badges/sample-aws-cert",
                "https://s3.../aws-cert.pdf"
        );
        qualification = qualificationRepository.save(qualification);

        // [ASSERT - Kiểm tra lưu trữ và truy vấn theo Foreign Key]
        assertThat(experience.getId()).as("Experience ID phải được sinh tự động").isNotNull();
        assertThat(education.getId()).as("Education ID phải được sinh tự động").isNotNull();
        assertThat(qualification.getId()).as("Qualification ID phải được sinh tự động").isNotNull();

        // Kiểm tra thông tin Experience
        List<Experience> experiences = experienceRepository.findByUserIdOrderByStartDateDesc(freelancer.getId());
        assertThat(experiences).hasSize(1);
        assertThat(experiences.get(0).getTitle()).isEqualTo("Backend Lead Engineer");
        assertThat(experiences.get(0).getOrganization()).isEqualTo("Occupify Inc");
        assertThat(experiences.get(0).getEmploymentType()).isEqualTo(EmploymentType.FULL_TIME.name());
        assertThat(experiences.get(0).getLocationType()).isEqualTo(LocationType.REMOTE.name());
        assertThat(experiences.get(0).isCurrent()).isTrue();
        assertThat(experiences.get(0).getEndDate()).isNull();

        // Kiểm tra thông tin Education
        List<Education> educations = educationRepository.findByUserIdOrderByStartDateDesc(freelancer.getId());
        assertThat(educations).hasSize(1);
        assertThat(educations.get(0).getInstitution().getName()).isEqualTo("Danang University of Technology");
        assertThat(educations.get(0).getDegree()).isEqualTo("Bachelor of Science");
        assertThat(educations.get(0).getGrade()).isEqualTo("3.85 / 4.0");
        assertThat(educations.get(0).getDescription()).contains("resilient event-driven microservices");
        assertThat(educations.get(0).getDocumentUrl()).isEqualTo("https://s3.../diploma.pdf");

        // Kiểm tra thông tin Qualification
        List<Qualification> qualifications = qualificationRepository.findByUserIdOrderByIssueDateDesc(freelancer.getId());
        assertThat(qualifications).hasSize(1);
        assertThat(qualifications.get(0).getOrganization()).isEqualTo("Amazon Web Services");
        assertThat(qualifications.get(0).getIssueDate()).isEqualTo(LocalDate.of(2023, 4, 10));
        assertThat(qualifications.get(0).getExpirationDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(qualifications.get(0).getCredentialId()).isEqualTo("AWS-SAA-12345678");
        assertThat(qualifications.get(0).getCredentialUrl()).isEqualTo("https://www.credly.com/badges/sample-aws-cert");
        assertThat(qualifications.get(0).getDocumentUrl()).isEqualTo("https://s3.../aws-cert.pdf");
    }

    /**
     * ------------------------------------------------------------------------------------------
     * TEST CASE 03: Skills & Many-to-Many Composite Key (SkillUser)
     * ------------------------------------------------------------------------------------------
     * <b>Mục tiêu kiểm thử:</b>
     * - Kiểm tra bảng danh mục `skills` (lưu trữ tên kỹ năng, danh mục).
     * - Kiểm tra bảng quan hệ N-N `skills_users` sử dụng Composite Primary Key ({@link SkillUserId}).
     * - Xác nhận các trường mở rộng: `is_top_skill`, `proficiency_level`, `years_of_experience`.
     * - Kiểm tra truy vấn tìm kiếm Top Skills theo người dùng.
     */
    @Test
    @Order(3)
    @DisplayName("TC03 - Skills & Composite Key: Lưu trữ kỹ năng, trình độ, thâm niên và Top Skills")
    void testSkillsAndCompositeKeyMapping() {
        // [ARRANGE - Tạo User và Danh mục kỹ năng]
        User developer = createTestUser("dev.spring@occupify.com", UserRole.EMPLOYEE, "Pham Van C");

        Skill springBootSkill = new Skill(null, "Spring Boot", "Frameworks & Backend");
        springBootSkill = skillRepository.save(springBootSkill);
        assertThat(springBootSkill.getId()).isNotNull();

        // [ACT - Gắn Kỹ năng cho User thông qua Composite Key kèm thông tin chuyên sâu]
        SkillUser skillUser = new SkillUser(
                developer,
                springBootSkill,
                true,
                ProficiencyLevel.ADVANCED.name(),
                4
        );
        skillUserRepository.save(skillUser);

        // [ASSERT - Kiểm tra truy vấn bằng Composite Key]
        SkillUserId compositeId = new SkillUserId(developer.getId(), springBootSkill.getId());
        Optional<SkillUser> foundSkillUser = skillUserRepository.findById(compositeId);

        assertThat(foundSkillUser).as("Phải tìm thấy liên kết N-N qua Composite Key (userId, skillId)").isPresent();
        assertThat(foundSkillUser.get().getUser().getEmail()).isEqualTo("dev.spring@occupify.com");
        assertThat(foundSkillUser.get().getSkill().getName()).isEqualTo("Spring Boot");
        assertThat(foundSkillUser.get().isTopSkill()).isTrue();
        assertThat(foundSkillUser.get().getProficiencyLevel()).isEqualTo(ProficiencyLevel.ADVANCED.name());
        assertThat(foundSkillUser.get().getYearsOfExperience()).isEqualTo(4);

        // Kiểm tra truy vấn danh sách Top Skills của User
        List<SkillUser> topSkills = skillUserRepository.findByIdUserIdAndIsTopSkillTrue(developer.getId());
        assertThat(topSkills).hasSize(1);
        assertThat(topSkills.get(0).getSkill().getName()).isEqualTo("Spring Boot");
    }

    /**
     * ------------------------------------------------------------------------------------------
     * TEST CASE 04: Contract & Review Lifecycle Workflow
     * ------------------------------------------------------------------------------------------
     * <b>Mục tiêu kiểm thử:</b>
     * - Kiểm tra quan hệ thương mại giữa 2 đối tượng trong hệ thống: `Employer` và `Employee`.
     * - Entity {@link Contract}:
     *   + Liên kết Foreign Key tới 2 bảng users khác nhau (`employer_id` và `employee_id`).
     *   + Hỗ trợ trường tham chiếu phân tán microservice (`project_role_id` - không đặt DB FK constraint).
     *   + Lưu trữ điều khoản tài chính: tỷ giá/mức phí (`agreed_rate`), trạng thái thanh toán (`payment_status`).
     * - Entity {@link Review}:
     *   + Ràng buộc 1-1 với Contract qua Foreign Key `contract_id`.
     *   + Định danh rõ ràng Người chấm điểm (`reviewer_user_id`) và Người được chấm điểm (`reviewee_user_id`).
     *   + Kiểm tra thang điểm Rating từ 1 đến 5 sao.
     */
    @Test
    @Order(4)
    @DisplayName("TC04 - Contract & Review: Thiết lập hợp đồng freelance và viết đánh giá nghiệm thu")
    void testContractAndReviewWorkflow() {
        // [ARRANGE - Tạo Employer và Employee riêng biệt]
        User employer = createTestUser("company.hr@occupify.com", UserRole.EMPLOYER, "HR Director");
        User employee = createTestUser("expert.freelancer@occupify.com", UserRole.EMPLOYEE, "Expert Freelancer");

        // [ACT 1 - Ký kết Hợp đồng làm việc (Contract)]
        UUID crossServiceProjectRoleId = UUID.randomUUID(); // ID tham chiếu từ Project Microservice
        Contract contract = new Contract(
                null,
                crossServiceProjectRoleId,
                employer,
                employee,
                "PROJECT",
                UUID.randomUUID(),
                new BigDecimal("2500.00"),
                "MILESTONE",
                "COMPLETED",
                "RELEASED",
                null,
                null
        );
        contract = contractRepository.save(contract);
        assertThat(contract.getId()).isNotNull();

        // [ACT 2 - Employer viết Đánh giá nghiệm thu (Review) cho Employee]
        Review review = new Review(
                null,
                contract,
                employer, // Reviewer: Nhà tuyển dụng
                employee, // Reviewee: Freelancer
                5,        // Rating: 5 sao
                "Dự án hoàn thành xuất sắc, đúng tiến độ và chất lượng mã nguồn rất cao!"
        );
        review = reviewRepository.save(review);
        assertThat(review.getId()).isNotNull();

        // [ASSERT - Kiểm tra truy vấn Hợp đồng và Đánh giá]
        List<Contract> employerContracts = contractRepository.findByEmployerId(employer.getId());
        assertThat(employerContracts).hasSize(1);
        assertThat(employerContracts.get(0).getProjectRoleId()).isEqualTo(crossServiceProjectRoleId);
        assertThat(employerContracts.get(0).getAgreedRate()).isEqualByComparingTo("2500.00");

        List<Review> contractReviews = reviewRepository.findByContractId(contract.getId());
        assertThat(contractReviews).hasSize(1);
        assertThat(contractReviews.get(0).getRating()).isEqualTo(5);
        assertThat(contractReviews.get(0).getReviewer().getId()).isEqualTo(employer.getId());
        assertThat(contractReviews.get(0).getReviewee().getId()).isEqualTo(employee.getId());
    }

    /**
     * ------------------------------------------------------------------------------------------
     * TEST CASE 05: Complete End-to-End Schema Persistence (Toàn diện 8 Thực thể)
     * ------------------------------------------------------------------------------------------
     * <b>Mục tiêu kiểm thử:</b>
     * Kịch bản tích hợp chuỗi nghiệp vụ hoàn chỉnh chạy liên hoàn qua cả 8 bảng dữ liệu trong ERD:
     * 1. Khởi tạo tài khoản Nhà tuyển dụng (Employer) & Ứng viên (Employee).
     * 2. Hoàn thiện hồ sơ Freelancer với Experience, Education, Qualification.
     * 3. Đăng ký Kỹ năng (Skill) và gắn cho Freelancer với Top Skill và mức độ thành thạo.
     * 4. Giao kết Hợp đồng (Contract) giữa Employer và Employee kèm theo ngân sách dự án.
     * 5. Nghiệm thu dự án và để lại Đánh giá (Review) 5 sao kèm nhận xét chi tiết.
     */
    @Test
    @Order(5)
    @DisplayName("TC05 - E2E Integration: Kiểm tra luồng liên thông toàn bộ 8 thực thể trong ERD")
    void testCompleteSchemaPersistence() {
        // --- BƯỚC 1: Khởi tạo Employer và Employee ---
        User employer = new User();
        employer.setEmail("employer@occupify.com");
        employer.setPassword("hashedPassword123");
        employer.setFullName("Nguyen Van Employer");
        employer.setHeadline("Tech Founder");
        employer.setRole(UserRole.EMPLOYER.name());
        employer.setStatus(UserStatus.ACTIVE.name());
        employer = userRepository.save(employer);

        User employee = new User();
        employee.setEmail("employee@occupify.com");
        employee.setPassword("hashedPassword456");
        employee.setFullName("Tran Van Freelancer");
        employee.setHeadline("Senior Java Engineer");
        employee.setRole(UserRole.EMPLOYEE.name());
        employee.setStatus(UserStatus.ACTIVE.name());
        employee = userRepository.save(employee);

        assertThat(employer.getId()).isNotNull();
        assertThat(employee.getId()).isNotNull();

        // --- BƯỚC 2: Thêm Kinh nghiệm, Học vấn và Bằng cấp cho Employee ---
        Experience experience = new Experience(
                null,
                employee,
                "Senior Backend Developer",
                "Tech Pioneers Co",
                EmploymentType.CONTRACT.name(),
                "Ho Chi Minh City",
                LocationType.HYBRID.name(),
                LocalDate.of(2023, 1, 1),
                null,
                true,
                "Developing scalable microservices using Spring Boot and Postgres"
        );
        experience = experienceRepository.save(experience);
        assertThat(experience.getId()).isNotNull();

        List<Experience> experiences = experienceRepository.findByUserIdOrderByStartDateDesc(employee.getId());
        assertThat(experiences).hasSize(1);
        assertThat(experiences.get(0).getTitle()).isEqualTo("Senior Backend Developer");
        assertThat(experiences.get(0).getOrganization()).isEqualTo("Tech Pioneers Co");

        Institution vnuInstitution = createTestInstitution("VNU University");
        Education education = new Education(
                null,
                employee,
                vnuInstitution,
                "Computer Science",
                "Bachelor of Engineering",
                "3.9 / 4.0",
                "Excellent graduation project in distributed caching systems",
                LocalDate.of(2018, 9, 1),
                LocalDate.of(2022, 6, 30),
                null
        );
        education = educationRepository.save(education);
        assertThat(education.getId()).isNotNull();

        Qualification qualification = new Qualification(
                null,
                employee,
                "AWS Certified Solutions Architect",
                "Amazon Web Services",
                LocalDate.of(2023, 5, 1),
                LocalDate.of(2026, 5, 1),
                "AWS-SAA-998877",
                "https://aws.amazon.com/verification",
                null
        );
        qualification = qualificationRepository.save(qualification);
        assertThat(qualification.getId()).isNotNull();

        // --- BƯỚC 3: Tạo Skill và liên kết N-N qua Composite PK (SkillUser) ---
        Skill javaSkill = new Skill(null, "Java", "Programming Languages");
        javaSkill = skillRepository.save(javaSkill);
        assertThat(javaSkill.getId()).isNotNull();

        SkillUser skillUser = new SkillUser(
                employee,
                javaSkill,
                true,
                ProficiencyLevel.EXPERT.name(),
                5
        );
        skillUser = skillUserRepository.save(skillUser);

        Optional<SkillUser> foundSkillUser = skillUserRepository.findById(new SkillUserId(employee.getId(), javaSkill.getId()));
        assertThat(foundSkillUser).isPresent();
        assertThat(foundSkillUser.get().getUser().getEmail()).isEqualTo("employee@occupify.com");
        assertThat(foundSkillUser.get().getSkill().getName()).isEqualTo("Java");
        assertThat(foundSkillUser.get().isTopSkill()).isTrue();
        assertThat(foundSkillUser.get().getProficiencyLevel()).isEqualTo(ProficiencyLevel.EXPERT.name());

        // --- BƯỚC 4: Tạo Hợp đồng (Contract) giữa Employer và Employee ---
        UUID externalProjectRoleId = UUID.randomUUID();
        Contract contract = new Contract(
                null,
                externalProjectRoleId,
                employer,
                employee,
                "PROJECT",
                UUID.randomUUID(),
                new BigDecimal("1500.00"),
                "MONTHLY",
                "ACTIVE",
                "ESCROWED",
                null,
                null
        );
        contract = contractRepository.save(contract);
        assertThat(contract.getId()).isNotNull();

        List<Contract> contracts = contractRepository.findByEmployerId(employer.getId());
        assertThat(contracts).hasSize(1);
        assertThat(contracts.get(0).getProjectRoleId()).isEqualTo(externalProjectRoleId);

        // --- BƯỚC 5: Tạo Đánh giá (Review) sau khi hoàn tất Hợp đồng ---
        Review review = new Review(
                null,
                contract,
                employer,
                employee,
                5,
                "Outstanding backend delivery! Highly recommended."
        );
        review = reviewRepository.save(review);
        assertThat(review.getId()).isNotNull();

        List<Review> reviews = reviewRepository.findByContractId(contract.getId());
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).getRating()).isEqualTo(5);
        assertThat(reviews.get(0).getReviewer().getId()).isEqualTo(employer.getId());
        assertThat(reviews.get(0).getReviewee().getId()).isEqualTo(employee.getId());
    }
}
