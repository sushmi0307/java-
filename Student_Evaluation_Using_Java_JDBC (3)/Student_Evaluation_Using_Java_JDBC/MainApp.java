import java.time.LocalDate;
import java.util.*;

public class MainApp {

    private static final AcademicDAO dao = new AcademicDAO();
    private static final Scanner input = new Scanner(System.in);

    public static void main(String[] args) {

        if (!DBConnection.testConnection()) {
            System.out.println("Unable to connect to database.");
            System.out.println("Please verify MySQL and database configuration.");
            return;
        }

        boolean active = true;

        while (active) {
            displayMenu();

            String option = readText("Select an option: ");

            switch (option) {
                case "1":
                    registerStudent();
                    break;

                case "2":
                    addCourse();
                    break;

                case "3":
                    enrollStudent();
                    break;

                case "4":
                    enterMarks();
                    break;

                case "5":
                    markSheet();
                    break;

                case "6":
                    defaulters(readInt("Enter semester: "));
                    break;

                case "7":
                    departmentSummary(readInt("Enter semester: "));
                    break;

                case "8":
                    condonation();
                    break;

                case "0":
                    active = false;
                    break;

                default:
                    System.out.println("Please select a valid menu option.");
            }
        }

        System.out.println("Student Evaluation System terminated.");
    }

    // Display the available operations
    private static void displayMenu() {
        System.out.println("\n==============================================");
        System.out.println("       KEC ACADEMIC EVALUATION PORTAL");
        System.out.println("             REGULATION 2024");
        System.out.println("==============================================");
        System.out.println(" 1. Register Student");
        System.out.println(" 2. Add Course Details");
        System.out.println(" 3. Enroll Student and Update Attendance");
        System.out.println(" 4. Enter Internal / External Marks");
        System.out.println(" 5. View Semester Grade Sheet");
        System.out.println(" 6. Attendance Shortage Report");
        System.out.println(" 7. Department Result Analysis");
        System.out.println(" 8. Medical Condonation");
        System.out.println(" 0. Close Application");
        System.out.println("----------------------------------------------");
    }

    // Read and validate user input
    private static String readText(String message) {
        System.out.print(message);
        return input.nextLine().trim();
    }

    private static int readInt(String message) {
        while (true) {
            try {
                return Integer.parseInt(readText(message));
            } catch (NumberFormatException ex) {
                System.out.println("Enter an integer value.");
            }
        }
    }

    private static double readDouble(String message) {
        while (true) {
            try {
                return Double.parseDouble(readText(message));
            } catch (NumberFormatException ex) {
                System.out.println("Enter a valid numeric value.");
            }
        }
    }

    // Student registration
    private static void registerStudent() {
        String rollNo = readText("Enter Roll Number: ").toUpperCase();
        String studentName = readText("Enter Student Name: ");
        String department = readText("Enter Department: ");
        int semester = readInt("Enter Current Semester: ");

        boolean saved = dao.addStudent(
                rollNo, studentName, department, semester
        );

        if (saved) {
            System.out.println("Student details registered successfully.");
        } else {
            System.out.println("Registration failed. Verify the entered details.");
        }
    }

    // Add a course to the catalogue
    private static void addCourse() {
        String courseCode = readText("Enter Course Code: ").toUpperCase();
        String courseName = readText("Enter Course Title: ");
        int creditValue = readInt("Enter Credit Points: ");

        System.out.println("1. THEORY");
        System.out.println("2. PRACTICAL");
        System.out.println("3. EMBEDDED");

        String choice = readText("Choose Course Type: ");
        String courseType;

        switch (choice) {
            case "1":
                courseType = "THEORY";
                break;
            case "2":
                courseType = "PRACTICAL";
                break;
            case "3":
                courseType = "EMBEDDED";
                break;
            default:
                System.out.println("Course type is not valid.");
                return;
        }

        int semester = readInt("Enter Offering Semester: ");

        boolean inserted = dao.addCourse(
                courseCode, courseName, creditValue, courseType, semester
        );

        if (inserted) {
            System.out.println("Course added successfully: " + courseCode);
        } else {
            System.out.println("Unable to add the course.");
        }
    }

    // Student course enrollment and attendance
    private static void enrollStudent() {
        String rollNo = readText("Enter Roll Number: ").toUpperCase();
        String courseCode = readText("Enter Course Code: ").toUpperCase();
        int semester = readInt("Enter Semester: ");
        int totalClasses = readInt("Total Classes Conducted: ");
        int attendedClasses = readInt("Classes Attended: ");

        if (totalClasses < 0 || attendedClasses < 0
                || attendedClasses > totalClasses) {
            System.out.println("Please check the attendance values.");
            return;
        }

        double attendance = totalClasses == 0
                ? 0.0
                : attendedClasses * 100.0 / totalClasses;

        System.out.printf("Attendance Percentage: %.2f%%%n", attendance);

        if (attendance >= 75) {
            System.out.println("Status: Eligible for ESE.");
        } else if (attendance >= 65) {
            System.out.println("Status: Medical condonation may be required.");
        } else {
            System.out.println("Status: Attendance shortage - SA.");
        }

        int enrollmentId = dao.enroll(
                rollNo, courseCode, semester, totalClasses, attendedClasses
        );

        if (enrollmentId > 0) {
            System.out.println("Enrollment completed. Reference ID: "
                    + enrollmentId);
        } else {
            System.out.println("Enrollment unsuccessful. Check student and course.");
        }
    }

    // Approve or reject medical condonation
    private static void condonation() {
        String rollNo = readText("Enter Roll Number: ").toUpperCase();
        String courseCode = readText("Enter Course Code: ").toUpperCase();
        int semester = readInt("Enter Semester: ");

        int enrollmentId = dao.getEnrollmentId(
                rollNo, courseCode, semester
        );

        if (enrollmentId <= 0) {
            System.out.println("No matching enrollment was found.");
            return;
        }

        String response = readText(
                "Approve medical condonation? (y/n): "
        );

        boolean approved = response.equalsIgnoreCase("y");
        boolean updated = dao.setCondonation(enrollmentId, approved);

        System.out.println(updated
                ? "Condonation status updated."
                : "Unable to update condonation status.");
    }

    // Record marks for the selected assessment phase
    private static void enterMarks() {
        String rollNo = readText("Enter Roll Number: ").toUpperCase();
        String courseCode = readText("Enter Course Code: ").toUpperCase();
        int semester = readInt("Enter Semester: ");

        int enrollmentId = dao.getEnrollmentId(
                rollNo, courseCode, semester
        );

        if (enrollmentId <= 0) {
            System.out.println("Enrollment details are unavailable.");
            return;
        }

        System.out.println("\n--------- MARK ENTRY ---------");
        System.out.println("1. CIA Test 1 and CIA Test 2");
        System.out.println("2. Assignment / Quiz / Lab");
        System.out.println("3. End Semester Examination");

        boolean saved;

        switch (readText("Select assessment phase: ")) {
            case "1":
                double ciaOne = readDouble("Enter CIA Test 1 Marks: ");
                double ciaTwo = readDouble("Enter CIA Test 2 Marks: ");

                saved = dao.recordPhase1Marks(
                        enrollmentId, ciaOne, ciaTwo
                );
                break;

            case "2":
                double assignment = readDouble(
                        "Enter Assignment / Quiz / Lab Marks: "
                );

                saved = dao.recordPhase2Marks(
                        enrollmentId, assignment
                );
                break;

            case "3":
                boolean absent = readText(
                        "Was the student absent for ESE? (y/n): "
                ).equalsIgnoreCase("y");

                Double eseMarks = absent
                        ? null
                        : readDouble("Enter ESE Marks: ");

                saved = dao.recordPhase3Marks(
                        enrollmentId, eseMarks, absent
                );
                break;

            default:
                System.out.println("Unknown assessment phase.");
                return;
        }

        System.out.println(saved
                ? "Assessment marks saved successfully."
                : "Marks could not be saved.");
    }

    // Display the semester grade sheet
    private static void markSheet() {
        String rollNo = readText("Enter Student Roll Number: ").toUpperCase();
        int semester = readInt("Enter Semester: ");

        List<AcademicDAO.Result> results =
                dao.getResults(rollNo, semester);

        if (results.isEmpty()) {
            System.out.println("No academic records are available.");
            return;
        }

        String border = "=".repeat(128);

        System.out.println("\n" + border);
        System.out.println(center(
                "KONGU ENGINEERING COLLEGE (AUTONOMOUS)", 128
        ));
        System.out.println(center(
                "SEMESTER PERFORMANCE STATEMENT - R2024", 128
        ));
        System.out.println(border);

        AcademicDAO.Result student = results.get(0);

        System.out.printf(
                "Roll Number: %s | Name: %s | Department: %s | Semester: %d | Date: %s%n",
                rollNo, student.name, student.dept, semester, LocalDate.now()
        );

        System.out.println("-".repeat(128));

        System.out.printf(
                "%-9s %-28s %-9s %2s %6s %6s %6s %6s %7s %7s %7s %7s %-4s %-8s%n",
                "Code", "Course Title", "Type", "Cr", "Att%",
                "CIA1", "CIA2", "Asg", "Sc.CIA", "ESE",
                "Sc.ESE", "Total", "Gr", "Status"
        );

        System.out.println("-".repeat(128));

        int totalCredits = 0;
        int earnedCredits = 0;
        int gradePoints = 0;

        boolean hasArrear = false;
        boolean marksPending = false;

        for (AcademicDAO.Result result : results) {
            totalCredits += result.credits;
            gradePoints += result.credits * result.gp;

            if (result.gp > 0) {
                earnedCredits += result.credits;
            }

            if (result.pending) {
                marksPending = true;
            } else if (result.gp == 0) {
                hasArrear = true;
            }

            boolean unavailable =
                    result.pending || (result.gp == 0 && result.total == 0);

            System.out.printf(
                    "%-9s %-28s %-9s %2d %6.1f %6s %6s %6s %7s %7s %7s %7s %-4s %-8s%n",
                    result.code,
                    cut(result.title, 28),
                    result.type,
                    result.credits,
                    result.attPct,
                    formatMark(result.cia1),
                    formatMark(result.cia2),
                    formatMark(result.asg),
                    unavailable ? "-" : String.format("%.2f", result.scaledCia),
                    result.absent ? "AB" : formatMark(result.ese),
                    unavailable ? "-" : String.format("%.2f", result.scaledEse),
                    unavailable ? "-" : String.format("%.2f", result.total),
                    result.grade,
                    result.status
            );
        }

        double sgpa = AcademicDAO.sgpa(results);

        System.out.println("-".repeat(128));
        System.out.printf(
                "Registered Credits: %d | Earned Credits: %d | Grade Points: %d%n",
                totalCredits, earnedCredits, gradePoints
        );

        System.out.printf(
                "Semester GPA: %.2f / 10.00%s%n",
                sgpa,
                marksPending ? " (Provisional)" : ""
        );

        System.out.printf("Equivalent Percentage: %.2f%%%n", sgpa * 10);

        String outcome = marksPending
                ? "INCOMPLETE"
                : hasArrear
                    ? "ARREAR - EXAMINATION REQUIRED"
                    : "PASSED";

        System.out.println("Final Result: " + outcome);
        System.out.println(border);
        System.out.println(
                "Grade Scale: O=10 | A+=9 | A=8 | B+=7 | B=6 | RA/SA/AB=0"
        );
    }

    // List students whose attendance is below the required level
    private static void defaulters(int semester) {
        List<AcademicDAO.Result> results = dao.getResults(null, semester);

        System.out.println("\nATTENDANCE SHORTAGE LIST - SEMESTER " + semester);

        System.out.printf(
                "%-10s %-18s %-6s %-9s %-8s %-20s%n",
                "Roll No", "Student Name", "Dept", "Course", "Att%", "Remarks"
        );

        System.out.println("-".repeat(78));

        int count = 0;

        for (AcademicDAO.Result result : results) {
            if (result.attPct >= 75) {
                continue;
            }

            String status;

            if (result.attPct < 65) {
                status = "SA - Barred";
            } else if (result.condoned) {
                status = "Condonation OK";
            } else {
                status = "Condonation Needed";
            }

            System.out.printf(
                    "%-10s %-18s %-6s %-9s %6.1f%% %-20s%n",
                    result.roll,
                    cut(result.name, 18),
                    result.dept,
                    result.code,
                    result.attPct,
                    status
            );

            count++;
        }

        if (count == 0) {
            System.out.println("All listed students meet the attendance requirement.");
        }

        System.out.println(
                "Attendance below 65%: barred | 65% to below 75%: condonation may apply."
        );
    }

    // Summarize results department by department
    private static void departmentSummary(int semester) {
        Map<String, List<AcademicDAO.Result>> studentGroups =
                new LinkedHashMap<>();

        for (AcademicDAO.Result result : dao.getResults(null, semester)) {
            studentGroups
                    .computeIfAbsent(result.roll, key -> new ArrayList<>())
                    .add(result);
        }

        class DepartmentStats {
            int students;
            int passed;
            int arrears;
            int incomplete;
            int counted;
            double sgpaTotal;
        }

        Map<String, DepartmentStats> departmentData = new TreeMap<>();

        for (List<AcademicDAO.Result> courses : studentGroups.values()) {
            String department = courses.get(0).dept;

            DepartmentStats stats = departmentData.computeIfAbsent(
                    department, key -> new DepartmentStats()
            );

            stats.students++;

            boolean pending = courses.stream().anyMatch(r -> r.pending);
            boolean failed = courses.stream()
                    .anyMatch(r -> !r.pending && r.gp == 0);

            if (pending) {
                stats.incomplete++;
                continue;
            }

            if (failed) {
                stats.arrears++;
            } else {
                stats.passed++;
            }

            stats.sgpaTotal += AcademicDAO.sgpa(courses);
            stats.counted++;
        }

        System.out.println("\nSEMESTER RESULT ANALYSIS - SEMESTER " + semester);

        System.out.printf(
                "%-10s %8s %8s %12s %11s %10s %10s%n",
                "Dept", "Students", "Passed", "Arrears",
                "Incomplete", "Pass %", "Avg SGPA"
        );

        System.out.println("-".repeat(78));

        for (Map.Entry<String, DepartmentStats> entry
                : departmentData.entrySet()) {

            DepartmentStats stats = entry.getValue();

            double passRate = stats.counted == 0
                    ? 0
                    : stats.passed * 100.0 / stats.counted;

            double averageGpa = stats.counted == 0
                    ? 0
                    : stats.sgpaTotal / stats.counted;

            System.out.printf(
                    "%-10s %8d %8d %12d %11d %9.2f%% %10.2f%n",
                    entry.getKey(),
                    stats.students,
                    stats.passed,
                    stats.arrears,
                    stats.incomplete,
                    passRate,
                    averageGpa
            );
        }
    }

    // Format nullable mark values for display
    private static String formatMark(Double mark) {
        return mark == null ? "-" : String.format("%.1f", mark);
    }

    private static String cut(String text, int width) {
        if (text == null || text.length() <= width) {
            return text == null ? "" : text;
        }

        return text.substring(0, width - 1) + ".";
    }

    private static String center(String text, int width) {
        int spaces = Math.max(0, (width - text.length()) / 2);
        return " ".repeat(spaces) + text;
    }
}