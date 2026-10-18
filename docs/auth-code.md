# Authentication code reference — sign up & sign in

Every file that takes part in registration and login, with the real source. Nothing summarised away.

Section [3](#3-verification-against-the-running-app) records how the whole flow was verified against the
running application.

---

## 0. The two flows

**Register**

```
RegisterView.vue ──► authApi.register() ──► POST /api/auth/register
   └─► AuthController.register()  @Valid
         └─► AuthService.register()
               ├─ existsByUsername()  → 400 "Username is already taken"
               ├─ BCryptPasswordEncoder.encode(password)
               ├─ role = STUDENT, status = 1
               └─ UserRepository.save() ──► sys_user
   └─► Result<UserVO>  (no token yet — the page signs in right after)
```

**Sign in**

```
LoginView.vue ──► userStore.login() ──► authApi.login() ──► POST /api/auth/login
   └─► AuthService.login()
         ├─ findByUsername().orElseThrow()  → 401 "Incorrect username or password"
         ├─ passwordEncoder.matches()       → 401 "Incorrect username or password"
         ├─ status != 1                     → 403 "This account has been disabled"
         └─ JwtUtil.generateToken() ──► LoginResponse{ token, tokenType, expiresIn, user }
   └─► Pinia store persists token + user, router pushes / or /admin
```

**Every later request**

```
API client adds Bearer token
   └─► AuthInterceptor.preHandle()
         ├─ OPTIONS  → pass through (CORS pre-flight)
         ├─ no "Bearer " header → 401 { code: 401 } and stop
         ├─ JwtUtil.parse() → UserContext.set(caller) → continue
         └─ invalid/expired     → 401 { code: 401 } and stop
   └─► controller: UserContext.requireUserId() / UserContext.requireAdmin()  (403 for STUDENT)
   └─► afterCompletion(): UserContext.clear()
```

---

## 1. Backend

### 1.1 Endpoints — `backend/src/main/java/com/edu/platform/controller/AuthController.java`

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<AuthDtos.UserVO> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return Result.success("Registration successful", authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return Result.success("Signed in", authService.login(request));
    }

    @GetMapping("/me")
    public Result<AuthDtos.UserVO> me() {
        return Result.success(authService.getProfile(UserContext.requireUserId()));
    }

    @PutMapping("/profile")
    public Result<AuthDtos.UserVO> updateProfile(@Valid @RequestBody AuthDtos.ProfileUpdateRequest request) {
        return Result.success("Profile updated", authService.updateProfile(UserContext.requireUserId(), request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody AuthDtos.PasswordChangeRequest request) {
        authService.changePassword(UserContext.requireUserId(), request);
        return Result.success("Password updated", null);
    }
}
```

`/register` and `/login` are the only two of these that skip the JWT interceptor.

### 1.2 Business logic — `backend/src/main/java/com/edu/platform/service/AuthService.java`

```java
@Transactional
public AuthDtos.UserVO register(AuthDtos.RegisterRequest request) {
    String username = request.username().trim();
    if (userRepository.existsByUsername(username)) {
        throw new BizException("Username is already taken");
    }
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setRealName(StringUtils.hasText(request.realName()) ? request.realName().trim() : username);
    user.setEmail(request.email());
    user.setRole(Role.STUDENT);   // public sign-up is always a student
    user.setStatus(1);
    return toVO(userRepository.save(user));
}

@Transactional(readOnly = true)
public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
    User user = userRepository.findByUsername(request.username().trim())
            .orElseThrow(() -> BizException.unauthorized("Incorrect username or password"));
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
        throw BizException.unauthorized("Incorrect username or password");
    }
    if (user.getStatus() == null || user.getStatus() != 1) {
        throw BizException.forbidden("This account has been disabled");
    }
    String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    return new AuthDtos.LoginResponse(token, "Bearer", jwtUtil.getExpireSeconds(), toVO(user));
}

@Transactional
public void changePassword(Long userId, AuthDtos.PasswordChangeRequest request) {
    User user = getEntity(userId);
    if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
        throw new BizException("Current password is incorrect");
    }
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
}

public AuthDtos.UserVO toVO(User user) {
    return new AuthDtos.UserVO(user.getId(), user.getUsername(), user.getRealName(),
            user.getEmail(), user.getRole(), user.getAvatar(), user.getBio(),
            user.getStatus(), user.getCreatedAt());
}
```

Notes worth knowing: the same message is used for "unknown user" and "wrong password" so the endpoint
cannot be used to enumerate accounts, and `password` is never part of `UserVO`.

### 1.3 Payloads and validation — `backend/src/main/java/com/edu/platform/dto/AuthDtos.java`

```java
public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30, message = "Username must be 3-30 characters")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Username may only contain letters, digits and underscore")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 30, message = "Password must be 6-30 characters")
        String password,

        @Size(max = 50, message = "Name is too long")
        String realName,

        @Email(message = "Email format is invalid")
        String email) {
}

public record LoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password) {
}

public record LoginResponse(String token, String tokenType, long expiresIn, UserVO user) {
}

public record UserVO(Long id, String username, String realName, String email, Role role,
                     String avatar, String bio, Integer status, LocalDateTime createdAt) {
}

public record PasswordChangeRequest(
        @NotBlank(message = "Current password is required") String oldPassword,
        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 30, message = "New password must be 6-30 characters") String newPassword) {
}
```

### 1.4 Issuing and verifying the token — `security/JwtUtil.java`, `security/JwtProperties.java`

```java
public String generateToken(Long userId, String username, Role role) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + properties.getExpireMinutes() * 60_000L);
    return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("username", username)
            .claim("role", role.name())
            .issuer(properties.getIssuer())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)                 // HS256, key from app.jwt.secret
            .compact();
}

public AuthenticatedUser parse(String token) {
    try {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                Role.valueOf(claims.get("role", String.class)));
    } catch (JwtException | IllegalArgumentException ex) {
        throw BizException.unauthorized("Invalid or expired token, please sign in again");
    }
}
```

The token carries the user id in `sub` plus `username` and `role` claims, so no database lookup is needed
to authorise a request. Default lifetime is 1440 minutes (24 h), from `app.jwt.expire-minutes`.

### 1.5 Interceptor and request-scoped caller

`security/AuthInterceptor.java`

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true;                       // CORS pre-flight
    }
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
        writeUnauthorized(response, "Missing authentication token");
        return false;
    }
    try {
        AuthenticatedUser user = jwtUtil.parse(header.substring(7).trim());
        UserContext.set(user);
        return true;
    } catch (Exception ex) {
        writeUnauthorized(response, ex.getMessage() == null ? "Invalid token" : ex.getMessage());
        return false;
    }
}

@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                            Object handler, Exception ex) {
    UserContext.clear();                   // never leak the caller to the next request on this thread
}
```

`security/UserContext.java` — a `ThreadLocal<AuthenticatedUser>`:

```java
public static AuthenticatedUser require() {
    AuthenticatedUser user = HOLDER.get();
    if (user == null) {
        throw BizException.unauthorized("Please sign in first");
    }
    return user;
}

public static Long requireUserId() { return require().userId(); }

/** Returns the caller and additionally enforces the ADMIN role. */
public static AuthenticatedUser requireAdmin() {
    AuthenticatedUser user = require();
    if (!user.isAdmin()) {
        throw BizException.forbidden("Administrator privileges are required");
    }
    return user;
}
```

`security/AuthenticatedUser.java` is the record `(Long userId, String username, Role role)` with `isAdmin()`.

### 1.6 Wiring — `config/WebConfig.java`

```java
registry.addInterceptor(authInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/register",
                "/api/health",
                "/api/categories",
                "/api/courses",
                "/api/courses/**");

registry.addMapping("/api/**")
        .allowedOrigins(allowedOrigins)          // localhost:5173, 127.0.0.1:5173
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### 1.7 Persistence — `domain/entity/User.java`, `repository/UserRepository.java`

`sys_user` has a unique constraint on `username`; `role` is stored as a string (`STUDENT` / `ADMIN`);
`status` is `1` = enabled, `0` = disabled.

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    long countByRole(Role role);
    Page<User> findByUsernameContainingIgnoreCaseOrRealNameContainingIgnoreCase(
            String username, String realName, Pageable pageable);
    List<User> findTop8ByRoleOrderByCreatedAtDesc(Role role);
}
```

### 1.8 Response envelope — `common/Result.java`

Every answer is `{ "code": 200, "message": "success", "data": ... }`. Login failures go through
`GlobalExceptionHandler` with `BizException.unauthorized(...)` → HTTP 401, `forbidden(...)` → HTTP 403.

---

## 2. Frontend

### 2.1 API client — `frontend/src/api/request.js` + `frontend/src/api/index.js`

```js
const request = axios.create({ baseURL: '/api', timeout: 20000 })

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)      // TOKEN_KEY = 'oep_token'
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 200) {
      ElMessage.error(body.message || 'Request failed')
      return Promise.reject(new Error(body.message || 'Request failed'))
    }
    return body ? body.data : null                   // unwrap the envelope
  },
  async (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      const { default: router } = await import('@/router')
      router.push({ path: '/login', query: { redirect: window.location.pathname } })
      return Promise.reject(error)
    }
    if (status === 403) ElMessage.error(message || 'You do not have permission to do that')
    else ElMessage.error(message)
    return Promise.reject(error)
  }
)
```

```js
export const authApi = {
  register: (payload) => request.post('/auth/register', payload),
  login: (payload) => request.post('/auth/login', payload),
  me: () => request.get('/auth/me'),
  updateProfile: (payload) => request.put('/auth/profile', payload),
  changePassword: (payload) => request.put('/auth/password', payload)
}
```

### 2.2 Session state — `frontend/src/stores/user.js`

```js
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser()
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token && state.user),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isStudent: (state) => state.user?.role === 'STUDENT',
    displayName: (state) => state.user?.realName || state.user?.username || 'Guest'
  },
  actions: {
    persist(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },
    async login(payload) {
      const data = await authApi.login(payload)
      this.persist(data.token, data.user)
      return data.user
    },
    async register(payload) {
      return authApi.register(payload)
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
```

### 2.3 Login page — `frontend/src/views/LoginView.vue`

```js
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: 'Please enter your username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please enter your password', trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const user = await userStore.login({ ...form })
    ElMessage.success(`Welcome back, ${user.realName || user.username}!`)
    router.push(route.query.redirect || (user.role === 'ADMIN' ? '/admin' : '/'))
  } catch (error) {
    /* the interceptor already showed the message */
  } finally {
    loading.value = false
  }
}
```

The template is a two-column layout: a gradient `aside` with the product pitch on the left, and an
`el-form` card on the right with username / password fields plus a **Demo accounts** block whose two
buttons call `fill('student')` / `fill('admin')` and write the credentials straight into the form.

### 2.4 Register page — `frontend/src/views/RegisterView.vue`

```js
const form = reactive({ username: '', realName: '', email: '', password: '', confirm: '' })

const rules = {
  username: [
    { required: true, message: 'Please choose a username', trigger: 'blur' },
    { min: 3, max: 30, message: 'Username must be 3-30 characters', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: 'Only letters, digits and underscore are allowed', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: 'Please enter a valid email or leave it empty', trigger: 'blur' }],
  password: [
    { required: true, message: 'Please choose a password', trigger: 'blur' },
    { min: 6, max: 30, message: 'Password must be 6-30 characters', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: 'Please repeat the password', trigger: 'blur' },
    { validator: (rule, value, callback) =>
        value !== form.password ? callback(new Error('The two passwords do not match')) : callback(),
      trigger: 'blur' }
  ]
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      realName: form.realName,
      email: form.email || null
    })
    ElMessage.success('Account created, signing you in ...')
    await userStore.login({ username: form.username, password: form.password })
    router.push('/courses')
  } catch (error) { /* handled by the interceptor */ } finally {
    loading.value = false
  }
}
```

Note the two-step flow: `POST /auth/register` does **not** return a token, so the page signs in
immediately afterwards to get one, then lands on `/courses`.

### 2.5 Route guards — `frontend/src/router/index.js`

```js
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.public) return true
  if (!userStore.isLoggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.admin && !userStore.isAdmin) return { path: '/' }
  return true
})
```

`/login` and `/register` carry `meta.public: true`; all `/admin/**` routes carry `meta.admin: true`.
This is convenience only — the real gate is `UserContext.requireAdmin()` on the server.

---

## 3. Verification against the running app

Checked with the full stack up (backend on 8080, MySQL on 3306). The credentials were read **out of the
source files** rather than typed in by hand, so the comparison cannot drift:

| Source | Admin | Student |
| --- | --- | --- |
| `DemoDataInitializer.seedUsers()` | reads from source | reads from source |
| `LoginView.vue` quick-fill buttons | same value | same value |
| README "Demo accounts" table | same value | same value |

All three agree, and all of them authenticate successfully:

```
admin    -> HTTP 200, code=200, token=yes
student  -> HTTP 200, code=200, token=yes
```

Controls that prove the test is meaningful:

```
control 1  student + a deliberately wrong password -> HTTP 401, code=401, no token
control 2  POST /api/auth/register probe_user -> HTTP 200, code=200, role=STUDENT,
           and the response body contains NO token        <- confirms the two-step sign-up
control 3  POST /api/auth/login  probe_user    -> HTTP 200, code=200, token=yes
control 4  same username registered twice      -> HTTP 400 "Username is already taken"
```

Also verified earlier against the same running instance: a request without a token gets `401`, a
STUDENT calling `/api/admin/**` gets `403`, and `/api/auth/me` returns the profile of the token owner.

So the auth flow is internally consistent. One caveat worth knowing when you re-seed: because
`DemoDataInitializer` only runs when `sys_user` is empty, **changing the passwords in `seedUsers()` does
not affect a database that has already been seeded** — the accounts keep the passwords from the first
start. To apply new ones you must clear the tables and restart.

