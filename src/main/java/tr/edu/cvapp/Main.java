package tr.edu.cvapp;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Uygulamanın başlangıç noktası. Örnek verileri hazırlar, PDF üretimini
 * {@link CvPdfGenerator} sınıfına devreder ve sonucu kullanıcıya bildirir.
 * PDF çizimi bu sınıfta yapılmaz.
 */
public class Main {

    private static final String PHOTO_PATH = "assets/profil.jpg";
    private static final Path OUTPUT_PATH = Path.of("output", "ozgecmis.pdf");

    public static void main(String[] args) {
        try {
            Cv cv = createSampleCv();
            CvPdfGenerator generator = new CvPdfGenerator();

            Path pdf = generator.generate(cv, OUTPUT_PATH);
            System.out.println("PDF başarıyla oluşturuldu: " + pdf);

            report(PdfInspector.inspect(pdf), cv);
        } catch (IllegalArgumentException e) {
            System.err.println("Özgeçmiş bilgileri geçersiz: " + e.getMessage());
            System.exit(1);
        } catch (PdfGenerationException e) {
            System.err.println("HATA: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Cv createSampleCv() {
        Experience first = new Experience(
                "ABC Teknoloji",
                "Junior Java Developer",
                "2024–2025",
                "Kurumsal bir uygulamanın REST servislerinin geliştirilmesine ve hata düzeltmelerine katkı sağladım. "
                        + "SQL sorguları yazdım, birim testleri hazırladım ve ekip içi kod incelemelerine katıldım. "
                        + "Git ile dal tabanlı çalışma düzenine uyum sağladım.");

        Experience second = new Experience(
                "Mavi Yazılım",
                "Yazılım Stajyeri",
                "2023–2024",
                "Mevcut bir Java uygulamasına küçük özellikler ekledim ve hata kayıtlarını inceledim. "
                        + "Test senaryolarını belgeledim, ekip toplantılarında görev takibine katıldım "
                        + "ve ekibin kod standartlarını öğrendim.");

        Experience third = new Experience(
                "Nova Bilişim",
                "Bilgi Teknolojileri Asistanı",
                "2022–2023",
                "Kullanıcı destek taleplerini karşıladım ve bilgisayar ile ağ kurulumlarında teknik ekibe yardımcı oldum. "
                        + "Donanım envanter kayıtlarını güncelledim, temel veritabanı raporlarını hazırladım.");

        return new Cv(
                "Deniz Yılmaz",
                "deniz.yilmaz@example.com",
                "+90 555 123 45 67",
                "İstanbul, Türkiye",
                "Örnek Üniversite – Bilgisayar Programcılığı",
                "Nesne yönelimli programlama ve veritabanı konularında temel bilgiye sahip, öğrenmeye açık ve "
                        + "takım çalışmasına yatkın bir bilgisayar programcısıyım. Temiz, okunabilir kod yazmaya "
                        + "ve problemleri adım adım çözmeye önem veriyorum.",
                PHOTO_PATH,
                List.of(first, second, third),
                List.of("Java", "OOP", "SQL", "Git", "Problem çözme", "Takım çalışması"));
    }

    /** PDF denetim sonucunu yazdırır; beklenen içerik eksikse uyarı verir. */
    private static void report(PdfInspector.Result result, Cv cv) {
        System.out.println("Sayfa sayısı: " + result.pageCount());
        System.out.println("Çıkarılan metin uzunluğu: " + result.text().length() + " karakter");

        String normalized = result.text().replaceAll("\\s+", " ");
        List<String> expected = new ArrayList<>();
        expected.add(cv.getFullName());
        cv.getExperiences().forEach(experience -> expected.add(experience.getCompany()));

        List<String> missing = new ArrayList<>();
        for (String item : expected) {
            if (!normalized.contains(item)) {
                missing.add(item);
            }
        }
        if (missing.isEmpty()) {
            System.out.println("Doğrulama: ad soyad ve " + cv.getExperiences().size()
                    + " iş deneyiminin şirket adları PDF metninde bulundu.");
        } else {
            System.out.println("UYARI: PDF metninde bulunamayan öğeler: " + missing);
        }
    }
}
