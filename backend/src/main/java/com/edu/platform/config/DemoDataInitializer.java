package com.edu.platform.config;

import com.edu.platform.domain.entity.Category;
import com.edu.platform.domain.entity.Course;
import com.edu.platform.domain.entity.Enrollment;
import com.edu.platform.domain.entity.Lesson;
import com.edu.platform.domain.entity.LessonProgress;
import com.edu.platform.domain.entity.Question;
import com.edu.platform.domain.entity.QuestionOption;
import com.edu.platform.domain.entity.Quiz;
import com.edu.platform.domain.entity.StudyPlan;
import com.edu.platform.domain.entity.StudyPlanItem;
import com.edu.platform.domain.entity.User;
import com.edu.platform.domain.enums.CourseLevel;
import com.edu.platform.domain.enums.EnrollmentStatus;
import com.edu.platform.domain.enums.PlanItemStatus;
import com.edu.platform.domain.enums.PlanStatus;
import com.edu.platform.domain.enums.QuestionType;
import com.edu.platform.domain.enums.Role;
import com.edu.platform.repository.CategoryRepository;
import com.edu.platform.repository.CourseRepository;
import com.edu.platform.repository.EnrollmentRepository;
import com.edu.platform.repository.LessonProgressRepository;
import com.edu.platform.repository.LessonRepository;
import com.edu.platform.repository.QuestionOptionRepository;
import com.edu.platform.repository.QuestionRepository;
import com.edu.platform.repository.QuizAttemptRepository;
import com.edu.platform.repository.QuizRepository;
import com.edu.platform.repository.StudyPlanItemRepository;
import com.edu.platform.repository.StudyPlanRepository;
import com.edu.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Inserts the demo accounts, categories, courses, lessons and quizzes the first
 * time the application starts against an empty database. Controlled by
 * {@code app.init-demo-data} in application.yml.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;
    private final StudyPlanRepository planRepository;
    private final StudyPlanItemRepository planItemRepository;
    private final QuizAttemptRepository attemptRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init-demo-data:true}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (userRepository.count() > 0) {
            log.info("Demo data skipped: the database already contains users");
            return;
        }
        log.info("Seeding demo data ...");

        seedUsers();
        seedCatalogue();
        seedDemoStudentActivity();

        log.info("Demo data ready. Admin: admin/admin123  Student: student/student123");
    }

    // ================================================================== accounts

    private void seedUsers() {
        createUser("admin", "admin123", "Platform Admin", "admin@edu-platform.local", Role.ADMIN,
                "Platform administrator");
        createUser("student", "student123", "Alex Chen", "alex@stu.edu-platform.local", Role.STUDENT,
                "Computer science undergraduate");
    }

    private User createUser(String username, String password, String realName, String email,
                            Role role, String bio) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setEmail(email);
        user.setRole(role);
        user.setBio(bio);
        user.setStatus(1);
        return userRepository.save(user);
    }

    // ================================================================== catalogue

    private Long programmingId;
    private Long designId;
    private Long dataId;
    private Long businessId;
    private Long languageId;
    private Long personalId;

    private void seedCatalogue() {
        programmingId = createCategory("Programming", "PROG", "Software development and engineering", "code", 1);
        designId = createCategory("Design", "DESIGN", "UI, UX and visual design", "palette", 2);
        dataId = createCategory("Data Science", "DATA", "Data analysis, statistics and AI", "chart", 3);
        businessId = createCategory("Business", "BUS", "Management, marketing and finance", "briefcase", 4);
        languageId = createCategory("Language", "LANG", "Foreign languages and communication", "globe", 5);
        personalId = createCategory("Personal Development", "PERSONAL", "Study skills and productivity",
                "sparkles", 6);

        // ---- 1. Java -------------------------------------------------------
        Course java = createCourse(programmingId, "Java Programming Fundamentals",
                "Start from zero and write your first object oriented programs",
                "A hands-on introduction to Java: syntax, control flow, classes, collections and exceptions. "
                        + "Every chapter ends with a small exercise you can try in the online quiz.",
                "Dr. Emily Zhang", CourseLevel.BEGINNER, "0", 36, "java,programming,backend,oop", 1, 182, "4.70");
        lesson(java, "Setting up the Java toolchain", "JDK, IDE and your first Hello World", 45);
        lesson(java, "Variables, types and operators", "Primitive types, casting and arithmetic", 50);
        lesson(java, "Control flow", "if / else, switch, for and while loops", 55);
        lesson(java, "Classes and objects", "Fields, constructors, methods and encapsulation", 70);
        lesson(java, "Collections framework", "List, Set, Map and when to use each of them", 65);
        lesson(java, "Exception handling", "Checked vs unchecked exceptions, try with resources", 45);

        // ---- 2. Spring Boot ------------------------------------------------
        Course spring = createCourse(programmingId, "Spring Boot REST API Development",
                "Build production ready REST services with Spring Boot 3",
                "Design layered applications with controllers, services and repositories. Covers validation, "
                        + "exception handling, JWT authentication and MySQL persistence.",
                "Dr. Emily Zhang", CourseLevel.INTERMEDIATE, "0", 42, "java,spring,backend,api", 1, 141, "4.80");
        lesson(spring, "Spring Boot project anatomy", "Starters, auto configuration and profiles", 40);
        lesson(spring, "REST controllers and DTOs", "Mapping HTTP verbs to Java methods", 55);
        lesson(spring, "Persistence with Spring Data JPA", "Entities, repositories and transactions", 70);
        lesson(spring, "Validation and error handling", "Bean validation and a global handler", 45);
        lesson(spring, "Securing the API with JWT", "Stateless authentication explained", 60);

        // ---- 3. Vue --------------------------------------------------------
        Course vue = createCourse(programmingId, "Vue 3 Front-end Engineering",
                "Composition API, routing, state management and component design",
                "Learn how a modern single page application is structured: components, props, reactive state, "
                        + "Vue Router, Pinia and Axios integration with a REST backend.",
                "Marcus Lin", CourseLevel.INTERMEDIATE, "0", 38, "javascript,vue,frontend,web", 1, 167, "4.60");
        lesson(vue, "Reactivity and the Composition API", "ref, reactive and computed", 50);
        lesson(vue, "Components and props", "Splitting the UI into reusable pieces", 45);
        lesson(vue, "Vue Router in depth", "Nested routes, guards and lazy loading", 55);
        lesson(vue, "State management with Pinia", "Stores, actions and persistence", 50);
        lesson(vue, "Talking to the backend", "Axios interceptors and error handling", 45);

        // ---- 4. MySQL ------------------------------------------------------
        Course mysql = createCourse(programmingId, "MySQL Database Design",
                "Model, normalise and query relational data with confidence",
                "From entity relationship modelling to indexes and query optimisation. Includes the full "
                        + "schema of a real course platform as a case study.",
                "Sofia Wang", CourseLevel.BEGINNER, "0", 30, "database,sql,backend,mysql", 1, 203, "4.50");
        lesson(mysql, "Relational modelling basics", "Entities, keys and relationships", 40);
        lesson(mysql, "Normalisation", "1NF to 3NF with practical examples", 45);
        lesson(mysql, "Writing SELECT queries", "Joins, grouping and sub queries", 60);
        lesson(mysql, "Indexes and performance", "Reading the execution plan", 50);
        lesson(mysql, "Transactions and locking", "ACID, isolation levels and deadlocks", 45);

        // ---- 5. UI/UX ------------------------------------------------------
        Course uiux = createCourse(designId, "UI/UX Design Essentials",
                "Design interfaces people actually enjoy using",
                "User research, wireframing, design systems and usability testing, illustrated with a full "
                        + "mobile application case study.",
                "Nora Ibrahim", CourseLevel.BEGINNER, "0", 28, "design,ui,ux,figma", 1, 118, "4.40");
        lesson(uiux, "Understanding your users", "Interviews and personas", 40);
        lesson(uiux, "Information architecture", "Flows, sitemaps and card sorting", 45);
        lesson(uiux, "Wireframes and prototypes", "From paper sketch to clickable prototype", 55);
        lesson(uiux, "Design systems and components", "Colour, type and spacing scales", 50);
        lesson(uiux, "Usability testing", "Running a moderated test session", 40);

        // ---- 6. Data analysis ---------------------------------------------
        Course python = createCourse(dataId, "Data Analysis with Python",
                "Turn raw spreadsheets into clear, defensible conclusions",
                "pandas, NumPy and Matplotlib applied to real datasets, with a strong focus on descriptive "
                        + "statistics and honest visualisation.",
                "Dr. Ravi Menon", CourseLevel.INTERMEDIATE, "0", 40, "python,data,analysis,statistics", 1, 156, "4.70");
        lesson(python, "Python for data", "Notebooks, NumPy arrays and vectorisation", 50);
        lesson(python, "Data frames with pandas", "Loading, cleaning and reshaping data", 65);
        lesson(python, "Descriptive statistics", "Central tendency, spread and outliers", 55);
        lesson(python, "Visualising data", "Charts that do not mislead", 50);
        lesson(python, "Case study", "An end to end analysis report", 60);

        // ---- 7. Machine learning ------------------------------------------
        Course ml = createCourse(dataId, "Machine Learning Foundations",
                "The maths and intuition behind supervised learning",
                "Regression, classification, regularisation and model evaluation, implemented from scratch "
                        + "before using scikit-learn.",
                "Dr. Ravi Menon", CourseLevel.ADVANCED, "0", 48, "python,machine-learning,data,statistics", 1, 97, "4.90");
        lesson(ml, "What learning really means", "Hypothesis spaces and loss functions", 55);
        lesson(ml, "Linear regression from scratch", "Gradient descent step by step", 70);
        lesson(ml, "Classification and logistic regression", "Decision boundaries and probabilities", 65);
        lesson(ml, "Regularisation and overfitting", "Ridge, lasso and cross validation", 60);
        lesson(ml, "Evaluating a model", "Precision, recall, ROC and calibration", 55);

        // ---- 8. Business English ------------------------------------------
        Course english = createCourse(languageId, "Business English Communication",
                "Write and speak clearly in an international workplace",
                "Emails, meetings, presentations and negotiation vocabulary, practised through role plays and "
                        + "short writing assignments.",
                "Claire Dubois", CourseLevel.BEGINNER, "0", 24, "language,english,business,communication", 1, 134, "4.30");
        lesson(english, "Professional email writing", "Tone, structure and common phrases", 40);
        lesson(english, "Running and joining meetings", "Interrupting politely and summarising", 45);
        lesson(english, "Presenting with confidence", "Signposting and handling questions", 50);
        lesson(english, "Negotiation language", "Hedging, conditions and closing", 45);

        // ---- 9. Productivity ----------------------------------------------
        Course productivity = createCourse(personalId, "Personal Productivity & Study Skills",
                "Study smarter, not longer",
                "Evidence based techniques for planning, focus, note taking and revision, plus how to build "
                        + "a study habit that survives a busy week.",
                "Hannah Park", CourseLevel.BEGINNER, "0", 18, "productivity,study,time-management,habits", 1, 221, "4.60");
        lesson(productivity, "How memory actually works", "Spacing, retrieval and interleaving", 35);
        lesson(productivity, "Planning your week", "Goal setting and time blocking", 40);
        lesson(productivity, "Deep focus in a noisy world", "Attention management strategies", 40);
        lesson(productivity, "Notes that you will re-read", "Cornell method and concept maps", 35);

        // ---- 10. Marketing (draft) ----------------------------------------
        Course marketing = createCourse(businessId, "Digital Marketing Strategy",
                "Plan a campaign that reaches the right people",
                "Positioning, channel selection, content calendars and measuring what actually matters.",
                "Diego Alvarez", CourseLevel.INTERMEDIATE, "0", 26, "marketing,business,social-media,content", 0, 0, "0.00");
        lesson(marketing, "Positioning and audience", "Segmentation and value propositions", 45);
        lesson(marketing, "Choosing your channels", "Organic vs paid, reach vs intent", 50);
        lesson(marketing, "Content planning", "Calendars, formats and repurposing", 45);
        lesson(marketing, "Measuring results", "Funnels, attribution and KPIs", 50);

        // ---------------------------------------------------------- quizzes
        quizFor(java.getId(), "Java Basics Check-up",
                "Six questions covering the first half of the Java course.", 20, 60, 0,
                q("Which keyword is used to define a constant in Java?", QuestionType.SINGLE, "B", 10,
                        "final prevents reassignment of the variable.",
                        o("A", "const"), o("B", "final"), o("C", "static"), o("D", "immutable")),
                q("What is the size of an int in Java?", QuestionType.SINGLE, "C", 10,
                        "int is a 32 bit signed two's complement integer.",
                        o("A", "8 bits"), o("B", "16 bits"), o("C", "32 bits"), o("D", "64 bits")),
                q("Which collection does not allow duplicate elements?", QuestionType.SINGLE, "B", 10,
                        "Set is defined by uniqueness of its elements.",
                        o("A", "List"), o("B", "Set"), o("C", "Queue"), o("D", "Deque")),
                q("A String in Java is immutable.", QuestionType.TRUE_FALSE, "T", 10,
                        "Every modification creates a new String instance.",
                        o("T", "True"), o("F", "False")),
                q("Which of the following are primitive types in Java?", QuestionType.MULTIPLE, "A,C,D", 10,
                        "String is a reference type, the other three are primitives.",
                        o("A", "boolean"), o("B", "String"), o("C", "char"), o("D", "double")),
                q("What does the JVM do?", QuestionType.SINGLE, "A", 10,
                        "The JVM executes bytecode, which makes Java platform independent.",
                        o("A", "Executes compiled bytecode"),
                        o("B", "Compiles Java source into bytecode"),
                        o("C", "Manages the database connection"),
                        o("D", "Formats the source code")));

        quizFor(vue.getId(), "Vue 3 Composition API Quiz",
                "Check your understanding of reactivity, routing and state.", 15, 60, 0,
                q("Which function creates a reactive reference for a primitive value?", QuestionType.SINGLE, "A", 10,
                        "ref() wraps a primitive in a reactive reference.",
                        o("A", "ref()"), o("B", "reactive()"), o("C", "computed()"), o("D", "watch()")),
                q("Which lifecycle hook should replace a fetch call in the Composition API?", QuestionType.SINGLE, "C", 10,
                        "onMounted runs after the component has been inserted into the DOM.",
                        o("A", "onCreated()"), o("B", "onBeforeCreate()"),
                        o("C", "onMounted()"), o("D", "onDestroyed()")),
                q("Pinia stores can be used outside of components.", QuestionType.TRUE_FALSE, "T", 10,
                        "As long as the Pinia instance has been installed, useStore() works anywhere.",
                        o("T", "True"), o("F", "False")),
                q("Which of these are valid ways to pass data into a child component?", QuestionType.MULTIPLE, "A,C", 10,
                        "Props and provide/inject both flow data downwards; events flow upwards.",
                        o("A", "props"), o("B", "defineEmits"), o("C", "provide / inject"), o("D", "v-model on the parent")),
                q("What does v-model do on a form input?", QuestionType.SINGLE, "B", 10,
                        "It creates a two-way binding between the input and the state.",
                        o("A", "One way binding from state to input"),
                        o("B", "Two way binding between state and input"),
                        o("C", "Validates the input"),
                        o("D", "Formats the input value")));

        quizFor(mysql.getId(), "SQL & Relational Modelling Quiz",
                "Five questions on keys, normalisation and query writing.", 15, 60, 0,
                q("Which key uniquely identifies each row of a table?", QuestionType.SINGLE, "A", 10,
                        "A primary key is unique and not null by definition.",
                        o("A", "Primary key"), o("B", "Foreign key"), o("C", "Candidate index"), o("D", "Composite key")),
                q("A table is in 2NF when it is in 1NF and ...", QuestionType.SINGLE, "B", 10,
                        "No non key column may depend on part of a composite key.",
                        o("A", "all columns are indexed"),
                        o("B", "there are no partial dependencies"),
                        o("C", "there are no transitive dependencies"),
                        o("D", "it has a surrogate key")),
                q("Which JOIN returns only the rows that exist in both tables?", QuestionType.SINGLE, "C", 10,
                        "INNER JOIN keeps the intersection of the two tables.",
                        o("A", "LEFT JOIN"), o("B", "RIGHT JOIN"), o("C", "INNER JOIN"), o("D", "CROSS JOIN")),
                q("An index always makes writes faster.", QuestionType.TRUE_FALSE, "F", 10,
                        "Indexes speed up reads but must be maintained on every insert or update.",
                        o("T", "True"), o("F", "False")),
                q("Which aggregate functions ignore NULL values?", QuestionType.MULTIPLE, "A,B,D", 10,
                        "COUNT(*) counts rows including NULLs, the other three skip NULL values.",
                        o("A", "SUM"), o("B", "AVG"), o("C", "COUNT(*)"), o("D", "MAX")));

        quizFor(productivity.getId(), "Study Skills Self-assessment",
                "A short check on spacing, focus and planning.", 10, 60, 0,
                q("Spaced repetition works because ...", QuestionType.SINGLE, "B", 10,
                        "Forgetting between sessions makes each retrieval effortful, which strengthens memory.",
                        o("A", "it reduces the total study time"),
                        o("B", "each retrieval after partial forgetting strengthens memory"),
                        o("C", "it removes the need for sleep"),
                        o("D", "it makes material easier to read")),
                q("Time blocking means ...", QuestionType.SINGLE, "A", 10,
                        "You assign a specific task to a specific slot in your calendar.",
                        o("A", "assigning a task to a specific calendar slot"),
                        o("B", "working on whatever feels urgent"),
                        o("C", "blocking distracting websites"),
                        o("D", "studying only in the morning")),
                q("Multitasking usually increases the total time needed to finish tasks.", QuestionType.TRUE_FALSE, "T", 10,
                        "Task switching carries a cognitive cost, so sequential work is usually faster.",
                        o("T", "True"), o("F", "False")),
                q("Which activities count as active recall?", QuestionType.MULTIPLE, "A,C,D", 10,
                        "Re-reading is passive; testing yourself and explaining out loud are active retrieval.",
                        o("A", "Doing practice questions"),
                        o("B", "Re-reading highlighted notes"),
                        o("C", "Explaining the topic out loud"),
                        o("D", "Writing a summary from memory")),
                q("What is the recommended first step when building a study habit?", QuestionType.SINGLE, "D", 10,
                        "Starting small makes the habit stick; you can increase the load later.",
                        o("A", "Plan four hours per day"),
                        o("B", "Wait for motivation"),
                        o("C", "Buy a new laptop"),
                        o("D", "Start with a very small, repeatable session")));
    }

    private Long createCategory(String name, String code, String description, String icon, int order) {
        Category category = new Category();
        category.setName(name);
        category.setCode(code);
        category.setDescription(description);
        category.setIcon(icon);
        category.setSortOrder(order);
        return categoryRepository.save(category).getId();
    }

    private Course createCourse(Long categoryId, String title, String subtitle, String description,
                                String teacher, CourseLevel level, String price, int hours, String tags,
                                int status, int studentCount, String rating) {
        Course course = new Course();
        course.setCategoryId(categoryId);
        course.setTitle(title);
        course.setSubtitle(subtitle);
        course.setDescription(description);
        course.setTeacherName(teacher);
        course.setLevel(level);
        course.setPrice(new BigDecimal(price));
        course.setDurationHours(hours);
        course.setTags(tags);
        course.setStatus(status);
        course.setStudentCount(studentCount);
        course.setRating(new BigDecimal(rating));
        return courseRepository.save(course);
    }

    private void lesson(Course course, String title, String summary, int minutes) {
        Lesson lesson = new Lesson();
        lesson.setCourseId(course.getId());
        lesson.setTitle(title);
        lesson.setSummary(summary);
        lesson.setContent(summary + "\n\nThis chapter contains a short video walkthrough, a written summary "
                + "and a practice exercise. Mark the chapter as finished when you are done so that your "
                + "progress bar updates.");
        lesson.setDurationMinutes(minutes);
        lesson.setSortOrder((int) lessonRepository.countByCourseId(course.getId()) + 1);
        lessonRepository.save(lesson);
    }

    // ------------------------------------------------------------ quiz helpers

    /** A question together with its options, before it is persisted. */
    private record QuestionDraft(Question question, List<QuestionOption> options) {
    }

    private QuestionOption o(String key, String content) {
        QuestionOption option = new QuestionOption();
        option.setOptionKey(key);
        option.setContent(content);
        return option;
    }

    private QuestionDraft q(String content, QuestionType type, String correctAnswer, int score,
                            String analysis, QuestionOption... options) {
        Question question = new Question();
        question.setContent(content);
        question.setType(type);
        question.setCorrectAnswer(correctAnswer);
        question.setScore(score);
        question.setAnalysis(analysis);
        return new QuestionDraft(question, new ArrayList<>(List.of(options)));
    }

    private void quizFor(Long courseId, String title, String description, int minutes, int passScore,
                         int maxAttempts, QuestionDraft... drafts) {
        Quiz quiz = new Quiz();
        quiz.setCourseId(courseId);
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setDurationMinutes(minutes);
        quiz.setPassScore(passScore);
        quiz.setMaxAttempts(maxAttempts);
        quiz.setStatus(1);
        Quiz savedQuiz = quizRepository.save(quiz);

        int order = 1;
        for (QuestionDraft draft : drafts) {
            Question question = draft.question();
            question.setQuizId(savedQuiz.getId());
            question.setSortOrder(order++);
            Question savedQuestion = questionRepository.save(question);

            int optionOrder = 1;
            for (QuestionOption option : draft.options()) {
                option.setQuestionId(savedQuestion.getId());
                option.setSortOrder(optionOrder++);
                optionRepository.save(option);
            }
        }
    }

    // ================================================================== student activity

    /** Gives the demo student some history so the recommendation engine has data. */
    private void seedDemoStudentActivity() {
        User student = userRepository.findByUsername("student").orElse(null);
        if (student == null) {
            return;
        }
        List<Course> courses = courseRepository.findAll();
        Course java = findCourse(courses, "Java Programming Fundamentals");
        Course vue = findCourse(courses, "Vue 3 Front-end Engineering");
        Course productivity = findCourse(courses, "Personal Productivity & Study Skills");

        // The history is deliberately narrow (one Programming course started, one in
        // progress, one finished) so that the recommendation engine still has
        // same-category and shared-tag candidates left to suggest. Enrolling the student
        // in every related course would leave the engine with nothing but the
        // popularity fallback.
        //
        // Enrol and finish part of the Java course (3 of 6 lessons).
        if (java != null) {
            markLessons(student, java, 3);
            enrollAndPlan(student, java, "Java in four weeks",
                    "Finish the Java fundamentals course and pass the check-up quiz above 80%.",
                    -4, 24, 45, 3);
        }
        // Just started the Vue course (1 of 5 lessons).
        if (vue != null) {
            markLessons(student, vue, 1);
            enroll(student, vue);
        }
        // Finished the productivity course completely.
        if (productivity != null) {
            markLessons(student, productivity, Integer.MAX_VALUE);
            enroll(student, productivity);
            Enrollment enrollment = enrollmentRepository
                    .findByUserIdAndCourseId(student.getId(), productivity.getId()).orElse(null);
            if (enrollment != null) {
                enrollment.setStatus(EnrollmentStatus.COMPLETED);
                enrollment.setProgressPercent(100);
                enrollment.setCompletedAt(LocalDateTime.now().minusDays(2));
                enrollmentRepository.save(enrollment);
            }
        }
        // "MySQL Database Design" and "Spring Boot REST API Development" are intentionally
        // left untouched: they share the "backend" tag with the Java course, so the home
        // page shows real tag-driven reasons such as
        // "Matches the topics you are learning: java, backend".
    }

    private Course findCourse(List<Course> courses, String title) {
        return courses.stream().filter(c -> title.equals(c.getTitle())).findFirst().orElse(null);
    }

    private void enroll(User student, Course course) {
        if (enrollmentRepository.findByUserIdAndCourseId(student.getId(), course.getId()).isPresent()) {
            return;
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getId());
        enrollment.setCourseId(course.getId());
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setEnrolledAt(LocalDateTime.now().minusDays(6));
        enrollment.setLastStudyAt(LocalDateTime.now().minusDays(1));
        enrollment.setTotalLessons((int) lessonRepository.countByCourseId(course.getId()));
        enrollment.setCompletedLessons(0);
        enrollment.setProgressPercent(0);
        enrollmentRepository.save(enrollment);
    }

    /** Enrols the student, completes the first {@code lessonCount} lessons and adds a plan. */
    private void enrollAndPlan(User student, Course course, String planTitle, String goal,
                               int startOffsetDays, int endOffsetDays, int dailyMinutes, int lessonCount) {
        enroll(student, course);
        List<Lesson> lessons = markLessons(student, course, lessonCount);

        StudyPlan plan = new StudyPlan();
        plan.setUserId(student.getId());
        plan.setCourseId(course.getId());
        plan.setTitle(planTitle);
        plan.setGoal(goal);
        plan.setStartDate(LocalDate.now().plusDays(startOffsetDays));
        plan.setEndDate(LocalDate.now().plusDays(endOffsetDays));
        plan.setDailyMinutes(dailyMinutes);
        plan.setStatus(PlanStatus.IN_PROGRESS);
        StudyPlan savedPlan = planRepository.save(plan);

        int index = 0;
        for (Lesson lesson : lessonRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId())) {
            StudyPlanItem item = new StudyPlanItem();
            item.setPlanId(savedPlan.getId());
            item.setLessonId(lesson.getId());
            item.setTitle(lesson.getTitle());
            item.setPlannedDate(LocalDate.now().plusDays(index * 3L));
            boolean done = index < lessons.size();
            item.setStatus(done ? PlanItemStatus.DONE : PlanItemStatus.PENDING);
            item.setCompletedAt(done ? LocalDateTime.now().minusDays(2) : null);
            planItemRepository.save(item);
            index++;
        }
    }

    /** Marks the first N lessons of a course as finished. Returns them. */
    private List<Lesson> markLessons(User student, Course course, int count) {
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId());
        List<Lesson> done = lessons.subList(0, Math.min(count, lessons.size()));
        for (Lesson lesson : done) {
            LessonProgress progress = progressRepository
                    .findByUserIdAndLessonId(student.getId(), lesson.getId())
                    .orElseGet(() -> {
                        LessonProgress p = new LessonProgress();
                        p.setUserId(student.getId());
                        p.setCourseId(course.getId());
                        p.setLessonId(lesson.getId());
                        return p;
                    });
            progress.setCompleted(true);
            progress.setWatchedMinutes(lesson.getDurationMinutes());
            progress.setLastViewedAt(LocalDateTime.now().minusDays(1));
            progressRepository.save(progress);
        }
        var enrollment = enrollmentRepository.findByUserIdAndCourseId(student.getId(), course.getId())
                .orElse(null);
        if (enrollment != null) {
            enrollment.setTotalLessons(lessons.size());
            enrollment.setCompletedLessons(done.size());
            enrollment.setProgressPercent(lessons.isEmpty() ? 0
                    : (int) Math.round(done.size() * 100.0 / lessons.size()));
            enrollmentRepository.save(enrollment);
        }
        return done;
    }
}
