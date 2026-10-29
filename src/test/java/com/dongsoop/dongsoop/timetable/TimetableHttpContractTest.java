package com.dongsoop.dongsoop.timetable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dongsoop.dongsoop.timetable.controller.TimetableController;
import com.dongsoop.dongsoop.timetable.dto.CreateTimetableRequest;
import com.dongsoop.dongsoop.timetable.dto.UpdateTimetableRequest;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.service.TimetableService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TimetableHttpContractTest {
  @Mock TimetableService service;
  @InjectMocks TimetableController controller;
  private MockMvc mvc;
  private final ObjectMapper json = JsonMapper.builder().findAndAddModules().build();

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void semesterPathBindsYearAndEnumForReadAndDelete() throws Exception {
    when(service.getTimetableView(Year.of(2026), SemesterType.SECOND)).thenReturn(List.of());
    mvc.perform(get("/timetable/2026/SECOND"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
    mvc.perform(delete("/timetable/2026/SECOND")).andExpect(status().isNoContent());
    verify(service).deleteTimetable(Year.of(2026), SemesterType.SECOND);
  }

  @Test
  void creationReturnsSemesterLocationAndBoundPayload() throws Exception {
    var request = request("Algorithms");
    mvc.perform(
            post("/timetable")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/timetable/2026/SECOND"));
    verify(service).createTimetable(request);
  }

  @ParameterizedTest
  @CsvSource({"0,201", "1,207", "2,409"})
  void batchStatusReflectsFailedItemsOnly(int failures, int statusCode) throws Exception {
    var requests = List.of(request("Algorithms"), request("Databases"));
    when(service.createTimetable(requests)).thenReturn(requests.subList(0, failures));
    var result =
        mvc.perform(
                post("/timetable/multiple")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(requests)))
            .andExpect(status().is(statusCode));
    if (failures == 1) {
      result
          .andExpect(jsonPath("$[0].name").value("Algorithms"))
          .andExpect(jsonPath("$.length()").value(1));
      return;
    }
    result.andExpect(content().string(""));
  }

  @Test
  void updateAndDeleteBindIdentifiersAndPartialFields() throws Exception {
    mvc.perform(
            patch("/timetable")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":19,\"name\":\"Changed\",\"startAt\":\"11:00:00\"}"))
        .andExpect(status().isNoContent());
    var update = ArgumentCaptor.forClass(UpdateTimetableRequest.class);
    verify(service).updateTimetable(update.capture());
    assertThat(update.getValue())
        .isEqualTo(
            new UpdateTimetableRequest(
                19L, "Changed", null, null, null, LocalTime.of(11, 0), null));
    mvc.perform(delete("/timetable/19")).andExpect(status().isNoContent());
    verify(service).deleteTimetable(19L);
  }

  @Test
  void invalidInputCannotReachService() throws Exception {
    mvc.perform(get("/timetable/2026/UNKNOWN")).andExpect(status().isBadRequest());
    mvc.perform(post("/timetable").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            patch("/timetable")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Changed\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  private CreateTimetableRequest request(String name) {
    return new CreateTimetableRequest(
        name,
        "Prof",
        "Room101",
        DayOfWeek.MONDAY,
        LocalTime.of(9, 0),
        LocalTime.of(10, 0),
        Year.of(2026),
        SemesterType.SECOND);
  }
}
