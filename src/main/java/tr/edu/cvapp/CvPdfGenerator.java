package tr.edu.cvapp;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bir {@link Cv} nesnesinden A4 boyutunda, fotoğraflı PDF özgeçmiş üretir.
 * PDF çizimi, sayfa düzeni, metin sarma ve dosyaya kaydetme işlemlerinin tamamı bu sınıftadır.
 */
public class CvPdfGenerator {

    // --- Font kaynakları (src/main/resources altında) ---
    private static final String REGULAR_FONT_RESOURCE = "/fonts/DejaVuSans.ttf";
    private static final String BOLD_FONT_RESOURCE = "/fonts/DejaVuSans-Bold.ttf";

    // --- Sayfa ölçüleri (1 punto = 1/72 inç) ---
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 40f;
    private static final float TOP_MARGIN = 50f;
    private static final float BOTTOM_MARGIN = 50f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float RIGHT_EDGE = PAGE_WIDTH - MARGIN;

    // --- Üst bant ve fotoğraf ---
    private static final float HEADER_HEIGHT = 170f;
    private static final float PHOTO_WIDTH = 100f;
    private static final float PHOTO_HEIGHT = 125f;
    private static final float PHOTO_BORDER = 3f;
    private static final float HEADER_TEXT_GAP = 28f;

    // --- Yazı boyutları ---
    private static final float NAME_SIZE = 26f;
    private static final float CONTACT_SIZE = 11f;
    private static final float HEADING_SIZE = 13f;
    private static final float BODY_SIZE = 10.5f;
    private static final float BODY_LEADING = 15f;
    private static final float POSITION_SIZE = 11.5f;
    private static final float POSITION_LEADING = 16f;
    private static final float COMPANY_LEADING = 15f;
    private static final float EXPERIENCE_GAP = 12f;
    private static final float EXPERIENCE_INDENT = 14f;

    // --- Renkler (lacivert / beyaz ağırlıklı) ---
    private static final Color NAVY = new Color(20, 40, 75);
    private static final Color TINT = new Color(232, 236, 244);
    private static final Color TEXT = new Color(40, 40, 40);
    private static final Color MUTED = new Color(100, 108, 122);
    private static final Color HEADER_SUBTEXT = new Color(220, 226, 240);
    private static final Color RULE = new Color(200, 206, 218);

    /**
     * Özgeçmişi PDF olarak üretir ve kaydeder.
     *
     * @param cv         özgeçmiş bilgileri
     * @param outputPath oluşturulacak PDF dosyasının yolu
     * @return oluşturulan dosyanın mutlak yolu
     * @throws PdfGenerationException fotoğraf, font veya dosya yazma sorunlarında
     */
    public Path generate(Cv cv, Path outputPath) throws PdfGenerationException {
        Objects.requireNonNull(cv, "Cv nesnesi null olamaz.");
        Objects.requireNonNull(outputPath, "Çıktı yolu null olamaz.");

        Path photoPath = Path.of(cv.getPhotoPath());
        if (!Files.isRegularFile(photoPath)) {
            throw new PdfGenerationException("Fotoğraf dosyası bulunamadı: " + photoPath.toAbsolutePath()
                    + ". Lütfen geçerli bir JPG/PNG dosyasını bu konuma koyun.");
        }
        createOutputDirectory(outputPath);

        try (PDDocument document = new PDDocument()) {
            PDFont regular = loadFont(document, REGULAR_FONT_RESOURCE);
            PDFont bold = loadFont(document, BOLD_FONT_RESOURCE);
            PDImageXObject photo = loadPhoto(document, photoPath);

            // İçerik akışı, document.save() çağrılmadan ÖNCE kapanmalıdır.
            try (PageWriter writer = new PageWriter(document, regular, bold)) {
                drawHeader(writer, cv, photo);
                drawSectionHeading(writer, "PROFİL");
                drawParagraph(writer, regular, BODY_SIZE, BODY_LEADING, TEXT, MARGIN, CONTENT_WIDTH, cv.getProfile());
                writer.moveDown(10f);

                drawSectionHeading(writer, "EĞİTİM");
                drawParagraph(writer, regular, BODY_SIZE, BODY_LEADING, TEXT, MARGIN, CONTENT_WIDTH, cv.getEducation());
                writer.moveDown(10f);

                drawSectionHeading(writer, "İŞ DENEYİMİ");
                for (Experience experience : cv.getExperiences()) {
                    drawExperience(writer, experience);
                }
                writer.moveDown(4f);

                drawSectionHeading(writer, "YETENEKLER");
                drawSkills(writer, cv.getSkills());
            }

            document.save(outputPath.toFile());
        } catch (IOException e) {
            throw new PdfGenerationException("PDF oluşturulurken veya kaydedilirken hata oluştu ("
                    + outputPath.toAbsolutePath() + "): " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            // PDFBox, fontta olmayan bir karakter yazılmaya çalışılırsa bu hatayı verir.
            throw new PdfGenerationException("Metinde seçili fontun desteklemediği bir karakter var: "
                    + e.getMessage(), e);
        }
        return outputPath.toAbsolutePath();
    }

    // ------------------------------------------------------------------
    // Kaynak yükleme
    // ------------------------------------------------------------------

    private void createOutputDirectory(Path outputPath) throws PdfGenerationException {
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new PdfGenerationException("Çıktı klasörü oluşturulamadı: " + parent + " (" + e.getMessage() + ")", e);
        }
    }

    private PDFont loadFont(PDDocument document, String resource) throws PdfGenerationException {
        try (InputStream in = CvPdfGenerator.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new PdfGenerationException("Font dosyası bulunamadı: src/main/resources" + resource
                        + ". DejaVu Sans .ttf dosyalarının bu klasörde olduğundan emin olun.");
            }
            return PDType0Font.load(document, in);
        } catch (IOException e) {
            throw new PdfGenerationException("Font yüklenemedi (" + resource + "): " + e.getMessage(), e);
        }
    }

    private PDImageXObject loadPhoto(PDDocument document, Path photoPath) throws PdfGenerationException {
        try {
            return PDImageXObject.createFromFileByContent(photoPath.toFile(), document);
        } catch (IOException e) {
            throw new PdfGenerationException("Fotoğraf okunamadı veya desteklenmeyen/bozuk bir dosya: "
                    + photoPath.toAbsolutePath() + " (" + e.getMessage() + ")", e);
        }
    }

    // ------------------------------------------------------------------
    // Bölümlerin çizimi
    // ------------------------------------------------------------------

    /** Lacivert üst bant: solda fotoğraf, sağında ad soyad ve iletişim bilgileri. Metin fotoğrafın sağında başlar. */
    private void drawHeader(PageWriter w, Cv cv, PDImageXObject photo) throws IOException {
        float bandBottom = PAGE_HEIGHT - HEADER_HEIGHT;
        w.fillRect(0, bandBottom, PAGE_WIDTH, HEADER_HEIGHT, NAVY);

        float photoX = MARGIN;
        float photoY = bandBottom + (HEADER_HEIGHT - PHOTO_HEIGHT) / 2f;
        w.fillRect(photoX - PHOTO_BORDER, photoY - PHOTO_BORDER,
                PHOTO_WIDTH + 2 * PHOTO_BORDER, PHOTO_HEIGHT + 2 * PHOTO_BORDER, Color.WHITE);
        w.drawImage(photo, photoX, photoY, PHOTO_WIDTH, PHOTO_HEIGHT);

        float textX = photoX + PHOTO_WIDTH + HEADER_TEXT_GAP;
        float textWidth = RIGHT_EDGE - textX;

        List<String> nameLines = wrap(w.bold, NAME_SIZE, cv.getFullName(), textWidth);
        List<String> contactLines = new ArrayList<>();
        contactLines.addAll(wrap(w.regular, CONTACT_SIZE, "E-posta: " + cv.getEmail(), textWidth));
        contactLines.addAll(wrap(w.regular, CONTACT_SIZE, "Telefon: " + cv.getPhone(), textWidth));
        contactLines.addAll(wrap(w.regular, CONTACT_SIZE, "Şehir: " + cv.getCity(), textWidth));

        float nameLeading = 32f;
        float contactLeading = 17f;
        float blockHeight = 24f + nameLeading * (nameLines.size() - 1) + 26f
                + contactLeading * (contactLines.size() - 1) + 5f;
        float baseline = bandBottom + (HEADER_HEIGHT + blockHeight) / 2f - 24f;

        for (String line : nameLines) {
            w.text(w.bold, NAME_SIZE, Color.WHITE, textX, baseline, line);
            baseline -= nameLeading;
        }
        baseline += nameLeading - 26f;
        for (String line : contactLines) {
            w.text(w.regular, CONTACT_SIZE, HEADER_SUBTEXT, textX, baseline, line);
            baseline -= contactLeading;
        }

        w.moveTo(bandBottom - 32f);
    }

    /** Başlık ile en az iki satır içerik aynı sayfada kalır (başlık sayfa sonunda yalnız kalmaz). */
    private void drawSectionHeading(PageWriter w, String title) throws IOException {
        w.ensureSpace(34f + 2 * BODY_LEADING);
        w.fillRect(MARGIN, w.y - 17f, 4f, 17f, NAVY);
        w.text(w.bold, HEADING_SIZE, NAVY, MARGIN + 11f, w.y - 13f, title);
        w.fillRect(MARGIN, w.y - 23f, CONTENT_WIDTH, 0.75f, RULE);
        w.moveDown(33f);
    }

    /** Bir iş deneyimini tek blok olarak çizer; sığmıyorsa bloğun tamamı yeni sayfaya geçer. */
    private void drawExperience(PageWriter w, Experience e) throws IOException {
        float textX = MARGIN + EXPERIENCE_INDENT;
        float textWidth = CONTENT_WIDTH - EXPERIENCE_INDENT;
        float periodWidth = w.width(w.regular, BODY_SIZE - 0.5f, e.getPeriod());

        List<String> positionLines = wrap(w.bold, POSITION_SIZE, e.getPosition(), textWidth - periodWidth - 12f);
        List<String> descriptionLines = wrap(w.regular, BODY_SIZE, e.getDescription(), textWidth);

        float blockHeight = positionLines.size() * POSITION_LEADING + COMPANY_LEADING
                + descriptionLines.size() * BODY_LEADING + EXPERIENCE_GAP;
        w.ensureSpace(blockHeight);

        float blockTop = w.y;
        for (int i = 0; i < positionLines.size(); i++) {
            w.text(w.bold, POSITION_SIZE, NAVY, textX, w.y - POSITION_SIZE, positionLines.get(i));
            if (i == 0) {
                w.text(w.regular, BODY_SIZE - 0.5f, MUTED, RIGHT_EDGE - periodWidth, w.y - POSITION_SIZE, e.getPeriod());
            }
            w.moveDown(POSITION_LEADING);
        }
        w.text(w.regular, BODY_SIZE, MUTED, textX, w.y - BODY_SIZE, e.getCompany());
        w.moveDown(COMPANY_LEADING);

        for (String line : descriptionLines) {
            w.text(w.regular, BODY_SIZE, TEXT, textX, w.y - BODY_SIZE, line);
            w.moveDown(BODY_LEADING);
        }
        // Soldaki ince lacivert çizgi: blok yüksekliğince
        w.fillRect(MARGIN, w.y + 2f, 3f, blockTop - w.y - 2f, NAVY);
        w.moveDown(EXPERIENCE_GAP);
    }

    /** Yetenekleri açık mavi etiketler hâlinde, satır sonunda otomatik alt satıra geçerek çizer. */
    private void drawSkills(PageWriter w, List<String> skills) throws IOException {
        float chipHeight = 21f;
        float chipGap = 8f;
        float padX = 10f;
        float size = 10f;
        float x = MARGIN;

        w.ensureSpace(chipHeight);
        for (String skill : skills) {
            String label = ellipsize(w.regular, size, skill.strip(), CONTENT_WIDTH - 2 * padX);
            float chipWidth = w.width(w.regular, size, label) + 2 * padX;
            if (x > MARGIN && x + chipWidth > RIGHT_EDGE) {
                x = MARGIN;
                w.moveDown(chipHeight + chipGap);
                w.ensureSpace(chipHeight);
            }
            w.fillRect(x, w.y - chipHeight, chipWidth, chipHeight, TINT);
            w.text(w.regular, size, NAVY, x + padX, w.y - chipHeight + 7f, label);
            x += chipWidth + chipGap;
        }
        w.moveDown(chipHeight + chipGap);
    }

    private void drawParagraph(PageWriter w, PDFont font, float size, float leading, Color color,
                               float x, float maxWidth, String text) throws IOException {
        for (String line : wrap(font, size, text, maxWidth)) {
            w.ensureSpace(leading);
            w.text(font, size, color, x, w.y - size, line);
            w.moveDown(leading);
        }
    }

    // ------------------------------------------------------------------
    // Metin ölçme ve satırlara bölme
    // ------------------------------------------------------------------

    /** Kelimeleri gerçek yazı tipi genişliğine göre satırlara böler; satırlar maxWidth'i aşmaz. */
    private List<String> wrap(PDFont font, float size, String text, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String clean = text.replaceAll("\\p{Cntrl}+", " ").strip();
        if (clean.isEmpty()) {
            return lines;
        }

        StringBuilder current = new StringBuilder();
        for (String word : clean.split("\\s+")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (textWidth(font, candidate, size) <= maxWidth) {
                current = new StringBuilder(candidate);
                continue;
            }
            if (current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            // Tek başına satıra sığmayan çok uzun kelimeyi karakter düzeyinde böl.
            String remaining = word;
            while (textWidth(font, remaining, size) > maxWidth) {
                int cut = fittingLength(font, remaining, size, maxWidth);
                lines.add(remaining.substring(0, cut));
                remaining = remaining.substring(cut);
            }
            current.append(remaining);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    /** maxWidth'e sığan en uzun önek uzunluğu (en az 1, sonsuz döngüyü önlemek için). */
    private int fittingLength(PDFont font, String text, float size, float maxWidth) throws IOException {
        int length = 1;
        while (length < text.length() && textWidth(font, text.substring(0, length + 1), size) <= maxWidth) {
            length++;
        }
        return length;
    }

    private String ellipsize(PDFont font, float size, String text, float maxWidth) throws IOException {
        String clean = text.replaceAll("\\p{Cntrl}+", " ").strip();
        if (textWidth(font, clean, size) <= maxWidth) {
            return clean;
        }
        String ellipsis = "…";
        float available = maxWidth - textWidth(font, ellipsis, size);
        return clean.substring(0, fittingLength(font, clean, size, available)) + ellipsis;
    }

    private static float textWidth(PDFont font, String text, float size) throws IOException {
        return font.getStringWidth(text) / 1000f * size;
    }

    // ------------------------------------------------------------------
    // Sayfa yönetimi: içerik akışını ve imleç konumunu (y) tutar
    // ------------------------------------------------------------------

    /**
     * Geçerli sayfayı ve içerik akışını yönetir. AutoCloseable olduğu için try-with-resources ile
     * kullanılır; böylece son sayfanın içerik akışı her durumda kapanır.
     */
    private static final class PageWriter implements AutoCloseable {
        private final PDDocument document;
        final PDFont regular;
        final PDFont bold;
        private PDPageContentStream stream;
        /** İmleç: bir sonraki öğenin ÜST kenarı (PDF koordinatlarında, alttan ölçülür). */
        float y;

        PageWriter(PDDocument document, PDFont regular, PDFont bold) throws IOException {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
            startPage();
        }

        private void startPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - TOP_MARGIN;
        }

        /** Mevcut sayfada needed kadar yer yoksa yeni sayfa açar. */
        void ensureSpace(float needed) throws IOException {
            if (y - needed < BOTTOM_MARGIN) {
                startPage();
            }
        }

        void moveDown(float amount) {
            y -= amount;
        }

        void moveTo(float newY) {
            y = newY;
        }

        float width(PDFont font, float size, String text) throws IOException {
            return textWidth(font, text, size);
        }

        void text(PDFont font, float size, Color color, float x, float baseline, String value) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline);
            stream.showText(value);
            stream.endText();
        }

        void fillRect(float x, float bottom, float width, float height, Color color) throws IOException {
            stream.setNonStrokingColor(color);
            stream.addRect(x, bottom, width, height);
            stream.fill();
        }

        void drawImage(PDImageXObject image, float x, float bottom, float width, float height) throws IOException {
            stream.drawImage(image, x, bottom, width, height);
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }
}
