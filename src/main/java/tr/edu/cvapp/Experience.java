package tr.edu.cvapp;

/**
 * Tek bir iş deneyimini temsil eden değiştirilemez (immutable) sınıf.
 */
public final class Experience {

    private final String company;
    private final String position;
    private final String period;
    private final String description;

    public Experience(String company, String position, String period, String description) {
        this.company = requireText(company, "Şirket adı");
        this.position = requireText(position, "Pozisyon");
        this.period = requireText(period, "Çalışma dönemi");
        this.description = requireText(description, "İş açıklaması");
    }

    public String getCompany() {
        return company;
    }

    public String getPosition() {
        return position;
    }

    public String getPeriod() {
        return period;
    }

    public String getDescription() {
        return description;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " boş olamaz.");
        }
        return value.strip();
    }
}
