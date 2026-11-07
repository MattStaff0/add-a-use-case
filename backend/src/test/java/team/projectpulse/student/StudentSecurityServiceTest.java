package team.projectpulse.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.projectpulse.course.Course;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.UserUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Who may deactivate a student (BR-student-lifecycle, UC-STU-deactivate-student 6b). These run inside an
 * {@code AuthorizationManager}, so an unanswerable question has to come back as a denial, never an exception.
 *
 * <p>The course admin who is not among the course section's instructors is built here in memory, because the seed
 * puts every course admin in her course sections.
 */
@ExtendWith(MockitoExtension.class)
class StudentSecurityServiceTest {

    @Mock
    StudentRepository studentRepository;
    @Mock
    UserUtils userUtils;

    @InjectMocks
    StudentSecurityService studentSecurityService;

    Student john;


    @BeforeEach
    void setUp() {
        Instructor courseAdmin = new Instructor("b.wei@abc.edu", "Bingyang", "Wei", "b.wei@abc.edu", "123456", true, "admin instructor");
        courseAdmin.setId(1);
        Instructor sectionInstructor = new Instructor("b.gates@abc.edu", "Bill", "Gates", "b.gates@abc.edu", "123456", true, "instructor");
        sectionInstructor.setId(2);

        Course course = new Course("COSC 40993 Senior Design", "Senior design project course");
        course.setCourseAdmin(courseAdmin);

        Section section = new Section();
        section.setCourse(course);
        section.addInstructor(sectionInstructor); // The course admin is deliberately not one of them

        this.john = new Student("j.smith@abc.edu", "John", "Smith", "j.smith@abc.edu", "123456", true, "student");
        this.john.setId(4);
        section.addStudent(this.john);
    }

    @Test
    @DisplayName("The course admin of her course may deactivate her without being one of her course section's instructors")
    void canChangeStudentStatusAdmitsTheCourseAdmin() {
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.john));

        assertThat(this.studentSecurityService.canChangeStudentStatus(4)).isTrue();
    }

    @Test
    @DisplayName("An instructor assigned to her course section may deactivate her")
    void canChangeStudentStatusAdmitsAnInstructorOfHerCourseSection() {
        given(this.userUtils.getUserId()).willReturn(2);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.john));

        assertThat(this.studentSecurityService.canChangeStudentStatus(4)).isTrue();
    }

    @Test
    @DisplayName("Any other user, the student herself included, may not")
    void canChangeStudentStatusDeniesEveryoneElse() {
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.john));

        given(this.userUtils.getUserId()).willReturn(3); // An instructor of another course
        assertThat(this.studentSecurityService.canChangeStudentStatus(4)).isFalse();

        given(this.userUtils.getUserId()).willReturn(4); // John himself
        assertThat(this.studentSecurityService.canChangeStudentStatus(4)).isFalse();
    }

    @Test
    @DisplayName("An id that names no student is a denial, not an exception")
    void canChangeStudentStatusDeniesAnUnknownStudent() {
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(999999)).willReturn(Optional.empty());

        assertThat(this.studentSecurityService.canChangeStudentStatus(999999)).isFalse();
    }

}
