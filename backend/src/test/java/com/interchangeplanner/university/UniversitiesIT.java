package com.interchangeplanner.university;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interchangeplanner.support.IntegrationTest;
import com.interchangeplanner.university.application.IntakeWindowCalculator;
import com.interchangeplanner.university.domain.UniversityEnums.Term;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UniversitiesIT extends IntegrationTest {

    static final LocalDate TODAY = LocalDate.now(USER_ZONE);

    TestSession bia;
    String tum;
    String kiel;
    String tumInformatik;
    String kielCs;

    String university(TestSession s, String name, Integer ranking, String city, String cost, String employability)
            throws Exception {
        return read(postAs(s, "/api/universities", """
                {"name":"%s","ranking":%s,"institutionType":"PUBLIC","nature":"TU","city":"%s","countryCode":"DE",
                 "costOfLivingAmount":%s,"costOfLivingCurrency":"EUR","employability":"%s"}"""
                .formatted(name, ranking, city, cost, employability)).andExpect(status().isCreated()), "$.id");
    }

    String program(TestSession s, String universityId, String name, String fee, String chance, String gre) throws Exception {
        return read(postAs(s, "/api/programs", """
                {"universityId":"%s","name":"%s","degree":"M.Sc.","languageSummary":"Englisch","durationSemesters":4,
                 "gre":"%s","interview":"NOT_REQUIRED","nc":"NOT_REQUIRED","applicationChannel":"PORTAL","portalName":"TUMonline",
                 "semesterFeeAmount":%s,"semesterFeeCurrency":"EUR","admissionChance":"%s",
                 "applicationDeadlineText":"01/Dezembro - 01/Março",
                 "languageRequirements":[{"language":"en","minCefr":"B2","testName":"TOEFL iBT","minScore":88,
                   "rawText":"B2 (TOEFL >= 88)"}],
                 "intakes":[{"term":"WINTER","openMonth":12,"openDay":1,"closeMonth":3,"closeDay":1,"startMonth":10}]}"""
                .formatted(universityId, name, gre, fee, chance)).andExpect(status().isCreated()), "$.id");
    }

    @BeforeEach
    void setUp() throws Exception {
        bia = registerUser("uni");
        tum = university(bia, "Technische Universität München", 1, "München", "1500", "EXCELLENT");
        kiel = university(bia, "Christian-Albrechts-Universität zu Kiel", 23, "Kiel", "1000", "MEDIUM");
        tumInformatik = program(bia, tum, "Informatik M.Sc.", "172", "LOW_MEDIUM", "REQUIRED");
        kielCs = program(bia, kiel, "Computer Science M.Sc.", "127", "MEDIUM_HIGH", "NOT_REQUIRED");
    }

    @Nested
    class Catalogo {

        @Test
        void programaTrazRequisitosIntakesProximaJanelaEUniversidade() throws Exception {
            IntakeWindowCalculator.Window w = IntakeWindowCalculator.next(
                    new IntakeWindowCalculator.Spec(Term.WINTER, 12, 1, 3, 1, 10), TODAY);
            getAs(bia, "/api/programs/{id}", tumInformatik)
                    .andExpect(jsonPath("$.university.name").value("Technische Universität München"))
                    .andExpect(jsonPath("$.university.costOfLiving.amount").value(1500.0))
                    .andExpect(jsonPath("$.languageRequirements[0].minCefr").value("B2"))
                    .andExpect(jsonPath("$.languageRequirements[0].minScore").value(88.0))
                    .andExpect(jsonPath("$.intakes[0].nextWindow.intakeYear").value(w.intakeYear()))
                    .andExpect(jsonPath("$.intakes[0].nextWindow.closesOn").value(w.closesOn().toString()))
                    .andExpect(jsonPath("$.nextDeadline").value(w.closesOn().toString()))
                    .andExpect(jsonPath("$.chosen").value(false))
                    .andExpect(jsonPath("$.application").isEmpty());

            getAs(bia, "/api/universities").andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].programCount").value(1));
            getAs(bia, "/api/universities/{id}", tum).andExpect(jsonPath("$.programs", hasSize(1)));
        }

        @Test
        void filtrosDeProgramas() throws Exception {
            getAs(bia, "/api/programs?gre=NOT_REQUIRED").andExpect(jsonPath("$[*].name", contains("Computer Science M.Sc.")));
            getAs(bia, "/api/programs?minAdmissionChance=MEDIUM").andExpect(jsonPath("$", hasSize(1)));
            getAs(bia, "/api/programs?maxSemesterFee=150").andExpect(jsonPath("$[0].name").value("Computer Science M.Sc."));
            getAs(bia, "/api/programs?q=munchen").andExpect(jsonPath("$[0].name").value("Informatik M.Sc."));
            getAs(bia, "/api/programs?language=en").andExpect(jsonPath("$", hasSize(2)));
            getAs(bia, "/api/universities?q=kiel").andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void comparadorDestacaOMelhorDeCadaLinha() throws Exception {
            getAs(bia, "/api/programs/compare?ids={a},{b}", tumInformatik, kielCs)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.programs[0].id").value(tumInformatik))
                    .andExpect(jsonPath("$.highlights.ranking[0]").value(tumInformatik))
                    .andExpect(jsonPath("$.highlights.semesterFee[0]").value(kielCs))
                    .andExpect(jsonPath("$.highlights.costOfLiving[0]").value(kielCs))
                    .andExpect(jsonPath("$.highlights.admissionChance[0]").value(kielCs))
                    .andExpect(jsonPath("$.highlights.employability[0]").value(tumInformatik))
                    // mesmo prazo nos dois: sem destaque
                    .andExpect(jsonPath("$.highlights.nextDeadline", hasSize(0)));
            getAs(bia, "/api/programs/compare?ids={a}", tumInformatik).andExpect(status().isUnprocessableContent());
        }

        @Test
        void validaDiaDoIntakeEExcluiEmCascata() throws Exception {
            postAs(bia, "/api/programs", """
                    {"universityId":"%s","name":"X","intakes":[{"term":"WINTER","closeMonth":2,"closeDay":30,"startMonth":10}]}"""
                    .formatted(tum)).andExpect(status().isUnprocessableContent());
            deleteAs(bia, "/api/universities/{id}", tum).andExpect(status().isNoContent());
            getAs(bia, "/api/programs/{id}", tumInformatik).andExpect(status().isNotFound());
        }
    }

    @Nested
    class Candidaturas {

        String choose(String programId) throws Exception {
            return read(patchAs(bia, "/api/programs/" + programId + "/chosen", "{\"chosen\":true}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chosen").value(true))
                    .andExpect(jsonPath("$.application.status").value("AWAITING_OPENING")), "$.application.id");
        }

        @Test
        void escolherCriaCandidaturaDoProximoCicloEDesmarcarArquiva() throws Exception {
            IntakeWindowCalculator.Window w = IntakeWindowCalculator.next(
                    new IntakeWindowCalculator.Spec(Term.WINTER, 12, 1, 3, 1, 10), TODAY);
            String app = choose(kielCs);
            getAs(bia, "/api/applications/{id}", app)
                    .andExpect(jsonPath("$.intakeYear").value(w.intakeYear()))
                    .andExpect(jsonPath("$.term").value("WINTER"))
                    .andExpect(jsonPath("$.deadline").value(w.closesOn().toString()))
                    .andExpect(jsonPath("$.window.opensOn").value(w.opensOn().toString()))
                    .andExpect(jsonPath("$.result").value("PENDING"))
                    .andExpect(jsonPath("$.program.universityName").value("Christian-Albrechts-Universität zu Kiel"));

            patchAs(bia, "/api/programs/" + kielCs + "/chosen", "{\"chosen\":false}")
                    .andExpect(jsonPath("$.chosen").value(false))
                    .andExpect(jsonPath("$.application").isEmpty());
            getAs(bia, "/api/applications").andExpect(jsonPath("$", hasSize(0)));
            getAs(bia, "/api/applications?includeArchived=true").andExpect(jsonPath("$[0].archived").value(true));

            // marcar de novo reativa a mesma candidatura (histórico preservado)
            String again = choose(kielCs);
            org.assertj.core.api.Assertions.assertThat(again).isEqualTo(app);
            getAs(bia, "/api/programs?chosen=true").andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void statusComHistoricoESolicitadoItemAutomatico() throws Exception {
            String app = choose(tumInformatik);
            patchAs(bia, "/api/applications/" + app + "/status", """
                    {"status":"DOCUMENTS_UNDER_REVIEW","note":"Enviado pelo TUMonline"}""")
                    .andExpect(jsonPath("$.status").value("DOCUMENTS_UNDER_REVIEW"));

            String req = read(postAs(bia, "/api/applications/{id}/requests", """
                    {"type":"DOCUMENT","description":"Certificado de proficiência atualizado","dueDate":"%s"}"""
                    .formatted(TODAY.plusDays(10)), app).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.pending").value(true)), "$.id");
            getAs(bia, "/api/applications/{id}", app)
                    .andExpect(jsonPath("$.status").value("ITEM_REQUESTED"))
                    .andExpect(jsonPath("$.pendingRequests").value(1))
                    .andExpect(jsonPath("$.nextRequestDue").value(TODAY.plusDays(10).toString()));

            putAs(bia, "/api/applications/{a}/requests/{r}", """
                    {"type":"DOCUMENT","description":"Certificado de proficiência atualizado","fulfilledOn":"%s"}"""
                    .formatted(TODAY), app, req).andExpect(jsonPath("$.pending").value(false));
            getAs(bia, "/api/applications/{id}", app).andExpect(jsonPath("$.status").value("DOCUMENTS_UNDER_REVIEW"));

            patchAs(bia, "/api/applications/" + app + "/status", "{\"status\":\"ACCEPTED\"}")
                    .andExpect(jsonPath("$.result").value("ACCEPTED"))
                    .andExpect(jsonPath("$.actualResponseDate").value(TODAY.toString()));

            getAs(bia, "/api/applications/{id}/history", app)
                    .andExpect(jsonPath("$[*].status", contains("AWAITING_OPENING", "DOCUMENTS_UNDER_REVIEW", "ITEM_REQUESTED",
                            "DOCUMENTS_UNDER_REVIEW", "ACCEPTED")))
                    .andExpect(jsonPath("$[1].note").value("Enviado pelo TUMonline"));

            getAs(bia, "/api/dashboard")
                    .andExpect(jsonPath("$.applications.active").value(1))
                    .andExpect(jsonPath("$.applications.accepted").value(1));
        }

        @Test
        void estimativaDeRespostaEmOrdem() throws Exception {
            String app = choose(tumInformatik);
            String body = """
                    {"intakeYear":%d,"term":"WINTER","responseMinDate":"%s","responseLikelyDate":"%s","responseMaxDate":"%s",
                     "responseConfidence":"MEDIUM","responseEstimateBasis":"Ano passado responderam em maio"}""";
            int year = TODAY.getYear() + 1;
            putAs(bia, "/api/applications/{id}", body.formatted(year, "2027-05-01", "2027-04-01", "2027-06-01"), app)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errors[0].field").value("responseLikelyDate"));
            putAs(bia, "/api/applications/{id}", body.formatted(year, "2027-04-01", "2027-05-01", "2027-06-01"), app)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.responseConfidence").value("MEDIUM"));
        }

        @Test
        void criarManualmenteDuplicadoNoMesmoCicloEhConflito() throws Exception {
            choose(kielCs);
            postAs(bia, "/api/applications", "{\"programId\":\"%s\"}".formatted(kielCs)).andExpect(status().isConflict());
        }
    }

    @Test
    void usuarioBNaoVeNemAlteraUniversidadesProgramasOuCandidaturasDeA() throws Exception {
        String app = read(patchAs(bia, "/api/programs/" + kielCs + "/chosen", "{\"chosen\":true}"), "$.application.id");
        TestSession bruno = registerUser("unib");
        getAs(bruno, "/api/universities/{id}", tum).andExpect(status().isNotFound());
        getAs(bruno, "/api/programs/{id}", kielCs).andExpect(status().isNotFound());
        patchAs(bruno, "/api/programs/" + kielCs + "/chosen", "{\"chosen\":false}").andExpect(status().isNotFound());
        patchAs(bruno, "/api/applications/" + app + "/status", "{\"status\":\"REJECTED\"}").andExpect(status().isNotFound());
        postAs(bruno, "/api/programs", "{\"universityId\":\"%s\",\"name\":\"Invasor\"}".formatted(tum))
                .andExpect(status().isNotFound());
        getAs(bruno, "/api/programs/compare?ids={a},{b}", tumInformatik, kielCs).andExpect(status().isNotFound());
        getAs(bruno, "/api/universities").andExpect(jsonPath("$", hasSize(0)));
        getAs(bruno, "/api/applications").andExpect(jsonPath("$", hasSize(0)));
    }
}
