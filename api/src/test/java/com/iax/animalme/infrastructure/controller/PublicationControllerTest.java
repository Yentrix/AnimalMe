package com.iax.animalme.infrastructure.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.iax.animalme.application.dto.AdoptionRequestCreateDto;
import com.iax.animalme.application.dto.PublicationCreateRequestDto;
import com.iax.animalme.application.dto.PublicationUpdateRequestDto;
import com.iax.animalme.application.service.PublicationApplicationService;
import com.iax.animalme.domain.enums.PublicationStatus;
import com.iax.animalme.domain.enums.RequestStatus;
import com.iax.animalme.domain.model.AdoptionRequest;
import com.iax.animalme.domain.model.Publication;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PublicationControllerTest {

    @Mock
    private PublicationApplicationService publicationApplicationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PublicationController controller;

    @Test
    void createAndUpdateReturnOkWhenServicesWork() throws Exception {
        String json = "{}";
        PublicationCreateRequestDto createDto = new PublicationCreateRequestDto();
        PublicationUpdateRequestDto updateDto = new PublicationUpdateRequestDto();
        Publication publication = new Publication();

        when(objectMapper.readValue(json, PublicationCreateRequestDto.class)).thenReturn(createDto);
        when(publicationApplicationService.createPublication(createDto, 1L, null)).thenReturn(publication);

        when(objectMapper.readValue(json, PublicationUpdateRequestDto.class)).thenReturn(updateDto);
        when(publicationApplicationService.updatePublication(10L, 1L, updateDto, null)).thenReturn(publication);

        assertEquals(HttpStatus.OK, controller.create(json, null, 1L).getStatusCode());
        assertEquals(HttpStatus.OK, controller.updatePublication(10L, 1L, json, null).getStatusCode());
    }

    @Test
    void createAndUpdateHandleExceptions() throws Exception {
        String json = "{}";

        when(objectMapper.readValue(json, PublicationCreateRequestDto.class)).thenThrow(new IllegalArgumentException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, controller.create(json, null, 1L).getStatusCode());

        when(objectMapper.readValue("x", PublicationCreateRequestDto.class)).thenThrow(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.create("x", null, 1L).getStatusCode());

        when(objectMapper.readValue(json, PublicationUpdateRequestDto.class)).thenThrow(new IllegalArgumentException("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, controller.updatePublication(1L, 1L, json, null).getStatusCode());

        when(objectMapper.readValue("y", PublicationUpdateRequestDto.class)).thenThrow(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.updatePublication(1L, 1L, "y", null).getStatusCode());
    }

    @Test
    void otherEndpointsDelegateToService() {
        Publication publication = new Publication();
        AdoptionRequest request = new AdoptionRequest();
        AdoptionRequestCreateDto createDto = new AdoptionRequestCreateDto();
        createDto.setMessage("hola");

        when(publicationApplicationService.findByStatusOrderByCreatedAtDesc(PublicationStatus.OPEN)).thenReturn(List.of(publication));
        when(publicationApplicationService.findByAuthorIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(publication));
        when(publicationApplicationService.createAdoptionRequest(9L, 2L, "hola")).thenReturn(request);
        when(publicationApplicationService.getPublicationRequests(9L, 1L)).thenReturn(List.of(request));
        when(publicationApplicationService.changeAdoptionRequestStatus(9L, 4L, 1L, RequestStatus.ACCEPTED)).thenReturn(request);

        assertEquals(1, controller.getActive().size());
        assertEquals(1, controller.getByAuthor(1L).size());
        assertEquals(HttpStatus.OK, controller.createAdoptionRequest(9L, 2L, createDto).getStatusCode());
        assertEquals(HttpStatus.OK, controller.getAdoptionRequests(9L, 1L).getStatusCode());
        assertEquals(HttpStatus.OK,
                controller.updateAdoptionRequestStatus(9L, 4L, 1L, RequestStatus.ACCEPTED).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteAdoptionRequest(9L, 4L, 1L).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.deletePublication(9L, 1L).getStatusCode());

        verify(publicationApplicationService).deleteAdoptionRequest(9L, 4L, 1L);
        verify(publicationApplicationService).deletePublication(9L, 1L);
    }

    @Test
    void illegalArgumentHandlerReturnsBadRequest() {
        ResponseEntity<Void> response = controller.handleIllegalArgument();
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
