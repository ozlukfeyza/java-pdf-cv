# Java PDF CV Generator

Apache PDFBox kullanarak fotoğraf içeren, lacivert–beyaz temalı, A4 boyutunda PDF özgeçmiş üreten bir Java konsol uygulaması.

## Proje hakkında

Uygulama, hayalî bir kişiye (Deniz Yılmaz) ait kişisel bilgileri, eğitimi, yetenekleri ve üç iş deneyimini nesneler hâlinde hazırlar; ardından bu nesnelerden `output/ozgecmis.pdf` dosyasını üretir. Grafik arayüz yoktur; uygulama terminalden çalışır.

## Projenin amacı

- Nesne yönelimli programlama kavramlarını (sınıf, nesne, kapsülleme, sorumlulukların ayrılması) uygulamalı olarak göstermek.
- Harici bir kütüphaneyi (PDFBox) Maven ile projeye dahil etmeyi ve kullanmayı öğrenmek.
- Kaynak yönetimini (`try-with-resources`) ve hata yönetimini gerçek bir örnek üzerinde uygulamak.
- Projeyi Git ve GitHub ile teslim edilebilir hâle getirmek.

## Kullanılan teknolojiler

| Teknoloji | Açıklama |
|---|---|
| Java 17 (veya daha yeni) | `maven.compiler.release` değeri 17'dir |
| Maven | Derleme ve bağımlılık yönetimi |
| Apache PDFBox 3.0.5 | PDF oluşturma ve okuma (tek harici bağımlılık) |
| DejaVu Sans | Türkçe karakterleri destekleyen, PDF'ye gömülen yazı tipi |
| Git / GitHub | Sürüm kontrolü ve teslim |

## Özellikler

- A4 boyutunda, fotoğraflı PDF çıktısı
- Türkçe karakter desteği (ç, ğ, ı, İ, ö, ş, ü) — DejaVu Sans PDF'ye gömülür
- Kelime genişliği hesaplanarak otomatik satır bölme; metin sayfa kenarına taşmaz
- İçerik sığmazsa otomatik yeni sayfa; bölüm başlığı sayfa sonunda tek başına kalmaz, bir iş deneyimi bloğu bölünmeden bir arada tutulur
- Fotoğraf üst bantta, metinlerden ayrı bir alanda yer alır; üst üste binmez
- Anlaşılır hata mesajları (fotoğraf yok, font bulunamadı, dosya yazılamadı vb.)
- Üretilen PDF'nin PDFBox ile otomatik denetimi (sayfa sayısı, metin çıkarma)

## Proje klasör yapısı

```
java-pdf-cv/
├── pom.xml
├── README.md
├── .gitignore
├── assets/
│   └── profil.jpg
├── output/
│   └── .gitkeep            (ozgecmis.pdf uygulama çalıştırılınca buraya yazılır)
└── src/
    └── main/
        ├── java/tr/edu/cvapp/
        │   ├── Main.java
        │   ├── Cv.java
        │   ├── Experience.java
        │   ├── CvPdfGenerator.java
        │   ├── PdfGenerationException.java
        │   └── PdfInspector.java
        └── resources/fonts/
            ├── DejaVuSans.ttf
            ├── DejaVuSans-Bold.ttf
            └── LICENSE-DejaVu.txt
```

## Sınıfların görevleri

| Sınıf | Görev |
|---|---|
| `Main` | Başlangıç noktası. Örnek verileri hazırlar, `Experience` ve `Cv` nesnelerini oluşturur, `CvPdfGenerator`'ı çağırır, hataları yakalayıp kullanıcıya mesaj verir. PDF çizim kodu içermez. |
| `Cv` | Özgeçmiş bilgilerini tutar. Alanlar `private final`; listeler `List.copyOf()` ile kopyalanır. |
| `Experience` | Tek bir iş deneyimini (şirket, pozisyon, dönem, açıklama) temsil eder. |
| `CvPdfGenerator` | PDF'in tüm çizimini, sayfa düzenini, metin sarmasını ve kaydetmeyi yapar. |
| `PdfGenerationException` | Kullanıcıya gösterilebilir PDF hatalarını taşıyan özel istisna sınıfı. |
| `PdfInspector` | Üretilen PDF'i PDFBox ile açıp sayfa sayısını ve metnini denetler. |

## Kullanılan nesneler ve neden kullanıldıkları

| Nesne | Neden |
|---|---|
| `PDDocument` | Bellekteki PDF belgesini temsil eder; sayfalar, fontlar ve görseller buna eklenir ve en sonunda kaydedilir. |
| `PDPage` | Tek bir sayfadır; `PDRectangle.A4` ile A4 boyutu verilir. |
| `PDPageContentStream` | Sayfaya metin, dikdörtgen ve görsel çizmeyi sağlar. |
| `PDFont` / `PDType0Font` | Gömülü DejaVu Sans fontu; Türkçe karakterler ve metin genişliği ölçümü için gereklidir. |
| `PDImageXObject` | Fotoğraf dosyasını PDF'e eklenebilir görsele çevirir. |
| `List<Experience>`, `List<String>` | Deneyimleri ve yetenekleri sıralı tutar. |
| `PageWriter` (iç sınıf) | Geçerli sayfayı, içerik akışını ve imleç konumunu yönetir; `AutoCloseable` olduğu için akış her durumda kapanır. |

## Nesne yönelimli programlama prensipleri

- **Kapsülleme:** `Cv` ve `Experience` alanları `private`; erişim yalnızca getter metotlarıyla.
- **Tek sorumluluk:** Veri (`Cv`, `Experience`), başlatma (`Main`), PDF çizimi (`CvPdfGenerator`) ve denetim (`PdfInspector`) ayrı sınıflardadır.
- **Değiştirilemezlik (immutability):** Tüm alanlar `final`; listeler kopyalanıp değiştirilemez hâle getirilir.
- **Soyutlama:** `Main`, PDF'in nasıl çizildiğini bilmez; yalnızca `generate(cv, yol)` çağırır.
- **Doğrulama:** Yapıcı metotlar boş/`null` değerleri `IllegalArgumentException` / `NullPointerException` ile reddeder, böylece geçersiz nesne oluşmaz.

## Erişim düzenleyiciler

- `private`: alanlar ve yardımcı metotlar (`requireText`, `wrap`, `drawHeader` vb.)
- `public`: yapıcı metotlar, getter'lar, `generate`, `inspect`, `main`
- `final`: alanlar, sınıflar (`Cv`, `Experience`, `PdfInspector`, `PageWriter`) ve sabitler (`static final`)

## Nesne yaşam döngüsü ve kaynak yönetimi

1. `Main` içinde `Experience` ve `Cv` nesneleri oluşturulur ve geçerlilikleri yapıcıda denetlenir.
2. `CvPdfGenerator.generate()` içinde `PDDocument` bir `try-with-resources` bloğunda açılır.
3. Fontlar `getResourceAsStream` ile okunur; akış `try-with-resources` ile kapanır.
4. `PageWriter` da bir `try-with-resources` bloğundadır; sayfa değişirken ve çıkışta içerik akışını kapatır.
5. İçerik akışı kapandıktan **sonra** `document.save()` çağrılır; ardından `PDDocument` otomatik kapanır.
6. `PdfInspector` PDF'i yeniden açar, okur ve yine `try-with-resources` ile kapatır.

## Kurulum gereksinimleri

- macOS
- JDK 17 veya üzeri (`java -version` ile kontrol edin)
- Apache Maven 3.8+ (`mvn -version` ile kontrol edin)
- Visual Studio Code ve "Extension Pack for Java" eklentisi (önerilir)
- İnternet bağlantısı (Maven ilk çalıştırmada PDFBox'ı indirir)

Homebrew ile kurulum örneği:

```bash
brew install openjdk@17 maven
```

## Mac ve VS Code üzerinde çalıştırma

1. Projeyi VS Code ile açın (`File > Open Folder` ile `java-pdf-cv` klasörü).
2. Terminali açın (`Terminal > New Terminal`) ve proje kök klasöründe olduğunuzdan emin olun.
3. Derleyin:

   ```bash
   mvn clean compile
   ```

4. Çalıştırın:

   ```bash
   mvn exec:java
   ```

Uygulama proje kök klasöründen çalıştırılmalıdır; çünkü `assets/profil.jpg` ve `output/ozgecmis.pdf` yolları kök klasöre göredir.

Başarılı çalışmada konsolda PDF'in tam yolu, sayfa sayısı ve doğrulama sonucu yazılır; PDF `output/ozgecmis.pdf` olarak oluşur.

**Fotoğraf hakkında:** `assets/profil.jpg`, bu proje için üretilmiş yer tutucu bir siluettir; gerçek bir kişinin fotoğrafı değildir. Kendi fotoğrafınızı kullanmak için aynı dosya adıyla (`profil.jpg`) bu dosyanın üzerine yazın.

## Örnek çıktı

`mvn exec:java` çalıştırıldıktan sonra `output/ozgecmis.pdf` dosyası oluşur. PDF şunları içerir:

- Lacivert üst bantta fotoğraf, ad soyad ve iletişim bilgileri
- **PROFİL**, **EĞİTİM**, **İŞ DENEYİMİ** (üç deneyim) ve **YETENEKLER** bölümleri

PDF'i Finder'da açmak için: `open output/ozgecmis.pdf`

## Yapay zekâ desteğinin nasıl kullanıldığı

Bu projenin kodları, README ve rapor taslağı yapay zekâ (Claude) desteğiyle hazırlanmıştır. Yapay zekâ proje iskeletini, sınıfları ve belgeleri yazmıştır. Yapay zekânın çalıştığı ortamda Maven Central'a erişim engelli olduğu için proje orada **derlenememiş ve çalıştırılamamıştır**; `output/ozgecmis.pdf` bu nedenle yapay zekâ tarafından üretilmemiştir. Derleme, çalıştırma, PDF'in görsel kontrolü ve gerekli düzeltmeler proje sahibi tarafından kendi bilgisayarında yapılmalıdır. Kodun anlaşılması ve gerektiğinde değiştirilmesi de proje sahibinin sorumluluğundadır.

## Bilinen sınırlamalar

- Derleme ve PDF çıktısı henüz gerçek ortamda doğrulanmamıştır (yukarıya bakın).
- Fotoğraf yer tutucudur; kendi fotoğrafınızla değiştirilmelidir.
- Fotoğraf dikdörtgen olarak yerleştirilir (4:5 oran); farklı oranlı fotoğraflar esner. 4:5 oranında (örn. 480×600 piksel) bir fotoğraf kullanın.
- Fontta bulunmayan karakterler (örn. bazı emojiler) hata verir.
- Sayfa numarası ve çok sütunlu düzen yoktur.
- Veriler `Main` içinde sabit yazılmıştır; komut satırından veri girişi yoktur.
- Yazı tipi lisansı: DejaVu fontları serbest lisanslıdır; lisans metni `src/main/resources/fonts/LICENSE-DejaVu.txt` dosyasındadır.
