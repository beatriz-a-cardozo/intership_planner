package com.interchangeplanner.university;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Imagens das universidades: tipo pelos bytes, capa automática, conteúdo e isolamento. */
class UniversityImagesIT extends IntegrationTest {

    static final byte[] PNG = withHeader(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 64);
    static final byte[] JPEG = withHeader(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, 64);

    TestSession bia;
    String uni;

    static byte[] withHeader(byte[] header, int size) {
        byte[] data = Arrays.copyOf(header, size);
        Arrays.fill(data, header.length, size, (byte) 7);
        return data;
    }

    ResultActions upload(TestSession s, String universityId, String name, String declaredType, byte[] data)
            throws Exception {
        return mvc.perform(MockMvcRequestBuilders.multipart("/api/universities/{id}/images", universityId)
                .file(new MockMultipartFile("file", name, declaredType, data))
                .header(HttpHeaders.AUTHORIZATION, s.bearer()));
    }

    @BeforeEach
    void setUp() throws Exception {
        bia = registerUser("img");
        uni = read(postAs(bia, "/api/universities", "{\"name\":\"TUM\",\"city\":\"München\"}")
                .andExpect(status().isCreated()), "$.id");
    }

    @Test
    void primeiraViraCapaETrocarEExcluirPromovemOutra() throws Exception {
        String first = read(upload(bia, uni, "campus.png", "image/png", PNG)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.cover").value(true)), "$.id");
        // o Content-Type declarado é ignorado: vale o conteúdo
        String second = read(upload(bia, uni, "foto.png", "image/png", JPEG)
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.cover").value(false)), "$.id");

        getAs(bia, "/api/universities").andExpect(jsonPath("$[0].coverImageId").value(first));
        getAs(bia, "/api/universities/{id}/images/{img}/content", uni, first)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("private")))
                .andExpect(content().bytes(PNG));

        patchAs(bia, "/api/universities/{id}/images/{img}/cover", "{}", uni, second)
                .andExpect(jsonPath("$[?(@.cover == true)].id").value(org.hamcrest.Matchers.contains(second)));
        deleteAs(bia, "/api/universities/{id}/images/{img}", uni, second).andExpect(status().isNoContent());
        getAs(bia, "/api/universities/{id}", uni).andExpect(jsonPath("$.university.coverImageId").value(first));
        getAs(bia, "/api/universities/{id}/images", uni).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void recusaArquivoQueNaoEhImagem() throws Exception {
        upload(bia, uni, "virus.png", "image/png", "<script>alert(1)</script>".getBytes())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("file"));
        upload(bia, uni, "vazio.png", "image/png", new byte[0])
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void outraPessoaNaoVeNemEnvia() throws Exception {
        String img = read(upload(bia, uni, "a.png", "image/png", PNG), "$.id");
        TestSession leo = registerUser("img-b");
        getAs(leo, "/api/universities/{id}/images/{img}/content", uni, img).andExpect(status().isNotFound());
        getAs(leo, "/api/universities/{id}/images", uni).andExpect(status().isNotFound());
        upload(leo, uni, "b.png", "image/png", PNG).andExpect(status().isNotFound());
        deleteAs(leo, "/api/universities/{id}/images/{img}", uni, img).andExpect(status().isNotFound());
    }
}
