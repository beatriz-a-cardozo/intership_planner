package com.interchangeplanner.luggage;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Malas, itens, pesos e alertas (Fase 7). */
class LuggageIT extends IntegrationTest {

    TestSession bia;

    @BeforeEach
    void setUp() throws Exception {
        bia = registerUser("mala");
    }

    String bag(String name, String type, Integer maxG) throws Exception {
        return read(postAs(bia, "/api/luggage/bags", """
                {"name":"%s","type":"%s","maxWeightG":%s}""".formatted(name, type, maxG))
                .andExpect(status().isCreated()), "$.id");
    }

    String item(String bag, String name, int qty, Integer unitG, boolean required) throws Exception {
        return read(postAs(bia, "/api/luggage/items", """
                {"bagId":%s,"name":"%s","category":"DOCUMENTS","quantity":%d,"unitWeightG":%s,"required":%s}"""
                .formatted(bag == null ? "null" : "\"" + bag + "\"", name, qty, unitG, required))
                .andExpect(status().isCreated()), "$.id");
    }

    @Test
    void pesoAlertasNaoAlocadosEChecklist() throws Exception {
        String mao = bag("Mala de mão", "CARRY_ON", 5_800);
        String despachada = bag("Despachada", "CHECKED", 23_000);
        item(mao, "Notebook", 1, 2_000, true);
        String livros = item(mao, "Livros", 3, 1_200, false);
        item(mao, "Passaporte", 1, null, true);
        item(null, "Casaco", 1, 1_500, false);

        getAs(bia, "/api/luggage/summary")
                .andExpect(jsonPath("$.bags", hasSize(2)))
                .andExpect(jsonPath("$.bags[0].name").value("Mala de mão"))
                .andExpect(jsonPath("$.bags[0].weightG").value(5_600))
                .andExpect(jsonPath("$.bags[0].itemsWithoutWeight").value(1))
                .andExpect(jsonPath("$.bags[0].state").value("WARNING"))
                .andExpect(jsonPath("$.bags[0].percent").value(96.6))
                .andExpect(jsonPath("$.bags[1].state").value("OK"))
                .andExpect(jsonPath("$.unallocatedCount").value(1))
                .andExpect(jsonPath("$.unallocatedWeightG").value(1_500))
                .andExpect(jsonPath("$.totalWeightG").value(7_100))
                .andExpect(jsonPath("$.requiredNotPacked").value(2));

        // um livro a mais estoura o limite
        putAs(bia, "/api/luggage/items/{id}", """
                {"bagId":"%s","name":"Livros","quantity":4,"unitWeightG":1200}""".formatted(mao), livros)
                .andExpect(jsonPath("$.totalWeightG").value(4_800));
        getAs(bia, "/api/luggage/bags")
                .andExpect(jsonPath("$[0].state").value("OVER"))
                .andExpect(jsonPath("$[0].excessG").value(1_000));

        // embalar e mover
        List<String> passaporte = read(getAs(bia, "/api/luggage/items?bagId={b}", mao), "$[?(@.name == 'Passaporte')].id");
        patchAs(bia, "/api/luggage/items/{id}/packed", "{\"packed\":true}", passaporte.getFirst())
                .andExpect(jsonPath("$.packed").value(true));
        getAs(bia, "/api/luggage/items?packed=true").andExpect(jsonPath("$", hasSize(1)));
        getAs(bia, "/api/luggage/items?unallocated=true").andExpect(jsonPath("$[0].name").value("Casaco"));

        // excluir a mala devolve os itens para "não alocados"
        deleteAs(bia, "/api/luggage/bags/{id}", mao).andExpect(status().isNoContent());
        getAs(bia, "/api/luggage/summary")
                .andExpect(jsonPath("$.bags", hasSize(1)))
                .andExpect(jsonPath("$.unallocatedCount").value(4))
                .andExpect(jsonPath("$.bags[0].id").value(despachada));
    }

    @Test
    void validacoesEIsolamento() throws Exception {
        postAs(bia, "/api/luggage/items", """
                {"name":"X","quantity":0}""").andExpect(status().isBadRequest());
        postAs(bia, "/api/luggage/bags", """
                {"name":"Gigante","maxWeightG":200000}""").andExpect(status().isBadRequest());

        String mala = bag("Minha", "CHECKED", 23_000);
        TestSession leo = registerUser("mala-b");
        postAs(leo, "/api/luggage/items", """
                {"bagId":"%s","name":"Intruso"}""".formatted(mala))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("bagId"));
        deleteAs(leo, "/api/luggage/bags/{id}", mala).andExpect(status().isNotFound());
        getAs(leo, "/api/luggage/summary").andExpect(jsonPath("$.bags", hasSize(0)));
    }
}
