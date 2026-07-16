package top.sharpcaterpillar.campusecho;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import top.sharpcaterpillar.campusecho.category.application.CategoryQueryService;
import top.sharpcaterpillar.campusecho.category.application.CategoryAdminService;
import top.sharpcaterpillar.campusecho.category.api.CategoryAdminResponse;
import top.sharpcaterpillar.campusecho.category.api.CategoryCreateRequest;
import top.sharpcaterpillar.campusecho.category.api.CategoryUpdateRequest;
import top.sharpcaterpillar.campusecho.category.domain.ItemCategoryRepository;
import top.sharpcaterpillar.campusecho.admin.application.AdminService;
import top.sharpcaterpillar.campusecho.admin.api.AdminDashboardResponse;
import top.sharpcaterpillar.campusecho.admin.api.AdminUserResponse;
import top.sharpcaterpillar.campusecho.item.api.ItemCreateRequest;
import top.sharpcaterpillar.campusecho.item.api.ItemResponse;
import top.sharpcaterpillar.campusecho.item.api.ItemImageResponse;
import top.sharpcaterpillar.campusecho.item.application.ItemService;
import top.sharpcaterpillar.campusecho.item.application.ItemImageService;
import top.sharpcaterpillar.campusecho.item.application.ItemImageContent;
import top.sharpcaterpillar.campusecho.item.domain.ItemCondition;
import top.sharpcaterpillar.campusecho.item.domain.ItemStatus;
import top.sharpcaterpillar.campusecho.item.domain.ItemModerationStatus;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItemRepository;
import top.sharpcaterpillar.campusecho.trade.api.TradeRequestCreateRequest;
import top.sharpcaterpillar.campusecho.trade.api.TradeRequestResponse;
import top.sharpcaterpillar.campusecho.trade.application.TradeRequestService;
import top.sharpcaterpillar.campusecho.trade.domain.TradeStatus;
import top.sharpcaterpillar.campusecho.trade.domain.TradeRequestRepository;
import top.sharpcaterpillar.campusecho.dispute.api.DisputeCreateRequest;
import top.sharpcaterpillar.campusecho.dispute.api.DisputeResponse;
import top.sharpcaterpillar.campusecho.dispute.application.DisputeService;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeReason;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeResolution;
import top.sharpcaterpillar.campusecho.dispute.domain.DisputeStatus;
import top.sharpcaterpillar.campusecho.message.api.MessageResponse;
import top.sharpcaterpillar.campusecho.message.api.UnreadCountResponse;
import top.sharpcaterpillar.campusecho.message.application.MessageService;
import top.sharpcaterpillar.campusecho.message.domain.MessageType;
import top.sharpcaterpillar.campusecho.report.api.ItemModerationResponse;
import top.sharpcaterpillar.campusecho.report.api.ReportCreateRequest;
import top.sharpcaterpillar.campusecho.report.api.ReportResponse;
import top.sharpcaterpillar.campusecho.report.application.ReportService;
import top.sharpcaterpillar.campusecho.report.domain.ReportAction;
import top.sharpcaterpillar.campusecho.report.domain.ReportReason;
import top.sharpcaterpillar.campusecho.report.domain.ReportStatus;
import top.sharpcaterpillar.campusecho.report.domain.ReportTargetType;
import top.sharpcaterpillar.campusecho.review.api.ReviewCreateRequest;
import top.sharpcaterpillar.campusecho.review.api.ReviewResponse;
import top.sharpcaterpillar.campusecho.review.application.ReviewService;
import top.sharpcaterpillar.campusecho.user.api.RegisterRequest;
import top.sharpcaterpillar.campusecho.user.api.UserProfileResponse;
import top.sharpcaterpillar.campusecho.user.api.CurrentUserResponse;
import top.sharpcaterpillar.campusecho.user.api.UserProfileUpdateRequest;
import top.sharpcaterpillar.campusecho.user.api.LoginRequest;
import top.sharpcaterpillar.campusecho.user.api.LoginResponse;
import top.sharpcaterpillar.campusecho.user.application.AuthenticationService;
import top.sharpcaterpillar.campusecho.user.application.UserRegistrationService;
import top.sharpcaterpillar.campusecho.user.application.UserProfileService;
import top.sharpcaterpillar.campusecho.user.application.AvatarService;
import top.sharpcaterpillar.campusecho.user.application.AvatarContent;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserRole;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.security.JwtService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.jwt.secret=test-only-signing-key-with-at-least-32-characters")
class CampusEchoApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRegistrationService userRegistrationService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private AvatarService avatarService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CategoryQueryService categoryQueryService;

    @Autowired
    private CategoryAdminService categoryAdminService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemImageService itemImageService;

    @Autowired
    private ItemCategoryRepository itemCategoryRepository;

    @Autowired
    private MarketplaceItemRepository marketplaceItemRepository;

    @Autowired
    private TradeRequestService tradeRequestService;

    @Autowired
    private TradeRequestRepository tradeRequestRepository;

    @Autowired
    private ReportService reportService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private DisputeService disputeService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void contextLoadsAndMigratesDatabase() {
        String database = jdbcTemplate.queryForObject("select current_database()", String.class);
        assertEquals("campusecho", database);
        assertEquals(6, categoryQueryService.getTree().size());
        var publicItems = itemService.listPublic(null, null, 1, 20);
        assertEquals(1, publicItems.page());
        assertTrue(publicItems.totalElements() >= publicItems.content().size());
    }

    @Test
    @Transactional
    void registrationStoresOnlyHashedPassword() {
        UserProfileResponse profile = userRegistrationService.register(new RegisterRequest(
                "test_user_" + java.util.UUID.randomUUID().toString().substring(0, 8),
                "test-" + java.util.UUID.randomUUID() + "@example.test",
                "safe-password",
                "测试用户",
                null,
                "主校区"));

        UserAccount user = userAccountRepository.findById(profile.id()).orElseThrow();
        assertFalse(user.getPasswordHash().equals("safe-password"));
        assertTrue(passwordEncoder.matches("safe-password", user.getPasswordHash()));

        LoginResponse login = authenticationService.login(new LoginRequest(user.getUsername(), "safe-password"));
        AuthenticatedUser authenticatedUser = jwtService.parse(login.accessToken());
        assertEquals(user.getId(), authenticatedUser.id());
        assertEquals(user.getUsername(), authenticatedUser.username());
    }

    @Test
    @Transactional
    void itemCanBePublishedAndOffShelvedByItsSeller() {
        UserProfileResponse profile = userRegistrationService.register(new RegisterRequest(
                "seller_" + java.util.UUID.randomUUID().toString().substring(0, 8),
                "seller-" + java.util.UUID.randomUUID() + "@example.test",
                "safe-password",
                "测试卖家",
                null,
                "主校区"));
        AuthenticatedUser seller = new AuthenticatedUser(profile.id(), profile.username(), top.sharpcaterpillar.campusecho.user.domain.UserRole.USER);
        Long categoryId = itemCategoryRepository.findAllByOrderBySortOrderAscIdAsc().getFirst().getId();

        ItemResponse item = itemService.create(seller, new ItemCreateRequest(
                categoryId,
                "算法导论",
                "九成新教材，含笔记。",
                new java.math.BigDecimal("45.00"),
                ItemCondition.GOOD,
                "图书馆门口"));

        assertEquals(ItemStatus.ON_SALE, item.status());
        assertEquals(1, itemService.listPublic(categoryId, "算法", 1, 20).totalElements());

        ItemResponse offShelved = itemService.offShelf(item.id(), seller);
        assertEquals(ItemStatus.OFF_SHELF, offShelved.status());
        assertEquals(0, itemService.listPublic(categoryId, "算法", 1, 20).totalElements());
    }

    @Test
    void publicItemListIsAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(get("/api/v1/items/mine"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/categories")
                        .contentType("application/json")
                        .content("{\"name\":\"未授权分类\",\"sortOrder\":0}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void adminCanMaintainCategoriesAndUsersCannot() throws Exception {
        UserProfileResponse adminProfile = registerUser("category_admin_");
        UserAccount adminAccount = userAccountRepository.findById(adminProfile.id()).orElseThrow();
        adminAccount.changeRole(UserRole.ADMIN);
        userAccountRepository.saveAndFlush(adminAccount);
        AuthenticatedUser admin = new AuthenticatedUser(adminProfile.id(), adminProfile.username(), UserRole.ADMIN);

        UserProfileResponse userProfile = registerUser("category_user_");
        AuthenticatedUser user = authenticatedUser(userProfile);
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);

        CategoryAdminResponse root = categoryAdminService.create(admin,
                new CategoryCreateRequest("管理根分类_" + suffix, null, 20));
        CategoryAdminResponse child = categoryAdminService.create(admin,
                new CategoryCreateRequest("管理子分类_" + suffix, root.id(), 1));
        CategoryAdminResponse updated = categoryAdminService.update(child.id(), admin,
                new CategoryUpdateRequest("管理子分类_改名_" + suffix, root.id(), 2));

        assertEquals(root.id(), updated.parentId());
        assertEquals(2, updated.sortOrder());
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> categoryAdminService.update(root.id(), admin,
                        new CategoryUpdateRequest("非法循环分类", child.id(), 20)));
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> categoryAdminService.delete(root.id(), admin));
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> categoryAdminService.create(user, new CategoryCreateRequest("普通用户分类", null, 0)));

        LoginResponse userLogin = authenticationService.login(new LoginRequest(userProfile.username(), "safe-password"));
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + userLogin.accessToken())
                        .contentType("application/json")
                        .content("{\"name\":\"普通用户分类\",\"sortOrder\":0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        String adminToken = jwtService.issue(adminAccount).value();
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"name\":\"HTTP管理员分类_" + suffix + "\",\"sortOrder\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("HTTP管理员分类_" + suffix));

        categoryAdminService.delete(child.id(), admin);
        categoryAdminService.delete(root.id(), admin);
    }

    @Test
    @Transactional
    void adminCanManageUsersAndRoleChangesTakeEffectImmediately() throws Exception {
        UserProfileResponse adminProfile = registerUser("system_admin_");
        UserAccount adminAccount = userAccountRepository.findById(adminProfile.id()).orElseThrow();
        adminAccount.changeRole(UserRole.ADMIN);
        userAccountRepository.saveAndFlush(adminAccount);
        AuthenticatedUser admin = new AuthenticatedUser(
                adminAccount.getId(), adminAccount.getUsername(), UserRole.ADMIN);
        UserProfileResponse targetProfile = registerUser("managed_user_");
        UserAccount targetAccount = userAccountRepository.findById(targetProfile.id()).orElseThrow();
        String targetToken = jwtService.issue(targetAccount).value();

        AdminDashboardResponse dashboard = adminService.dashboard(admin);
        assertTrue(dashboard.totalUsers() >= 2);
        assertTrue(dashboard.activeAdmins() >= 1);
        assertTrue(adminService.listUsers(admin, null, null, null, 1, 20).totalElements() >= 2);
        assertEquals(1, adminService.listUsers(
                admin, targetProfile.username(), UserRole.USER, UserStatus.ACTIVE, 1, 20).totalElements());

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isForbidden());

        AdminUserResponse suspended = adminService.changeStatus(
                targetProfile.id(), UserStatus.SUSPENDED, admin);
        assertEquals(UserStatus.SUSPENDED, suspended.status());
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isUnauthorized());
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> authenticationService.login(new LoginRequest(targetProfile.username(), "safe-password")));

        adminService.changeStatus(targetProfile.id(), UserStatus.ACTIVE, admin);
        adminService.changeRole(targetProfile.id(), UserRole.ADMIN, admin);
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isOk());

        adminService.changeRole(targetProfile.id(), UserRole.USER, admin);
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isForbidden());

        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> adminService.changeRole(adminAccount.getId(), UserRole.USER, admin));
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> adminService.changeStatus(adminAccount.getId(), UserStatus.SUSPENDED, admin));

        String adminToken = jwtService.issue(adminAccount).value();
        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", targetProfile.id())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    @Transactional
    void acceptedTradeCompletesAfterBothParticipantsConfirm() throws Exception {
        UserProfileResponse sellerProfile = registerUser("trade_seller_");
        UserProfileResponse requesterProfile = registerUser("trade_buyer_");
        AuthenticatedUser seller = authenticatedUser(sellerProfile);
        AuthenticatedUser requester = authenticatedUser(requesterProfile);
        Long categoryId = itemCategoryRepository.findAllByOrderBySortOrderAscIdAsc().getFirst().getId();
        ItemResponse item = itemService.create(seller, new ItemCreateRequest(
                categoryId,
                "二手显示器",
                "功能正常，自提。",
                new java.math.BigDecimal("300.00"),
                ItemCondition.GOOD,
                "教学楼"));

        TradeRequestResponse request = tradeRequestService.create(
                requester,
                new TradeRequestCreateRequest(item.id(), "周五可以当面交易"));
        assertEquals(TradeStatus.PENDING, request.status());

        TradeRequestResponse accepted = tradeRequestService.accept(request.id(), seller);
        assertEquals(TradeStatus.ACCEPTED, accepted.status());
        assertEquals(ItemStatus.RESERVED, marketplaceItemRepository.findById(item.id()).orElseThrow().getStatus());
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> reviewService.create(request.id(), requester, new ReviewCreateRequest(4, "尚未完成")));

        TradeRequestResponse requesterConfirmed = tradeRequestService.confirmCompletion(request.id(), requester);
        assertEquals(TradeStatus.ACCEPTED, requesterConfirmed.status());
        TradeRequestResponse completed = tradeRequestService.confirmCompletion(request.id(), seller);
        assertEquals(TradeStatus.COMPLETED, completed.status());
        assertTrue(completed.requesterConfirmed());
        assertTrue(completed.sellerConfirmed());
        assertEquals(ItemStatus.SOLD, marketplaceItemRepository.findById(item.id()).orElseThrow().getStatus());

        String requesterToken = jwtService.issue(
                userAccountRepository.findById(requesterProfile.id()).orElseThrow()).value();
        mockMvc.perform(post("/api/v1/trade-requests/{id}/reviews", request.id())
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType("application/json")
                        .content("{\"rating\":6,\"comment\":\"超出范围\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations.rating").exists());

        ReviewResponse buyerReview = reviewService.create(
                request.id(), requester, new ReviewCreateRequest(4, "沟通顺畅，物品符合描述。"));
        ReviewResponse sellerReview = reviewService.create(
                request.id(), seller, new ReviewCreateRequest(2, "取货时间多次调整。"));
        assertEquals(sellerProfile.id(), buyerReview.revieweeId());
        assertEquals(requesterProfile.id(), sellerReview.revieweeId());
        assertEquals(new java.math.BigDecimal("4.00"),
                userAccountRepository.findById(sellerProfile.id()).orElseThrow().getReputationScore());
        assertEquals(new java.math.BigDecimal("2.00"),
                userAccountRepository.findById(requesterProfile.id()).orElseThrow().getReputationScore());
        assertEquals(2, reviewService.listForTrade(request.id(), requester).size());
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> reviewService.create(request.id(), requester, new ReviewCreateRequest(5, "重复评价")));

        UserProfileResponse secondBuyerProfile = registerUser("trade_buyer_second_");
        AuthenticatedUser secondBuyer = authenticatedUser(secondBuyerProfile);
        ItemResponse secondItem = itemService.create(seller, new ItemCreateRequest(
                categoryId,
                "机械键盘",
                "功能正常。",
                new java.math.BigDecimal("120.00"),
                ItemCondition.GOOD,
                "教学楼"));
        TradeRequestResponse secondRequest = tradeRequestService.create(
                secondBuyer, new TradeRequestCreateRequest(secondItem.id(), null));
        tradeRequestService.accept(secondRequest.id(), seller);
        tradeRequestService.confirmCompletion(secondRequest.id(), secondBuyer);
        tradeRequestService.confirmCompletion(secondRequest.id(), seller);
        reviewService.create(secondRequest.id(), secondBuyer, new ReviewCreateRequest(2, "交易已完成。"));

        assertEquals(new java.math.BigDecimal("3.00"),
                userAccountRepository.findById(sellerProfile.id()).orElseThrow().getReputationScore());
        assertEquals(2, reviewService.listReceived(sellerProfile.id(), 1, 20).totalElements());
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> reviewService.listForTrade(request.id(), secondBuyer));

        mockMvc.perform(get("/api/v1/users/{userId}/reviews", sellerProfile.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @Transactional
    void itemImagesEnforcePrimaryAndOwnerRules() {
        UserProfileResponse sellerProfile = registerUser("image_seller_");
        AuthenticatedUser seller = authenticatedUser(sellerProfile);
        Long categoryId = itemCategoryRepository.findAllByOrderBySortOrderAscIdAsc().getFirst().getId();
        ItemResponse item = itemService.create(seller, new ItemCreateRequest(
                categoryId,
                "二手相机",
                "成色良好。",
                new java.math.BigDecimal("800.00"),
                ItemCondition.GOOD,
                "校门口"));

        byte[] pngHeader = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        ItemImageResponse first = itemImageService.upload(item.id(), seller,
                new MockMultipartFile("file", "first.png", "image/png", pngHeader), false);
        ItemImageResponse second = itemImageService.upload(item.id(), seller,
                new MockMultipartFile("file", "second.png", "image/png", pngHeader), true);

        assertTrue(first.primary());
        assertTrue(second.primary());
        assertEquals(1, itemImageService.list(item.id()).stream().filter(ItemImageResponse::primary).count());
        assertEquals(second.id(), itemImageService.list(item.id()).get(1).id());

        ItemImageContent content = itemImageService.content(first.id());
        assertTrue(java.nio.file.Files.isRegularFile(content.path()));
        itemImageService.delete(item.id(), second.id(), seller);
        assertTrue(itemImageService.list(item.id()).getFirst().primary());
    }

    @Test
    @Transactional
    void userCanUpdateProfileAndManageAvatar() {
        UserProfileResponse profile = registerUser("profile_user_");
        AuthenticatedUser user = authenticatedUser(profile);

        CurrentUserResponse updated = userProfileService.updateCurrentUser(user,
                new UserProfileUpdateRequest("新昵称", " 13800138000 ", " 新校区 "));
        assertEquals("新昵称", updated.displayName());
        assertEquals("13800138000", updated.phone());
        assertEquals("新校区", updated.campus());

        byte[] pngHeader = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        CurrentUserResponse withAvatar = avatarService.upload(user,
                new MockMultipartFile("file", "avatar.png", "image/png", pngHeader));
        assertTrue(withAvatar.avatarUrl().startsWith("/api/v1/avatars/"));

        String storageKey = withAvatar.avatarUrl().substring("/api/v1/avatars/".length());
        AvatarContent content = avatarService.content(storageKey);
        assertTrue(java.nio.file.Files.isRegularFile(content.path()));
        assertEquals(withAvatar.avatarUrl(), userProfileService.getPublicProfile(profile.id()).avatarUrl());

        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> avatarService.upload(user,
                        new MockMultipartFile("file", "avatar.txt", "text/plain", "not-an-image".getBytes())));

        CurrentUserResponse withoutAvatar = avatarService.delete(user);
        assertEquals(null, withoutAvatar.avatarUrl());
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> avatarService.content(storageKey));
    }

    @Test
    @Transactional
    void userCanReportItemsAndUsersWithoutAccessingOtherReports() throws Exception {
        UserProfileResponse sellerProfile = registerUser("report_seller_");
        UserProfileResponse reporterProfile = registerUser("reporter_");
        UserProfileResponse otherProfile = registerUser("other_reporter_");
        AuthenticatedUser seller = authenticatedUser(sellerProfile);
        AuthenticatedUser reporter = authenticatedUser(reporterProfile);
        AuthenticatedUser other = authenticatedUser(otherProfile);
        Long categoryId = itemCategoryRepository.findAllByOrderBySortOrderAscIdAsc().getFirst().getId();
        ItemResponse item = itemService.create(seller, new ItemCreateRequest(
                categoryId,
                "违规测试物品",
                "用于验证举报流程的测试物品。",
                new java.math.BigDecimal("20.00"),
                ItemCondition.GOOD,
                "测试地点"));

        ReportResponse itemReport = reportService.create(reporter, new ReportCreateRequest(
                ReportTargetType.ITEM,
                item.id(),
                ReportReason.PROHIBITED_ITEM,
                "该物品疑似属于平台禁止发布的内容。"));
        assertEquals(ReportStatus.PENDING, itemReport.status());
        assertEquals(item.id(), itemReport.targetId());
        assertEquals(1, reportService.listMine(
                reporter, ReportTargetType.ITEM, ReportStatus.PENDING, 1, 20).totalElements());

        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> reportService.create(reporter, new ReportCreateRequest(
                        ReportTargetType.ITEM,
                        item.id(),
                        ReportReason.PROHIBITED_ITEM,
                        "对同一物品重复提交一条待处理举报。")));
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> reportService.detailMine(itemReport.id(), other));
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> reportService.create(seller, new ReportCreateRequest(
                        ReportTargetType.ITEM,
                        item.id(),
                        ReportReason.OTHER,
                        "卖家不应该能够举报自己发布的物品。")));

        ReportResponse userReport = reportService.create(reporter, new ReportCreateRequest(
                ReportTargetType.USER,
                sellerProfile.id(),
                ReportReason.IMPERSONATION,
                "该账户资料疑似冒充其他校园用户。"));
        assertEquals(ReportTargetType.USER, userReport.targetType());

        String normalUserToken = jwtService.issue(
                userAccountRepository.findById(reporterProfile.id()).orElseThrow()).value();
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + normalUserToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reports/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void adminCanBlockAndRestoreReportedItemWithTradeCleanup() {
        UserProfileResponse adminProfile = registerUser("report_admin_");
        UserAccount adminAccount = userAccountRepository.findById(adminProfile.id()).orElseThrow();
        adminAccount.changeRole(UserRole.ADMIN);
        userAccountRepository.saveAndFlush(adminAccount);
        AuthenticatedUser admin = new AuthenticatedUser(adminProfile.id(), adminProfile.username(), UserRole.ADMIN);
        UserProfileResponse sellerProfile = registerUser("blocked_seller_");
        UserProfileResponse buyerProfile = registerUser("blocked_buyer_");
        UserProfileResponse reporterProfile = registerUser("blocked_reporter_");
        AuthenticatedUser seller = authenticatedUser(sellerProfile);
        AuthenticatedUser buyer = authenticatedUser(buyerProfile);
        AuthenticatedUser reporter = authenticatedUser(reporterProfile);
        Long categoryId = itemCategoryRepository.findAllByOrderBySortOrderAscIdAsc().getFirst().getId();
        ItemResponse item = itemService.create(seller, new ItemCreateRequest(
                categoryId,
                "待屏蔽物品",
                "用于验证管理员屏蔽和恢复流程。",
                new java.math.BigDecimal("66.00"),
                ItemCondition.GOOD,
                "测试地点"));
        TradeRequestResponse trade = tradeRequestService.create(
                buyer, new TradeRequestCreateRequest(item.id(), "申请交易"));
        tradeRequestService.accept(trade.id(), seller);
        ReportResponse report = reportService.create(reporter, new ReportCreateRequest(
                ReportTargetType.ITEM,
                item.id(),
                ReportReason.MISLEADING_INFORMATION,
                "物品描述与实际展示的信息明显不一致。"));

        ReportResponse resolved = reportService.resolve(
                report.id(), ReportAction.BLOCK_ITEM, "核查后确认描述严重失实。", admin);
        assertEquals(ReportStatus.RESOLVED, resolved.status());
        assertEquals(ReportAction.BLOCK_ITEM, resolved.action());
        assertEquals(ItemModerationStatus.BLOCKED,
                marketplaceItemRepository.findById(item.id()).orElseThrow().getModerationStatus());
        assertEquals(ItemStatus.OFF_SHELF,
                marketplaceItemRepository.findById(item.id()).orElseThrow().getStatus());
        assertEquals(TradeStatus.CANCELLED,
                tradeRequestRepository.findById(trade.id()).orElseThrow().getStatus());
        assertEquals(0, itemService.listPublic(categoryId, "待屏蔽", 1, 20).totalElements());
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> itemService.publish(item.id(), seller));
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> reportService.resolve(
                        report.id(), ReportAction.NONE, "不能重复处理。", admin));

        ItemModerationResponse restored = reportService.restoreItem(
                item.id(), "复核后允许卖家修改并重新发布。", admin);
        assertEquals(ItemModerationStatus.NORMAL, restored.moderationStatus());
        assertEquals(ItemStatus.OFF_SHELF, restored.status());
        ItemResponse republished = itemService.publish(item.id(), seller);
        assertEquals(ItemStatus.ON_SALE, republished.status());
        assertTrue(adminService.dashboard(admin).totalReports() >= 1);
    }

    @Test
    @Transactional
    void tradeParticipantsCanOpenDisputeAndAdminCanArbitrate() throws Exception {
        UserProfileResponse adminProfile = registerUser("dispute_admin_");
        UserAccount adminAccount = userAccountRepository.findById(adminProfile.id()).orElseThrow();
        adminAccount.changeRole(UserRole.ADMIN);
        userAccountRepository.saveAndFlush(adminAccount);
        AuthenticatedUser admin = new AuthenticatedUser(adminProfile.id(), adminProfile.username(), UserRole.ADMIN);
        UserProfileResponse sellerProfile = registerUser("dispute_seller_");
        UserProfileResponse buyerProfile = registerUser("dispute_buyer_");
        UserProfileResponse outsiderProfile = registerUser("dispute_outsider_");
        AuthenticatedUser seller = authenticatedUser(sellerProfile);
        AuthenticatedUser buyer = authenticatedUser(buyerProfile);
        AuthenticatedUser outsider = authenticatedUser(outsiderProfile);
        Long categoryId = itemCategoryRepository.findAllByOrderBySortOrderAscIdAsc().getFirst().getId();
        ItemResponse item = itemService.create(seller, new ItemCreateRequest(
                categoryId,
                "争议测试物品",
                "用于验证交易争议与管理员仲裁。",
                new java.math.BigDecimal("75.00"),
                ItemCondition.GOOD,
                "测试地点"));
        TradeRequestResponse trade = tradeRequestService.create(
                buyer, new TradeRequestCreateRequest(item.id(), "希望当面交易"));
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> disputeService.open(buyer, new DisputeCreateRequest(
                        trade.id(), DisputeReason.NO_SHOW, "交易尚未接受时不能发起争议说明。")));
        tradeRequestService.accept(trade.id(), seller);

        DisputeResponse dispute = disputeService.open(buyer, new DisputeCreateRequest(
                trade.id(),
                DisputeReason.ITEM_NOT_AS_DESCRIBED,
                "当面核验后发现物品成色与描述明显不符。"));
        assertEquals(DisputeStatus.OPEN, dispute.status());
        assertEquals(TradeStatus.DISPUTED, tradeRequestRepository.findById(trade.id()).orElseThrow().getStatus());
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> tradeRequestService.confirmCompletion(trade.id(), buyer));
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> disputeService.open(seller, new DisputeCreateRequest(
                        trade.id(), DisputeReason.NO_SHOW, "同一交易不能同时存在两个待处理争议。")));
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> disputeService.detailMine(dispute.id(), outsider));
        assertEquals(1, disputeService.listMine(buyer, DisputeStatus.OPEN, null, 1, 20).totalElements());
        assertEquals(1, messageService.listMine(seller, true, MessageType.DISPUTE_OPENED, 1, 20).totalElements());

        DisputeResponse resolved = disputeService.resolve(
                dispute.id(),
                DisputeResolution.CANCEL_TRADE,
                "核验后同意取消本次交易。",
                admin);
        assertEquals(DisputeStatus.RESOLVED, resolved.status());
        assertEquals(DisputeResolution.CANCEL_TRADE, resolved.resolution());
        assertEquals(TradeStatus.CANCELLED, tradeRequestRepository.findById(trade.id()).orElseThrow().getStatus());
        assertEquals(ItemStatus.ON_SALE, marketplaceItemRepository.findById(item.id()).orElseThrow().getStatus());
        assertEquals(1, messageService.listMine(buyer, true, MessageType.DISPUTE_RESOLVED, 1, 20).totalElements());

        ItemResponse secondItem = itemService.create(seller, new ItemCreateRequest(
                categoryId,
                "二次争议物品",
                "用于验证完成裁定与举报职责分离。",
                new java.math.BigDecimal("40.00"),
                ItemCondition.GOOD,
                "测试地点"));
        TradeRequestResponse secondTrade = tradeRequestService.create(
                buyer, new TradeRequestCreateRequest(secondItem.id(), "再次申请"));
        tradeRequestService.accept(secondTrade.id(), seller);
        DisputeResponse secondDispute = disputeService.open(seller, new DisputeCreateRequest(
                secondTrade.id(),
                DisputeReason.PAYMENT_ISSUE,
                "买家确认后未按约定完成付款交接。"));
        DisputeResponse completed = disputeService.resolve(
                secondDispute.id(),
                DisputeResolution.COMPLETE_TRADE,
                "证据显示交易已实际完成。",
                admin);
        assertEquals(DisputeResolution.COMPLETE_TRADE, completed.resolution());
        assertEquals(TradeStatus.COMPLETED,
                tradeRequestRepository.findById(secondTrade.id()).orElseThrow().getStatus());
        assertEquals(ItemStatus.SOLD,
                marketplaceItemRepository.findById(secondItem.id()).orElseThrow().getStatus());
        reviewService.create(secondTrade.id(), buyer, new ReviewCreateRequest(5, "仲裁后完成评价。"));

        String buyerToken = jwtService.issue(
                userAccountRepository.findById(buyerProfile.id()).orElseThrow()).value();
        mockMvc.perform(get("/api/v1/admin/disputes")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/disputes/mine"))
                .andExpect(status().isUnauthorized());
        assertTrue(adminService.dashboard(admin).openDisputes() >= 0);
        assertTrue(adminService.dashboard(admin).totalDisputes() >= 2);
    }

    @Test
    @Transactional
    void tradeAndReportActionsCreateReadableNotifications() throws Exception {
        UserProfileResponse adminProfile = registerUser("notify_admin_");
        UserAccount adminAccount = userAccountRepository.findById(adminProfile.id()).orElseThrow();
        adminAccount.changeRole(UserRole.ADMIN);
        userAccountRepository.saveAndFlush(adminAccount);
        AuthenticatedUser admin = new AuthenticatedUser(adminProfile.id(), adminProfile.username(), UserRole.ADMIN);
        UserProfileResponse sellerProfile = registerUser("notify_seller_");
        UserProfileResponse buyerProfile = registerUser("notify_buyer_");
        UserProfileResponse reporterProfile = registerUser("notify_reporter_");
        AuthenticatedUser seller = authenticatedUser(sellerProfile);
        AuthenticatedUser buyer = authenticatedUser(buyerProfile);
        AuthenticatedUser reporter = authenticatedUser(reporterProfile);
        Long categoryId = itemCategoryRepository.findAllByOrderBySortOrderAscIdAsc().getFirst().getId();
        ItemResponse item = itemService.create(seller, new ItemCreateRequest(
                categoryId,
                "通知测试物品",
                "用于验证交易与举报通知。",
                new java.math.BigDecimal("50.00"),
                ItemCondition.GOOD,
                "测试地点"));

        TradeRequestResponse trade = tradeRequestService.create(
                buyer, new TradeRequestCreateRequest(item.id(), "希望尽快交易"));
        assertEquals(1, messageService.unreadCount(seller).unreadCount());
        assertEquals(MessageType.TRADE_REQUEST_CREATED,
                messageService.listMine(seller, true, null, 1, 20).content().getFirst().messageType());

        tradeRequestService.accept(trade.id(), seller);
        assertEquals(1, messageService.unreadCount(buyer).unreadCount());
        assertEquals(MessageType.TRADE_REQUEST_ACCEPTED,
                messageService.listMine(buyer, true, MessageType.TRADE_REQUEST_ACCEPTED, 1, 20)
                        .content().getFirst().messageType());

        tradeRequestService.confirmCompletion(trade.id(), buyer);
        assertEquals(MessageType.TRADE_CONFIRMATION_RECEIVED,
                messageService.listMine(seller, true, MessageType.TRADE_CONFIRMATION_RECEIVED, 1, 20)
                        .content().getFirst().messageType());
        tradeRequestService.confirmCompletion(trade.id(), seller);
        assertEquals(1, messageService.listMine(buyer, true, MessageType.TRADE_COMPLETED, 1, 20).totalElements());
        assertEquals(1, messageService.listMine(seller, true, MessageType.TRADE_COMPLETED, 1, 20).totalElements());

        ItemResponse blockedItem = itemService.create(seller, new ItemCreateRequest(
                categoryId,
                "待处理举报物品",
                "用于验证举报处理通知。",
                new java.math.BigDecimal("30.00"),
                ItemCondition.GOOD,
                "测试地点"));
        TradeRequestResponse openTrade = tradeRequestService.create(
                buyer, new TradeRequestCreateRequest(blockedItem.id(), "申请购买"));
        tradeRequestService.accept(openTrade.id(), seller);
        ReportResponse report = reportService.create(reporter, new ReportCreateRequest(
                ReportTargetType.ITEM,
                blockedItem.id(),
                ReportReason.MISLEADING_INFORMATION,
                "物品描述与实际展示的信息明显不一致。"));
        reportService.resolve(report.id(), ReportAction.BLOCK_ITEM, "确认描述失实，屏蔽物品。", admin);

        assertEquals(1, messageService.listMine(reporter, true, MessageType.REPORT_RESOLVED, 1, 20).totalElements());
        assertEquals(1, messageService.listMine(seller, true, MessageType.ITEM_BLOCKED, 1, 20).totalElements());
        assertEquals(1, messageService.listMine(buyer, true, MessageType.TRADE_CANCELLED_BY_MODERATION, 1, 20)
                .totalElements());

        MessageResponse firstUnread = messageService.listMine(reporter, true, null, 1, 20).content().getFirst();
        MessageResponse marked = messageService.markRead(firstUnread.id(), reporter);
        assertTrue(marked.read());
        UnreadCountResponse afterMarkAll = messageService.markAllRead(seller);
        assertEquals(0, afterMarkAll.unreadCount());
        assertEquals(0, messageService.unreadCount(seller).unreadCount());

        String reporterToken = jwtService.issue(
                userAccountRepository.findById(reporterProfile.id()).orElseThrow()).value();
        mockMvc.perform(get("/api/v1/messages/unread-count")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").exists());
        mockMvc.perform(get("/api/v1/messages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void suspendingReportedUserCancelsTradeAndHidesListings() throws Exception {
        UserProfileResponse adminProfile = registerUser("suspend_admin_");
        UserAccount adminAccount = userAccountRepository.findById(adminProfile.id()).orElseThrow();
        adminAccount.changeRole(UserRole.ADMIN);
        userAccountRepository.saveAndFlush(adminAccount);
        AuthenticatedUser admin = new AuthenticatedUser(adminProfile.id(), adminProfile.username(), UserRole.ADMIN);
        UserProfileResponse sellerProfile = registerUser("active_seller_");
        UserProfileResponse targetProfile = registerUser("reported_buyer_");
        UserProfileResponse reporterProfile = registerUser("user_reporter_");
        AuthenticatedUser seller = authenticatedUser(sellerProfile);
        AuthenticatedUser target = authenticatedUser(targetProfile);
        AuthenticatedUser reporter = authenticatedUser(reporterProfile);
        Long categoryId = itemCategoryRepository.findAllByOrderBySortOrderAscIdAsc().getFirst().getId();
        ItemResponse sellerItem = itemService.create(seller, new ItemCreateRequest(
                categoryId,
                "交易联动物品",
                "停用买家后应恢复为在售。",
                new java.math.BigDecimal("88.00"),
                ItemCondition.GOOD,
                "测试地点"));
        ItemResponse targetListing = itemService.create(target, new ItemCreateRequest(
                categoryId,
                "停用用户物品",
                "停用账户的在售物品不应公开展示。",
                new java.math.BigDecimal("99.00"),
                ItemCondition.GOOD,
                "测试地点"));
        TradeRequestResponse trade = tradeRequestService.create(
                target, new TradeRequestCreateRequest(sellerItem.id(), "申请交易"));
        tradeRequestService.accept(trade.id(), seller);
        String targetToken = jwtService.issue(
                userAccountRepository.findById(targetProfile.id()).orElseThrow()).value();
        ReportResponse report = reportService.create(reporter, new ReportCreateRequest(
                ReportTargetType.USER,
                targetProfile.id(),
                ReportReason.FRAUD,
                "该用户在交易沟通中存在明显欺诈行为。"));

        reportService.resolve(report.id(), ReportAction.SUSPEND_USER, "核查后停用涉事账户。", admin);
        assertEquals(UserStatus.SUSPENDED,
                userAccountRepository.findById(targetProfile.id()).orElseThrow().getStatus());
        assertEquals(TradeStatus.CANCELLED,
                tradeRequestRepository.findById(trade.id()).orElseThrow().getStatus());
        assertEquals(ItemStatus.ON_SALE,
                marketplaceItemRepository.findById(sellerItem.id()).orElseThrow().getStatus());
        assertEquals(0, itemService.listPublic(categoryId, "停用用户物品", 1, 20).totalElements());
        assertThrows(top.sharpcaterpillar.campusecho.common.error.BusinessException.class,
                () -> itemService.getPublicDetail(targetListing.id()));
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isUnauthorized());
    }

    private UserProfileResponse registerUser(String prefix) {
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        return userRegistrationService.register(new RegisterRequest(
                prefix + suffix,
                prefix + suffix + "@example.test",
                "safe-password",
                "测试用户",
                null,
                "主校区"));
    }

    private AuthenticatedUser authenticatedUser(UserProfileResponse profile) {
        return new AuthenticatedUser(
                profile.id(),
                profile.username(),
                top.sharpcaterpillar.campusecho.user.domain.UserRole.USER);
    }

}
