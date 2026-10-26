
import java.sql.*;
import java.util.*;

/** Handles academic database operations and KEC R2024 result processing. */
public class AcademicDAO {

    /** Stores marks and evaluation details for a course. */
    public static class Result {
        String roll, name, dept, code, title, type;
        int credits;
        double attPct;
        Double cia1, cia2, asg, ese;
        boolean absent, condoned;
        double scaledCia, scaledEse, total;
        String grade = "-", status = "PENDING";
        int gp;
        boolean pending;
    }

    // ------------ Student and course operations ------------

    public boolean addStudent(String roll, String name, String dept, int sem) {
        String query = "INSERT INTO students "
                + "(roll_no, name, department, current_semester) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, roll);
            ps.setString(2, name);
            ps.setString(3, dept.toUpperCase());
            ps.setInt(4, sem);

            return ps.executeUpdate() == 1;

        } catch (SQLException ex) {
            System.out.println("[ERROR] " + ex.getMessage());
            return false;
        }
    }

    public boolean addCourse(String code, String title, int credits,
                             String type, int sem) {

        String query = "INSERT INTO courses "
                + "(course_code, title, credits, course_type, semester) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, code);
            ps.setString(2, title);
            ps.setInt(3, credits);
            ps.setString(4, type);
            ps.setInt(5, sem);

            return ps.executeUpdate() == 1;

        } catch (SQLException ex) {
            System.out.println("[ERROR] " + ex.getMessage());
            return false;
        }
    }

    // ------------ Enrollment management ------------

    public int enroll(String roll, String course, int sem,
                      int total, int attended) {

        String query = "INSERT INTO enrollments "
                + "(roll_no, course_code, semester, total_classes, attended_classes) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "total_classes = VALUES(total_classes), "
                + "attended_classes = VALUES(attended_classes)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, roll);
            ps.setString(2, course);
            ps.setInt(3, sem);
            ps.setInt(4, total);
            ps.setInt(5, attended);
            ps.executeUpdate();

            int enrollmentId = getEnrollmentId(roll, course, sem);

            if (enrollmentId < 0) {
                return -1;
            }

            String markQuery =
                    "INSERT IGNORE INTO assessment_marks (enrollment_id) VALUES (?)";

            try (PreparedStatement markPs = con.prepareStatement(markQuery)) {
                markPs.setInt(1, enrollmentId);
                markPs.executeUpdate();
            }

            return enrollmentId;

        } catch (SQLException ex) {
            System.out.println("[ERROR] " + ex.getMessage());
            return -1;
        }
    }

    public int getEnrollmentId(String roll, String course, int sem) {
        String query = "SELECT enrollment_id FROM enrollments "
                + "WHERE roll_no = ? AND course_code = ? AND semester = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, roll);
            ps.setString(2, course);
            ps.setInt(3, sem);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("enrollment_id");
                }
            }

        } catch (SQLException ex) {
            System.out.println("[ERROR] " + ex.getMessage());
        }

        return -1;
    }

    public boolean setCondonation(int enrollmentId, boolean approved) {
        String query = "UPDATE enrollments SET is_condoned = ? "
                + "WHERE enrollment_id = ?";

        return update(query, approved ? 1 : 0, enrollmentId);
    }

    // ------------ Assessment mark entry ------------

    public boolean recordPhase1Marks(int id, double cia1, double cia2) {
        if (!isValidMark(cia1) || !isValidMark(cia2)) {
            return false;
        }

        String query = "UPDATE assessment_marks "
                + "SET cia1_marks = ?, cia2_marks = ? "
                + "WHERE enrollment_id = ?";

        return update(query, cia1, cia2, id);
    }

    public boolean recordPhase2Marks(int id, double assignment) {
        if (!isValidMark(assignment)) {
            return false;
        }

        String query = "UPDATE assessment_marks "
                + "SET assignment_marks = ? WHERE enrollment_id = ?";

        return update(query, assignment, id);
    }

    public boolean recordPhase3Marks(int id, Double ese, boolean absent) {
        if (absent) {
            String query = "UPDATE assessment_marks "
                    + "SET ese_marks = NULL, is_ese_absent = 1 "
                    + "WHERE enrollment_id = ?";

            return update(query, id);
        }

        if (ese == null || !isValidMark(ese)) {
            return false;
        }

        String query = "UPDATE assessment_marks "
                + "SET ese_marks = ?, is_ese_absent = 0 "
                + "WHERE enrollment_id = ?";

        return update(query, ese, id);
    }

    private boolean isValidMark(double mark) {
        if (mark < 0 || mark > 100) {
            System.out.println("[ERROR] Enter marks between 0 and 100.");
            return false;
        }
        return true;
    }

    // ------------ Common database update method ------------

    private boolean update(String query, Object... values) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            for (int index = 0; index < values.length; index++) {
                ps.setObject(index + 1, values[index]);
            }

            return ps.executeUpdate() > 0;

        } catch (SQLException ex) {
            System.out.println("[ERROR] " + ex.getMessage());
            return false;
        }
    }

    // ------------ KEC R2024 result evaluation ------------

    public static void evaluate(Result result) {

        // Check attendance eligibility first.
        if (result.attPct < 65 ||
                (result.attPct < 75 && !result.condoned)) {

            result.grade = "SA";
            result.gp = 0;
            result.status = "SA";
            return;
        }

        // Handle students absent for the final examination.
        if (result.absent) {
            result.grade = "AB";
            result.gp = 0;
            result.status = "AB";
            return;
        }

        // Evaluation requires all assessment marks.
        if (result.cia1 == null || result.cia2 == null
                || result.asg == null || result.ese == null) {

            result.pending = true;
            result.status = "PENDING";
            return;
        }

        double ciaWeight;
        double eseWeight;
        double minimumEse;

        switch (result.type.toUpperCase()) {
            case "PRACTICAL":
                ciaWeight = 0.60;
                eseWeight = 0.40;
                minimumEse = 50;
                break;

            case "EMBEDDED":
                ciaWeight = 0.50;
                eseWeight = 0.50;
                minimumEse = 45;
                break;

            default:
                ciaWeight = 0.40;
                eseWeight = 0.60;
                minimumEse = 45;
                break;
        }

        // Calculate the weighted assessment scores.
        double internalAverage =
                (result.cia1 + result.cia2 + result.asg) / 3.0;

        result.scaledCia = internalAverage * ciaWeight;
        result.scaledEse = result.ese * eseWeight;
        result.total = result.scaledCia + result.scaledEse;

        boolean passed = result.total >= 50
                && result.ese >= minimumEse;

        long roundedTotal = Math.round(result.total);

        if (!passed) {
            result.grade = "RA";
            result.gp = 0;
            result.status = "FAIL";

        } else if (roundedTotal >= 91) {
            result.grade = "O";
            result.gp = 10;
            result.status = "PASS";

        } else if (roundedTotal >= 81) {
            result.grade = "A+";
            result.gp = 9;
            result.status = "PASS";

        } else if (roundedTotal >= 71) {
            result.grade = "A";
            result.gp = 8;
            result.status = "PASS";

        } else if (roundedTotal >= 61) {
            result.grade = "B+";
            result.gp = 7;
            result.status = "PASS";

        } else {
            result.grade = "B";
            result.gp = 6;
            result.status = "PASS";
        }
    }

    // ------------ Semester GPA calculation ------------

    public static double sgpa(List<Result> results) {
        int totalCredits = 0;
        int weightedPoints = 0;

        for (Result result : results) {
            totalCredits += result.credits;
            weightedPoints += result.credits * result.gp;
        }

        if (totalCredits == 0) {
            return 0.0;
        }

        return (double) weightedPoints / totalCredits;
    }

    // ------------ Retrieve evaluated results ------------

    public List<Result> getResults(String roll, int sem) {

        String query = "SELECT "
                + "s.roll_no, s.name, s.department, "
                + "c.course_code, c.title, c.credits, c.course_type, "
                + "e.total_classes, e.attended_classes, e.is_condoned, "
                + "m.cia1_marks, m.cia2_marks, m.assignment_marks, "
                + "m.ese_marks, m.is_ese_absent "
                + "FROM enrollments e "
                + "INNER JOIN students s ON e.roll_no = s.roll_no "
                + "INNER JOIN courses c ON e.course_code = c.course_code "
                + "LEFT JOIN assessment_marks m "
                + "ON e.enrollment_id = m.enrollment_id "
                + "WHERE e.semester = ? "
                + (roll != null ? "AND e.roll_no = ? " : "")
                + "ORDER BY s.roll_no, c.course_code";

        List<Result> results = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, sem);

            if (roll != null) {
                ps.setString(2, roll);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    Result result = new Result();

                    result.roll = rs.getString("roll_no");
                    result.name = rs.getString("name");
                    result.dept = rs.getString("department");

                    result.code = rs.getString("course_code");
                    result.title = rs.getString("title");
                    result.credits = rs.getInt("credits");
                    result.type = rs.getString("course_type");

                    int totalClasses = rs.getInt("total_classes");
                    int attendedClasses = rs.getInt("attended_classes");

                    result.attPct = totalClasses <= 0
                            ? 0.0
                            : attendedClasses * 100.0 / totalClasses;

                    result.condoned = rs.getBoolean("is_condoned");

                    result.cia1 = getNullableMark(rs, "cia1_marks");
                    result.cia2 = getNullableMark(rs, "cia2_marks");
                    result.asg = getNullableMark(rs, "assignment_marks");
                    result.ese = getNullableMark(rs, "ese_marks");

                    result.absent = rs.getBoolean("is_ese_absent");

                    evaluate(result);
                    results.add(result);
                }
            }

        } catch (SQLException ex) {
            System.out.println("[ERROR] " + ex.getMessage());
        }

        return results;
    }

    private static Double getNullableMark(ResultSet rs, String column)
            throws SQLException {

        double mark = rs.getDouble(column);

        if (rs.wasNull()) {
            return null;
        }

        return mark;
    }
}

