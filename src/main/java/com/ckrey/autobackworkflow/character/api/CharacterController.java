package com.ckrey.autobackworkflow.character.api;

import com.ckrey.autobackworkflow.character.application.CharacterApplicationService;
import com.ckrey.autobackworkflow.common.api.ApiResponse;
import com.ckrey.autobackworkflow.domain.AdsCharacter;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CharacterController {
    private final CharacterApplicationService characters;

    public CharacterController(CharacterApplicationService characters) {
        this.characters = characters;
    }

    @GetMapping("/projects/{projectId}/characters")
    public ApiResponse<List<AdsCharacter>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(characters.list(projectId));
    }

    @PostMapping("/projects/{projectId}/characters")
    public ApiResponse<AdsCharacter> create(@PathVariable Long projectId, @Valid @RequestBody CharacterDtos.CreateCharacterRequest request) {
        return ApiResponse.ok(characters.create(projectId, request));
    }

    @PutMapping("/characters/{id}")
    public ApiResponse<AdsCharacter> update(@PathVariable Long id, @Valid @RequestBody CharacterDtos.UpdateCharacterRequest request) {
        return ApiResponse.ok(characters.update(id, request));
    }

    @DeleteMapping("/characters/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam Integer version) {
        characters.delete(id, version);
        return ApiResponse.ok(null);
    }
}
