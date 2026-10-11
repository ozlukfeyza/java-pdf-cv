package tr.edu.cvapp;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Oluşturulan PDF'nin gerçekten okunabilir olduğunu PDFBox ile denetler:
 * dosya boş mu, kaç sayfa var, metin çıkarılabiliyor mu.
 */
public final class PdfInspector {

    /** Denetim sonucu: sayfa sayısı ve PDF'den çıkarılan metin. */
    public record Result(int pageCount, String text) {
    }

    private PdfInspector() {
    }

    public static Result inspect(Path pdf) throws PdfGenerationException {
        try {
            if (!Files.isRegularFile(pdf) || Files.size(pdf) == 0) {
                throw new PdfGenerationException("PDF dosyası yok veya boş: " + pdf.toAbsolutePath());
            }
            try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
                String text = new PDFTextStripper().getText(document);
                return new Result(document.getNumberOfPages(), text);
            }
        } catch (IOException e) {
            throw new PdfGenerationException("PDF okunamadı: " + pdf.toAbsolutePath() + " (" + e.getMessage() + ")", e);
        }
    }
}
