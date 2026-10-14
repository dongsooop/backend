package com.dongsoop.dongsoop.blinddate.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorageImpl;
import com.dongsoop.dongsoop.blinddate.service.BlindDateServiceImpl;
import com.dongsoop.dongsoop.blinddate.support.ManualBlindDateTaskScheduler;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BlindDateControllerTest {
    private final BlindDateParticipantStorageImpl participants = new BlindDateParticipantStorageImpl();
    private final BlindDateSessionStorageImpl sessions = new BlindDateSessionStorageImpl(event -> {});
    private final BlindDateStorageImpl operation = new BlindDateStorageImpl();
    private final BlindDateEventQueue queue = new BlindDateEventQueue();
    private final BlindDateServiceImpl service = new BlindDateServiceImpl(
            participants, operation, mock(BlindDateNotification.class), sessions,
            mock(SimpMessagingTemplate.class), new ManualBlindDateTaskScheduler(), queue);
    private final BlindDateController controller = new BlindDateController(service);

    @AfterEach
    void tearDown() {
        queue.shutdown();
    }

    @Test
    void availabilityReturnsActualOperationState() {
        assertThat(controller.isAvailable().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.isAvailable().getBody()).isFalse();
        service.startBlindDate(new StartBlindDateRequest(LocalDateTime.now().plusHours(1), 5));
        assertThat(controller.isAvailable().getBody()).isTrue();
    }

    @Test
    void startReturnsCreatedAndAppliesRequestCapacity() {
        var response = controller.startBlindDate(new StartBlindDateRequest(LocalDateTime.now().plusHours(1), 5));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNull();
        assertThat(operation.isAvailable()).isTrue();
        assertThat(operation.getMaxSessionMemberCount()).isEqualTo(5);
    }

    @Test
    void duplicateStartReturnsConflictWithoutChangingCapacityOrSessionPointer() {
        service.startBlindDate(new StartBlindDateRequest(LocalDateTime.now().plusHours(1), 3));
        operation.setPointer("existing");
        var response = controller.startBlindDate(new StartBlindDateRequest(LocalDateTime.now().plusHours(2), 5));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(operation.getMaxSessionMemberCount()).isEqualTo(3);
        assertThat(operation.getPointer()).isEqualTo("existing");
    }

    @Test
    void bothResetRoutesReturnEmpty204AndRemoveParticipation() throws Exception {
        service.startBlindDate(new StartBlindDateRequest(LocalDateTime.now().plusHours(1), 5));
        MockMvc http = MockMvcBuilders.standaloneSetup(controller).build();
        for (boolean useDelete : new boolean[]{false, true}) {
            String id = sessions.create().getSessionId();
            operation.setPointer(id);
            participants.addParticipant(id, 1L, "one");
            http.perform(useDelete ? delete("/blinddate/participants") : post("/blinddate/participants"))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));
            queue.awaitIdle();
            assertThat(sessions.getState(id)).isNull();
            assertThat(participants.getByMemberId(1L)).isNull();
            assertThat(operation.getPointer()).isNull();
            assertThat(operation.isAvailable()).isTrue();
        }
    }
}
