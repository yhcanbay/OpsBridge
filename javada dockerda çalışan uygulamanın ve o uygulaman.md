javada dockerda çalışan uygulamanın ve o
uygulamanın githubdaki reposundan commit ve
branch bilgilerini çekme
Evet, Java ile bunu yapabilirsin. Ancak iki farklı bilgiyi ayrı kaynaklardan toplamalısın:
1. Dockerʼda çalışan uygulama hangi commit ve branchʼten üretildi?
2. GitHub reposunda şu anda hangi branchʼler var ve bunların son commitʼleri ne?
Bunları aynı bilgi gibi düşünmemelisin: GitHubʼdaki main branchʼi ilerlemiş olabilir ama
container hâlâ eski commitʼten üretilmiş uygulamayı çalıştırıyor olabilir.
DevOps izleme panelin için önerdiğim akış şu:
Build sırasında Git bilgisi
│
▼
Docker image / uygulama metadata'sı
│
▼
Java monitoring backend ──────► GitHub REST API
│ branch ve commit bilgileri
▼
Karşılaştırma
│
▼
React izleme paneli
1. Çalışan uygulamanın commit bilgisini nasıl alırsın?
Burada tasarım önerim, commit SHA ve branch bilgisini build sırasında uygulamanın içine
veya Docker image metadataʼsına kaydetmen.
Spring Boot uygulamaları için bunun hazır bir yolu var: build sırasında git.properties üretip
bu bilgiyi Actuator üzerinden sunmak. Dosyada branch, commit SHA ve commit zamanı
bulunabilir; /actuator/info bu bilgileri döndürebilir.[1][2]
Örneğin şu yapılandırma kullanılır:
management.endpoints.web.exposure.include=health,info
management.info.git.enabled=true

Beklenen cevap şu şekildedir:
{
"git": {
"branch": "main",
"commit": {
"id": "91c16c2",
"time": "2026-11-11T12:00:00Z"
}
}
}
Bunun çalışması için yalnızca Actuator eklemek yeterli değil; Maven veya Gradle buildʼinde
git.properties dosyasını üretmen gerekir. Maven tarafında io.github.git-commit-id:git-
commit-id-maven-plugin bu amaçla kullanılabilir.[2][3]
Monitoring backendʼin de ilgili uygulamanın /actuator/info adresini okuyarak çalışan
sürümün bilgisini toplar.
Ancak panelin farklı teknolojilerdeki uygulamaları da desteklemesini istiyorsan, yalnızca
Actuatorʼa bağımlı bir model kurma. Ortak bir metadata sözleşmesi tasarlaman daha uygun:
repository
deployedCommitSha
buildBranch
buildNumber
buildTime
Burada buildBranch, “build sırasında kaydedilen branch adı” anlamına gelsin; GitHubʼdaki
güncel branch durumunu temsil etmesin.
2. GitHubʼdaki branch ve commit bilgilerini nasıl çekersin?
Java backendʼinden GitHub REST APIʼye HTTP isteği gönderebilirsin. İhtiyacın olan temel
endpointʼler şunlar:[4][5]
İhtiyaç Endpoint
Repodaki branch’leri listeleme GET /repos/{owner}/{repo}/branches
Bir branch’in bilgisi ve son commit’i GET /repos/{owner}/{repo}/branches/{branch}
Bir branch’in commit geçmişi GET /repos/{owner}/{repo}/commits?sha={branch}
Belirli bir commit’in detayları GET /repos/{owner}/{repo}/commits/{sha}
Private repo için erişim yetkisi gerekir. Bu okuma işlemleri için fine-grained token
kullanıyorsan ilgili repositoryʼde Contents: read izni yeterlidir. Tokenʼı frontendʼe göndermek
yerine backend tarafında tut.[5][4]

Java ile örnek
Aşağıdaki örnek Java 11+ HttpClient kullanarak bir branchʼin bilgisini çeker. OWNER ve REPO
alanlarını kendi repository bilgilerinle değiştirmelisin:
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
public class GitHubClient {
private final HttpClient client = HttpClient.newBuilder()
.connectTimeout(Duration.ofSeconds(10))
.build();
private final String token = System.getenv("GITHUB_TOKEN");
public String getBranch(
String owner,
String repo,
String branch
) throws IOException, InterruptedException {
String encodedBranch = URLEncoder
.encode(branch, StandardCharsets.UTF_8)
.replace("+", "%20");
String url = "https://api.github.com/repos/"
+ owner + "/" + repo
+ "/branches/" + encodedBranch;
HttpRequest.Builder builder = HttpRequest.newBuilder()
.uri(URI.create(url))
.timeout(Duration.ofSeconds(15))
.header("Accept", "application/vnd.github+json")
.GET();
if (token != null && !token.isBlank()) {
builder.header("Authorization", "Bearer " + token);
}
HttpResponse<String> response = client.send(
builder.build(),
HttpResponse.BodyHandlers.ofString()
);
if (response.statusCode() != 200) {
throw new IOException(
"GitHub API hatası: " + response.statusCode()
);

        }
return response.body();
    }
| public static | void main(String[] args) | throws Exception { |
| ------------- | ------------------------ | ------------------ |
| GitHubClient  | github = new             | GitHubClient();    |
| String        | json = github.getBranch( |                    |
"OWNER",
"REPO",
"main"
        );
        System.out.println(json);
    }
}
Branch cevabındaki commit.sha, o branchʼin GitHubʼdaki son commit SHAʼsıdır. JSONʼu Spring
tarafında bir DTOʼya veya JsonNodeʼa dönüştürüp bu alanı okuyabilirsin.[5]
3. Panelinde bu iki veriyi nasıl birleştirmelisin?
Backendʼinde kaynakları ayırmanı öneririm:
DeploymentMetadataService
  → çalışan uygulamanın build bilgisini okur
GitHubService
  → repo, branch ve commit bilgilerini okur
ProjectVersionService
  → iki kaynaktan gelen verileri birleştirir
Frontendʼe örneğin şu modeli döndürebilirsin:
{
| "project": "task-app", |               |     |
| ---------------------- | ------------- | --- |
| "repository":          | "OWNER/REPO", |     |
| "trackedBranch":       | "main",       |     |
| "deployedCommitSha":   | "abc123...",  |     |
| "remoteHeadCommitSha": | "def456...",  |     |
| "matchesBranchHead":   | false         |     |
}
Karşılaştırmada tam SHA kullanmanı, arayüzde ise kısaltılmış SHA göstermeni öneririm.
Ayrıca SHAʼlar farklıysa ilk aşamada yalnızca “Çalışan sürüm, takip edilen branchʼin son
commitʼiyle aynı değil” sonucunu üret. Bu kontrol tek başına “kaç commit geride”, “ileride”
veya “farklı bir geçmişte” olduğunu söylemez.

4. Branch konusunda önemli ayrıntı
Bir commit SHAʼsından her zaman tek bir branch adı çıkaramazsın. GitHubʼın branches-where-
head endpointʼi, commitʼin herhangi bir yerde bulunduğu bütün branchʼleri değil, o commitʼin
son commit olduğu branchʼleri döndürür. Bu nedenle “Bu sürüm hangi branchʼten build
edildi?” bilgisini build sırasında ayrıca kaydetmek daha doğru olur.[4]
Senin panelin için başlangıçta şu üç alan yeterli:
Çalışan commit: uygulamanın build metadataʼsından.
Takip edilen branch: proje yapılandırmandan.
Branchʼin güncel commitʼi: GitHub APIʼden.
Bu ayrım sayesinde panel hem “şu anda ne çalışıyor?” hem de “repositoryʼdeki hedef sürümle
aynı mı?” sorusunu açık biçimde cevaplar.
⁂
1. https://rieckpil.de/howto-expose-git-information-with-spring-boots-actuator/
2. https://codemia.io/knowledge-hub/path/provide_spring_boot_git_and_build_information_via_actuatorinfo_endpoin
t_when_using_maven_as_a_build_tool
3. https://github.com/aa1588/springboot-git-build-info
4. https://docs.github.com/en/rest/commits/commits
5. https://docs.github.com/en/rest/branches/branches
6. https://github.com/github/docs/blob/main/content/rest/authentication/endpoints-available-for-fine-grained-personal
-access-tokens.md
7. https://github.com/github/docs/blob/main/content/rest/authentication/permissions-required-for-fine-grained-person
al-access-tokens.md
8. https://stackoverflow.com/questions/74493073/using-a-github-fine-grained-token-with-git-pull-over-https
9. https://github.com/gary258796/git-info-actuator
10. https://www.codegenes.net/blog/github-api-retrieve-all-commits-for-all-branches-for-a-repo/
11. https://stackoverflow.com/questions/9179828/github-api-retrieve-all-commits-for-all-branches-for-a-repo
12. projects.devsecops_management_platform
13. learning.devops
14. tools.github.profile
15. skills.tech_stack.spring_react
16. https://www.perplexity.ai/search/435083aa-61a6-4852-b537-0d47d6d97d4d
17. https://www.perplexity.ai/search/5c648b41-9c43-43a9-b929-463549e6745a
18. https://www.perplexity.ai/search/e09f285f-e478-4c03-8986-8ee7797a58a7
19. https://www.perplexity.ai/search/a7ac6dae-e38c-4b78-b9a0-1bbad136c651
20. https://docs.github.com/en/rest/commits/commits?apiVersion=2022–11-28
21. https://stackoverflow.com/questions/59556405/provide-spring-boot-git-and-build-information-via-actuator-info-endp
oint-when-u

22. https://runebook.dev/en/docs/spring_boot/actuator/actuator.endpoints.info.git-commit-information
23. https://runebook.dev/en/docs/spring_boot/actuator/howtohowto.build.generate-git-info