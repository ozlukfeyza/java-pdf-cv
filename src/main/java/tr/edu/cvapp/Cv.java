package tr.edu.cvapp;

import java.util.List;
import java.util.Objects;

/**
 * Özgeçmiş bilgilerini tutan değiştirilemez (immutable) sınıf.
 * Listeler List.copyOf() ile kopyalandığı için dışarıdan değiştirilemez.
 */
public final class Cv {

    private final String fullName;
    private final String email;
    private final String phone;
    private final String city;
    private final String education;
    private final String profile;
    private final String photoPath;
    private final List<Experience> experiences;
    private final List<String> skills;

    public Cv(String fullName, String email, String phone, String city,
              String education, String profile, String photoPath,
              List<Experience> experiences, List<String> skills) {
        this.fullName = requireText(fullName, "Ad soyad");
        this.email = requireText(email, "E-posta");
        this.phone = requireText(phone, "Telefon");
        this.city = requireText(city, "Şehir");
        this.education = requireText(education, "Eğitim bilgisi");
        this.profile = requireText(profile, "Kişisel profil");
        this.photoPath = requireText(photoPath, "Fotoğraf yolu");
        this.experiences = List.copyOf(Objects.requireNonNull(experiences, "İş deneyimleri listesi null olamaz."));
        this.skills = List.copyOf(Objects.requireNonNull(skills, "Yetenekler listesi null olamaz."));
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCity() {
        return city;
    }

    public String getEducation() {
        return education;
    }

    public String getProfile() {
        return profile;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    /** Değiştirilemez bir liste döndürür. */
    public List<Experience> getExperiences() {
        return experiences;
    }

    /** Değiştirilemez bir liste döndürür. */
    public List<String> getSkills() {
        return skills;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " boş olamaz.");
        }
        return value.strip();
    }
}
