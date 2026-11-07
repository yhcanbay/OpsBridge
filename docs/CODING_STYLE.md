# OpsBridge kod yazım rehberi

Son güncelleme: **2026-11-07 — Europe/Istanbul**.

Kullanıcı, önceki kodu kendi yazımına uymadığı için anlamakta zorlandığını belirtti ve `C:\Yazılım\Sozluk_Uygulamasi` projesini açıkça yazım referansı seçti. Yeni kod ve mevcut kod düzenlemeleri bu projedeki benzer örneklere dayanır.

## Yerel referans

Kaynak kopyası [.reference-code/Sozluk_Uygulamasi](../.reference-code/Sozluk_Uygulamasi/) altında bulunur. Backend Java kaynakları, frontend JS/JSX/CSS kaynakları ve bağımlılık tanımları kopyalandı. `.env`, gerçek uygulama ayarları, Git geçmişi, derleme çıktıları ve kaynak projenin ajan talimatları kopyalanmadı. Bu klasör `.gitignore` kapsamındadır; commit'e veya uygulama derlemesine dahil edilmez. Kopya kendiliğinden senkronlanmaz.

İşin ilgili olduğu akıştan bir veya iki örnek incelemek yeterlidir:

| Konu | Referanstaki örnek |
|---|---|
| Controller arayüzü ve uygulaması | `backend/src/main/java/com/brobeyler/controller/ICategoryController.java`, `controller/impl/CategoryControllerImpl.java` |
| Servis ve adım adım liste dönüşümü | `backend/src/main/java/com/brobeyler/service/ICategoryService.java`, `service/impl/CategoryServiceImpl.java` |
| DTO ve dönüşüm | `backend/src/main/java/com/brobeyler/dto/DtoNotificationSettings.java`, `adapter/NotificationSettingsAdapter.java` |
| React bileşeni ve olaylar | `frontend/src/components/common/DeleteConfirmModal.jsx`, `EditDictionaryModal.jsx` |

## Backend yazımı

- Controller sözleşmesi `I…Controller` arayüzünde, HTTP mapping ve istek doğrulama anotasyonlarıyla yer alır. `controller/impl/…ControllerImpl` sınıfı base path'i tanımlar ve servisi çağırır.
- Servis sözleşmesi `I…Service`, uygulaması `service/impl/…ServiceImpl` olur. Metotlar işlemi ve modeli söyler: `getAllProjects`, `getProjectById`, `saveProject`, `updateProject`, `deleteProject`.
- DTO'lar alanları ayrı satırlarda görünen normal sınıflardır. Lombok `@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` kullanılır. Alan okuma `getName()` şeklindedir. Uzun sıralı constructor çağrıları yerine adı görünen builder alanları kullanılır.
- Entity'de `@Getter`, `@Setter`, `@NoArgsConstructor` kullanılır. `ProjectProfile` koleksiyon güncellemesi ve yaşam döngüsü metotları açık kalır; kimlik ve tarihlerin setter'ları üretilmez.
- Bağımlılıklar `@RequiredArgsConstructor` ve `private final` alanlarla tanımlanır. Bu, referansın yeni sınıflar için belirttiği constructor enjeksiyonuna da uyar.
- Liste dönüşümlerinde `List` değişkenleri, `for` döngüsü ve ara DTO değişkeni kullanılır. Repository sonucu `Optional` değişkenine alınır; bulunamama kontrolü açık `if` ile yapılır.
- Import sırası Java, Spring/diğer `org`, proje `com`, Jakarta, Lombok gruplarıdır. Girinti dört boşluktur; gereksiz satır sıkıştırılmaz.

Lombok'un getter/setter ve builder üretimi [Data](https://projectlombok.org/features/Data), [Builder](https://projectlombok.org/features/Builder) ve [Maven kurulumu](https://projectlombok.org/setup/maven) belgelerine dayanır. Sürümü mevcut Spring Boot bağımlılık yönetimi belirler; annotation processor Maven'de açıkça tanımlıdır.

DTO dönüşümü OpsBridge'in mevcut `mapper/` paketinde Spring bean olarak kalır; metotlar referanstaki `toDto`, `createEntity`, `updateEntity` isimlerini kullanır. Request/response ayrımı, API adresleri, HTTP durumları ve JSON alanları korunur. Referans projedeki singleton adapter düzeni, kimlik doğrulama sistemi veya genel temel arayüzler bu yazım talebiyle OpsBridge'e taşınmaz.

## Frontend yazımı

- Bileşen: `function ProjectForm(...) { ... }`, dosya sonunda `export default ProjectForm`.
- JS/JSX: dört boşluk, tek tırnak, noktalı virgülsüz, son elemandan sonra virgülsüz. JSON ve CSS iki boşlukla devam eder. Prettier ve `.editorconfig` bu düzeni uygular.
- State isimleri anlamını söyler: `isSaving`, `isFormOpen`, `editingProject`, `fieldErrors`.
- Formun input'ları JSX içinde açıkça görünür. `handleChange`, `handleCapabilityChange`, `handleSubmit` ve `handleCancel` ayrı fonksiyonlardır. Tek bir JSX ifadesine birden fazla işlem sıkıştırılmaz.
- HTTP işlemleri servis katmanındadır. Proje servisi `getAllProjects`, `saveProject`, `updateProject`, `deleteProject`, `getIntegrationStatuses` isimlerini kullanır.
- İstek iptali, oturum kontrolü ve CSRF ortak HTTP/context/hook katmanlarında korunur. Reference projede Redux/Formik bulunması, OpsBridge'e bunların da eklenmesini gerektirmez.

## Kontrol

Backend sınıfları taşındığında `mvn clean verify`; frontend için `npm.cmd run format:check`, `npm.cmd test`, `npm.cmd run build` çalıştırılır. Biçim kontrolünden geçmek davranış kanıtı değildir; mevcut API/oturum testleri ve değişen formun davranış testleri de geçmelidir. Son gerçek sonuçlar [.wiki/STATUS.md](../.wiki/STATUS.md) içindedir.
